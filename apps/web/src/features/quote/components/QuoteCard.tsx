import { Link } from 'react-router-dom';
import { StatusBadge } from '../../../components/feedback/StatusBadge';
import type { BuildSummary } from '../types';

export function QuoteCard({ build }: { build: BuildSummary }) {
  return (
    <div className="panel w-[260px] p-4">
      <div className="mb-3 h-24 rounded bg-gradient-to-br from-slate-100 to-brand-pale" />
      <div className="font-bold">{build.name}</div>
      <div className="mt-1 text-xs text-slate-500">{build.useCase}</div>
      <div className="mt-3 text-2xl font-bold text-brand-blue">{build.price.toLocaleString()}원</div>
      <div className="mt-3 flex items-center gap-2">
        <StatusBadge status={build.confidence} />
        <span className="text-xs text-orange-600">{build.warning}</span>
      </div>
      <div className="mt-4 flex gap-2">
        <Link to={`/builds/${build.id}`} className="rounded bg-brand-blue px-3 py-2 text-xs font-semibold text-white">상세 보기</Link>
        <Link to={`/builds/${build.id}/change-part`} className="rounded border border-slate-300 px-3 py-2 text-xs font-semibold">부품 변경</Link>
      </div>
    </div>
  );
}
