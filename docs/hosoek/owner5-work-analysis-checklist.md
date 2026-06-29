# 5번 작업 분석 체크리스트

작성일: 2026-06-29

## 결론

5번 담당 범위는 **Auth 공통 연동/AdminShell/AdminDashboard/공통 UI/Infra/CI**다.

로그인/회원가입의 프론트와 백엔드 구현은 1번에게 이관했다. 5번은 Auth API 구현 자체가 아니라 `api.ts`의 token 전달, `RequireAdmin`, `/admin/*` 권한 분기, AdminShell, AdminDashboard, Health, Infra/CI를 담당한다.

Figma 기준으로 5번이 직접 맡아야 할 화면은 `153:1880 STATE-15 ADMIN-01 운영 대시보드 / degraded`다. `153:2011 STATE-16 ADMIN-04 AS 티켓 관리자 / assigned success`는 4번 담당 화면이지만, 같은 AdminShell을 쓰므로 5번은 shell/guard 관점에서 참고해야 한다.

5번 기능 단위 상태는 1번 이관 작업을 제외하고 이렇게 정리한다.

| 기능 단위 | 상태 | 5번에게 남은 작업 |
| --- | --- | --- |
| Auth 공통 연동 | 진행중 | `Authorization` header와 token helper는 완료. 1번 Auth 구현 후 token 만료/refresh 실패 처리와 `RequireAdmin` 연동 검토 필요 |
| JWT/Token 연동 | 진행중 | 프론트 header 전달은 완료. 1번의 실제 JWT 구현 이후 admin guard/security 전환 검토 필요 |
| Auth Error 연동 | 진행중 | admin 401/403 분기는 완료. 1번 Auth 오류 응답이 공통 `ErrorResponse`와 충돌 없는지 검토 필요 |
| RequireAdmin | 완료 | `/admin/*` guard, `auth/me` role 확인, 401/403 화면 분기와 테스트 완료 |
| AdminShell | 진행중 | 기본 shell 완료. nav 세분화, topbar search/export/action 정책 미완료 |
| AdminDashboard | 완료 | DTO 정합성, metric, loading/error/success, degraded alert, 운영 작업, 관리자 할 일 link frame 완료 |
| Admin Audit Logs | 진행중 | 백엔드 endpoint와 권한 테스트 완료. 프론트 표시 연결과 seed 표시 정책 미완료 |
| Common API Client | 진행중 | token header 첨부 완료. refresh retry, token 만료 시 clear 정책, error normalization 미완료 |
| Common UI | 완료 | 공통 UI barrel/layout/display/feedback 구조 유지 완료 |
| Health | 진행중 | `/api/health` DB probe, 503 DOWN, 테스트/OpenAPI 완료. Docker runtime smoke 미완료 |
| Docker Compose | 진행중 | `docker compose config` 완료. `docker compose up --build` 전체 실행 검증 미완료 |
| Redis | 시작안함 | 1번 OAuth one-time code 또는 공통 cache/job state 사용 범위 미확정 |
| RabbitMQ | 시작안함 | Agent/price/mail job 중 5번 검증 범위 미확정 |
| Mailpit | 시작안함 | 메일 발송 smoke 또는 실제 연동 범위 미확정 |
| CI/GitHub Actions | 완료 | frontend build/test, OpenAPI, backend bootJar, compose config, health smoke 구성 완료 |
| k6/부하 테스트 | 진행중 | `infra/k6/smoke.js` skeleton 있음. smoke와 300명/1000명 부하 시나리오 분리 미완료 |
| 테스트/검증 | 완료 | `npm build/test`, `gradlew test/bootJar`, OpenAPI validation, compose config 검증 완료 |

5번이 하면 안 되는 것도 명확합니다.

| 기능 | 담당 |
| --- | --- |
| 로그인/회원가입 화면 UI, form state, validation 문구 | 1번 |
| 로그인/회원가입 백엔드 Auth/User 구현 | 1번 |
| `POST /api/users`, `POST /api/auth/login`, refresh/logout/OAuth 구현 | 1번 |
| 부품/가격 비즈니스 로직 | 2번 |
| LLM/RAG/Agent 내부 로직 | 3번 |
| PC Agent, AS 티켓 내부 로직 | 4번 |
| `/admin/as-tickets` 화면 내부 기능 | 4번 |
| `/admin/parts` 화면 내부 기능 | 2번 |

