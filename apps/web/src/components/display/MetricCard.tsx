export function MetricCard({ label, value, tone = 'blue' }: { label: string; value: string; tone?: 'blue' | 'green' | 'orange' }) {
  const color = tone === 'green' ? 'text-emerald-600' : tone === 'orange' ? 'text-orange-600' : 'text-brand-blue';
  return (
    <div className="panel min-h-[84px] p-4">
      <div className="text-xs text-slate-500">{label}</div>
      <div className={`mt-2 text-2xl font-bold ${color}`}>{value}</div>
    </div>
  );
}
