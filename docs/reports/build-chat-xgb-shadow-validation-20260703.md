# Build Chat 200개 시나리오 + XGBoost Shadow 검증 요약

- 검증일: 2026-07-03
- 대상 profile: `BUILD_CHAT_54_MINI_FAST`
- Reranker 상태: `RECOMMENDATION_RERANKER_ENABLED=false`, `RECOMMENDATION_RERANKER_SHADOW_ENABLED=true`
- Scorer: 로컬 임시 XGBoost 모델 `xgb-shadow-smoke-20260703`
- 상세 API 리포트: `docs/reports/build-chat-profile-benchmark-20260702-xgb-shadow-200.md`
- 웹 smoke 스크린샷/원본 리포트: `F:\buildgraph-ai-benchmark\web-smoke-16`

## 결론

XGBoost shadow scoring 파이프라인은 정상 동작했다. 200개 API 실행 중 `recommendation_shadow_scores`가 231건 저장됐고, 모델 버전도 `recommendation_model_versions`에 기록됐다. 즉 “기존 추천 순서를 바꾸지 않고 뒤에서 점수만 수집하는 구조”는 붙어 있다.

반면 Build Chat 자체 품질은 200개 엄격 시나리오 기준으로 아직 통과가 아니다. 전체 성공률은 29.0%였고, 주요 실패는 action 생성 누락, 카테고리/방향성 보존 실패, 추천 build 3개 미달에서 발생했다.

## API 200개 결과

| 항목 | 결과 |
|---|---:|
| 전체 성공률 | 29.0% |
| schema valid | 100.0% |
| 평균 latency | 3,182ms |
| p95 latency | 4,826ms |
| 최대 latency | 13,435ms |
| 10초 초과 slow case | 1건 |
| directionOk | 64.5% |
| categoryOk | 64.5% |
| actionPayloadOk | 47.5% |
| requiredTermsOk | 99.5% |
| shadow score 저장 | 231건 |

## 실패 유형

| 실패 유형 | 건수 | 의미 |
|---|---:|---|
| actionPayload 실패 | 105 | `ADD_PART_TO_DRAFT`, `REPLACE_DRAFT_PART` 등 실행 가능한 action이 기대만큼 나오지 않음 |
| category 실패 | 71 | 요청한 부품 카테고리와 응답 후보/action 카테고리가 어긋남 |
| direction 실패 | 71 | “더 좋은/더 싼/비슷한” 방향성을 지키지 못함 |
| build 3개 미달 | 28 | 견적 추천 요청에서 추천 build가 3개보다 적게 반환됨 |
| required term 실패 | 1 | `리안리 216` 같은 구체 지시가 응답에 보존되지 않음 |

## 그룹별 성공률

| 그룹 | 케이스 | 실패 | 성공률 |
|---|---:|---:|---:|
| 기존 base | 42 | 25 | 40.5% |
| 기존 방향성 matrix | 24 | 21 | 12.5% |
| 예산/용도 추천 확장 | 20 | 17 | 15.0% |
| 하드 조건 확장 | 30 | 25 | 16.7% |
| 방향성 확장 | 56 | 49 | 12.5% |
| 화면 이동 | 20 | 2 | 90.0% |
| 일반/설명/알림 | 8 | 3 | 62.5% |

## 웹 16개 Smoke

웹 대표 흐름은 16/16 통과했다. 확인한 흐름은 로그인, 홈 챗봇 5090 추천, AI 추천 적용, 셀프 견적 8개 카테고리 교체 요청, 9950X3D 상세 이동, 체크아웃, 내 견적함 이동, 비로그인 담기 redirect다.

다만 웹 smoke는 “화면이 깨지지 않는다”는 검증이고, API 200개에서 드러난 추천 품질 문제를 덮지는 못한다.

## 다음 보완 우선순위

1. Build Chat 응답에서 실행 가능한 action을 안정적으로 생성해야 한다.
   - 부품 추천은 `partRecommendation`만이 아니라 UI가 즉시 실행 가능한 `ADD_PART_TO_DRAFT` 또는 `REPLACE_DRAFT_PART` action을 일관되게 반환해야 한다.
2. 전 카테고리 교체 방향성을 다시 고쳐야 한다.
   - “더 좋은”, “더 싼”, “비슷한 가격”이 GPU뿐 아니라 CPU, 메인보드, RAM, SSD, PSU, 케이스, 쿨러에서 모두 유지돼야 한다.
3. 견적 추천은 3개 반환 기준을 안정화해야 한다.
   - Tool FAIL 제외 후 3개 미만이면 안전한 대체 후보를 재탐색하거나 부족 사유를 명확히 반환해야 한다.
4. 구체 모델명/브랜드 지시 보존을 보강해야 한다.
   - `리안리 216`, `MSI 메인보드`, `9950X3D` 같은 요청은 exact/alias resolver가 우선 처리해야 한다.

## PR 판단

이번 검증 작업 자체는 가치가 있다. 200개 시나리오와 shadow scoring 리포트는 추천 품질의 현재 위치를 정량화했다.

다만 이 결과를 “Build Chat 품질 통과”로 PR 설명하면 안 된다. PR을 보낸다면 “XGBoost shadow/평가체계 추가 및 품질 결함 발견”으로 설명해야 하며, 추천 품질 보완 PR은 별도로 잡는 것이 맞다.
