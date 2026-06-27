export function Field({ label, value, wide }: { label: string; value: string; wide?: boolean }) {
  return (
    <label className={wide ? 'col-span-2' : ''}>
      <div className="mb-1 text-xs font-bold text-slate-500">{label}</div>
      <input className="h-10 w-full rounded border border-slate-300 px-3 text-sm" defaultValue={value} />
    </label>
  );
}
