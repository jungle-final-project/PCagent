import { Link } from 'react-router-dom';
import { AdminShell, DataTable, MetricCard, Panel, StateMessage, StatusBadge } from '../../../components/ui';

export function AdminDashboardPage() {
  return (
    <AdminShell title="운영 대시보드">
      <div className="grid grid-cols-4 gap-4">
        <MetricCard label="LLM Queue" value="18초" tone="orange" />
        <MetricCard label="API p95" value="420ms" tone="green" />
        <MetricCard label="AS OPEN" value="12건" tone="orange" />
        <MetricCard label="추천 성공률" value="94%" tone="green" />
      </div>
      <div className="mt-5 grid grid-cols-[1fr_420px] gap-5">
        <Panel title="최근 Agent 세션">
          <DataTable columns={['id', 'user', 'status', 'action']} rows={[
            { id: 'demo-session', user: 'user@example.com', status: <StatusBadge status="PASS" />, action: <Link className="font-bold text-brand-blue" to="/admin/agent-sessions/demo-session">상세</Link> },
            { id: 'session-1002', user: 'dev@example.com', status: <StatusBadge status="WARN" />, action: '대기' }
          ]} />
        </Panel>
        <Panel title="운영 경고">
          <StateMessage type="warn" title="가격 Job 지연" body="네이버 API mock job이 마지막 갱신 후 23시간 경과했습니다." />
        </Panel>
      </div>
    </AdminShell>
  );
}
