import type { PartRow } from '../types';

export const parts: PartRow[] = [
  { id: 'cpu-7600', category: 'CPU', name: 'AMD Ryzen 5 7600', price: 259000, status: 'PASS', score: 94 },
  { id: 'mb-b650', category: 'MAINBOARD', name: 'B650M WiFi', price: 179000, status: 'PASS', score: 91 },
  { id: 'ram-32', category: 'RAM', name: 'DDR5 32GB 5600', price: 128000, status: 'PASS', score: 96 },
  { id: 'gpu-4070s', category: 'GPU', name: 'RTX 4070 SUPER 12GB', price: 890000, status: 'WARN', score: 89 },
  { id: 'ssd-1tb', category: 'SSD', name: 'NVMe Gen4 1TB', price: 99000, status: 'PASS', score: 88 },
  { id: 'psu-750', category: 'PSU', name: '750W 80+ Gold', price: 126000, status: 'PASS', score: 93 }
];
