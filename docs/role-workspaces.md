# Role Workspaces

| Owner | Workspace | Primary Responsibility |
| --- | --- | --- |
| 1 | `apps/web/src/features/quote`, `apps/web/src/features/auth` | Consumer quote flow, recommendation UI, auth screens |
| 2 | `apps/api/src/main/java/com/buildgraph/prototype/part`, `apps/api/src/main/java/com/buildgraph/prototype/price`, `apps/web/src/features/parts` | Parts DB, validation tools, rule-based performance, target-price alerts |
| 3 | `apps/api/src/main/java/com/buildgraph/prototype/agent`, `apps/api/src/main/java/com/buildgraph/prototype/rag` | LLM/RAG/Agent orchestration skeleton and fallback policies |
| 4 | `apps/pc-agent`, `apps/api/src/main/java/com/buildgraph/prototype/log`, `apps/api/src/main/java/com/buildgraph/prototype/ticket`, `apps/web/src/features/support` | PC Agent, log upload, AS tickets |
| 5 | `infra`, `apps/api/src/main/java/com/buildgraph/prototype/user`, `apps/api/src/main/java/com/buildgraph/prototype/admin`, `apps/web/src/features/admin` | Auth, admin, Docker, queue/cache, load-test environment |

## First PR Checklist

| Owner | First PR target | Done when |
| --- | --- | --- |
| 1 | Replace quote/auth mock screen data with API calls and local form state | Requirement parse, build recommend, login/signup flows can be tested from the UI |
| 2 | Create part/price DTO and service skeletons behind the existing controllers | `/api/parts`, `/api/tools/{tool}/check`, `/api/price-alerts`, `/api/price-snapshots/collect` no longer depend on shared `MockData` |
| 3 | Implement Agent session state model and RAG evidence boundary | `/api/agent/sessions`, `/api/agent/sessions/{id}/run`, `/api/rag/search`, admin Agent detail show state timeline and evidence |
| 4 | Connect PC Agent JSONL export to upload and AS ticket creation | AS request creates AgentLogUpload and AsTicket records with consent, 30-minute range, and admin-only cause candidates |
| 5 | Add auth guard, admin guard, and infra smoke scripts | User/admin routes distinguish roles, Docker services stay reproducible, and k6 script skeleton exists |

## Seed Ownership

Seed/mock data follows the same module boundary as the final plan. `common/MockData.java` is only a small utility for `map()` and `now()`.

| Module | Seed file | Owner |
| --- | --- | --- |
| build | `apps/api/src/main/java/com/buildgraph/prototype/build/BuildSeed.java` | 1 |
| part/tool | `apps/api/src/main/java/com/buildgraph/prototype/part/PartSeed.java`, `ToolSeed.java` | 2 |
| price | `apps/api/src/main/java/com/buildgraph/prototype/price/PriceSeed.java` | 2 |
| agent | `apps/api/src/main/java/com/buildgraph/prototype/agent/AgentSeed.java` | 3 |
| rag | `apps/api/src/main/java/com/buildgraph/prototype/rag/RagSeed.java` | 3 |
| log | `apps/api/src/main/java/com/buildgraph/prototype/log/LogSeed.java` | 4 |
| ticket | `apps/api/src/main/java/com/buildgraph/prototype/ticket/TicketSeed.java` | 4 |
| user | `apps/api/src/main/java/com/buildgraph/prototype/user/UserSeed.java` | 5 |
| admin | `apps/api/src/main/java/com/buildgraph/prototype/admin/AdminSeed.java` | 5 |

## Frontend Ownership

Frontend files are split by implementation owner so feature work does not pile into one large page file.

| Owner | Frontend area | Notes |
| --- | --- | --- |
| 1 | `apps/web/src/features/quote/pages`, `apps/web/src/features/quote/components`, `apps/web/src/features/quote/quoteApi.ts`, `apps/web/src/features/auth` | Requirement input, build result, part-change flow, quote history, auth UI |
| 2 | `apps/web/src/features/parts/pages`, `apps/web/src/features/parts/mocks`, `apps/web/src/features/parts/partsApi.ts` | Self quote, parts table, tool checks, price-alert API boundary |
| 3 | `apps/web/src/features/admin/pages/AgentSessionAdminPage.tsx`, `ToolInvocationAdminPage.tsx`, `RagEvidenceAdminPage.tsx`, `apps/web/src/features/admin/mocks/adminMock.ts` | Agent/RAG/Tool evidence review screens |
| 4 | `apps/web/src/features/support`, `apps/web/src/features/support/supportApi.ts` | AS request, ticket detail, log upload policy |
| 5 | `apps/web/src/components/layout`, `apps/web/src/components/display`, `apps/web/src/components/feedback`, `apps/web/src/features/admin/pages/AdminDashboardPage.tsx` | Shared shell, common UI, admin dashboard |

## Shared Contract Rules

- Do not change an existing API response shape without updating `docs/openapi.yaml` in the same PR.
- If a feature needs seed data, add it inside the owning module seed file; do not add domain data to `common/MockData.java`.
- If a frontend feature needs mock data, place it under the owning feature `mocks` directory; keep `src/data/prototypeData.ts` as a compatibility barrel only.
- Add feature API calls in the owning `*Api.ts` file instead of calling `api()` directly from page components.
- Admin detail screens are implementation targets, not final UX. Keep them explicit about state, owner, API, and evidence fields.
- Keep MVP exclusions intact: no payment, shipping, custom remote control, exact FPS guarantee, or lowest-price guarantee.
