import type { ToolRow } from '../types';

export const toolRows: ToolRow[] = [
  { tool: 'compatibility', status: 'PASS', confidence: 'HIGH', summary: 'CPU와 메인보드 소켓 호환' },
  { tool: 'power', status: 'WARN', confidence: 'MEDIUM', summary: '피크 전력 기준 PSU 여유율 낮음' },
  { tool: 'performance', status: 'PASS', confidence: 'MEDIUM', summary: 'QHD 게임 기준 GPU 우선 구성 적합' },
  { tool: 'price', status: 'PASS', confidence: 'LOW', summary: '최근 스냅샷 기준 예산 내 구성' }
];
