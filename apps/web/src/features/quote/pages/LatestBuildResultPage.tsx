import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { X } from 'lucide-react';
import { Link } from 'react-router-dom';
import { Panel, Screen, StateMessage, StatusBadge } from '../../../components/ui';
import { latestUserMessage, temporaryBuildToBuildSummary } from '../components/BuildDetailSections';
import {
  AI_ASSISTANT_BUILD_HISTORY_LIMIT,
  markAssistantBuildSaved,
  readAssistantSession,
  type AiBuildTier,
  type AiRecommendedBuild
} from '../aiSelection';
import { saveBuildFromChat } from '../quoteApi';

type RecommendationFilter = 'all' | AiBuildTier;

export function LatestBuildResultPage() {
  const assistantSession = readAssistantSession();
  const builds = assistantSession.latestBuilds;
  const [selectedBuildId, setSelectedBuildId] = useState<string | null>(null);
  const [tierFilter, setTierFilter] = useState<RecommendationFilter>('all');
  const [savedBuildIds, setSavedBuildIds] = useState(assistantSession.savedBuildIds);
  const visibleBuilds = useMemo(
    () => tierFilter === 'all' ? builds : builds.filter((build) => build.tier === tierFilter),
    [builds, tierFilter]
  );
  const selectedBuild = selectedBuildId && visibleBuilds.some((build) => build.id === selectedBuildId)
    ? builds.find((build) => build.id === selectedBuildId)
    : undefined;
  const selectedSavedBuildId = selectedBuild ? savedBuildIds[selectedBuild.id] : undefined;
  const lastUserMessage = latestUserMessage(assistantSession);
  const closeDetail = useCallback(() => setSelectedBuildId(null), []);
  const saveMutation = useMutation({
    mutationFn: (build: AiRecommendedBuild) => saveBuildFromChat({
      sourceBuildId: build.id,
      lastUserMessage,
      build
    }),
    onSuccess: (response, sourceBuild) => {
      markAssistantBuildSaved(sourceBuild.id, response.id);
      setSavedBuildIds((current) => ({ ...current, [sourceBuild.id]: response.id }));
    }
  });

  useEffect(() => {
    if (selectedBuildId && !visibleBuilds.some((build) => build.id === selectedBuildId)) {
      setSelectedBuildId(null);
    }
  }, [selectedBuildId, visibleBuilds]);

  return (
    <Screen>
      <div
        data-testid="latest-build-results-layout"
        className="space-y-5"
      >
        <Panel
          title="추천 결과"
          subtitle="AI 챗봇이 방금 제안한 임시 추천 조합입니다. 저장 전까지 내 견적함에는 추가되지 않습니다."
        >
          {builds.length > 0 ? (
            <div className="space-y-4">
              <div className="flex flex-col gap-3 rounded-md border border-blue-100 bg-blue-50 px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
                <p className="text-sm font-bold text-brand-blue">
                  최근 AI 추천 조합을 최대 {AI_ASSISTANT_BUILD_HISTORY_LIMIT}개까지 보관합니다. 현재 {builds.length}/{AI_ASSISTANT_BUILD_HISTORY_LIMIT}개
                </p>
                <span className="text-xs font-semibold text-slate-500">최신 추천이 앞에 표시됩니다.</span>
              </div>
              <RecommendationFilterTabs value={tierFilter} onChange={setTierFilter} />
              {visibleBuilds.length > 0 ? (
                <div
                  data-testid="latest-build-card-grid"
                  className={`grid gap-4 ${selectedBuild ? 'lg:grid-cols-2' : 'lg:grid-cols-3'}`}
                >
                  {visibleBuilds.map((build) => (
                    <TemporaryBuildCard
                      key={build.id}
                      build={build}
                      selected={build.id === selectedBuildId}
                      savedBuildId={savedBuildIds[build.id]}
                      onSelect={() => setSelectedBuildId(build.id)}
                    />
                  ))}
                </div>
              ) : (
                <StateMessage
                  type="info"
                  title="선택한 필터에 해당하는 추천 조합이 없습니다."
                  body="전체 필터로 돌아가면 최근 AI 추천 조합을 모두 확인할 수 있습니다."
                />
              )}
            </div>
          ) : (
            <div className="space-y-4">
              <StateMessage
                type="info"
                title="AI 챗봇에게 먼저 추천을 받아보세요"
                body="홈 AI 챗봇에서 예산이나 용도를 말하면 추천 결과가 이곳에 임시로 표시됩니다."
              />
              <Link to="/" className="inline-flex min-h-10 items-center justify-center rounded-md bg-brand-blue px-4 text-sm font-bold text-white hover:bg-blue-700 focus:outline-none focus:ring-4 focus:ring-blue-100">
                홈에서 AI 챗봇 열기
              </Link>
            </div>
          )}
        </Panel>
        {selectedBuild ? (
          <LatestBuildDetailDrawer
            build={selectedBuild}
            savedBuildId={selectedSavedBuildId}
            isSaving={saveMutation.isPending && saveMutation.variables?.id === selectedBuild.id}
            saveError={saveMutation.isError && saveMutation.variables?.id === selectedBuild.id}
            onSave={() => saveMutation.mutate(selectedBuild)}
            onClose={closeDetail}
          />
        ) : null}
      </div>
    </Screen>
  );
}