## 5번 담당 범위

| 구분 | 5번 책임 | 주요 파일/API |
| --- | --- | --- |
| 관리자 첫 화면 | `/admin` 운영 대시보드, dashboard frame, 운영 요약 | `apps/web/src/features/admin/pages/AdminDashboardPage.tsx`, `GET /api/admin/dashboard`, `GET /api/admin/audit-logs/recent` |
| 관리자 공통 shell | sidebar, topbar, route title, navigation, layout slot | `apps/web/src/components/layout/AdminShell.tsx`, `features/admin/shell/**` |
| 관리자 권한 | `/admin/*` guard, `ADMIN` role 확인, 401/403 분기 | `apps/web/src/features/auth/RequireAdmin.tsx`, `GET /api/auth/me` 연동 |
| Auth 공통 연동 | token 저장/전달, Authorization header, token 만료 시 공통 처리 정책 협업 | `apps/web/src/lib/api.ts`, `RequireAdmin`, Auth API 계약 리뷰 |
| 공통 UI | layout/display/feedback component contract 유지 | `apps/web/src/components/**`, `components/ui.tsx` barrel |
| 백엔드 공통 | admin/common/health, admin 권한 분기, security 연동 협업 | `apps/api/src/main/java/com/buildgraph/prototype/admin`, `common`, config/security |
| 인프라/CI | Docker Compose, OpenAPI 검증, k6 smoke, GitHub Actions | `infra`, `tools`, `.github/workflows`, `GET /api/health` |

## 5번이 맡으면 안 되는 내부 화면

| 화면 | 주 owner | 5번이 맡는 부분 |
| --- | --- | --- |
| `/admin/parts` | 2번 | AdminShell, guard, 공통 component contract |
| `/admin/agent-sessions/:id` | 3번 | AdminShell, guard, route slot |
| `/admin/tool-invocations/:id` | 3번 | AdminShell, guard, route slot |
| `/admin/rag-evidence/:id` | 3번 | AdminShell, guard, route slot |
| `/admin/as-tickets` | 4번 | AdminShell, guard, route slot |
| `/admin/as-tickets/:ticketId` | 4번 | AdminShell, guard, route slot |
| `/login`, `/signup`, `/auth/callback`, Auth/User 백엔드 | 1번 | token 공통 전달, `RequireAdmin`, admin guard 연동만 협업 |

## 한 일

### Figma/문서 분석

- [x] Figma URL의 섹션 노드 `138:2`를 분석했다.
- [x] 5번 직접 담당 화면을 `153:1880 STATE-15 ADMIN-01 운영 대시보드 / degraded`로 판정했다.
- [x] 4번 AS 티켓 관리자 화면 `153:2011`은 AdminShell 참고 화면으로 분리했다.
- [x] 전체 섹션 캡처를 저장했다.
- [x] 5번 직접 담당 대시보드 캡처를 저장했다.
- [x] AdminShell 참고 AS 티켓 화면 캡처를 저장했다.
- [x] `docs/ROUTE_OWNERSHIP.md`에서 5번의 route/API/DB/file owner를 확인했다.
- [x] `docs/role-workspaces.md`에서 `AdminDashboardPage.tsx`와 `AdminShell`이 5번 담당임을 확인했다.
- [x] 5번 전용 2주 계획 문서를 만들었다.

### 관리자 route/guard

