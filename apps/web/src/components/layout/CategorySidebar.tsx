import { Link } from 'react-router-dom';

export function CategorySidebar({ items }: { items: string[] }) {
  return (
    <aside className="panel w-[216px] p-4">
      <div className="mb-1 text-base font-bold">PC 카테고리</div>
      <div className="mb-4 text-xs text-slate-500">프로젝트 범위만 표시</div>
      <div className="space-y-2">
        {items.map((item, idx) => (
          <Link key={item} to={idx === 0 ? '/requirements/new' : idx === 1 ? '/self-quote' : '/'} className="block rounded border border-slate-200 bg-slate-50 px-3 py-2 text-sm hover:border-brand-blue hover:bg-brand-pale">
            {item}
          </Link>
        ))}
      </div>
    </aside>
  );
}
