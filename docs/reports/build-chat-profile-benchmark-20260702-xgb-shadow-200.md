# Build Chat AI Profile Benchmark

- generatedAt: 2026-07-02T23:18:56
- totalCases: 200

## Summary

- slowThresholdMs: 10000

| variant | profile | successRate | avgLatencyMs | p95LatencyMs | maxLatencyMs | slowCases | slowOkRate | schemaValidRate | directionOkRate | categoryOkRate | actionPayloadOkRate | requiredTermsOkRate |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | 29.0% | 3182 | 4826 | 13435 | 1 | 99.5% | 100.0% | 64.5% | 64.5% | 47.5% | 99.5% |

## XGBoost Shadow Scoring

- queryStatus: OK
- startedAt: 2026-07-02T22:08:19.962925+00:00
- shadowScoreRows: 231
- distinctModelVersions: 1

| modelName | modelVersion | rows |
|---|---|---:|
| xgboost-reranker | xgb-shadow-smoke-20260703 | 231 |

## Worst Latency Cases

| variant | profile | case | repeat | latencyMs | ok | slow |
|---|---|---|---:|---:|---:|---:|
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | hard-9950x3d-build | 1 | 13435 | yes | yes |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-motherboard-cheaper-1 | 1 | 7895 | no | no |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | storage-heavy | 1 | 7588 | yes | no |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | premium-with-budget | 1 | 6681 | no | no |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | rtx-5090-under-budget | 1 | 5797 | no | no |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | hard-noctua-cooler | 1 | 5548 | no | no |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | hard-pcie5-ssd | 1 | 5519 | no | no |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | hard-ram-64-kit | 1 | 5266 | no | no |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | route-parts-page-msi-board | 1 | 5185 | no | no |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-storage-similar_price-1 | 1 | 5136 | no | no |

## Cases