- [x] `apps/web/src/App.tsx`에서 `/admin` route가 `RequireAdmin`으로 감싸져 있음을 확인했다.
- [x] `/admin/agent-sessions/:id` route가 `RequireAdmin`으로 감싸져 있음을 확인했다.
- [x] `/admin/tool-invocations/:id` route가 `RequireAdmin`으로 감싸져 있음을 확인했다.
- [x] `/admin/rag-evidence/:id` route가 `RequireAdmin`으로 감싸져 있음을 확인했다.
- [x] `/admin/parts` route가 `RequireAdmin`으로 감싸져 있음을 확인했다.
- [x] `/admin/as-tickets` route가 `RequireAdmin`으로 감싸져 있음을 확인했다.
- [x] `/admin/as-tickets/:ticketId` route가 `RequireAdmin`으로 감싸져 있음을 확인했다.
- [x] `RequireAdmin`이 localStorage의 `buildgraph.token` 존재 여부를 먼저 확인한다.
- [x] token이 있으면 `GET /api/auth/me`로 role을 확인한다.
- [x] `role === "ADMIN"`일 때만 관리자 화면을 렌더링한다.
- [x] token 없음, 401, USER role 상태에서 관리자 화면 본문을 숨긴다.
- [x] 권한 확인 중 loading 화면이 있다.
- [x] 권한 실패 시 로그인 이동/홈 이동 버튼이 있다.

### AdminShell

- [x] `AdminShell`이 sidebar, topbar, main layout을 담당한다.
- [x] `AdminShell`에 auth/permission 판단 코드가 들어가 있지 않다.
- [x] 도메인별 관리자 page 데이터를 `AdminShell`로 끌어올리지 않았다.
- [x] 현재 shell 공통 버튼은 frame 수준이며, 도메인 동작을 직접 실행하지 않는다.

### AdminDashboard/API

- [x] `AdminDashboardPage.tsx`가 5번 소유 화면임을 확인했다.
- [x] `AdminDashboardPage.tsx`가 `getAdminDashboard()` wrapper를 사용한다.
- [x] page component에서 `api()`를 직접 호출하지 않는다.
- [x] loading 상태가 있다.
- [x] error 상태가 있다.
- [x] `adminApi.ts`에 `AdminDashboard` 타입이 있다.
- [x] 백엔드 `GET /api/admin/dashboard` endpoint가 있다.
- [x] 백엔드 `GET /api/admin/audit-logs/recent` endpoint가 있다.
- [x] 백엔드 admin endpoint에서 `requireAdmin()`으로 demo admin token을 검사한다.
- [x] `AdminDashboard` DTO 정합성은 백엔드/OpenAPI 계약 기준으로 프론트를 맞추기로 결정했다.
- [x] `adminApi.ts`의 `AdminDashboard` 타입을 `agentRunning`, `openTickets`, `priceJobsRunning`, `degraded`, `generatedAt` 기준으로 수정했다.
- [x] `AdminDashboardPage.tsx` metric 표시를 실제 API 응답 필드 기준으로 수정했다.
- [x] `/admin` success 상태에서 `undefined` 값이 화면에 나오지 않도록 fallback과 테스트를 추가했다.
- [x] degraded 상태일 때 `/admin` 상단에 운영 상태 주의 alert frame을 표시하도록 수정했다.
- [x] 최근 Agent 세션은 3번 owner 상세 로직을 만들지 않고 summary/link frame으로만 표시했다.
- [x] 운영 작업 영역은 가격 Job, Mailpit, Mock Worker, k6 Smoke 요약으로 정리했다.
- [x] 관리자 할 일 table은 2번/3번/4번/5번 owner별 이동 link frame으로 정리했다.

### Auth/API 공통

