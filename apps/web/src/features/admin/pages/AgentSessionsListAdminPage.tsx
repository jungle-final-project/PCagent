import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { AdminShell, DataTable, Panel, StateMessage, StatusBadge } from '../../../components/ui';
import { getAdminAgentSessions } from '../adminApi';

export function AgentSessionsListAdminPage() {
  const { data, isError, isLoading } = useQuery({
    queryKey: ['admin-agent-sessions'],
    queryFn: getAdminAgentSessions
  });
  const sessions = data?.items ?? [];
  const rows = sessions.map((session) => ({
    id: <Link className="font-bold text-brand-blue" to={`/admin/agent-sessions/${session.id}`}>{session.id}</Link>,
    status: <StatusBadge status={session.status} />,
    userId: session.userId ?? '-',
    createdAt: formatDateTime(session.createdAt)
  }));
  const exportRows = sessions.map((session) => ({
    id: session.id,
    status: session.status,
    userId: session.userId ?? '',
    createdAt: formatDateTime(session.createdAt)
  }));

  return (
    <AdminShell title="Agent 세션 목록" exportRows={exportRows} exportFileName="admin-agent-sessions.csv">
      <Panel title="Agent 세션 목록" subtitle="agent_sessions 기준 최근 실행 trace">
        {isLoading ? (
          <StateMessage type="info" title="Agent 세션 로딩 중" body="관리자 Agent 세션 목록을 불러오고 있습니다." />
        ) : isError ? (
          <StateMessage type="warn" title="Agent 세션 조회 실패" body="GET /api/admin/agent-sessions 응답을 불러오지 못했습니다." />
        ) : rows.length ? (
          <DataTable columns={['id', 'status', 'userId', 'createdAt']} rows={rows} />
        ) : (
          <StateMessage type="info" title="Agent 세션 없음" body="표시할 Agent 세션이 없습니다." />
        )}
      </Panel>
    </AdminShell>
  );
}

function formatDateTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 19) : '-';
}
