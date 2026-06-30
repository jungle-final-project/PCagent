# AS Chat AI Profile Benchmark

- generatedAt: 2026-06-30T04:18:39
- totalCases: 30

## Summary

| profile | provider | successRate | avgFirstEventMs | avgFinalLatencyMs | p95FinalLatencyMs | avgInputTokens | avgOutputTokens | avgTokens | schemaValidRate |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|
| AS_CHAT_FAST | openai | 100.0% | 6 | 8332 | 9183 | 1235 | 577 | 1812 | 100.0% |
| AS_CHAT_BALANCED | openai | 100.0% | 12 | 11415 | 13344 | 1344 | 861 | 2205 | 100.0% |
| AS_CHAT_HIGH_QUALITY | openai | 100.0% | 6 | 14377 | 17572 | 1887 | 1148 | 3035 | 100.0% |
| AS_CHAT_GEMINI_FAST | gemini | 0.0% | 0 | 368 | 655 | 0 | 0 | 0 | 0.0% |
| AS_CHAT_GEMINI_BALANCED | gemini | 0.0% | 0 | 303 | 346 | 0 | 0 | 0 | 0.0% |

## Provider Availability

- Gemini profile은 API까지 도달했지만 현재 key가 `HTTP 429 RESOURCE_EXHAUSTED`를 반환해 품질/속도 비교를 완료하지 못했다.

## Cases

