import { Link } from 'react-router-dom';
import { AdminShell, DataTable, Panel, StatusBadge } from '../../../components/ui';
import { tickets } from '../../support/mocks/supportMock';

export function AdminTicketsPage() {
  const supportRows = tickets.map((ticket) => ({
    id: <Link className="font-bold text-brand-blue" to={`/admin/as-tickets/${ticket.id}`}>{ticket.id}</Link>,
    user: ticket.user,
    symptom: ticket.symptom,
    status: <StatusBadge status={ticket.status} />,
    cause: ticket.cause
  }));
  return (
    <AdminShell title="AS 티켓 관리자">
      <div className="grid grid-cols-[1fr_480px] gap-5">
        <Panel title="티켓 큐">
          <DataTable columns={['id', 'user', 'symptom', 'status', 'cause']} rows={supportRows} />
        </Panel>
        <Panel title="선택 티켓 상세">
          <DataTable columns={['필드', '값']} rows={[
            { 필드: 'ticketId', 값: tickets[0].id },
            { 필드: '원인 후보', 값: tickets[0].cause },
            { 필드: '최근 로그', 값: 'GPU 88도, 사용률 96%, VRAM 89%' },
            { 필드: '추천 조치', 값: '쿨링 확인 및 드라이버 재설치 안내' }
          ]} />
          <div className="mt-5 flex gap-3">
            <button className="rounded bg-brand-blue px-4 py-3 text-sm font-bold text-white">담당자 배정</button>
            <button className="rounded border border-slate-300 px-4 py-3 text-sm font-bold">상태 저장</button>
          </div>
        </Panel>
      </div>
    </AdminShell>
  );
}
