# Support/Chat 리스크 수정 체크리스트

## 목적

이 문서는 현재 Support/AS Chat 관련 코드에서 확인된 리스크를 실제 수정 작업 단위로 쪼갠 체크리스트다.
대상은 문서상 목표 기능과 실제 앱 동작이 어긋나는 지점, 사용자/관리자 경험 누락, 계약 불일치, 테스트 공백이다.

## 기준 문서

- `AGENTS.md`
- `docs/API_CONTRACT.md`
- `docs/DB_SCHEMA.md`
- `docs/ROUTE_OWNERSHIP.md`
- `docs/openapi.yaml`
- `apps/pc-agent/README.md`

## 우선순위 요약

| 우선순위 | 항목 | 이유 |
| --- | --- | --- |
| P1 | 사용자 AS AI Chat 관리자용 진단 정보 노출 제거 | 사용자 화면 정책과 충돌 가능성이 큼 |
| P1 | `SupportChatWidget` 마운트 및 신규 상담 시작 흐름 연결 | 문서상 핵심 UX가 실제 화면에 붙어 있지 않음 |
| P2 | `admin_unread_count` 읽음 처리 보완 | 관리자 상담 운영 UX와 알림 신뢰도 문제 |
| P2 | Build Chat API 계약/구현 정합성 정리 | 후속 개발자가 문서 기준으로 잘못 구현할 가능성 |
| P2/P3 | Support/AS Chat 백엔드 테스트 보강 | 양방향 상담/권한/읽음 처리 회귀 방지 |

---

## 1. 사용자 AS AI Chat 관리자용 진단 정보 노출 제거

### 현상

- 사용자 화면 `/support/ai-chat`에서 `LLM 구조화 결과`, `원인 후보`, `Tool 결과`, `RAG 근거`가 노출된다.
- 관련 코드:
  - `apps/web/src/features/support/SupportPages.tsx`
  - `AsChatPage`의 `Panel title="LLM 구조화 결과"`
  - `Panel title="Tool 결과"`
  - `Panel title="RAG 근거"`

### 리스크

- 사용자에게 원인 후보가 확정 진단처럼 보일 수 있다.
- 내부 RAG 근거, Tool 결과, confidence가 사용자에게 과노출될 수 있다.
- 프로젝트 지침의 “사용자 화면에는 AS 원인 후보 같은 관리자용 진단 정보를 노출하지 않는다”와 충돌한다.

### 수정 체크리스트

- [ ] 사용자 화면에 노출할 AS AI Chat 필드 범위를 확정한다.
- [ ] 사용자 화면에서는 원인 후보, Tool 결과, RAG 근거 원문을 숨긴다.
- [ ] 사용자 화면은 다음 정보 중심으로 재구성한다.
  - [ ] AI 요약 답변
  - [ ] 사용자가 즉시 할 수 있는 안전 조치
  - [ ] 상담원 연결 필요 여부
  - [ ] AS 티켓 번호 또는 접수 진행 상태
- [ ] 관리자용 진단 정보는 관리자 화면 또는 관리자 API 응답으로만 분리한다.
- [ ] 사용자 API DTO와 관리자 API DTO를 분리할지 결정한다.
- [ ] `docs/API_CONTRACT.md`에서 사용자 AS Chat 응답 필드 설명을 실제 정책에 맞게 수정한다.
- [ ] `docs/openapi.yaml`에 사용자/관리자 응답 shape를 반영한다.

### 검증 체크리스트

- [ ] `/support/ai-chat`에서 `원인 후보`, `Tool 결과`, `RAG 근거` 텍스트가 보이지 않는다.
- [ ] 관리자 화면에서는 필요한 진단 정보가 계속 확인된다.
- [ ] 사용자 답변에는 확정 진단처럼 보이는 문구가 없다.
- [ ] AS Chat 프론트 테스트에서 사용자 화면 노출 금지 항목을 검증한다.

### 결정 필요

