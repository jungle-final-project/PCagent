export function StatusBadge({ status }: { status: string }) {
  const s = status.toUpperCase();
  const cls = s === 'PASS' || s === 'HIGH' || s === 'ACTIVE' || s === 'RESOLVED'
    ? 'bg-emerald-50 text-emerald-700 border-emerald-200'
    : s === 'WARN' || s === 'MEDIUM' || s === 'OPEN'
      ? 'bg-orange-50 text-orange-700 border-orange-200'
      : 'bg-slate-100 text-slate-600 border-slate-200';
  return <span className={`inline-flex rounded-full border px-2 py-1 text-[11px] font-bold ${cls}`}>{status}</span>;
}
