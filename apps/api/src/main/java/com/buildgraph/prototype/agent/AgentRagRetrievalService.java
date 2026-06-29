package com.buildgraph.prototype.agent;

import com.buildgraph.prototype.common.DbValueMapper;
import com.buildgraph.prototype.common.MockData;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AgentRagRetrievalService {
    private static final Pattern TOKEN_SPLIT = Pattern.compile("[^0-9A-Za-z가-힣]+");
    private final JdbcTemplate jdbcTemplate;

    public AgentRagRetrievalService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public AgentRagEvidenceDraft retrieveEvidence(AgentSessionRoot root, AgentRunProfile profile) {
        RootContext context = rootContext(root);
        List<String> queryTokens = tokens(context.queryText());
        return reusableEvidenceRows().stream()
                .map(row -> candidate(row, root, profile, context, queryTokens))
                .filter(candidate -> candidate.allowed())
                .max(Comparator.comparingDouble(RetrievalCandidate::rank))
                .map(RetrievalCandidate::draft)
                .orElseGet(() -> AgentRunTraceDrafts.ragEvidence(root, profile));
    }

    private List<Map<String, Object>> reusableEvidenceRows() {
        return jdbcTemplate.queryForList("""
                SELECT public_id::text AS id,
                       source_id,
                       chunk_text,
                       summary,
                       score,
                       metadata
                FROM rag_evidence
                WHERE agent_session_id IS NULL
                ORDER BY score DESC NULLS LAST, id
                """);
    }

    private RetrievalCandidate candidate(
            Map<String, Object> row,
            AgentSessionRoot root,
            AgentRunProfile profile,
            RootContext context,
            List<String> queryTokens
    ) {
        Map<String, Object> sourceMetadata = metadata(row);
        String sourceType = stringValue(sourceMetadata.get("sourceType"));
        String purpose = stringValue(sourceMetadata.get("purpose"));
        boolean sourceTypeAllowed = sourceType != null && profile.ragSourceTypes().contains(sourceType);
        boolean purposeMatched = purpose == null || purpose.equals(profile.purpose().name());
        boolean allowed = sourceTypeAllowed && purposeMatched;

        String searchableText = String.join(" ",
                safe(DbValueMapper.string(row, "source_id")),
                safe(DbValueMapper.string(row, "summary")),
                safe(DbValueMapper.string(row, "chunk_text")),
                sourceMetadata.toString()
        ).toLowerCase(Locale.ROOT);
        int matchedTokens = 0;
        for (String token : queryTokens) {
            if (searchableText.contains(token.toLowerCase(Locale.ROOT))) {
                matchedTokens++;
            }
        }
        double tokenScore = queryTokens.isEmpty() ? 0.0 : matchedTokens / (double) queryTokens.size();
        double baseScore = score(row);
        double rank = baseScore
                + (purpose != null && purpose.equals(profile.purpose().name()) ? 0.18 : 0.0)
                + (sourceTypeAllowed ? 0.12 : 0.0)
                + (tokenScore * 0.20);

        Map<String, Object> metadata = new LinkedHashMap<>(sourceMetadata);
        metadata.put("sourceEvidenceId", DbValueMapper.string(row, "id"));
        metadata.put("sourceTypes", profile.ragSourceTypes());
        metadata.put("purpose", profile.purpose().name());
        metadata.put("rootType", root.type().name());
        metadata.put("rootId", root.publicId());
        metadata.put("retrievalQuery", context.queryText());
        metadata.put("matchedTokenCount", matchedTokens);
        metadata.put("queryTokenCount", queryTokens.size());
        metadata.put("retrievalRank", rounded(rank));
        metadata.put("retrievedAt", MockData.now());

        AgentRagEvidenceDraft draft = new AgentRagEvidenceDraft(
                DbValueMapper.string(row, "source_id"),
                DbValueMapper.string(row, "chunk_text"),
                DbValueMapper.string(row, "summary"),
                BigDecimal.valueOf(Math.min(0.99000, Math.max(0.00000, rank)))
                        .setScale(5, RoundingMode.HALF_UP),
                metadata
        );
        return new RetrievalCandidate(allowed, rank, draft);
    }

    private RootContext rootContext(AgentSessionRoot root) {
        return switch (root.type()) {
            case REQUIREMENT -> requirementContext(root.publicId());
            case BUILD -> buildContext(root.publicId());
            case AS_TICKET -> asTicketContext(root.publicId());
        };
    }

    private RootContext requirementContext(String publicId) {
        return jdbcTemplate.queryForList("""
                        SELECT raw_message,
                               coalesce(array_to_string(usage_tags, ' '), '') AS usage_tags,
                               coalesce(parsed_context::text, '') AS parsed_context
                        FROM requirements
                        WHERE public_id = ?::uuid
                        """, publicId)
                .stream()
                .findFirst()
                .map(row -> new RootContext(String.join(" ",
                        safe(DbValueMapper.string(row, "raw_message")),
                        safe(DbValueMapper.string(row, "usage_tags")),
                        safe(DbValueMapper.string(row, "parsed_context"))
                )))
                .orElseGet(() -> new RootContext(publicId));
    }

    private RootContext buildContext(String publicId) {
        return jdbcTemplate.queryForList("""
                        SELECT b.name,
                               b.total_price::text AS total_price,
                               coalesce(b.warnings::text, '') AS warnings,
                               r.raw_message,
                               coalesce(string_agg(
                                 concat_ws(' ', p.category, p.name, p.manufacturer, p.attributes->>'shortSpec'),
                                 ' '
                                 ORDER BY bi.category
                               ), '') AS parts_text
                        FROM builds b
                        JOIN requirements r ON r.id = b.requirement_id
                        LEFT JOIN build_items bi ON bi.build_id = b.id
                        LEFT JOIN parts p ON p.id = bi.part_id
                        WHERE b.public_id = ?::uuid
                        GROUP BY b.id, r.id
                        """, publicId)
                .stream()
                .findFirst()
                .map(row -> new RootContext(String.join(" ",
                        safe(DbValueMapper.string(row, "name")),
                        safe(DbValueMapper.string(row, "total_price")),
                        safe(DbValueMapper.string(row, "warnings")),
                        safe(DbValueMapper.string(row, "raw_message")),
                        safe(DbValueMapper.string(row, "parts_text"))
                )))
                .orElseGet(() -> new RootContext(publicId));
    }

    private RootContext asTicketContext(String publicId) {
        return jdbcTemplate.queryForList("""
                        SELECT t.symptom,
                               coalesce(t.cause_candidates::text, '') AS cause_candidates,
                               coalesce(t.upgrade_candidates::text, '') AS upgrade_candidates,
                               coalesce(l.summary, '') AS log_summary
                        FROM as_tickets t
                        LEFT JOIN agent_log_uploads l ON l.id = t.log_upload_id
                        WHERE t.public_id = ?::uuid
                          AND t.deleted_at IS NULL
                        """, publicId)
                .stream()
                .findFirst()
                .map(row -> new RootContext(String.join(" ",
                        safe(DbValueMapper.string(row, "symptom")),
                        safe(DbValueMapper.string(row, "cause_candidates")),
                        safe(DbValueMapper.string(row, "upgrade_candidates")),
                        safe(DbValueMapper.string(row, "log_summary"))
                )))
                .orElseGet(() -> new RootContext(publicId));
    }

    private static List<String> tokens(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        Set<String> result = new LinkedHashSet<>();
        for (String token : TOKEN_SPLIT.split(value)) {
            if (token.length() >= 2) {
                result.add(token.toLowerCase(Locale.ROOT));
            }
        }
        return new ArrayList<>(result);
    }

    private static Map<String, Object> metadata(Map<String, Object> row) {
        Object parsed = DbValueMapper.json(row, "metadata", Map.of());
        if (parsed instanceof Map<?, ?> source) {
            Map<String, Object> result = new LinkedHashMap<>();
            source.forEach((key, value) -> result.put(String.valueOf(key), value));
            return result;
        }
        return new LinkedHashMap<>();
    }

    private static double score(Map<String, Object> row) {
        Object score = row.get("score");
        if (score instanceof Number number) {
            return number.doubleValue();
        }
        if (score == null) {
            return 0.5;
        }
        return Double.parseDouble(score.toString());
    }

    private static String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static double rounded(double value) {
        return BigDecimal.valueOf(value).setScale(5, RoundingMode.HALF_UP).doubleValue();
    }

    private record RootContext(String queryText) {
    }

    private record RetrievalCandidate(boolean allowed, double rank, AgentRagEvidenceDraft draft) {
    }
}
