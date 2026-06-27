import { api } from '../../lib/api';

export function listParts() {
  return api('/api/parts');
}

export function getPart(partId: string) {
  return api(`/api/parts/${partId}`);
}

export function runToolCheck(tool: 'compatibility' | 'power' | 'size' | 'performance' | 'price', payload: unknown) {
  return api(`/api/tools/${tool}/check`, {
    method: 'POST',
    body: JSON.stringify(payload)
  });
}

export function createPriceAlert(partId: string, targetPrice: number, email: string) {
  return api('/api/price-alerts', {
    method: 'POST',
    body: JSON.stringify({ partId, targetPrice, email })
  });
}

export function collectPriceSnapshots() {
  return api('/api/price-snapshots/collect', { method: 'POST' });
}