- [x] `apps/web/src/lib/api.ts`가 `buildgraph.token`을 읽어 `Authorization: Bearer <token>` header를 붙인다.
- [x] `saveToken`, `getToken`, `clearToken` helper가 있다.
- [x] Auth 화면 UI 세부 구현은 1번 담당으로 분리했다.
- [x] 5번은 Auth API, token 저장/전달, `RequireAdmin`, `auth/me` 정책만 담당한다고 정리했다.
- [x] 현재 구현된 `UserController` API skeleton 테스트를 작성했다. 테스트 파일: `apps/api/src/test/java/com/buildgraph/prototype/user/UserControllerTest.java`
- [x] `POST /api/auth/login` skeleton response shape를 테스트했다.
- [x] `POST /api/users` 신규/기존 사용자 response shape를 테스트했다.
- [x] `GET /api/auth/me` demo token 응답과 token 없음 401 `ErrorResponse`를 테스트했다.
- [x] 로그인/회원가입 완성도 점검 결과, 현재 구현은 완성 인증이 아니라 demo token 기반 skeleton임을 확인했다.
- [x] `POST /api/auth/login`은 현재 password를 검증하지 않고 email 조회만으로 `demo-access-*` token을 반환한다.
- [x] `POST /api/users`는 현재 실제 password hash가 아니라 고정 문자열 `seed-signup-password-hash`를 저장한다.
- [x] `GET /api/auth/me`는 JWT 검증이 아니라 `Bearer demo-access-*` 문자열 포함 여부로 admin/user를 판정한다.
- [x] `/login`, `/signup` 화면은 렌더링 smoke test는 통과하지만 실제 입력값을 읽지 않고 고정 email/password로 API를 호출한다.
- [x] password hashing, login password verification, JWT 발급/검증, refresh token hash 저장/회전, logout revoke, validation error shape, duplicate email 409 처리는 1번 Auth/User 백엔드 작업으로 이관했다.
- [x] OpenAPI에 있는 `POST /api/auth/refresh`, `POST /api/auth/logout`, `GET /api/auth/google/start`, `GET /api/auth/google/callback`, `POST /api/auth/exchange` 구현은 1번에게 이관했다.
- [x] 5번은 1번 Auth 구현 후 `api.ts`, `RequireAdmin`, admin guard, API 계약 충돌 여부를 검토한다.
- [x] `AdminController` 권한/응답 계약 테스트를 작성했다. 테스트 파일: `apps/api/src/test/java/com/buildgraph/prototype/admin/AdminControllerTest.java`
- [x] `GET /api/admin/dashboard`의 token 없음 401, USER token 403, ADMIN token 200 응답을 테스트했다.
- [x] `GET /api/admin/audit-logs/recent`의 token 없음 401, USER token 403, ADMIN token 200 응답을 테스트했다.
- [x] admin API 401/403 실패 응답이 공통 `ErrorResponse` shape와 맞는지 테스트했다.
- [x] 프론트 `RequireAdmin`과 백엔드 admin API의 401/403 권한 분기 테스트를 다시 실행해 통과를 확인했다.
- [x] `HealthController` 성공 응답 테스트를 작성했다. 테스트 파일: `apps/api/src/test/java/com/buildgraph/prototype/common/HealthControllerTest.java`
- [x] `GET /api/health`가 DB probe 성공 시 200, `status: "UP"`, `database: "UP"`를 반환하는지 테스트했다.
- [x] DB 연결 실패 시 `/api/health`가 503, `{ "status": "DOWN" }`를 반환하는 정책을 반영했다.
- [x] DB 연결 실패 health 정책을 `docs/API_CONTRACT.md`와 `docs/openapi.yaml`에 반영했다.

### 인프라/검증 문서화

- [x] `compose.yaml`에 postgres, redis, rabbitmq, mailpit, api, web 서비스가 있음을 기존 체크리스트에서 확인했다.
- [x] `docker compose config`가 기존 확인에서 정상 출력되었음을 문서화했다.
- [x] OpenAPI 검증은 `PYTHONPATH=.venv/lib/python3.11/site-packages python3 tools/validate_openapi.py` 방식이 필요할 수 있음을 문서화했다.
- [x] k6 smoke skeleton이 `/api/health`, `/api/builds/recommend`, `/api/parts`를 확인하는 수준임을 정리했다.

## 해야 할 일

### 1. 테스트 먼저 작성

#### Frontend guard 테스트

- [x] `RequireAdmin` 테스트를 먼저 작성한다. 기존 테스트 파일: `apps/web/tests/admin-guard.spec.ts`
- [x] token이 없으면 `auth/me`를 호출하지 않고 로그인 필요 상태를 보여주는지 테스트한다.
- [x] `auth/me`가 `role: "ADMIN"`을 반환하면 children을 렌더링하는지 테스트한다.
- [x] `auth/me`가 `role: "USER"`를 반환하면 관리자 권한 없음 상태를 보여주는지 테스트한다.
- [x] `auth/me`가 401을 반환하면 로그인 필요 상태를 보여주는지 테스트한다.
- [x] `auth/me`가 403을 반환하면 관리자 권한 없음 상태를 보여주는지 테스트한다.
- [x] `/admin/*` 주요 route가 token 없는 상태에서 guard 화면을 보여주는지 테스트한다.
- [x] `AdminDashboardPage` loading/error/success 상태 테스트를 작성한다.
- [x] `/admin`이 ADMIN role일 때 `/api/admin/dashboard` 응답을 화면에 표시하는지 테스트한다.
- [x] `getAdminDashboard()` 경로가 `/api/admin/dashboard`를 호출하는지 Playwright 통합 테스트에서 검증한다.
- [x] `api.ts`가 token이 있을 때 Authorization header를 붙이는지 Playwright 통합 테스트에서 검증한다.

