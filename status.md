# 2026-07-02 PCagent main Agent AS runtime QA

## Current goal

- `pcagent/main` 최신 커밋 `961d0ba` 기준으로 서버, 웹, PC Agent register/consent/heartbeat/upload/supportDecision happy path를 실제 런타임에서 검증한다.

## Done

- `qa/pcagent-main-runtime-qa` 브랜치를 `pcagent/main` 최신 커밋에서 생성했다.
- 문서 기준은 `docs/agent-as/E2E_HAPPY_PATH.md`, `docs/agent-as/README.md`, `apps/pc-agent/README.md`를 확인했다.
- 기존 Compose DB volume은 Flyway V53/V54 checksum mismatch가 있어 건드리지 않고, 별도 QA 컨테이너를 사용했다.
- QA API: `http://127.0.0.1:18080`, QA Web: `http://127.0.0.1:15173`, QA DB: `pcagent-postgres-runtime-qa`로 격리 실행했다.
- register 전 `agent status`는 `UNREGISTERED`, register 후 `REGISTERED`를 확인했다.
- `SERVER_UPLOAD` consent accepted, heartbeat `ACTIVE`, gzip upload, `ticketId` 반환을 확인했다.
- 관리자 `PATCH /api/admin/as-tickets/{ticketId}`로 `supportDecision=REMOTE_POSSIBLE`, `reviewStatus=APPROVED` 저장을 확인했다.
- 사용자 `GET /api/as-tickets/{ticketId}`와 웹 `/support/{ticketId}` 화면에서 `RULE_READY`, `APPROVED`, `REMOTE_POSSIBLE` 반영을 확인했다.
- 같은 upload `Idempotency-Key` 재시도 시 같은 `ticketId`가 반환되는 것을 확인했다.
- 화면 증거: `artifacts/qa/pcagent-support-decision.png`.

## Fixed

- multipart upload 요청을 generic Agent idempotency filter가 body caching으로 소비해 `file` part가 사라지는 문제를 수정했다.
- `/api/agent/log-uploads`는 service 레벨에서 `agent_upload_jobs(device_id,idempotency_key)` 기준 replay/conflict를 처리하도록 수정했다.
- upload SQL에서 `Instant`와 `delete_after` timestamp 파라미터 타입 추론 오류를 수정했다.
- PC Agent gzip 생성이 재시도마다 다른 gzip bytes를 만들지 않도록 deterministic gzip `mtime=0`을 적용했다.
- missing `Idempotency-Key` header도 공통 `VALIDATION_ERROR` 응답으로 처리되도록 보강했다.

## Remaining issues

- 기존 Compose DB volume은 V53/V54 checksum mismatch 상태다. 이번 QA에서는 데이터 삭제를 하지 않고 별도 QA DB로 우회했다.
- `apps/pc-agent`의 `supportUrl` 생성은 `apiBaseUrl`이 기본 `:8080`일 때만 웹 `:5173`으로 바꾼다. 이번 격리 QA처럼 `:18080`을 쓰면 출력 URL은 API 포트 기준이라 실제 웹 URL은 `http://127.0.0.1:15173/support/{ticketId}`를 사용해야 했다.

## Last verification

- `.\gradlew.bat test --tests com.buildgraph.prototype.config.security.PcAgentControllerSecurityTest --tests com.buildgraph.prototype.agent.PcAgentAsServiceTest --no-daemon` 성공. 한글 경로 ClassNotFound 기존 이슈를 피해 `C:\codex\pcagent-prototype` junction에서 실행했다.
- `docker build -t prototype-api:latest .\apps\api` 성공.
- `docker build -t prototype-web:latest .\apps\web` 성공.
- `python tools\validate_openapi.py` 성공. 결과: `OpenAPI validation passed: 63 paths`.
- `docker compose config --quiet` 성공.
- 최종 QA ticketId: `9e39f4bd-440a-439d-b690-6457ec3e0354`.

# Agent AS Goal 4/5 status

Updated: 2026-07-02

## Member A scope

- Goal 4: Agent Register + Consent hardening
- Goal 5: Agent Heartbeat hardening
- This note records policy points that remain ambiguous in the project docs. No new feature behavior is introduced here.

## Confirmed current behavior

- `POST /api/agent/devices/register` is the only `/api/agent/**` endpoint allowed before Agent token authentication.
- `POST /api/agent/consents`, `POST /api/agent/heartbeat`, and `POST /api/agent/log-uploads` require an Agent bearer token.
- Agent mutation APIs require `Idempotency-Key`, including heartbeat under the current implementation.
- Register returns the raw agent token only in the response and stores only `agent_token_hash`.
- Log upload checks accepted `SERVER_UPLOAD` consent before creating upload/ticket rows.

## Need confirmation

- Register duplicate policy is not fully settled by the contract docs.
- Current MVP behavior refreshes the existing device token when the same user and `registrationIdempotencyKey` are reused.
- It is still unclear whether the same `activationToken` may register multiple devices.
- It is still unclear whether duplicate `deviceFingerprintHash` with a new `registrationIdempotencyKey` should create a new device, reject with conflict, or rotate the existing token.
- Consent update policy is not fully settled. Current behavior appends a consent row instead of updating an older row.
- API contract docs still list legacy web JWT paths such as `/api/agent/sessions` and `/api/agent-logs/upload`; they do not yet describe the current Agent-token lifecycle endpoints.
