import { AlertTriangle, CheckCircle2, Shield } from 'lucide-react';

export function StateMessage({ type, title, body }: { type: 'info' | 'warn' | 'success'; title: string; body: string }) {
  const icon = type === 'success' ? <CheckCircle2 size={18} /> : type === 'warn' ? <AlertTriangle size={18} /> : <Shield size={18} />;
  const cls = type === 'success' ? 'border-emerald-200 bg-emerald-50 text-emerald-800' : type === 'warn' ? 'border-orange-200 bg-orange-50 text-orange-800' : 'border-blue-200 bg-blue-50 text-blue-800';
  return (
    <div className={`flex items-start gap-3 rounded border p-4 ${cls}`}>
      {icon}
      <div>
        <div className="font-bold">{title}</div>
        <div className="mt-1 text-xs leading-5">{body}</div>
      </div>
    </div>
  );
}