#### Backend 테스트

`RequireAdmin`은 React 컴포넌트라 `admin-guard.spec.ts`가 맞다. 로그인/회원가입 화면 UI와 form state는 1번 작업이다.

로그인/회원가입 프론트/백엔드는 1번에게 이관했다. 아래 `UserControllerTest` 관련 항목은 이관 전 skeleton 현황 확인 기록이며, 완성 Auth 테스트는 1번 작업이다.

- [x] `apps/api/src/test/java/com/buildgraph/prototype/admin/AdminControllerTest.java`를 만든다.
- [x] admin token이 없으면 `GET /api/admin/dashboard`가 401을 반환하는지 테스트한다.
- [x] USER token이면 `GET /api/admin/dashboard`가 403을 반환하는지 테스트한다.
- [x] ADMIN token이면 `GET /api/admin/dashboard`가 200과 `AdminDashboardDto` 필드를 반환하는지 테스트한다.
- [x] `GET /api/admin/audit-logs/recent`도 ADMIN 권한에서만 조회되는지 테스트한다.
- [x] admin API 실패 응답이 공통 `ErrorResponse`와 맞는지 테스트한다.
- [x] `apps/api/src/test/java/com/buildgraph/prototype/common/HealthControllerTest.java`를 만든다.
- [x] `GET /api/health`가 200과 `{ "status": "UP" }` 형태를 반환하는지 테스트한다.
- [x] DB 연결 실패 시 health 응답 정책을 확인한다. 정책: `503 Service Unavailable` + `{ "status": "DOWN" }`
- [x] 이관 전 현재 구현된 Auth API skeleton 확인용으로 `apps/api/src/test/java/com/buildgraph/prototype/user/UserControllerTest.java`를 만들었다.
- [x] 현재 API skeleton 기준으로만 `POST /api/users`, `POST /api/auth/login`, `GET /api/auth/me`의 status와 response shape를 테스트한다.
- [x] 비밀번호 검증, refresh token 회전, logout invalidation, OAuth 실제 교환 테스트는 1번 Auth/User 완성 작업으로 이관했다.
- [x] `POST /api/auth/refresh`, `POST /api/auth/logout`, Google OAuth start/callback/exchange는 1번 구현 PR에서 테스트해야 한다.
- [x] 로그인/회원가입 화면 UI, form state, validation 문구 테스트는 1번 작업으로 분리한다.

### 2. AdminDashboard DTO 정합성 해결

- [x] 현재 프론트 타입 `llmQueueP95`, `apiP95`, `asOpen`, `recommendationSuccess`와 OpenAPI/백엔드 타입 `agentRunning`, `openTickets`, `priceJobsRunning`, `degraded`, `generatedAt` 중 어느 쪽으로 맞출지 결정한다. 결정: 백엔드/OpenAPI 계약 기준으로 프론트를 맞춘다.
- [x] 결정 전에 API 계약 변경 여부를 확인한다.
- [x] API 계약을 바꾸면 `docs/API_CONTRACT.md`와 `docs/openapi.yaml`을 같은 변경에 포함한다. 이번 작업은 API 계약 변경 없이 프론트만 계약에 맞췄다.
- [x] 프론트를 맞추는 경우 `adminApi.ts`의 `AdminDashboard` 타입을 `AdminDashboardDto`에 맞춘다.
- [x] `AdminDashboardPage.tsx`의 metric 표시를 실제 응답 필드 기준으로 고친다.
- [x] success 상태에서 `undefined` 값이 화면에 나오지 않게 한다.

