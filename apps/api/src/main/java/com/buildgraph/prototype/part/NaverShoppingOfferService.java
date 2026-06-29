package com.buildgraph.prototype.part;

import com.buildgraph.prototype.common.MockData;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NaverShoppingOfferService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Set<String> CATEGORIES = Set.of("CPU", "GPU", "RAM", "MOTHERBOARD", "STORAGE", "PSU", "CASE", "COOLER");
    private static final String SOURCE = "NAVER_SHOPPING_SEARCH";
    private static final Pattern PSU_WATT_PATTERN = Pattern.compile("(\\d{3,4})\\s*[Ww]");

    private final String clientId;
    private final String clientSecret;
    private final RestClient restClient;
    private final JdbcTemplate jdbcTemplate;

    public NaverShoppingOfferService(
            @Value("${naver.search.client-id:}") String clientId,
            @Value("${naver.search.client-secret:}") String clientSecret,
            @Value("${naver.search.base-url:https://openapi.naver.com}") String baseUrl,
            JdbcTemplate jdbcTemplate
    ) {
        this.clientId = clientId == null ? "" : clientId.trim();
        this.clientSecret = clientSecret == null ? "" : clientSecret.trim();
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> refreshCatalog(String category, Integer limitPerQuery, Boolean publish, String query) {
        String normalizedCategory = requireCategory(category);
        int safeLimitPerQuery = limitPerQuery == null ? 3 : Math.min(Math.max(limitPerQuery, 1), 5);
        boolean publishToParts = Boolean.TRUE.equals(publish);
        List<String> queries = StringUtils.hasText(query) ? List.of(query.trim()) : queryPack(normalizedCategory);

        if (!configured()) {
            return MockData.map(
                    "configured", false,
                    "category", normalizedCategory,
                    "queryCount", queries.size(),
                    "limitPerQuery", safeLimitPerQuery,
                    "attempted", 0,
                    "discovered", 0,
                    "published", 0,
                    "failed", 0,
                    "message", "NAVER_SEARCH_CLIENT_ID 또는 NAVER_SEARCH_CLIENT_SECRET이 설정되지 않았습니다."
            );
        }

        Map<String, Object> job = createCatalogRefreshJob(normalizedCategory, queries);
        long jobId = ((Number) job.get("id")).longValue();
        int attempted = 0;
        int discovered = 0;
        int published = 0;
        int failed = 0;
        String errorSummary = null;

        try {
            for (String searchQuery : queries) {
                List<Map<String, Object>> offers = fetchOffers(searchQuery, safeLimitPerQuery);
                for (Map<String, Object> offer : offers) {
                    attempted += 1;
                    try {
                        CatalogCandidate candidate = upsertCandidate(jobId, normalizedCategory, searchQuery, offer);
                        discovered += 1;
                        if (publishToParts && publishCandidate(candidate, normalizedCategory, searchQuery, offer)) {
                            published += 1;
                        }
                    } catch (RuntimeException ignored) {
                        failed += 1;
                    }
                }
            }
            finishCatalogRefreshJob(jobId, "SUCCEEDED", attempted, discovered, published, null);
        } catch (RuntimeException exception) {
            errorSummary = exception.getMessage();
            finishCatalogRefreshJob(jobId, "FAILED", attempted, discovered, published, errorSummary);
            throw exception;
        }

        return MockData.map(
                "configured", true,
                "jobId", job.get("public_id"),
                "category", normalizedCategory,
                "queryCount", queries.size(),
                "limitPerQuery", safeLimitPerQuery,
                "publish", publishToParts,
                "attempted", attempted,
                "discovered", discovered,
                "published", published,
                "failed", failed,
                "errorSummary", errorSummary
        );
    }

    public Map<String, Object> refreshOffers(String category, Integer limit, Boolean force) {
        String normalizedCategory = normalizeCategory(category);
        int safeLimit = limit == null ? 20 : Math.min(Math.max(limit, 1), 100);
        boolean forceRefresh = Boolean.TRUE.equals(force);

        if (!configured()) {
            return MockData.map(
                    "configured", false,
                    "category", normalizedCategory,
                    "limit", safeLimit,
                    "attempted", 0,
                    "updated", 0,
                    "skipped", 0,
                    "failed", 0,
                    "message", "NAVER_SEARCH_CLIENT_ID 또는 NAVER_SEARCH_CLIENT_SECRET이 설정되지 않았습니다."
            );
        }

        List<Object> params = new ArrayList<>();
        StringBuilder sql = new StringBuilder("""
                SELECT p.id, p.public_id::text AS public_id, p.category, p.name, p.manufacturer
                FROM parts p
                LEFT JOIN part_external_offers peo
                  ON peo.part_id = p.id
                 AND peo.source = 'NAVER_SHOPPING_SEARCH'
                 AND peo.deleted_at IS NULL
                WHERE p.deleted_at IS NULL
                  AND p.status = 'ACTIVE'
                """);
        if (normalizedCategory != null) {
            sql.append(" AND p.category = ?");
            params.add(normalizedCategory);
        }
        if (!forceRefresh) {
            sql.append(" AND (peo.id IS NULL OR peo.refreshed_at < now() - interval '7 days')");
        }
        sql.append(" ORDER BY p.category, p.id LIMIT ?");
        params.add(safeLimit);

        List<Map<String, Object>> parts = jdbcTemplate.queryForList(sql.toString(), params.toArray());
        int attempted = 0;
        int updated = 0;
        int skipped = 0;
        int failed = 0;

        for (Map<String, Object> part : parts) {
            attempted += 1;
            String name = stringValue(part.get("name"));
            String manufacturer = stringValue(part.get("manufacturer"));
            String query = searchQuery(name, manufacturer);
            Optional<Map<String, Object>> offer = fetchOffer(query);
            if (offer.isEmpty()) {
                skipped += 1;
                continue;
            }
            try {
                upsertOffer(((Number) part.get("id")).longValue(), query, offer.get());
                updated += 1;
            } catch (RuntimeException ignored) {
                failed += 1;
            }
        }

        return MockData.map(
                "configured", true,
                "category", normalizedCategory,
                "limit", safeLimit,
                "force", forceRefresh,
                "attempted", attempted,
                "updated", updated,
                "skipped", skipped,
                "failed", failed
        );
    }

    private Optional<Map<String, Object>> fetchOffer(String query) {
        List<Map<String, Object>> offers = fetchOffers(query, 1);
        return offers.isEmpty() ? Optional.empty() : Optional.of(offers.get(0));
    }

    private List<Map<String, Object>> fetchOffers(String query, int display) {
        try {
            Map<?, ?> response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/search/shop.json")
                            .queryParam("query", query)
                            .queryParam("display", display)
                            .queryParam("start", 1)
                            .queryParam("sort", "sim")
                            .queryParam("exclude", "used:rental:cbshop")
                            .build())
                    .header("X-Naver-Client-Id", clientId)
                    .header("X-Naver-Client-Secret", clientSecret)
                    .retrieve()
                    .body(Map.class);

            if (response == null || !(response.get("items") instanceof List<?> items) || items.isEmpty()) {
                return List.of();
            }

            return items.stream()
                    .filter(Map.class::isInstance)
                    .map(Map.class::cast)
                    .map(item -> MockData.map(
                            "title", cleanText(stringValue(item.get("title"))),
                            "imageUrl", stringValue(item.get("image")),
                            "supplierName", stringValue(item.get("mallName")),
                            "offerUrl", stringValue(item.get("link")),
                            "lowPrice", integerValue(item.get("lprice")),
                            "source", SOURCE,
                            "manufacturerGuess", manufacturerGuess(item),
                            "sourceProductKey", sourceProductKey(item),
                            "rawPayload", item
                    ))
                    .filter(offer -> StringUtils.hasText(stringValue(offer.get("title"))))
                    .toList();
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    private Map<String, Object> createCatalogRefreshJob(String category, List<String> queries) {
        return jdbcTemplate.queryForMap("""
                INSERT INTO part_catalog_refresh_jobs (
                  source,
                  category,
                  search_query,
                  status
                )
                VALUES (?, ?, ?, 'RUNNING')
                RETURNING id, public_id::text
                """,
                SOURCE,
                category,
                joinQueries(queries)
        );
    }

    private void finishCatalogRefreshJob(long jobId, String status, int attempted, int discovered, int published, String errorSummary) {
        jdbcTemplate.update("""
                UPDATE part_catalog_refresh_jobs
                SET status = ?,
                    attempted_count = ?,
                    discovered_count = ?,
                    published_count = ?,
                    error_summary = ?,
                    finished_at = now()
                WHERE id = ?
                """,
                status,
                attempted,
                discovered,
                published,
                errorSummary,
                jobId
        );
    }

    private CatalogCandidate upsertCandidate(long jobId, String category, String query, Map<String, Object> offer) {
        String title = limited(stringValue(offer.get("title")), 500);
        String sourceProductKey = limited(stringValue(offer.get("sourceProductKey")), 500);
        Map<String, Object> row = jdbcTemplate.queryForMap("""
                INSERT INTO part_catalog_candidates (
                  refresh_job_id,
                  source,
                  category,
                  source_product_key,
                  search_query,
                  title,
                  manufacturer_guess,
                  image_url,
                  supplier_name,
                  offer_url,
                  low_price,
                  raw_payload,
                  discovered_at,
                  last_seen_at,
                  created_at,
                  updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, now(), now(), now(), now())
                ON CONFLICT (source, category, source_product_key) WHERE deleted_at IS NULL
                DO UPDATE SET
                  refresh_job_id = EXCLUDED.refresh_job_id,
                  search_query = EXCLUDED.search_query,
                  title = EXCLUDED.title,
                  manufacturer_guess = EXCLUDED.manufacturer_guess,
                  image_url = EXCLUDED.image_url,
                  supplier_name = EXCLUDED.supplier_name,
                  offer_url = EXCLUDED.offer_url,
                  low_price = EXCLUDED.low_price,
                  raw_payload = EXCLUDED.raw_payload,
                  last_seen_at = now(),
                  updated_at = now()
                RETURNING id, public_id::text AS public_id, published_part_id
                """,
                jobId,
                SOURCE,
                category,
                sourceProductKey,
                query,
                title,
                limited(stringValue(offer.get("manufacturerGuess")), 100),
                offer.get("imageUrl"),
                limited(stringValue(offer.get("supplierName")), 255),
                offer.get("offerUrl"),
                offer.get("lowPrice"),
                json(offer.get("rawPayload"))
        );
        Long publishedPartId = row.get("published_part_id") instanceof Number number ? number.longValue() : null;
        return new CatalogCandidate(((Number) row.get("id")).longValue(), stringValue(row.get("public_id")), publishedPartId);
    }

    private boolean publishCandidate(CatalogCandidate candidate, String category, String query, Map<String, Object> offer) {
        if (candidate.publishedPartId() != null) {
            upsertOffer(candidate.publishedPartId(), query, offer);
            return false;
        }
        Integer lowPrice = integerValue(offer.get("lowPrice"));
        if (lowPrice == null) {
            return false;
        }
        String title = stringValue(offer.get("title"));
        Long existingPartId = findExistingPartId(category, title);
        long partId = existingPartId == null
                ? insertPart(category, title, stringValue(offer.get("manufacturerGuess")), lowPrice, query, offer)
                : existingPartId;
        jdbcTemplate.update("""
                UPDATE part_catalog_candidates
                SET candidate_status = 'PUBLISHED',
                    published_part_id = ?,
                    updated_at = now()
                WHERE id = ?
                """, partId, candidate.id());
        upsertOffer(partId, query, offer);
        return existingPartId == null;
    }

    private Long findExistingPartId(String category, String title) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id
                FROM parts
                WHERE category = ?
                  AND lower(name) = lower(?)
                  AND deleted_at IS NULL
                LIMIT 1
                """, category, title);
        return rows.isEmpty() ? null : ((Number) rows.get(0).get("id")).longValue();
    }

    private long insertPart(String category, String title, String manufacturer, int price, String query, Map<String, Object> offer) {
        Map<String, Object> attributes = attributesFor(category, title, query, offer);
        Map<String, Object> row = jdbcTemplate.queryForMap("""
                INSERT INTO parts (
                  category,
                  name,
                  manufacturer,
                  price,
                  status,
                  attributes,
                  created_at,
                  updated_at
                )
                VALUES (?, ?, ?, ?, 'ACTIVE', ?::jsonb, now(), now())
                RETURNING id
                """,
                category,
                limited(title, 255),
                blankToNull(limited(manufacturer, 100)),
                price,
                json(attributes)
        );
        return ((Number) row.get("id")).longValue();
    }

    private void upsertOffer(long partId, String query, Map<String, Object> offer) {
        jdbcTemplate.update("""
                INSERT INTO part_external_offers (
                  part_id,
                  source,
                  search_query,
                  title,
                  image_url,
                  supplier_name,
                  offer_url,
                  low_price,
                  raw_payload,
                  refreshed_at,
                  created_at,
                  updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, now(), now(), now())
                ON CONFLICT (part_id, source) WHERE deleted_at IS NULL
                DO UPDATE SET
                  search_query = EXCLUDED.search_query,
                  title = EXCLUDED.title,
                  image_url = EXCLUDED.image_url,
                  supplier_name = EXCLUDED.supplier_name,
                  offer_url = EXCLUDED.offer_url,
                  low_price = EXCLUDED.low_price,
                  raw_payload = EXCLUDED.raw_payload,
                  refreshed_at = now(),
                  updated_at = now()
                """,
                partId,
                SOURCE,
                query,
                limited(stringValue(offer.get("title")), 500),
                offer.get("imageUrl"),
                limited(stringValue(offer.get("supplierName")), 255),
                offer.get("offerUrl"),
                offer.get("lowPrice"),
                json(offer.get("rawPayload"))
        );
    }

    private boolean configured() {
        return StringUtils.hasText(clientId) && StringUtils.hasText(clientSecret);
    }

    private static String searchQuery(String name, String manufacturer) {
        if (!StringUtils.hasText(manufacturer) || manufacturer.endsWith("Partner")) {
            return name;
        }
        return manufacturer + " " + name;
    }

    private static String normalizeCategory(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String upper = value.trim().toUpperCase();
        if (!CATEGORIES.contains(upper)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 부품 category입니다.");
        }
        return upper;
    }

    private static String requireCategory(String value) {
        String normalized = normalizeCategory(value);
        if (normalized == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "category는 필수입니다.");
        }
        return normalized;
    }

    private static List<String> queryPack(String category) {
        return switch (category) {
            case "GPU" -> List.of(
                    "ASUS RTX 5090", "MSI RTX 5090", "GIGABYTE RTX 5090", "ZOTAC RTX 5090", "PNY RTX 5090",
                    "ASUS RTX 5080", "MSI RTX 5080", "GIGABYTE RTX 5080", "ZOTAC RTX 5080",
                    "ASUS RTX 5070 Ti", "MSI RTX 5070 Ti", "GIGABYTE RTX 5070 Ti", "ZOTAC RTX 5070 Ti",
                    "ASUS RTX 5070", "MSI RTX 5070", "GIGABYTE RTX 5070", "ZOTAC RTX 5070",
                    "ASUS RTX 5060 Ti", "MSI RTX 5060 Ti", "GIGABYTE RTX 5060 Ti",
                    "ASUS RTX 5060", "MSI RTX 5060", "GIGABYTE RTX 5060", "ZOTAC RTX 5060"
            );
            case "MOTHERBOARD" -> List.of(
                    "ASUS Z890 DDR5", "MSI Z890 DDR5", "GIGABYTE Z890 DDR5", "ASRock Z890 DDR5",
                    "ASUS B860 DDR5", "MSI B860 DDR5", "GIGABYTE B860 DDR5", "ASRock B860 DDR5",
                    "ASUS X870E DDR5", "MSI X870E DDR5", "GIGABYTE X870E DDR5", "ASRock X870E DDR5",
                    "ASUS X870 DDR5", "MSI X870 DDR5", "GIGABYTE X870 DDR5",
                    "ASUS B850 DDR5", "MSI B850 DDR5", "GIGABYTE B850 DDR5", "ASRock B850 DDR5"
            );
            case "PSU" -> List.of(
                    "Corsair ATX 3.1 850W", "Corsair ATX 3.1 1000W", "Corsair ATX 3.1 1200W",
                    "Seasonic ATX 3.1 850W", "Seasonic ATX 3.1 1000W", "Seasonic ATX 3.1 1200W",
                    "FSP ATX 3.1 850W", "FSP ATX 3.1 1000W", "FSP ATX 3.1 1200W",
                    "Super Flower ATX 3.1 850W", "Super Flower ATX 3.1 1000W", "Super Flower ATX 3.1 1200W",
                    "MSI ATX 3.1 850W", "MSI ATX 3.1 1000W",
                    "Cooler Master ATX 3.1 850W", "Cooler Master ATX 3.1 1000W"
            );
            case "CPU" -> List.of(
                    "AMD Ryzen 9 9950X3D", "AMD Ryzen 9 9900X3D", "AMD Ryzen 7 9800X3D",
                    "AMD Ryzen 9 9950X", "AMD Ryzen 7 9700X", "AMD Ryzen 5 9600X",
                    "Intel Core Ultra 9 285K", "Intel Core Ultra 7 265K", "Intel Core Ultra 5 245K"
            );
            case "RAM" -> List.of(
                    "Samsung DDR5 32GB 6400", "Crucial DDR5 32GB 6000", "G.SKILL DDR5 32GB 6000",
                    "Corsair DDR5 32GB 6400", "Kingston DDR5 32GB 6000", "TeamGroup DDR5 64GB 6400"
            );
            case "STORAGE" -> List.of(
                    "Samsung 9100 PRO 2TB", "WD BLACK SN8100 2TB", "Crucial T705 2TB",
                    "SK hynix Platinum P51 2TB", "Kingston FURY Renegade G5 2TB", "Seagate FireCuda 540 2TB"
            );
            case "CASE" -> List.of(
                    "Fractal Design Meshify 3", "Lian Li LANCOOL 217", "NZXT H9 Flow 2025",
                    "Corsair FRAME 4000D", "Phanteks Evolv X2", "be quiet Light Base 900 DX"
            );
            case "COOLER" -> List.of(
                    "ARCTIC Liquid Freezer III 360", "DeepCool ASSASSIN IV", "Noctua NH-D15 G2",
                    "Corsair iCUE LINK TITAN 360", "NZXT Kraken Elite 360"
            );
            default -> List.of(category);
        };
    }

    private static Map<String, Object> attributesFor(String category, String title, String query, Map<String, Object> offer) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("shortSpec", title);
        attributes.put("catalogGeneration", "EXTERNAL_REFRESH");
        attributes.put("currentLineupOnly", true);
        attributes.put("toolReady", false);
        attributes.put("metadataVersion", 4);
        attributes.put("externalSources", MockData.map(
                "naver", MockData.map(
                        "keyword", query,
                        "sourceProductKey", offer.get("sourceProductKey"),
                        "catalogRefresh", true
                )
        ));

        String upperTitle = title.toUpperCase(Locale.ROOT);
        switch (category) {
            case "GPU" -> applyGpuAttributes(attributes, upperTitle);
            case "MOTHERBOARD" -> applyMotherboardAttributes(attributes, upperTitle);
            case "PSU" -> applyPsuAttributes(attributes, upperTitle);
            case "CPU" -> applyCpuAttributes(attributes, upperTitle);
            case "RAM" -> {
                attributes.put("memoryType", "DDR5");
                attributes.put("capacityGb", upperTitle.contains("64GB") ? 64 : 32);
            }
            case "STORAGE" -> {
                attributes.put("interface", "M.2 NVMe");
                attributes.put("capacityGb", upperTitle.contains("4TB") ? 4000 : 2000);
            }
            default -> {
            }
        }
        return attributes;
    }

    private static void applyGpuAttributes(Map<String, Object> attributes, String upperTitle) {
        attributes.put("architecture", "Blackwell");
        attributes.put("series", "GeForce RTX 50");
        if (upperTitle.contains("5090")) {
            attributes.put("wattage", 575);
            attributes.put("requiredSystemPowerW", 1000);
            attributes.put("vramGb", 32);
        } else if (upperTitle.contains("5080")) {
            attributes.put("wattage", 360);
            attributes.put("requiredSystemPowerW", 850);
            attributes.put("vramGb", 16);
        } else if (upperTitle.contains("5070 TI")) {
            attributes.put("wattage", 300);
            attributes.put("requiredSystemPowerW", 750);
            attributes.put("vramGb", 16);
        } else if (upperTitle.contains("5070")) {
            attributes.put("wattage", 250);
            attributes.put("requiredSystemPowerW", 650);
            attributes.put("vramGb", 12);
        } else if (upperTitle.contains("5060 TI")) {
            attributes.put("wattage", 180);
            attributes.put("requiredSystemPowerW", 600);
            attributes.put("vramGb", upperTitle.contains("16GB") ? 16 : 8);
        } else if (upperTitle.contains("5060")) {
            attributes.put("wattage", 145);
            attributes.put("requiredSystemPowerW", 550);
            attributes.put("vramGb", 8);
        }
        attributes.put("memoryType", "GDDR7");
    }

    private static void applyMotherboardAttributes(Map<String, Object> attributes, String upperTitle) {
        if (upperTitle.contains("Z890") || upperTitle.contains("B860")) {
            attributes.put("socket", "LGA1851");
        } else if (upperTitle.contains("X870") || upperTitle.contains("B850")) {
            attributes.put("socket", "AM5");
        }
        if (upperTitle.contains("Z890")) {
            attributes.put("chipset", "Z890");
        } else if (upperTitle.contains("B860")) {
            attributes.put("chipset", "B860");
        } else if (upperTitle.contains("X870E")) {
            attributes.put("chipset", "X870E");
        } else if (upperTitle.contains("X870")) {
            attributes.put("chipset", "X870");
        } else if (upperTitle.contains("B850")) {
            attributes.put("chipset", "B850");
        }
        attributes.put("memoryType", "DDR5");
        attributes.put("formFactor", "ATX");
    }

    private static void applyPsuAttributes(Map<String, Object> attributes, String upperTitle) {
        Matcher matcher = PSU_WATT_PATTERN.matcher(upperTitle);
        if (matcher.find()) {
            attributes.put("capacityW", Integer.parseInt(matcher.group(1)));
        }
        if (upperTitle.contains("ATX 3.1")) {
            attributes.put("atxSpec", "ATX 3.1");
        }
        attributes.put("gpuConnector", "12V-2x6");
        attributes.put("modular", true);
    }

    private static void applyCpuAttributes(Map<String, Object> attributes, String upperTitle) {
        if (upperTitle.contains("RYZEN")) {
            attributes.put("socket", "AM5");
            attributes.put("architecture", "Zen 5");
        } else if (upperTitle.contains("CORE ULTRA")) {
            attributes.put("socket", "LGA1851");
            attributes.put("architecture", "Arrow Lake");
        }
    }

    private static String manufacturerGuess(Map<?, ?> item) {
        String maker = stringValue(item.get("maker"));
        if (StringUtils.hasText(maker)) {
            return cleanText(maker);
        }
        String brand = stringValue(item.get("brand"));
        if (StringUtils.hasText(brand)) {
            return cleanText(brand);
        }
        return null;
    }

    private static String sourceProductKey(Map<?, ?> item) {
        String productId = stringValue(item.get("productId"));
        if (StringUtils.hasText(productId)) {
            return limited(productId, 500);
        }
        String link = stringValue(item.get("link"));
        if (StringUtils.hasText(link)) {
            return limited(link, 500);
        }
        return limited(cleanText(stringValue(item.get("title"))) + "::" + stringValue(item.get("mallName")), 500);
    }

    private static String joinQueries(List<String> queries) {
        String joined = String.join(" | ", queries);
        return joined.length() <= 255 ? joined : joined.substring(0, 255);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String limited(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private static String json(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value == null ? Map.of() : value);
        } catch (Exception ignored) {
            return "{}";
        }
    }

    private static String cleanText(String value) {
        if (value == null) {
            return null;
        }
        return value
                .replaceAll("<[^>]+>", "")
                .replace("&quot;", "\"")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .trim();
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Integer integerValue(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private record CatalogCandidate(long id, String publicId, Long publishedPartId) {
    }
}
