import { FormEvent, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Save } from 'lucide-react';
import { DataTable, Panel, Screen, StateMessage, StatusBadge } from '../../../components/ui';
import { createQuotePriceAlert, getBuildHistory, getPriceAlerts } from '../quoteApi';

export function MyQuotesPage() {
  const queryClient = useQueryClient();
  const [partId, setPartId] = useState('');
  const [targetPrice, setTargetPrice] = useState('850000');

  const buildsQuery = useQuery({ queryKey: ['build-history'], queryFn: getBuildHistory });
  const alertsQuery = useQuery({ queryKey: ['price-alerts'], queryFn: getPriceAlerts });

  const firstPartId = useMemo(() => {
    const firstBuild = buildsQuery.data?.items?.[0];
    return firstBuild?.items?.[0]?.partId ?? firstBuild?.items?.[0]?.id ?? '';
  }, [buildsQuery.data]);

  useEffect(() => {
    if (!partId && firstPartId) {
      setPartId(firstPartId);
    }
  }, [firstPartId, partId]);

  const createAlertMutation = useMutation({
    mutationFn: () => createQuotePriceAlert(partId.trim(), Number(targetPrice.replace(/,/g, ''))),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['price-alerts'] })
  });

  function submitAlert(event: FormEvent) {
    event.preventDefault();
    if (!partId.trim() || !Number(targetPrice.replace(/,/g, ''))) {
      return;
    }
    createAlertMutation.mutate();
  }

  const buildRows = (buildsQuery.data?.items ?? []).map((build) => ({
    견적: <Link to={`/builds/${build.id}`} className="font-bold text-brand-blue">{shortId(build.id)}</Link>,
    이름: build.name,
    총액: <span className="whitespace-nowrap font-black text-commerce-ink">{build.totalPrice.toLocaleString()}원</span>,
    신뢰도: <StatusBadge status={build.confidence} />,
    생성일: formatDateTime(build.createdAt),
    관리: <Link to={`/builds/${build.id}/change-part`} className="font-bold text-brand-blue">부품 변경</Link>
  }));

  const alertRows = (alertsQuery.data?.items ?? []).map((alert) => ({
    부품: alert.partName,
    목표가: `${alert.targetPrice.toLocaleString()}원`,
    현재가: <span className="whitespace-nowrap font-black text-commerce-ink">{alert.currentPrice.toLocaleString()}원</span>,
    상태: <StatusBadge status={alert.status} />,
    등록일: formatDateTime(alert.createdAt)
  }));

  return (
    <Screen>
      <div className="grid gap-5 lg:grid-cols-[minmax(0,1fr)_360px]">
        <Panel title="내 견적함" subtitle="로그인 사용자 기준 저장 Build 목록입니다.">
          {buildsQuery.isLoading ? (
            <StateMessage type="info" title="견적 불러오는 중" body="저장된 추천 견적을 조회하고 있습니다." />
          ) : buildsQuery.isError ? (
            <StateMessage type="warn" title="견적 조회 실패" body="GET /api/builds/history 응답을 불러오지 못했습니다." />
          ) : buildRows.length ? (
            <DataTable columns={['견적', '이름', '총액', '신뢰도', '생성일', '관리']} rows={buildRows} />
          ) : (
            <div className="space-y-3">
              <StateMessage type="info" title="저장된 견적 없음" body="구매 상담에서 추천 조합을 생성하면 여기에 표시됩니다." />
              <div className="flex flex-wrap gap-2">
                <Link to="/requirements/new" className="rounded-md bg-brand-blue px-4 py-2.5 text-sm font-bold text-white hover:bg-blue-700">AI 견적 시작</Link>
                <Link to="/self-quote" className="rounded-md border border-slate-300 px-4 py-2.5 text-sm font-bold text-slate-700 hover:border-commerce-ink hover:text-commerce-ink">셀프 견적 시작</Link>
              </div>
            </div>
          )}
        </Panel>

        <Panel title="목표가 알림 등록">
          <form onSubmit={submitAlert} className="space-y-3">
            <div>
              <label htmlFor="quote-alert-part-id" className="mb-1 block text-xs font-bold text-slate-600">부품 ID</label>
              <input id="quote-alert-part-id" className="h-11 w-full rounded border border-slate-300 px-3 text-sm" value={partId} onChange={(event) => setPartId(event.target.value)} />
            </div>
            <div>
              <label htmlFor="quote-alert-target-price" className="mb-1 block text-xs font-bold text-slate-600">목표가</label>
              <input id="quote-alert-target-price" className="h-11 w-full rounded border border-slate-300 px-3 text-sm" inputMode="numeric" value={targetPrice} onChange={(event) => setTargetPrice(event.target.value)} />
            </div>
            <button disabled={createAlertMutation.isPending || !partId.trim()} className="w-full rounded bg-brand-blue px-4 py-3 text-sm font-bold text-white disabled:bg-slate-400">
              <Save className="mr-1 inline" size={15} /> 알림 등록
            </button>
            {createAlertMutation.isSuccess ? <StateMessage type="success" title="알림 등록 완료" body="목표가 알림 목록에 반영했습니다." /> : null}
            {createAlertMutation.isError ? <StateMessage type="warn" title="알림 등록 실패" body="이미 같은 목표가 알림이 있거나 부품 ID가 유효하지 않습니다." /> : null}
          </form>
        </Panel>

        <Panel title="목표가 알림" className="lg:col-span-2">
          {alertsQuery.isLoading ? (
            <StateMessage type="info" title="알림 불러오는 중" body="등록된 목표가 알림을 조회하고 있습니다." />
          ) : alertsQuery.isError ? (
            <StateMessage type="warn" title="알림 조회 실패" body="GET /api/price-alerts 응답을 불러오지 못했습니다." />
          ) : alertRows.length ? (
            <DataTable columns={['부품', '목표가', '현재가', '상태', '등록일']} rows={alertRows} />
          ) : (
            <StateMessage type="info" title="등록된 알림 없음" body="부품 상세 또는 내 견적함에서 목표가를 등록할 수 있습니다." />
          )}
        </Panel>
      </div>
    </Screen>
  );
}

function shortId(id: string) {
  return id.length <= 12 ? id : `${id.slice(0, 8)}...${id.slice(-4)}`;
}

function formatDateTime(value?: string) {
  if (!value) {
    return '—';
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value.replace('T', ' ').slice(0, 19);
  }
  return date.toLocaleString('ko-KR', { dateStyle: 'medium', timeStyle: 'short' });
}
