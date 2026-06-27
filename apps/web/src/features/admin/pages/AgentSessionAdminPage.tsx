import { Link } from 'react-router-dom';
import { AdminShell, DataTable, Panel, StateMessage, StatusBadge } from '../../../components/ui';
import { agentStateRows, ragEvidenceRows, toolInvocationRows } from '../mocks/adminMock';

export function AgentSessionAdminPage() {
  return (
    <AdminShell title="Agent / RAG / Tool 근거 상세">
      <div className="grid grid-cols-[1fr_520px] gap-5">
        <Panel title="Agent 상태 전이" subtitle="3번 담당자가 session/run/fallback을 구현할 기준 상태">
          <DataTable columns={['step', 'state', 'owner', 'api', 'output']} rows={agentStateRows} />
        </Panel>
        <Panel title="실행 정책">
          <StateMessage type="info" title="제한된 Agent" body="무제한 자율 Agent가 아니라 RAG 검색, Tool 검증, 설명 생성 순서를 고정한 오케스트레이터로 구현합니다." />
          <div className="mt-4 rounded bg-slate-950 p-5 font-mono text-xs leading-6 text-slate-200">
            QUEUED → RUNNING → RAG_SEARCHED → TOOLS_CALLED → SUMMARY_READY<br />
            실패 시: FAILED → FALLBACK_READY
          </div>
        </Panel>
        <Panel title="Tool 호출 이력">
          <DataTable columns={['id', 'tool', 'status', 'confidence', 'latency', 'summary']} rows={toolInvocationRows.map((row) => ({ ...row, id: <Link className="font-bold text-brand-blue" to={`/admin/tool-invocations/${row.id}`}>{row.id}</Link>, status: <StatusBadge status={row.status} />, confidence: <StatusBadge status={row.confidence} /> }))} />
        </Panel>
        <Panel title="RAG Evidence">
          <DataTable columns={['id', 'sourceId', 'summary', 'score', 'owner']} rows={ragEvidenceRows.map((row) => ({ ...row, id: <Link className="font-bold text-brand-blue" to={`/admin/rag-evidence/${row.id}`}>{row.id}</Link> }))} />
        </Panel>
      </div>
    </AdminShell>
  );
}