### 3. Figma 기준 AdminDashboard 보강

- [x] `153:1880` 기준으로 degraded alert frame을 추가한다.
- [x] metric card 4개를 실제 API/계약에 맞게 다시 배치한다.
- [x] `최근 Agent 세션` 영역은 3번 데이터와 경계를 확인한 뒤 summary 수준으로만 둔다.
- [x] `운영 작업` 영역은 price job, email, mock-worker, k6-report 중 어떤 값을 5번이 표시할지 결정한다.
- [x] `관리자 할 일` table은 각 도메인 owner 데이터가 필요한지 확인한다.
- [x] 5번 단독으로 만들 수 없는 도메인 데이터는 mock summary 또는 link frame으로만 둔다.

### 4. AdminShell navigation 정리

- [ ] 현재 4개 nav 항목을 Figma 기준 8개 항목으로 나눌지 팀에 확인한다.
- [ ] `대시보드`는 `/admin`으로 연결한다.
- [ ] `Agent 세션`은 3번 owner route로 연결한다.
- [ ] `Tool 이력`은 3번 owner route로 연결한다.
- [ ] `RAG 근거`는 3번 owner route로 연결한다.
- [ ] `부품/가격`은 2번 owner route로 연결한다.
- [ ] `AS 티켓`은 4번 owner route로 연결한다.
- [ ] `가격 Job`을 `/admin/parts` 안에 둘지 별도 route로 둘지 2번과 합의한다.
- [ ] `부하 테스트` route가 MVP에 필요한지 확인한다.
- [ ] nav label/order 변경은 각 owner와 공유한다.

### 5. AdminShell topbar 정리

- [ ] Figma처럼 admin search input을 둘지 결정한다.
- [ ] 검색 범위가 세션/티켓/부품 전체라면 각 owner API와 충돌하는지 확인한다.
- [ ] 검색 동작이 미확정이면 placeholder UI만 둔다.
- [ ] `내보내기` 버튼의 실제 export 범위를 결정한다.
- [ ] `작업 실행` 버튼이 어떤 job을 실행하는지 결정한다.
- [ ] 동작이 미확정이면 버튼을 disabled 또는 no-op frame으로 둔다.

### 6. Auth 협업/관리자 권한 고도화

- [x] `POST /api/users`, `POST /api/auth/login`, `GET /api/auth/me`, refresh/logout/OAuth 구현은 1번에게 이관한다.
- [x] password hashing, login password verification, JWT 발급/검증, refresh token hash 저장/회전/폐기, duplicate email 409, Auth validation error 구현은 1번에게 이관한다.
- [ ] 1번 Auth API 구현 후 `apps/web/src/lib/api.ts`의 Authorization header/token 저장 정책과 충돌 없는지 확인한다.
- [ ] 1번 Auth API 구현 후 `RequireAdmin`이 실제 JWT/role 기반 `/api/auth/me` 응답과 맞는지 확인한다.
- [ ] 1번 Auth API 구현 후 admin API 권한 분기를 demo token에서 실제 JWT/security 정책으로 전환할지 결정한다.
- [ ] Auth API 계약 변경 시 `docs/API_CONTRACT.md`와 `docs/openapi.yaml`의 오류 응답/예시를 1번 변경과 같이 검토한다.
- [ ] 401과 403 메시지를 더 명확히 분리한다.
- [ ] token이 만료되었을 때 `clearToken()`을 호출할지 결정한다.
- [ ] refresh retry를 `api.ts` 공통에 둘지, 1번 Auth flow 내부에 둘지 결정한다.
- [ ] `/api/auth/logout` 호출 후 프론트 token 정리 흐름을 1번과 합의한다.
- [ ] OAuth callback/exchange는 1번 구현 범위이며, 5번은 token 저장/전달 연동만 검토한다.
- [ ] AdminController demo token 검사 방식에서 실제 JWT 검증으로 넘어갈 위치를 문서화한다.

### 7. Backend/Admin/Health

