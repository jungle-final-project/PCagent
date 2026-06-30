# AS Chat AI Profile Benchmark

- generatedAt: 2026-06-30T03:40:13
- totalCases: 18

## Summary

| profile | successRate | avgFirstEventMs | avgFinalLatencyMs | p95FinalLatencyMs | avgInputTokens | avgOutputTokens | avgTokens | schemaValidRate |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| AS_CHAT_FAST | 100.0% | 21 | 9272 | 11022 | 1236 | 586 | 1822 | 100.0% |
| AS_CHAT_BALANCED | 100.0% | 11 | 10654 | 11975 | 1354 | 821 | 2175 | 100.0% |
| AS_CHAT_HIGH_QUALITY | 100.0% | 11 | 14913 | 17573 | 1869 | 1098 | 2966 | 100.0% |

## Cases

| profile | case | risk | ok | firstEventMs | finalLatencyMs | model | inTok | outTok | tokens | evidence | tools | actions | keywords | error |
|---|---|---|---:|---:|---:|---|---:|---:|---:|---:|---:|---:|---:|---|
| AS_CHAT_FAST | gpu-thermal-frame-drop | medium | yes | 43 | 10960 | gpt-5.5 | 1241 | 516 | 1757 | 2 | 3 | 2 | 3/3 |  |
| AS_CHAT_FAST | driver-crash-event-log | medium | yes | 17 | 7588 | gpt-5.5 | 1228 | 627 | 1855 | 2 | 3 | 2 | 3/3 |  |
| AS_CHAT_FAST | memory-pressure | low | yes | 5 | 7341 | gpt-5.5 | 1235 | 555 | 1790 | 2 | 3 | 2 | 3/3 |  |
| AS_CHAT_FAST | storage-bottleneck | low | yes | 29 | 8275 | gpt-5.5 | 1256 | 535 | 1791 | 2 | 3 | 2 | 3/3 |  |
| AS_CHAT_FAST | power-instability | high | yes | 27 | 11022 | gpt-5.5 | 1249 | 626 | 1875 | 2 | 3 | 2 | 3/3 |  |
| AS_CHAT_FAST | mixed-thermal-driver | high | yes | 5 | 10449 | gpt-5.5 | 1208 | 655 | 1863 | 2 | 3 | 2 | 3/3 |  |
| AS_CHAT_BALANCED | gpu-thermal-frame-drop | medium | yes | 4 | 9982 | gpt-5.5 | 1326 | 843 | 2169 | 3 | 3 | 3 | 3/3 |  |
| AS_CHAT_BALANCED | driver-crash-event-log | medium | yes | 28 | 11234 | gpt-5.5 | 1370 | 808 | 2178 | 3 | 3 | 3 | 3/3 |  |
| AS_CHAT_BALANCED | memory-pressure | low | yes | 14 | 10226 | gpt-5.5 | 1351 | 767 | 2118 | 3 | 3 | 3 | 3/3 |  |
| AS_CHAT_BALANCED | storage-bottleneck | low | yes | 3 | 9433 | gpt-5.5 | 1367 | 691 | 2058 | 3 | 3 | 3 | 3/3 |  |
| AS_CHAT_BALANCED | power-instability | high | yes | 4 | 11975 | gpt-5.5 | 1361 | 916 | 2277 | 3 | 3 | 3 | 3/3 |  |
| AS_CHAT_BALANCED | mixed-thermal-driver | high | yes | 13 | 11072 | gpt-5.5 | 1348 | 900 | 2248 | 3 | 3 | 3 | 3/3 |  |
| AS_CHAT_HIGH_QUALITY | gpu-thermal-frame-drop | medium | yes | 5 | 15497 | gpt-5.5 | 1865 | 1170 | 3035 | 5 | 3 | 3 | 3/3 |  |
| AS_CHAT_HIGH_QUALITY | driver-crash-event-log | medium | yes | 14 | 14608 | gpt-5.5 | 1866 | 1085 | 2951 | 5 | 3 | 3 | 3/3 |  |
| AS_CHAT_HIGH_QUALITY | memory-pressure | low | yes | 15 | 14508 | gpt-5.5 | 1865 | 1019 | 2884 | 5 | 3 | 3 | 3/3 |  |
| AS_CHAT_HIGH_QUALITY | storage-bottleneck | low | yes | 25 | 12356 | gpt-5.5 | 1875 | 928 | 2803 | 5 | 3 | 3 | 3/3 |  |
| AS_CHAT_HIGH_QUALITY | power-instability | high | yes | 3 | 14938 | gpt-5.5 | 1873 | 1170 | 3043 | 5 | 3 | 3 | 3/3 |  |
| AS_CHAT_HIGH_QUALITY | mixed-thermal-driver | high | yes | 4 | 17573 | gpt-5.5 | 1869 | 1213 | 3082 | 5 | 3 | 3 | 3/3 |  |

## Selection Notes

- 기본 profile 후보는 schema valid 100%, 성공률 95% 이상을 먼저 만족해야 한다.
- 첫 진행 이벤트 평균이 1초 이하인 profile을 우선한다.
- 평균 응답 시간이 10초 이하인 profile을 우선한다.
- p95 응답 시간이 20초를 넘으면 사용자 체감상 감점한다.
- 품질 차이가 작으면 더 빠른 profile을 선택한다.

## Current Decision

- `AS_CHAT_FAST`가 successRate 100%, schemaValidRate 100%, avgFirstEventMs 21ms, avgFinalLatencyMs 9272ms, p95FinalLatencyMs 11022ms를 기록했다.
- 사용자 기본 profile은 `AS_CHAT_FAST`로 둔다. 이 profile은 `gpt-5.5`, low reasoning, RAG topK 2, compact prompt, 짧은 출력 제한을 사용한다.
- `AS_CHAT_BALANCED`는 품질 보강 후보로 유지한다. 평균은 10654ms로 FAST보다 느리지만 RAG topK 3과 조치 3개를 제공한다.
- `AS_CHAT_HIGH_QUALITY`는 관리자 검증/고위험 분석 후보로 유지한다. 평균은 14913ms지만 p95 17573ms로 20초 기준 안에 들어온다.
