import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { AdminShell, DataTable, Panel, StateMessage, StatusBadge } from '../../../components/ui';
import { getAdminToolInvocations } from '../adminApi';

export function ToolInvocationsListAdminPage() {
  const { data, isError, isLoading } = useQuery({
    queryKey: ['admin-tool-invocations'],
    queryFn: getAdminToolInvocations
  });
  const invocations = data?.items ?? [];
  const rows = invocations.map((invocation) => ({
    id: <Link className="font-bold text-brand-blue" to={`/admin/tool-invocations/${invocation.id}`}>{invocation.id}</Link>,
    session: invocation.agentSessionId
      ? <Link className="font-bold text-brand-blue" to={`/admin/agent-sessions/${invocation.agentSessionId}`}>{invocation.agentSessionId}</Link>
      : '-',
    tool: invocation.toolName,
    status: <StatusBadge status={invocation.status} />,
    confidence: <StatusBadge status={invocation.confidence} />,
    createdAt: formatDateTime(invocation.createdAt)
  }));
  const exportRows = invocations.map((invocation) => ({
    id: invocation.id,
    agentSessionId: invocation.agentSessionId,
    toolName: invocation.toolName,
    status: invocation.status,
    confidence: invocation.confidence,
    createdAt: formatDateTime(invocation.createdAt)
  }));

  return (
    <AdminShell title="Tool 호출 목록" exportRows={exportRows} exportFileName="admin-tool-invocations.csv">
      <Panel title="Tool 호출 목록" subtitle="tool_invocations 기준 검증 도구 실행 이력">
        {isLoading ? (
          <StateMessage type="info" title="Tool 호출 로딩 중" body="관리자 Tool 호출 목록을 불러오고 있습니다." />
        ) : isError ? (
          <StateMessage type="warn" title="Tool 호출 조회 실패" body="GET /api/admin/tool-invocations 응답을 불러오지 못했습니다." />
        ) : rows.length ? (
          <DataTable columns={['id', 'session', 'tool', 'status', 'confidence', 'createdAt']} rows={rows} />
        ) : (
          <StateMessage type="info" title="Tool 호출 없음" body="표시할 Tool 호출 기록이 없습니다." />
        )}
      </Panel>
    </AdminShell>
  );
}

function formatDateTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 19) : '-';
}
