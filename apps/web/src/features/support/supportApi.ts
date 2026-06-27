import { api } from '../../lib/api';

export function uploadAgentLog(logRangeMinutes: number, consent: boolean) {
  return api('/api/agent-logs/upload', {
    method: 'POST',
    body: JSON.stringify({ logRangeMinutes, consent })
  });
}

export function createSupportTicket(symptom: string, logUploadId: string) {
  return api('/api/as-tickets', {
    method: 'POST',
    body: JSON.stringify({ symptom, logUploadId })
  });
}

export function getSupportTicket(ticketId: string) {
  return api(`/api/as-tickets/${ticketId}`);
}