function LatestBuildDetailDrawer({
  build,
  savedBuildId,
  isSaving,
  saveError,
  onSave,
  onClose
}: {
  build: AiRecommendedBuild;
  savedBuildId?: string;
  isSaving: boolean;
  saveError: boolean;
  onSave: () => void;
  onClose: () => void;
}) {
  const desktopPanelRef = useRef<HTMLElement | null>(null);
  const displayBuild = temporaryBuildToBuildSummary(build);

  useEffect(() => {
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') {
        onClose();
      }
    }
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [onClose]);

  useEffect(() => {
    function handlePointerDown(event: PointerEvent) {
      const target = event.target;
      if (target instanceof Element && target.closest('[data-latest-build-card], [data-latest-build-filter]')) return;
      if (target instanceof Node && desktopPanelRef.current && !desktopPanelRef.current.contains(target)) {
        onClose();
      }
    }
    document.addEventListener('pointerdown', handlePointerDown);
    return () => document.removeEventListener('pointerdown', handlePointerDown);
  }, [onClose]);

  return (
    <>
      <section
        ref={desktopPanelRef}
        role="dialog"
        aria-modal="false"
        aria-label="추천 조합 상세"
        data-testid="latest-build-detail-drawer"
        className="fixed right-0 top-0 z-40 flex h-dvh w-full max-w-[520px] flex-col border-l border-commerce-line bg-white shadow-2xl"
      >
        <LatestBuildDetailPanelContent
          build={build}
          displayBuild={displayBuild}
          savedBuildId={savedBuildId}
          isSaving={isSaving}
          saveError={saveError}
          onSave={onSave}
          onClose={onClose}
        />
      </section>
    </>
  );
}

