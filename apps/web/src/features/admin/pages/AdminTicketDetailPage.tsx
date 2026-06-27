import { AdminShell, DataTable, Panel, StateMessage } from '../../../components/ui';
import { adminTicketDetailRows } from '../mocks/adminMock';

export function AdminTicketDetailPage() {
  return (
    <AdminShell title="AS 티켓 상세">
      <div className="grid grid-cols-[1fr_440px] gap-5">
        <Panel title="티켓 / 로그 / 동의 정책" subtitle="4번 담당자가 로그 업로드와 얕은 AS 후보를 연결할 기준 화면">
          <DataTable columns={['field', 'value']} rows={adminTicketDetailRows} />
        </Panel>
        <Panel title="관리자 조치">
          <StateMessage type="warn" title="사용자 화면 노출 제한" body="사용자에게는 티켓 번호와 접수 상태만 표시하고, 원인 후보와 로그 요약은 관리자 화면에서만 확인합니다." />
          <div className="mt-5 grid grid-cols-2 gap-3">
            <button className="rounded bg-brand-blue px-4 py-3 text-sm font-bold text-white">담당자 배정</button>
            <button className="rounded border border-slate-300 px-4 py-3 text-sm font-bold">상태 저장</button>
            <button className="rounded border border-slate-300 px-4 py-3 text-sm font-bold">로그 다운로드</button>
            <button className="rounded border border-slate-300 px-4 py-3 text-sm font-bold">업그레이드 후보 등록</button>
          </div>
        </Panel>
      </div>
    </AdminShell>
  );
}
