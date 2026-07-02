import { api } from '../../lib/api';
import type { AgentLogUploadDto, AsTicketDto } from './types';

export type UploadAgentLogMetadata = {
  rangeStartedAt?: string;
  rangeEndedAt?: string;
  incidentId?: string;
  triggerType?: string;
  symptomType?: string;
  detectedAt?: string;
  selectedByUser?: boolean;
  consentId?: string;
};

export function uploadAgentLog(rangeMinutes: number, consentAccepted: boolean, file: File, metadata: UploadAgentLogMetadata = {}) {
  const body = new FormData();
  body.append('file', file);
  body.append('rangeMinutes', String(rangeMinutes));
  body.append('consentAccepted', String(consentAccepted));
  Object.entries(metadata).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      body.append(key, String(value));
    }
  });

  return api<AgentLogUploadDto>('/api/agent-logs/upload', {
    method: 'POST',
    body
  });
}

export function createSupportTicket(symptom: string, logUploadId: string) {
  return api<AsTicketDto>('/api/as-tickets', {
    method: 'POST',
    body: JSON.stringify({ symptom, logUploadId })
  });
}

export function getSupportTicket(ticketId: string) {
  return api<AsTicketDto>(`/api/as-tickets/${ticketId}`);
}