- [ ] 사용자에게 evidence count 정도는 보여줄지, 완전히 숨길지 결정한다.
- [ ] 상담원 연결 필요 사유를 어느 수준까지 사용자에게 보여줄지 결정한다.

---

## 2. `SupportChatWidget` 마운트 및 신규 상담 시작 흐름 연결

### 현상

- `SupportChatWidget`은 구현되어 있지만 실제 앱에 import/마운트된 호출부가 없다.
- `createSupportChat()` API wrapper도 있지만 프론트 호출부가 없다.
- 관련 코드:
  - `apps/web/src/features/support/SupportChatWidget.tsx`
  - `apps/web/src/features/support/supportApi.ts`
  - `apps/web/src/App.tsx`
  - `apps/api/src/main/java/com/buildgraph/prototype/ticket/SupportChatController.java`

### 리스크

- 문서상 “홈 좌측 하단 상담 위젯”이 실제 사용자 화면에 표시되지 않을 수 있다.
- 기존 상담방이 없는 신규 사용자는 상담을 시작할 수 없다.
- 관리자 `/admin/customer-contacts`는 상담방을 보여주지만 사용자 진입점이 비어 기능이 반쪽이 된다.

### 수정 체크리스트

- [ ] `SupportChatWidget`을 어느 route에 노출할지 결정한다.
  - [ ] 전체 사용자 화면
  - [ ] 홈 화면만
  - [ ] `/support/*` 제외
  - [ ] `/admin`, `/login`, `/signup` 제외
- [ ] `App.tsx`에 전역 support chat wrapper를 추가한다.
- [ ] `AiBuildAssistant`와 위치가 겹치지 않도록 배치 정책을 정한다.
- [ ] 기존 상담방이 없을 때도 위젯 진입 버튼은 보이게 할지 결정한다.
- [ ] 상담방이 없는 상태에서 `createSupportChat()`을 호출하는 UI를 추가한다.
- [ ] 상담 시작 시 `supportRequestType` 선택 방식을 정한다.
  - [ ] 기본값 `DIAGNOSIS_ONLY`
  - [ ] 원격/방문/진단 선택 UI
- [ ] 첫 메시지를 입력받아 상담방 생성과 동시에 전송할지 결정한다.
- [ ] 생성 성공 후 current chat query와 messages query를 invalidate한다.
- [ ] WebSocket 연결 실패 시 REST polling fallback이 정상 동작하는지 확인한다.

### 검증 체크리스트

- [ ] 로그인한 사용자 화면에 상담 위젯이 표시된다.
- [ ] 상담방이 없는 신규 사용자가 상담을 시작할 수 있다.
- [ ] 상담방 생성 후 사용자 메시지가 관리자 `/admin/customer-contacts`에 표시된다.
- [ ] 관리자 답변이 사용자 위젯에 표시된다.
- [ ] WebSocket 연결 실패 시 polling으로 메시지가 갱신된다.
- [ ] 모바일 viewport에서 위젯이 주요 버튼/입력창을 가리지 않는다.

### 결정 필요

- [ ] 상담 위젯과 AI Build Assistant를 동시에 노출할지 결정한다.
- [ ] 일반 상담방과 AS 티켓 기반 상담방을 같은 UI로 처리할지 결정한다.

---

## 3. `admin_unread_count` 읽음 처리 보완

### 현상

- 사용자 메시지 전송 시 `admin_unread_count`는 증가한다.
- 사용자 상세 조회 시 `user_unread_count`는 reset된다.
- 관리자 상세 조회 시 `admin_unread_count`를 reset하는 코드가 보이지 않는다.
- 관련 코드:
  - `apps/api/src/main/java/com/buildgraph/prototype/ticket/SupportChatService.java`
  - `apps/api/src/main/java/com/buildgraph/prototype/admin/AdminCustomerContactService.java`
  - `apps/web/src/features/admin/pages/AdminCustomerContactsPage.tsx`

### 리스크