| profile | provider | case | risk | ok | firstEventMs | finalLatencyMs | model | inTok | outTok | tokens | evidence | tools | actions | keywords | error |
|---|---|---|---|---:|---:|---:|---|---:|---:|---:|---:|---:|---:|---:|---|
| AS_CHAT_FAST | openai | gpu-thermal-frame-drop | medium | yes | 15 | 9183 | gpt-5.5 | 1228 | 563 | 1791 | 2 | 3 | 2 | 2/3 |  |
| AS_CHAT_FAST | openai | driver-crash-event-log | medium | yes | 5 | 7152 | gpt-5.5 | 1219 | 531 | 1750 | 2 | 3 | 2 | 3/3 |  |
| AS_CHAT_FAST | openai | memory-pressure | low | yes | 5 | 8149 | gpt-5.5 | 1237 | 549 | 1786 | 2 | 3 | 2 | 2/3 |  |
| AS_CHAT_FAST | openai | storage-bottleneck | low | yes | 4 | 8243 | gpt-5.5 | 1237 | 531 | 1768 | 2 | 3 | 2 | 2/3 |  |
| AS_CHAT_FAST | openai | power-instability | high | yes | 4 | 8925 | gpt-5.5 | 1266 | 646 | 1912 | 2 | 3 | 2 | 3/3 |  |
| AS_CHAT_FAST | openai | mixed-thermal-driver | high | yes | 5 | 8338 | gpt-5.5 | 1223 | 641 | 1864 | 2 | 3 | 2 | 3/3 |  |
| AS_CHAT_BALANCED | openai | gpu-thermal-frame-drop | medium | yes | 4 | 13344 | gpt-5.5 | 1345 | 1056 | 2401 | 3 | 3 | 3 | 3/3 |  |
| AS_CHAT_BALANCED | openai | driver-crash-event-log | medium | yes | 28 | 10261 | gpt-5.5 | 1344 | 882 | 2226 | 3 | 3 | 3 | 3/3 |  |
| AS_CHAT_BALANCED | openai | memory-pressure | low | yes | 3 | 12923 | gpt-5.5 | 1338 | 789 | 2127 | 3 | 3 | 3 | 3/3 |  |
| AS_CHAT_BALANCED | openai | storage-bottleneck | low | yes | 27 | 9235 | gpt-5.5 | 1337 | 678 | 2015 | 3 | 3 | 3 | 3/3 |  |
| AS_CHAT_BALANCED | openai | power-instability | high | yes | 4 | 11394 | gpt-5.5 | 1361 | 877 | 2238 | 3 | 3 | 3 | 3/3 |  |
| AS_CHAT_BALANCED | openai | mixed-thermal-driver | high | yes | 4 | 11333 | gpt-5.5 | 1336 | 886 | 2222 | 3 | 3 | 3 | 3/3 |  |
| AS_CHAT_HIGH_QUALITY | openai | gpu-thermal-frame-drop | medium | yes | 4 | 14063 | gpt-5.5 | 1886 | 1171 | 3057 | 5 | 3 | 3 | 3/3 |  |
| AS_CHAT_HIGH_QUALITY | openai | driver-crash-event-log | medium | yes | 3 | 17572 | gpt-5.5 | 1887 | 1368 | 3255 | 5 | 3 | 3 | 3/3 |  |
| AS_CHAT_HIGH_QUALITY | openai | memory-pressure | low | yes | 4 | 13069 | gpt-5.5 | 1914 | 1086 | 3000 | 5 | 3 | 3 | 3/3 |  |
| AS_CHAT_HIGH_QUALITY | openai | storage-bottleneck | low | yes | 16 | 10171 | gpt-5.5 | 1867 | 917 | 2784 | 5 | 3 | 3 | 3/3 |  |
| AS_CHAT_HIGH_QUALITY | openai | power-instability | high | yes | 3 | 17465 | gpt-5.5 | 1875 | 1229 | 3104 | 5 | 3 | 3 | 3/3 |  |
| AS_CHAT_HIGH_QUALITY | openai | mixed-thermal-driver | high | yes | 4 | 13922 | gpt-5.5 | 1891 | 1119 | 3010 | 5 | 3 | 3 | 3/3 |  |
| AS_CHAT_GEMINI_FAST | gemini | gpu-thermal-frame-drop | medium | no | - | 655 | - | - | - | - | 0 | 0 | 0 | 0/3 | POST /api/ai/as-chat/stream failed: {'type': 'ResponseStatusException', 'message': '502 BAD_GATEWAY "Gemini 호출 실패: HTTP 429 RESOURCE_EXHAUSTED"'} |
| AS_CHAT_GEMINI_FAST | gemini | driver-crash-event-log | medium | no | - | 317 | - | - | - | - | 0 | 0 | 0 | 0/3 | POST /api/ai/as-chat/stream failed: {'type': 'ResponseStatusException', 'message': '502 BAD_GATEWAY "Gemini 호출 실패: HTTP 429 RESOURCE_EXHAUSTED"'} |
| AS_CHAT_GEMINI_FAST | gemini | memory-pressure | low | no | - | 267 | - | - | - | - | 0 | 0 | 0 | 0/3 | POST /api/ai/as-chat/stream failed: {'type': 'ResponseStatusException', 'message': '502 BAD_GATEWAY "Gemini 호출 실패: HTTP 429 RESOURCE_EXHAUSTED"'} |
| AS_CHAT_GEMINI_FAST | gemini | storage-bottleneck | low | no | - | 307 | - | - | - | - | 0 | 0 | 0 | 0/3 | POST /api/ai/as-chat/stream failed: {'type': 'ResponseStatusException', 'message': '502 BAD_GATEWAY "Gemini 호출 실패: HTTP 429 RESOURCE_EXHAUSTED"'} |
| AS_CHAT_GEMINI_FAST | gemini | power-instability | high | no | - | 414 | - | - | - | - | 0 | 0 | 0 | 0/3 | POST /api/ai/as-chat/stream failed: {'type': 'ResponseStatusException', 'message': '502 BAD_GATEWAY "Gemini 호출 실패: HTTP 429 RESOURCE_EXHAUSTED"'} |
| AS_CHAT_GEMINI_FAST | gemini | mixed-thermal-driver | high | no | - | 250 | - | - | - | - | 0 | 0 | 0 | 0/3 | POST /api/ai/as-chat/stream failed: {'type': 'ResponseStatusException', 'message': '502 BAD_GATEWAY "Gemini 호출 실패: HTTP 429 RESOURCE_EXHAUSTED"'} |
| AS_CHAT_GEMINI_BALANCED | gemini | gpu-thermal-frame-drop | medium | no | - | 267 | - | - | - | - | 0 | 0 | 0 | 0/3 | POST /api/ai/as-chat/stream failed: {'type': 'ResponseStatusException', 'message': '502 BAD_GATEWAY "Gemini 호출 실패: HTTP 429 RESOURCE_EXHAUSTED"'} |
| AS_CHAT_GEMINI_BALANCED | gemini | driver-crash-event-log | medium | no | - | 345 | - | - | - | - | 0 | 0 | 0 | 0/3 | POST /api/ai/as-chat/stream failed: {'type': 'ResponseStatusException', 'message': '502 BAD_GATEWAY "Gemini 호출 실패: HTTP 429 RESOURCE_EXHAUSTED"'} |
| AS_CHAT_GEMINI_BALANCED | gemini | memory-pressure | low | no | - | 346 | - | - | - | - | 0 | 0 | 0 | 0/3 | POST /api/ai/as-chat/stream failed: {'type': 'ResponseStatusException', 'message': '502 BAD_GATEWAY "Gemini 호출 실패: HTTP 429 RESOURCE_EXHAUSTED"'} |
| AS_CHAT_GEMINI_BALANCED | gemini | storage-bottleneck | low | no | - | 285 | - | - | - | - | 0 | 0 | 0 | 0/3 | POST /api/ai/as-chat/stream failed: {'type': 'ResponseStatusException', 'message': '502 BAD_GATEWAY "Gemini 호출 실패: HTTP 429 RESOURCE_EXHAUSTED"'} |
| AS_CHAT_GEMINI_BALANCED | gemini | power-instability | high | no | - | 310 | - | - | - | - | 0 | 0 | 0 | 0/3 | POST /api/ai/as-chat/stream failed: {'type': 'ResponseStatusException', 'message': '502 BAD_GATEWAY "Gemini 호출 실패: HTTP 429 RESOURCE_EXHAUSTED"'} |
| AS_CHAT_GEMINI_BALANCED | gemini | mixed-thermal-driver | high | no | - | 266 | - | - | - | - | 0 | 0 | 0 | 0/3 | POST /api/ai/as-chat/stream failed: {'type': 'ResponseStatusException', 'message': '502 BAD_GATEWAY "Gemini 호출 실패: HTTP 429 RESOURCE_EXHAUSTED"'} |

## Selection Notes

- 기본 profile 후보는 schema valid 100%, 성공률 95% 이상을 먼저 만족해야 한다.
- 첫 진행 이벤트 평균이 1초 이하인 profile을 우선한다.
- 평균 응답 시간이 10초 이하인 profile을 우선한다.
- p95 응답 시간이 20초를 넘으면 사용자 체감상 감점한다.
- 품질 차이가 작으면 더 빠른 profile을 선택한다.