function LatestBuildDetailPanelContent({
  build,
  displayBuild,
  savedBuildId,
  isSaving,
  saveError,
  onSave,
  onClose
}: {
  build: AiRecommendedBuild;
  displayBuild: ReturnType<typeof temporaryBuildToBuildSummary>;
  savedBuildId?: string;
  isSaving: boolean;
  saveError: boolean;
  onSave: () => void;
  onClose: () => void;
}) {
  const toolResults = displayBuild.toolResults ?? [];
  const passCount = toolResults.filter((row) => row.status === 'PASS').length;

  return (
    <div className="flex h-full min-h-0 flex-col">
      <header className="flex items-start justify-between gap-4 border-b border-commerce-line px-5 py-4">
        <div className="min-w-0">
          <div className="text-xs font-black text-brand-blue">추천 조합 상세</div>
          <h2 className="mt-1 break-keep text-lg font-black leading-7 text-commerce-ink">
            선택한 추천 조합 / {build.title}
          </h2>
          <p className="mt-1 text-xs font-semibold leading-5 text-slate-500">
            구성 부품, Tool 검증 결과, 저장 액션을 같은 자리에서 확인합니다.
          </p>
        </div>
        <button
          type="button"
          aria-label="추천 조합 상세 닫기"
          onClick={onClose}
          className="grid h-9 w-9 shrink-0 place-items-center rounded-md border border-commerce-line bg-white text-slate-500 hover:border-slate-300 hover:text-commerce-ink focus:outline-none focus:ring-4 focus:ring-blue-100"
        >
          <X size={18} aria-hidden="true" />
        </button>
      </header>

      <div className="min-h-0 flex-1 space-y-4 overflow-y-auto px-5 py-4">
        <div className="rounded-md border border-blue-100 bg-blue-50 px-4 py-3 text-sm font-bold text-brand-blue">
          저장 전 AI 챗봇 추천
        </div>

        <section className="rounded-md border border-commerce-line bg-white">
          <div className="border-b border-commerce-line px-4 py-3">
            <h3 className="text-sm font-black text-commerce-ink">구성 부품</h3>
          </div>
          <div className="divide-y divide-commerce-line">
            {displayBuild.items.map((item) => (
              <div key={`${displayBuild.id}-${item.category}`} className="px-4 py-3">
                <div className="flex items-start justify-between gap-3">
                  <span className="text-xs font-black text-slate-500">{labelForCategory(item.category)}</span>
                  <span className="whitespace-nowrap text-sm font-black text-brand-blue">{item.price.toLocaleString()}원</span>
                </div>
                <div className="mt-1 text-sm font-black leading-5 text-commerce-ink">
                  {item.partId ? (
                    <Link to={`/parts/${item.partId}`} className="hover:text-brand-blue hover:underline" title={item.name}>{item.name}</Link>
                  ) : (
                    item.name
                  )}
                </div>
                <div className="mt-1 text-xs font-semibold text-slate-500">{item.manufacturer ?? '-'}</div>
              </div>
            ))}
          </div>
        </section>

        <section className="rounded-md border border-commerce-line bg-white">
          <div className="border-b border-commerce-line px-4 py-3">
            <h3 className="text-sm font-black text-commerce-ink">Tool 검증 결과</h3>
          </div>
          <div className="space-y-3 p-4">
            {toolResults.length > 0 ? (
              <>
                <div className={`rounded-md border px-3 py-2 text-xs font-black ${
                  passCount === toolResults.length
                    ? 'border-emerald-100 bg-emerald-50 text-emerald-700'
                    : 'border-amber-100 bg-amber-50 text-amber-700'
                }`}
                >
                  {passCount === toolResults.length
                    ? `${toolResults.length}개 검증 모두 통과`
                    : `${toolResults.length}개 검증 중 ${passCount}개 통과 · 경고 항목을 확인하세요`}
                </div>
                <div className="space-y-2">
                  {toolResults.map((row) => (
                    <div key={`${row.tool}-${row.summary}`} className="rounded-md border border-slate-100 bg-slate-50 px-3 py-2">
                      <div className="flex items-center justify-between gap-2">
                        <span className="text-xs font-black text-slate-600">{toolDisplayLabel(row.tool)}</span>
                        <span className="flex items-center gap-1">
                          <StatusBadge status={row.status} />
                          <StatusBadge status={row.confidence} />
                        </span>
                      </div>
                      <p className="mt-2 text-xs leading-5 text-slate-600">{row.summary}</p>
                    </div>
                  ))}
                </div>
              </>
            ) : (
              <StateMessage type="info" title="검증 결과 없음" body="저장 버튼을 누르면 서버에서 다시 Tool 검증 후 견적으로 저장합니다." />
            )}
          </div>
        </section>

        <section className="rounded-md border border-commerce-line bg-white">
          <div className="border-b border-commerce-line px-4 py-3">
            <h3 className="text-sm font-black text-commerce-ink">견적 요약 / 액션</h3>
          </div>
          <div className="space-y-4 p-4">
            <div className="rounded-md border border-blue-100 bg-blue-50 px-4 py-3">
              <div className="text-xs font-black text-slate-500">총액</div>
              <div className="mt-1 text-2xl font-black text-brand-blue">{displayBuild.totalPrice.toLocaleString()}원</div>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className="rounded-md border border-commerce-line px-3 py-2">
                <div className="text-xs font-black text-slate-500">부품 수</div>
                <div className="mt-1 text-lg font-black text-commerce-ink">{displayBuild.items.length}개</div>
              </div>
              <div className="rounded-md border border-commerce-line px-3 py-2">
                <div className="text-xs font-black text-slate-500">경고</div>
                <div className="mt-1 text-lg font-black text-commerce-ink">{displayBuild.warnings.length > 0 ? `${displayBuild.warnings.length}건` : '없음'}</div>
              </div>
            </div>
            <StateMessage
              type={displayBuild.warnings.length > 0 ? 'warn' : 'success'}
              title={displayBuild.warnings.length > 0 ? '확인 필요' : '주요 조건 충족'}
              body={displayBuild.warnings[0]?.message ?? '저장 버튼을 누르면 서버에서 다시 Tool 검증 후 견적으로 저장합니다.'}
            />
            {savedBuildId ? (
              <div className="space-y-2">
                <StateMessage type="success" title="내 견적함에 저장되었습니다." body="내 견적함에서 저장된 견적을 다시 확인할 수 있습니다." />
                <button
                  type="button"
                  disabled
                  className="block w-full rounded bg-emerald-50 px-4 py-3 text-center text-sm font-bold text-emerald-700"
                >
                  저장됨
                </button>
                <Link to="/my/quotes" className="block rounded bg-brand-blue px-4 py-3 text-center text-sm font-bold text-white hover:bg-blue-700">내 견적함 보기</Link>
              </div>
            ) : (
              <div className="space-y-2">
                <button
                  type="button"
                  onClick={onSave}
                  disabled={isSaving}
                  className="block w-full rounded bg-brand-blue px-4 py-3 text-center text-sm font-bold text-white hover:bg-blue-700 disabled:cursor-wait disabled:bg-slate-400"
                >
                  {isSaving ? '저장 중' : '견적 저장'}
                </button>
                {saveError ? (
                  <StateMessage type="warn" title="견적 저장 실패" body="AI 챗봇 추천 견적을 저장하지 못했습니다. 잠시 후 다시 시도해 주세요." />
                ) : null}
              </div>
            )}
          </div>
        </section>
      </div>
    </div>
  );
}

