import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { AdminShell, DataTable, Panel, StateMessage } from '../../../components/ui';
import { getAdminRagEvidence } from '../adminApi';

export function RagEvidenceListAdminPage() {
  const { data, isError, isLoading } = useQuery({
    queryKey: ['admin-rag-evidence'],
    queryFn: getAdminRagEvidence
  });
  const evidenceItems = data?.items ?? [];
  const rows = evidenceItems.map((evidence) => ({
    id: <Link className="font-bold text-brand-blue" to={`/admin/rag-evidence/${evidence.id}`}>{evidence.id}</Link>,
    session: evidence.agentSessionId
      ? <Link className="font-bold text-brand-blue" to={`/admin/agent-sessions/${evidence.agentSessionId}`}>{evidence.agentSessionId}</Link>
      : '-',
    sourceId: evidence.sourceId,
    summary: evidence.summary,
    score: formatScore(evidence.score)
  }));
  const exportRows = evidenceItems.map((evidence) => ({
    id: evidence.id,
    agentSessionId: evidence.agentSessionId ?? '',
    sourceId: evidence.sourceId,
    summary: evidence.summary,
    score: formatScore(evidence.score)
  }));

  return (
    <AdminShell title="RAG 근거 목록" exportRows={exportRows} exportFileName="admin-rag-evidence.csv">
      <Panel title="RAG 근거 목록" subtitle="rag_evidence 기준 검색/추천 근거">
        {isLoading ? (
          <StateMessage type="info" title="RAG 근거 로딩 중" body="관리자 RAG 근거 목록을 불러오고 있습니다." />
        ) : isError ? (
          <StateMessage type="warn" title="RAG 근거 조회 실패" body="GET /api/admin/rag-evidence 응답을 불러오지 못했습니다." />
        ) : rows.length ? (
          <DataTable columns={['id', 'session', 'sourceId', 'summary', 'score']} rows={rows} />
        ) : (
          <StateMessage type="info" title="RAG 근거 없음" body="표시할 RAG 근거가 없습니다." />
        )}
      </Panel>
    </AdminShell>
  );
}

function formatScore(value?: string | number | null) {
  return value == null ? '-' : String(value);
}