- 관리자가 상담방을 확인해도 unread badge가 계속 남을 수 있다.
- 관리자 상담 목록의 미확인 표시를 신뢰하기 어려워진다.
- 실제 미응답 상담과 이미 확인한 상담을 구분하기 어렵다.

### 수정 체크리스트

- [ ] 관리자 읽음 처리 트리거를 결정한다.
  - [ ] 상세 조회 시 즉시 읽음 처리
  - [ ] 메시지 패널 open/focus 시 읽음 처리
  - [ ] 별도 mark-read API 호출
- [ ] `AdminCustomerContactService.contact()` 또는 명시적 mark-read API에서 `admin_unread_count = 0` 처리한다.
- [ ] 필요 시 `as_chat_messages.read_at`도 함께 갱신한다.
- [ ] WebSocket으로 메시지를 받는 경우 읽음 처리 정책을 정한다.
- [ ] 관리자 상세 응답의 `contact.adminUnreadCount`가 reset 후 값을 반영하도록 조정한다.
- [ ] 목록 query invalidate 타이밍을 점검한다.

### 검증 체크리스트

- [ ] 사용자가 메시지를 보내면 관리자 목록 badge가 증가한다.
- [ ] 관리자가 해당 상담방을 열면 badge가 0이 된다.
- [ ] 관리자가 답변하면 사용자 unread count가 증가한다.
- [ ] 사용자가 위젯/상담방을 열면 user unread count가 0이 된다.
- [ ] 동시 polling/WebSocket 상황에서도 unread count가 음수가 되지 않는다.

### 결정 필요

- [ ] `read_at`을 실제 메시지 단위 읽음 시각으로 사용할지 결정한다.
- [ ] 단순 count reset만 할지, 메시지별 읽음 상태까지 관리할지 결정한다.

---

## 4. Build Chat API 계약/구현 정합성 정리

### 현상

- `docs/API_CONTRACT.md`는 `/api/ai/build-chat` 응답에 `actions`, `partRecommendation`이 있다고 설명한다.
- 현재 `BuildChatService.responseMap()` 기본 응답은 `answerType`, `message`, `builds`, `warnings`, `evidenceIds`, `agentSessionId` 중심이다.
- 프론트 일부 코드에는 legacy field 제거 흔적이 있다.

### 리스크

- API 문서, OpenAPI, 백엔드 구현, 프론트 타입 중 기준이 불명확하다.
- 문서를 보고 `actions` 기반 UI를 구현하면 실제 응답과 맞지 않을 수 있다.
- 반대로 구현이 맞다면 문서가 후속 개발자를 오도한다.

### 수정 체크리스트

- [ ] `actions`, `partRecommendation`을 MVP 공개 계약으로 유지할지 결정한다.
- [ ] 유지한다면 백엔드 `BuildChatService.responseMap()`에서 해당 필드를 항상 계약대로 반환한다.
- [ ] 제거한다면 `docs/API_CONTRACT.md`에서 예시와 정책 설명을 정리한다.
- [ ] `docs/openapi.yaml`의 `/api/ai/build-chat` schema를 실제 응답과 맞춘다.
- [ ] 프론트 타입에서 legacy field 처리 방식을 정리한다.
- [ ] 캐시 payload에서 제거/유지할 필드를 명확히 한다.
- [ ] `XgboostShadowReranker`가 `partRecommendation`을 참조하는 경로에 영향이 없는지 확인한다.

### 검증 체크리스트

- [ ] API contract 문서 예시와 실제 `/api/ai/build-chat` 응답이 일치한다.
- [ ] OpenAPI validation이 통과한다.
- [ ] 프론트 타입 체크가 통과한다.
- [ ] Build Chat 관련 단위 테스트가 계약 필드를 검증한다.

### 결정 필요

- [ ] 현재 구현을 기준으로 계약을 축소할지, 문서를 기준으로 구현을 복구할지 결정한다.
- [ ] 견적 draft mutation을 Build Chat action으로 유지할지 별도 API/UI 흐름으로 분리할지 결정한다.

---

## 5. Support/AS Chat 백엔드 테스트 보강