| variant | profile | case | repeat | ok | latencyMs | answerType | builds | actions | hardConstraint | categoryOk | directionOk | forbiddenOk | actionPayloadOk | requiredTermsOk | warningOk | error |
|---|---|---|---:|---:|---:|---|---:|---|---:|---:|---:|---:|---:|---:|---:|---|
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | qhd-gaming-budget | 1 | no | 3550 | BUDGET | 2 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | rtx-5090-hard-constraint | 1 | yes | 3717 | BUDGET | 3 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | rtx-5090-under-budget | 1 | no | 5797 | BUDGET | 1 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | open-budget-enthusiast | 1 | yes | 3049 | BUDGET | 3 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | premium-with-budget | 1 | no | 6681 | BUDGET | 0 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | fhd-valorant | 1 | yes | 2804 | BUDGET | 3 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | qhd-pubg-144 | 1 | yes | 2950 | BUDGET | 3 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | 4k-cyberpunk | 1 | yes | 3657 | BUDGET | 3 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | lostark-cpu-focus | 1 | no | 3790 | PART | 0 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | game-dev-mixed | 1 | no | 3350 | GENERAL | 0 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | video-edit | 1 | yes | 2739 | BUDGET | 3 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | local-ai-cuda | 1 | yes | 4826 | BUDGET | 3 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | blender-3d | 1 | yes | 3775 | BUDGET | 3 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | quiet-night | 1 | no | 3764 | BUDGET | 2 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | upgrade-headroom | 1 | no | 3031 | BUDGET | 2 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | nvidia-preference | 1 | no | 2762 | PART | 0 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | intel-preference | 1 | no | 2355 | BUDGET | 2 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | white-build | 1 | yes | 2941 | BUDGET | 3 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | compact-case | 1 | yes | 3209 | BUDGET | 3 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | storage-heavy | 1 | yes | 7588 | BUDGET | 3 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | rtx-5070-part | 1 | no | 3434 | PART | 0 | - | yes | yes | yes | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | cpu-part-dev | 1 | no | 2997 | PART | 0 | - | yes | yes | yes | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | psu-part-5090 | 1 | no | 2984 | BUDGET | 0 | - | yes | yes | yes | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | case-part-airflow | 1 | no | 3090 | PART | 0 | - | yes | yes | yes | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | cooler-part-quiet | 1 | no | 3224 | PART | 0 | - | yes | yes | yes | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | ssd-part-fast | 1 | no | 3294 | PART | 0 | - | yes | yes | yes | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | ram-part-64 | 1 | no | 3495 | PART | 0 | - | yes | yes | yes | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | motherboard-part-am5 | 1 | no | 4103 | PART | 0 | - | yes | yes | yes | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | cheaper-gpu-draft-edit | 1 | no | 3587 | PART | 0 | ASK_FOLLOW_UP | yes | yes | yes | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | ram-64gb-draft-edit | 1 | yes | 3312 | PART | 0 | UPDATE_DRAFT_QUANTITY | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | remove-gpu-draft | 1 | yes | 2585 | PART | 0 | REMOVE_DRAFT_PART | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | lower-budget-draft | 1 | no | 3735 | PART | 0 | ASK_FOLLOW_UP | yes | yes | yes | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | replace-cooler-draft | 1 | no | 3706 | PART | 0 | - | yes | yes | yes | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | add-storage-draft | 1 | yes | 2861 | PART | 0 | ADD_PART_TO_DRAFT | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | price-alert-gpu | 1 | no | 3194 | GENERAL | 0 | - | yes | yes | yes | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | explain-build | 1 | yes | 3358 | GENERAL | 0 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | ask-followup-vague | 1 | no | 2772 | BUDGET | 2 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | ask-followup-monitor | 1 | yes | 2916 | GENERAL | 0 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | budget-office | 1 | yes | 3172 | BUDGET | 3 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | creator-ram-heavy | 1 | no | 3151 | PART | 0 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | power-safe-build | 1 | no | 2797 | PART | 0 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | cheap-gpu-no-draft | 1 | no | 4488 | PART | 0 | - | yes | yes | yes | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | live-direction-gpu-more | 1 | no | 2868 | PART | 0 | - | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | live-direction-gpu-cheap | 1 | no | 2702 | PART | 0 | ASK_FOLLOW_UP | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | live-direction-gpu-similar | 1 | no | 3410 | PART | 0 | - | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | live-direction-cpu-more | 1 | no | 3144 | PART | 0 | - | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | live-direction-cpu-cheap | 1 | no | 2493 | PART | 0 | ASK_FOLLOW_UP | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | live-direction-cpu-similar | 1 | no | 3535 | PART | 0 | - | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | live-direction-motherboard-more | 1 | no | 3015 | PART | 0 | - | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | live-direction-motherboard-cheap | 1 | no | 3640 | PART | 0 | ASK_FOLLOW_UP | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | live-direction-motherboard-similar | 1 | no | 3407 | PART | 0 | - | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | live-direction-ram-more | 1 | no | 2986 | PART | 0 | - | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | live-direction-ram-cheap | 1 | no | 2863 | PART | 0 | ASK_FOLLOW_UP | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | live-direction-ram-similar | 1 | no | 3337 | PART | 0 | - | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | live-direction-storage-more | 1 | no | 2916 | PART | 0 | - | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | live-direction-storage-cheap | 1 | no | 2632 | PART | 0 | ASK_FOLLOW_UP | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | live-direction-storage-similar | 1 | no | 3517 | PART | 0 | - | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | live-direction-psu-more | 1 | yes | 3338 | PART | 0 | REPLACE_DRAFT_PART | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | live-direction-psu-cheap | 1 | yes | 2908 | PART | 0 | REPLACE_DRAFT_PART | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | live-direction-psu-similar | 1 | yes | 2905 | PART | 0 | REPLACE_DRAFT_PART | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | live-direction-case-more | 1 | no | 2947 | PART | 0 | - | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | live-direction-case-cheap | 1 | no | 2477 | PART | 0 | ASK_FOLLOW_UP | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | live-direction-case-similar | 1 | no | 3416 | PART | 0 | - | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | live-direction-cooler-more | 1 | no | 3301 | PART | 0 | - | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | live-direction-cooler-cheap | 1 | no | 2920 | PART | 0 | ASK_FOLLOW_UP | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | live-direction-cooler-similar | 1 | no | 2777 | PART | 0 | - | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | budget-300-qhd-balanced | 1 | no | 2672 | BUDGET | 2 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | budget-180-fhd-esports | 1 | no | 3149 | BUDGET | 1 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | budget-350-4k-gaming | 1 | no | 2996 | BUDGET | 2 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | budget-open-creator | 1 | yes | 2830 | BUDGET | 3 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | budget-220-quiet-gaming | 1 | no | 2714 | BUDGET | 2 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | budget-260-white-nvidia | 1 | no | 3506 | BUDGET | 1 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | budget-150-office-dev | 1 | no | 2521 | BUDGET | 2 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | budget-500-enthusiast | 1 | yes | 2812 | BUDGET | 3 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | budget-320-ai-cuda | 1 | no | 3202 | BUDGET | 2 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | budget-240-pubg-stream | 1 | no | 3020 | BUDGET | 2 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | budget-200-low-noise | 1 | no | 2774 | BUDGET | 2 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | budget-280-upgrade-headroom | 1 | no | 3019 | BUDGET | 2 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | budget-450-rendering | 1 | no | 4097 | BUDGET | 0 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | budget-170-lostark | 1 | no | 4388 | BUDGET | 1 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | budget-230-storage-heavy | 1 | no | 3095 | BUDGET | 1 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | budget-open-no-limit | 1 | yes | 2665 | BUDGET | 3 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | budget-210-amd-prefer | 1 | no | 2698 | BUDGET | 0 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | budget-260-intel-prefer | 1 | no | 3062 | BUDGET | 1 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | budget-190-compact | 1 | no | 3098 | BUDGET | 2 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | budget-330-quiet-creator | 1 | no | 2799 | BUDGET | 1 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | hard-5090-korean | 1 | yes | 3279 | BUDGET | 3 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | hard-5090-budget-warning | 1 | no | 4240 | BUDGET | 1 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | hard-5090-white | 1 | yes | 2819 | BUDGET | 3 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | hard-5090-cuda | 1 | yes | 2843 | BUDGET | 3 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | hard-5080-gpu-part | 1 | no | 2834 | PART | 0 | - | yes | yes | yes | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | hard-5070ti-gpu-part | 1 | no | 3368 | PART | 0 | - | yes | yes | yes | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | hard-9950x3d-build | 1 | yes | 13435 | BUDGET | 3 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | hard-9700x-build | 1 | no | 3222 | PART | 0 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | hard-9950x3d-detail | 1 | yes | 2987 | GENERAL | 0 | OPEN_ROUTE | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | hard-ram-32-single | 1 | no | 3082 | PART | 0 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | hard-ram-64-kit | 1 | no | 5266 | PART | 0 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | hard-ddr5-only | 1 | no | 2950 | BUDGET | 2 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | hard-msi-board | 1 | no | 4101 | PART | 0 | - | yes | yes | yes | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | hard-lianli-216-case | 1 | no | 3414 | PART | 0 | - | yes | yes | yes | yes | no | no | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | hard-asus-gpu | 1 | no | 3167 | PART | 0 | - | yes | yes | yes | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | hard-samsung-ssd | 1 | no | 3609 | PART | 0 | - | yes | yes | yes | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | hard-corsair-psu | 1 | no | 3706 | PART | 0 | - | yes | yes | yes | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | hard-noctua-cooler | 1 | no | 5548 | PART | 0 | - | yes | yes | yes | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | hard-360-aio-cooler | 1 | no | 3151 | PART | 0 | - | yes | yes | yes | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | hard-2tb-ssd | 1 | no | 4231 | PART | 0 | - | yes | yes | yes | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | hard-1000w-psu | 1 | no | 3808 | PART | 0 | - | yes | yes | yes | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | hard-atx-board | 1 | no | 3114 | PART | 0 | - | yes | yes | yes | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | hard-wifi-board | 1 | no | 2875 | PART | 0 | - | yes | yes | yes | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | hard-big-gpu-case | 1 | no | 3120 | PART | 0 | - | yes | yes | yes | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | hard-airflow-case | 1 | no | 4669 | PART | 0 | - | yes | yes | yes | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | hard-pcie5-ssd | 1 | no | 5519 | PART | 0 | - | yes | yes | yes | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | hard-gold-psu | 1 | no | 4057 | PART | 0 | - | yes | yes | yes | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | hard-am5-board | 1 | no | 3023 | PART | 0 | - | yes | yes | yes | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | hard-intel-ultra | 1 | no | 3045 | PART | 0 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | hard-nvidia-only | 1 | no | 3972 | PART | 0 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-gpu-more_expensive-1 | 1 | no | 2965 | PART | 0 | - | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-gpu-more_expensive-2 | 1 | no | 3664 | PART | 0 | - | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-gpu-more_expensive-3 | 1 | no | 3040 | PART | 0 | - | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-gpu-cheaper-1 | 1 | no | 3065 | PART | 0 | ASK_FOLLOW_UP | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-gpu-cheaper-2 | 1 | no | 2774 | PART | 0 | ASK_FOLLOW_UP | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-gpu-similar_price-1 | 1 | no | 3909 | PART | 0 | - | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-gpu-similar_price-2 | 1 | no | 4493 | PART | 0 | - | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-cpu-more_expensive-1 | 1 | no | 2774 | PART | 0 | - | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-cpu-more_expensive-2 | 1 | no | 3097 | PART | 0 | - | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-cpu-more_expensive-3 | 1 | no | 3836 | PART | 0 | - | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-cpu-cheaper-1 | 1 | no | 2483 | PART | 0 | ASK_FOLLOW_UP | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-cpu-cheaper-2 | 1 | no | 3066 | PART | 0 | ASK_FOLLOW_UP | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-cpu-similar_price-1 | 1 | no | 3521 | PART | 0 | - | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-cpu-similar_price-2 | 1 | no | 3345 | PART | 0 | - | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-motherboard-more_expensive-1 | 1 | no | 3389 | PART | 0 | - | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-motherboard-more_expensive-2 | 1 | no | 2943 | PART | 0 | - | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-motherboard-more_expensive-3 | 1 | no | 3113 | PART | 0 | - | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-motherboard-cheaper-1 | 1 | no | 7895 | PART | 0 | ASK_FOLLOW_UP | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-motherboard-cheaper-2 | 1 | no | 3236 | PART | 0 | ASK_FOLLOW_UP | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-motherboard-similar_price-1 | 1 | no | 3321 | PART | 0 | - | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-motherboard-similar_price-2 | 1 | no | 3136 | PART | 0 | - | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-ram-more_expensive-1 | 1 | no | 3064 | PART | 0 | - | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-ram-more_expensive-2 | 1 | no | 2903 | PART | 0 | - | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-ram-more_expensive-3 | 1 | no | 3223 | PART | 0 | - | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-ram-cheaper-1 | 1 | no | 2978 | PART | 0 | ASK_FOLLOW_UP | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-ram-cheaper-2 | 1 | no | 2585 | PART | 0 | ASK_FOLLOW_UP | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-ram-similar_price-1 | 1 | no | 3793 | PART | 0 | - | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-ram-similar_price-2 | 1 | no | 3631 | PART | 0 | - | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-storage-more_expensive-1 | 1 | no | 2942 | PART | 0 | - | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-storage-more_expensive-2 | 1 | no | 3608 | PART | 0 | - | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-storage-more_expensive-3 | 1 | no | 3217 | PART | 0 | - | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-storage-cheaper-1 | 1 | no | 2945 | PART | 0 | ASK_FOLLOW_UP | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-storage-cheaper-2 | 1 | no | 2625 | PART | 0 | ASK_FOLLOW_UP | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-storage-similar_price-1 | 1 | no | 5136 | PART | 0 | - | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-storage-similar_price-2 | 1 | no | 3237 | PART | 0 | - | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-psu-more_expensive-1 | 1 | yes | 2581 | PART | 0 | REPLACE_DRAFT_PART | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-psu-more_expensive-2 | 1 | yes | 3261 | PART | 0 | REPLACE_DRAFT_PART | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-psu-more_expensive-3 | 1 | yes | 3683 | PART | 0 | REPLACE_DRAFT_PART | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-psu-cheaper-1 | 1 | yes | 2641 | PART | 0 | REPLACE_DRAFT_PART | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-psu-cheaper-2 | 1 | yes | 3222 | PART | 0 | REPLACE_DRAFT_PART | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-psu-similar_price-1 | 1 | yes | 3517 | PART | 0 | REPLACE_DRAFT_PART | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-psu-similar_price-2 | 1 | yes | 3925 | PART | 0 | REPLACE_DRAFT_PART | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-case-more_expensive-1 | 1 | no | 3267 | PART | 0 | - | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-case-more_expensive-2 | 1 | no | 3676 | PART | 0 | - | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-case-more_expensive-3 | 1 | no | 3433 | PART | 0 | - | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-case-cheaper-1 | 1 | no | 3295 | PART | 0 | ASK_FOLLOW_UP | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-case-cheaper-2 | 1 | no | 2866 | PART | 0 | ASK_FOLLOW_UP | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-case-similar_price-1 | 1 | no | 2957 | PART | 0 | - | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-case-similar_price-2 | 1 | no | 3307 | PART | 0 | - | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-cooler-more_expensive-1 | 1 | no | 2817 | PART | 0 | - | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-cooler-more_expensive-2 | 1 | no | 3114 | PART | 0 | - | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-cooler-more_expensive-3 | 1 | no | 2848 | PART | 0 | - | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-cooler-cheaper-1 | 1 | no | 2427 | PART | 0 | ASK_FOLLOW_UP | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-cooler-cheaper-2 | 1 | no | 3530 | PART | 0 | ASK_FOLLOW_UP | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-cooler-similar_price-1 | 1 | no | 3369 | PART | 0 | - | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | direction-200-cooler-similar_price-2 | 1 | no | 4441 | PART | 0 | - | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | route-gpu-category | 1 | yes | 5 | GENERAL | 0 | OPEN_ROUTE | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | route-cpu-category | 1 | yes | 17 | GENERAL | 0 | OPEN_ROUTE | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | route-mainboard-category | 1 | yes | 8 | GENERAL | 0 | OPEN_ROUTE | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | route-ram-category | 1 | yes | 3 | GENERAL | 0 | OPEN_ROUTE | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | route-ssd-category | 1 | yes | 4 | GENERAL | 0 | OPEN_ROUTE | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | route-psu-category | 1 | yes | 3 | GENERAL | 0 | OPEN_ROUTE | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | route-case-category | 1 | yes | 3 | GENERAL | 0 | OPEN_ROUTE | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | route-cooler-category | 1 | yes | 3 | GENERAL | 0 | OPEN_ROUTE | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | route-self-quote | 1 | yes | 23 | GENERAL | 0 | OPEN_ROUTE | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | route-my-quotes | 1 | yes | 15 | GENERAL | 0 | OPEN_ROUTE | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | route-ai-quote | 1 | yes | 3 | GENERAL | 0 | OPEN_ROUTE | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | route-support-new | 1 | yes | 12 | GENERAL | 0 | OPEN_ROUTE | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | route-support-ai-chat | 1 | yes | 12 | GENERAL | 0 | OPEN_ROUTE | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | route-checkout | 1 | yes | 18 | GENERAL | 0 | OPEN_ROUTE | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | route-parts-page-5090-ambiguous | 1 | yes | 3760 | GENERAL | 0 | OPEN_ROUTE | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | route-parts-page-9950x3d | 1 | yes | 2730 | GENERAL | 0 | OPEN_ROUTE | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | route-parts-page-lianli216 | 1 | yes | 2778 | GENERAL | 0 | OPEN_ROUTE | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | route-parts-page-msi-board | 1 | no | 5185 | GENERAL | 0 | - | yes | yes | yes | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | route-not-recommend-gpu | 1 | no | 2685 | PART | 0 | - | yes | yes | yes | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | route-not-change-ram | 1 | yes | 2747 | PART | 0 | UPDATE_DRAFT_QUANTITY | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | general-price-alert-5090 | 1 | no | 3712 | GENERAL | 0 | - | yes | yes | yes | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | general-explain-why | 1 | yes | 2650 | GENERAL | 0 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | general-compare-gpu | 1 | yes | 3210 | GENERAL | 0 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | general-vague-use | 1 | yes | 4593 | GENERAL | 0 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | general-cart-add-ambiguous | 1 | yes | 3570 | GENERAL | 0 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | general-budget-or-route | 1 | no | 2817 | BUDGET | 2 | - | yes | yes | yes | yes | yes | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | general-cheap-but-safe | 1 | no | 3930 | PART | 0 | ASK_FOLLOW_UP | yes | no | no | yes | no | yes | yes |  |
| xgb-shadow-200 | BUILD_CHAT_54_MINI_FAST | general-low-noise-explain | 1 | yes | 3193 | GENERAL | 0 | - | yes | yes | yes | yes | yes | yes | yes |  |

## Notes

- 이 벤치마크는 UI를 변경하지 않고 `/api/ai/build-chat`의 optional profile header만 바꿔 실행한다.
- 기본 서비스 profile은 현재 `BUILD_CHAT_54_MINI_FAST`이며, 모델 비교가 필요하면 `--profiles`로 후보를 명시한다.
- 5090 같은 명시 부품 조건은 추천 build의 GPU item에 보존되어야 한다.
- 장바구니 교체 케이스는 반환 partId를 `/api/parts/{id}`로 다시 조회해 현재 부품 대비 상향/하향/유사 가격 방향을 검증한다.