- [ ] `GET /api/admin/dashboard` 응답을 OpenAPI와 정확히 맞춘다.
- [ ] `GET /api/admin/audit-logs/recent` 응답 shape를 화면에서 쓸 수 있게 정리한다.
- [ ] `GET /api/health`가 DB 연결까지 확인하는지 runtime smoke로 검증한다.
- [ ] `admin_audit_logs` seed와 화면 표시 항목을 연결할지 결정한다.
- [x] admin endpoint 401/403 response가 공통 `ErrorResponse`와 일치하는지 확인한다.

### 8. Infra/CI/k6

- [ ] Redis를 Sprint 1에서 연결 확인만 할지 실제 기능에 쓸지 결정한다.
- [ ] RabbitMQ를 Agent job, price job, mail job 중 어디까지 검증할지 결정한다.
- [ ] Mailpit은 실행 확인만 할지 실제 메일 발송까지 할지 결정한다.
- [ ] `docker compose up --build`로 전체 실행을 확인한다.
- [ ] k6 smoke와 실제 부하 테스트 시나리오를 분리한다.
- [ ] 300명/1000명 목표가 이번 Sprint인지 이후 Sprint인지 확정한다.
- [ ] 성능 리포트 템플릿을 만든다.

### 9. PR 전 검증

- [x] `npm --prefix apps/web run build`
- [x] `npm --prefix apps/web run test`
- [ ] `python tools/validate_openapi.py`
- [x] 필요 시 `PYTHONPATH=.venv/lib/python3.11/site-packages python3 tools/validate_openapi.py`
- [x] `docker compose config`
- [x] 백엔드 테스트 변경 검증으로 `cd apps/api && ./gradlew test --no-daemon`을 실행한다.
- [x] 백엔드 운영 빌드 변경 시 `cd apps/api && ./gradlew bootJar --no-daemon`
- [ ] 인프라 변경 시 `docker compose up --build`
- [ ] 검증 결과를 PR 설명에 붙인다.

## 우선순위

### P0

- [x] `RequireAdmin` 테스트 작성: `apps/web/tests/admin-guard.spec.ts`
- [x] Frontend 테스트 보강: `AdminDashboardPage`, `adminApi`, `api.ts`
- [x] Backend Java 테스트 작성: `AdminControllerTest.java`, `HealthControllerTest.java`
- [x] `AdminDashboard` DTO 불일치 해결
- [x] `/admin` dashboard가 실제 API 응답을 안전하게 표시하도록 수정
- [x] 401/403 권한 분기 확인
- [x] PR 전 기본 검증 명령 실행
- [ ] 1번 Auth/User 구현 후 `api.ts`, `RequireAdmin`, admin guard 연동 검토

### P1

- [ ] AdminShell nav를 Figma 기준으로 세분화
- [ ] AdminShell topbar search/action frame 정리
- [ ] audit logs recent 표시 연결
- [ ] k6 smoke 리포트 템플릿 작성

### P2

- [ ] Redis/RabbitMQ/Mailpit 실제 사용 범위 확정
- [ ] 부하 테스트 300명/1000명 시나리오 확장
- [ ] 1번 refresh/JWT 구현 후 5번 공통 API client/admin guard 연동 고도화

## 확인 필요 질문

- [x] AdminDashboard DTO는 OpenAPI 기준으로 프론트를 맞출지, Figma 운영 지표 기준으로 OpenAPI를 확장할지 결정해야 한다. 결정: OpenAPI/백엔드 기준으로 프론트를 맞춘다.
- [x] DB 연결 실패 시 `/api/health`를 500 `INTERNAL_ERROR`, 503 `DOWN`, 또는 200 `status: "DOWN"` 중 어떤 정책으로 반환할지 결정해야 한다. 결정: `503 Service Unavailable` + `{ "status": "DOWN" }`
- [ ] AdminShell nav의 `가격 Job`을 2번의 `/admin/parts` 안에 둘지 별도 route로 둘지 결정해야 한다.
- [ ] `부하 테스트` 관리자 화면을 MVP route로 만들지, 문서/리포트 링크로 둘지 결정해야 한다.
- [ ] topbar `작업 실행`이 어떤 작업을 실행하는 버튼인지 결정해야 한다.
- [ ] admin search가 전역 검색인지, 단순 placeholder인지 결정해야 한다.