### 현상

- AS Chat profile 정책 테스트는 있으나, 상담방 생성/조회/메시지/권한/unread/WebSocket fallback에 대한 백엔드 테스트가 부족하다.
- 관련 코드:
  - `SupportChatController`
  - `SupportChatService`
  - `AdminCustomerContactService`
  - `SupportChatWebSocketHandler` 또는 관련 WebSocket 구성

### 리스크

- 사용자와 관리자 양방향 흐름은 회귀 가능성이 높은데 테스트가 적다.
- unread count 같은 상태성 버그가 리뷰 전까지 발견되기 어렵다.
- 본인 소유권/관리자 권한 경계가 깨져도 놓칠 수 있다.

### 테스트 추가 체크리스트

- [ ] `SupportChatService` 단위 또는 slice 테스트를 추가한다.
- [ ] `SupportChatController` 인증/권한 테스트를 추가한다.
- [ ] `AdminCustomerContactService` unread/reset 테스트를 추가한다.
- [ ] 상담방 생성 테스트를 추가한다.
  - [ ] 신규 사용자는 새 `as_chat_sessions` row가 생성된다.
  - [ ] active 상담방이 있으면 기존 상담방을 반환한다.
  - [ ] 첫 메시지가 있으면 `USER` message가 저장된다.
- [ ] 사용자 메시지 전송 테스트를 추가한다.
  - [ ] `admin_unread_count`가 증가한다.
  - [ ] `last_message_preview`가 갱신된다.
- [ ] 관리자 메시지 전송 테스트를 추가한다.
  - [ ] `user_unread_count`가 증가한다.
  - [ ] `assigned_admin_id`가 설정된다.
- [ ] 소유권 테스트를 추가한다.
  - [ ] 다른 사용자의 `sessionId` 접근은 `404`다.
  - [ ] 인증 없이는 `401`이다.
- [ ] 읽음 처리 테스트를 추가한다.
  - [ ] 사용자 조회 시 `user_unread_count`가 reset된다.
  - [ ] 관리자 조회 시 `admin_unread_count`가 정책대로 reset된다.
- [ ] WebSocket과 REST polling DTO shape가 같은지 검증한다.

### 검증 체크리스트

- [ ] `cd apps/api && ./gradlew test` 통과
- [ ] `cd apps/web && npm run test` 통과
- [ ] 필요 시 Playwright로 사용자-관리자 상담 왕복 시나리오 검증

---

## 권장 작업 순서

- [ ] 1단계: 사용자 AS AI Chat 노출 정책 확정 및 프론트 숨김 처리
- [ ] 2단계: `SupportChatWidget` 마운트 위치와 신규 상담 시작 UX 확정
- [ ] 3단계: 상담 위젯 생성/메시지 전송 흐름 구현
- [ ] 4단계: 관리자 unread reset 정책 확정 및 구현
- [ ] 5단계: Support/AS Chat 백엔드 테스트 추가
- [ ] 6단계: Build Chat 계약/구현 중 기준 선택
- [ ] 7단계: `docs/API_CONTRACT.md`와 `docs/openapi.yaml` 정합성 반영
- [ ] 8단계: 프론트/백엔드/OpenAPI 검증 명령 실행

## 완료 기준

- [ ] 사용자 화면에 관리자용 원인 후보/Tool/RAG 상세가 노출되지 않는다.
- [ ] 사용자가 상담 위젯에서 새 상담방을 만들고 메시지를 보낼 수 있다.
- [ ] 관리자가 고객 연락 화면에서 메시지를 확인하고 unread badge가 정상 reset된다.
- [ ] 사용자와 관리자의 unread count가 각각 기대대로 증가/감소한다.
- [ ] Build Chat API 문서와 실제 응답이 일치한다.
- [ ] Support/AS Chat 핵심 흐름에 백엔드 테스트가 추가되어 통과한다.
- [ ] 관련 변경 시 `docs/API_CONTRACT.md`, `docs/openapi.yaml`이 함께 갱신된다.

