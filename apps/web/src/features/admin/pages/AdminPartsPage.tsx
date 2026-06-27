import { AdminShell, DataTable, Panel, StateMessage, StatusBadge } from '../../../components/ui';
import { parts } from '../../parts/mocks/partsMock';

export function AdminPartsPage() {
  return (
    <AdminShell title="부품 / 가격 관리자">
      <div className="grid grid-cols-[1fr_360px] gap-5">
        <Panel title="부품 DB">
          <DataTable columns={['id', 'category', 'name', 'price', 'status']} rows={parts.map((part) => ({ ...part, price: `${part.price.toLocaleString()}원`, status: <StatusBadge status={part.status} /> }))} />
        </Panel>
        <Panel title="가격 Job 상태">
          <StateMessage type="info" title="목표가 비교 기준" body="배송비/쿠폰/카드할인 제외 표시 가격 기준으로 하루 1회 스냅샷을 저장합니다." />
          <button className="mt-5 w-full rounded bg-brand-blue px-4 py-3 text-sm font-bold text-white">가격 Job 실행</button>
        </Panel>
      </div>
    </AdminShell>
  );
}
