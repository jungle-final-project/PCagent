import { useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { DataTable, MetricCard, Panel, Screen, StateMessage, StatusBadge } from '../../../components/ui';
import { QuoteCard } from '../components/QuoteCard';
import { getBuild } from '../quoteApi';
import type { ToolResult } from '../types';

export function BuildResultPage() {
  const { buildId = '00000000-0000-4000-8000-000000002001' } = useParams();
  const { data: build, isLoading, isError } = useQuery({
    queryKey: ['build', buildId],
    queryFn: () => getBuild(buildId)
  });

  if (isLoading) {
    return (
      <Screen>
        <Panel title="추천 Build 결과">
          <StateMessage type="info" title="Build 로딩 중" body="추천 build 상세와 Tool 검증 결과를 불러오고 있습니다." />
        </Panel>
      </Screen>
    );
  }

  if (isError || !build) {
    return (
      <Screen>
        <Panel title="추천 Build 결과">
          <StateMessage type="warn" title="Build 조회 실패" body="선택한 추천 build를 불러오지 못했습니다." />
        </Panel>
      </Screen>
    );
  }

  const toolResults = build.toolResults ?? [];
  const passCount = toolResults.filter((row) => row.status === 'PASS').length;

  return (
    <Screen>
      <div className="grid gap-5 lg:grid-cols-[minmax(0,1fr)_320px]">
        <div className="min-w-0 space-y-5">
          <Panel title={`추천 Build 결과 / ${build.name}`} subtitle={`견적 ID ${build.id.slice(0, 8)}`}>
            <div className="flex gap-4 overflow-x-auto pb-1">
              <QuoteCard build={build} selected />
            </div>
          </Panel>
          <Panel title="구성 부품">
            <DataTable columns={['분류', '부품명', '제조사', '가격']} rows={build.items.map((item) => ({
              분류: item.category,
              부품명: item.partId ? (
                <Link to={`/parts/${item.partId}`} className="font-bold text-commerce-ink hover:text-brand-blue hover:underline">{item.name}</Link>
              ) : (
                item.name
              ),
              제조사: item.manufacturer ?? '-',
              가격: <span className="whitespace-nowrap font-black text-commerce-ink">{item.price.toLocaleString()}원</span>
            }))} />
          </Panel>
          <Panel title="Tool 검증 결과">
            {toolResults.length > 0 ? (
              <div className={`mb-3 rounded-md border px-4 py-3 text-sm font-black ${passCount === toolResults.length ? 'border-emerald-100 bg-emerald-50 text-emerald-700' : 'border-amber-100 bg-amber-50 text-amber-700'}`}>
                {passCount === toolResults.length
                  ? `${toolResults.length}개 검증 모두 통과`
                  : `${toolResults.length}개 검증 중 ${passCount}개 통과 · 아래에서 경고 항목을 확인하세요`}
              </div>
            ) : null}
            <DataTable columns={['검증 항목', '상태', '신뢰도', '요약']} rows={toolRows(toolResults)} />
          </Panel>
        </div>
        <Panel title="견적 요약 / 액션">
          <div className="space-y-4">
            <MetricCard label="총액" value={`${build.totalPrice.toLocaleString()}원`} />
            <div className="grid grid-cols-2 gap-3">
              <MetricCard label="부품 수" value={`${build.items.length}개`} />
              <MetricCard label="경고" value={build.warnings.length > 0 ? `${build.warnings.length}건` : '없음'} />
            </div>
            <StateMessage
              type={build.warnings.length > 0 ? 'warn' : 'success'}
              title={build.warnings.length > 0 ? '확인 필요' : '주요 조건 충족'}
              body={build.warnings[0]?.message ?? '현재 구성은 저장된 내부 자산 기준 Tool 검증을 통과했습니다.'}
            />
            <Link to="/my/quotes" className="block rounded bg-brand-blue px-4 py-3 text-center text-sm font-bold text-white hover:bg-blue-700">견적 저장</Link>
            <Link to={`/builds/${build.id}/change-part`} className="block rounded border border-slate-300 px-4 py-3 text-center text-sm font-bold hover:border-commerce-ink">부품 변경 비교</Link>
          </div>
        </Panel>
      </div>
    </Screen>
  );
}

function toolRows(results: ToolResult[]) {
  return results.map((row) => ({
    '검증 항목': toolLabel(row.tool),
    상태: <StatusBadge status={row.status} />,
    신뢰도: <StatusBadge status={row.confidence} />,
    요약: row.summary
  }));
}

function toolLabel(tool: string) {
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