function RecommendationFilterTabs({
  value,
  onChange
}: {
  value: RecommendationFilter;
  onChange: (value: RecommendationFilter) => void;
}) {
  const filters: Array<{ value: RecommendationFilter; label: string }> = [
    { value: 'all', label: '전체' },
    { value: 'budget', label: '실속형' },
    { value: 'balanced', label: '균형형' },
    { value: 'performance', label: '성능형' }
  ];

  return (
    <div role="group" aria-label="추천 조합 필터" data-latest-build-filter="true" className="flex flex-wrap gap-2">
      {filters.map((filter) => {
        const active = value === filter.value;
        return (
          <button
            key={filter.value}
            type="button"
            aria-pressed={active}
            onClick={() => onChange(filter.value)}
            className={`min-h-9 rounded-md border px-3 text-sm font-bold transition focus:outline-none focus:ring-4 focus:ring-blue-100 ${
              active
                ? 'border-brand-blue bg-brand-blue text-white shadow-product'
                : 'border-slate-200 bg-white text-slate-600 hover:border-blue-200 hover:text-brand-blue'
            }`}
          >
            {filter.label}
          </button>
        );
      })}
    </div>
  );
}

function TemporaryBuildCard({
  build,
  savedBuildId,
  selected,
  onSelect
}: {
  build: AiRecommendedBuild;
  savedBuildId?: string;
  selected: boolean;
  onSelect: () => void;
}) {
  const primaryWarning = build.warnings?.[0];
  const mainItems = build.items.slice(0, 5);

  return (
    <article data-latest-build-card="true" className={`rounded-lg border bg-white p-4 shadow-product transition ${selected ? 'border-brand-blue ring-2 ring-blue-100' : 'border-slate-200 hover:border-blue-200'}`}>
      <button
        type="button"
        onClick={onSelect}
        aria-pressed={selected}
        className="block w-full text-left focus:outline-none focus:ring-4 focus:ring-blue-100"
      >
        <div className="rounded border border-blue-100 bg-blue-50 p-3">
          <div className="flex items-start justify-between gap-3">
            <div>
              <div className="text-xs font-black text-brand-blue">{build.tierLabel}</div>
              <h3 className="mt-1 text-lg font-black text-commerce-ink">{build.title}</h3>
            </div>
            
          </div>
          <p className="mt-2 min-h-10 text-xs leading-5 text-slate-600">{build.summary}</p>
        </div>
        <div className="mt-4 text-2xl font-black text-brand-blue">{build.totalPrice.toLocaleString()}원</div>
        <div className="mt-2 text-xs font-semibold text-slate-500">
          {primaryWarning ?? '저장 전 서버에서 다시 Tool 검증합니다.'}
        </div>
        <div className="mt-4 space-y-2">
          {mainItems.map((item) => (
            <div key={`${build.id}-${item.category}`} className="flex items-center justify-between gap-2 text-xs">
              <span className="w-20 font-bold text-slate-500">{labelForCategory(item.category)}</span>
              <span className="flex-1 truncate text-slate-800" title={item.name}>{item.name}</span>
            </div>
          ))}
        </div>
      </button>
      <div className="mt-4 flex gap-2">
        <button type="button" onClick={onSelect} className="rounded bg-brand-blue px-3 py-2 text-xs font-bold text-white hover:bg-blue-700">
          상세 보기
        </button>
        {savedBuildId ? (
          <span className="rounded border border-emerald-200 bg-emerald-50 px-3 py-2 text-xs font-bold text-emerald-700" title={savedBuildId}>
            저장됨
          </span>
        ) : null}
      </div>
    </article>
  );
}

function toolDisplayLabel(tool: string) {
  switch (tool) {
    case 'compatibility':
      return '호환성 검증';
    case 'power':
      return '전력 검증';
    case 'size':
      return '규격 검증';
    case 'performance':
      return '성능 범위';
    case 'price':
      return '가격 확인';
    default:
      return tool;
  }
}

function labelForCategory(category: string) {
  switch (category) {
    case 'MOTHERBOARD':
      return '메인보드';
    case 'STORAGE':
      return 'SSD';
    case 'PSU':
      return '파워';
    case 'CASE':
      return '케이스';
    case 'COOLER':
      return '쿨러';
    default:
      return category;
  }
}
