import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { AdminShell, DataTable, Panel, StateMessage, StatusBadge } from '../../../components/ui';
import { getAdminTickets } from '../adminApi';
import type { AdminAsTicket } from '../adminApi';

export function AdminTicketsPage() {
  const { data, isError, isLoading } = useQuery({
    queryKey: ['admin-as-tickets'],
    queryFn: getAdminTickets
  });

  const tickets = data?.items ?? [];
  const ticketRows = tickets.map((ticket) => ({
    티켓: <Link className="font-bold text-brand-blue" to={`/admin/as-tickets/${ticket.id}`}>{shortId(ticket.id)}</Link>,
    상태: <StatusBadge status={ticket.status} />,
    검토: ticket.reviewStatus ? <StatusBadge status={ticket.reviewStatus} /> : '-',
    '지원 결정': ticket.supportDecision ? <StatusBadge status={ticket.supportDecision} /> : '-',
    '추천 서비스': recommendedSupportLabel(ticket),
    증상: <Link className="font-bold text-slate-800 hover:text-brand-blue" to={`/admin/as-tickets/${ticket.id}`}>{ticket.title ?? ticket.symptom}</Link>,
    사용자: userLabel(ticket),
    '접수 시간': formatDateTime(ticket.createdAt),
    담당자: ticket.assignedAdminId ?? '-'
  }));

  return (
    <AdminShell title="AS 티켓 관리">
      <Panel title="처리할 AS 티켓" subtitle="사용자 증상과 PC Agent 로그가 접수된 티켓을 확인합니다.">
        {isLoading ? <StateMessage type="info" title="AS 티켓 로딩 중" body="관리자 AS 티켓 목록을 불러오고 있습니다." /> : null}
        {isError ? <StateMessage type="warn" title="AS 티켓 조회 실패" body="AS 티켓 목록을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요." /> : null}
        {!isLoading && !isError && ticketRows.length === 0 ? (
          <StateMessage type="info" title="AS 티켓 없음" body="표시할 관리자 AS 티켓이 없습니다." />
        ) : null}
        {!isLoading && !isError && ticketRows.length > 0 ? (
          <DataTable columns={['티켓', '상태', '검토', '지원 결정', '추천 서비스', '증상', '사용자', '접수 시간', '담당자']} rows={ticketRows} />
        ) : null}
      </Panel>
    </AdminShell>
  );
}

function userLabel(ticket: AdminAsTicket) {
  return ticket.userEmail ?? ticket.userName ?? ticket.userId ?? '-';
}

function recommendedSupportLabel(ticket: AdminAsTicket) {
  const routing = objectValue(ticket.supportRouting);
  const explicitLabel = textValue(routing?.recommendedServiceLabel);
  if (explicitLabel) {
    return explicitLabel;
  }
  const service = textValue(routing?.recommendedService);
  if (service) {
    return supportServiceLabel(service);
  }
  return serviceLabelForDecision(ticket.supportDecision);
}

function supportServiceLabel(service: string) {
  switch (service) {
    case 'REMOTE_SUPPORT':
      return '원격지원 신청';
    case 'VISIT_SUPPORT':
      return '방문지원 신청';
    case 'DIAGNOSIS_ONLY':
      return '우선 진단만 받기';
    default:
      return service;
  }
}

function serviceLabelForDecision(decision?: string | null) {
  switch (decision) {
    case 'REMOTE_POSSIBLE':
      return '원격지원 신청';
    case 'VISIT_REQUIRED':
    case 'REPAIR_OR_REPLACE':
      return '방문지원 신청';
    case 'UNSUPPORTED':
      return '지원 범위 밖';
    default:
      return '우선 진단만 받기';
  }
}

function objectValue(value: unknown) {
  if (value && typeof value === 'object' && !Array.isArray(value)) {
    return value as Record<string, unknown>;
  }
  return null;
}

function textValue(value: unknown) {
  if (typeof value === 'string') {
    return value.trim() || null;
  }
  if (typeof value === 'number' || typeof value === 'boolean') {
    return String(value);
  }
  return null;
}

function shortId(id: string) {
  return id.length <= 12 ? id : `${id.slice(0, 8)}...${id.slice(-4)}`;
}

function formatDateTime(value?: string) {
  if (!value) return '-';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '-';
  const parts = new Intl.DateTimeFormat('ko-KR', {
    timeZone: 'Asia/Seoul',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  }).formatToParts(date);
  const part = (type: string) => parts.find((item) => item.type === type)?.value ?? '00';
  return `${part('year')}-${part('month')}-${part('day')} ${part('hour')}:${part('minute')}:${part('second')}`;
}
