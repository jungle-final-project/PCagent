import { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { Bot, FileText, LifeBuoy, LogIn, Search } from 'lucide-react';
import { PrimaryNav } from './PrimaryNav';

export function AppHeader() {
  return (
    <>
      <div className="h-[30px] bg-brand-navy text-xs text-slate-200">
        <div className="mx-auto flex h-full w-[1320px] items-center justify-between">
          <span>BuildGraph AI prototype · desktop only</span>
          <span>로그인 | 회원가입 | 관리자 | PC Agent</span>
        </div>
      </div>
      <header className="h-[72px] border-b border-slate-200 bg-white">
        <div className="mx-auto flex h-full w-[1320px] items-center gap-4">
          <Link to="/" className="flex items-center gap-3">
            <div className="grid h-10 w-10 place-items-center rounded bg-brand-blue text-sm font-bold text-white">BG</div>
            <div>
              <div className="text-xl font-bold leading-5 text-brand-navy">BuildGraph AI</div>
              <div className="text-xs text-slate-500">AI PC consulting platform</div>
            </div>
          </Link>
          <div className="ml-6 flex h-11 w-[560px] items-center rounded border border-slate-300 bg-slate-50 px-3">
            <Search size={17} className="text-slate-400" />
            <input className="ml-2 flex-1 bg-transparent text-sm outline-none" placeholder="예: QHD 게임용 200만원 PC" />
            <button className="rounded bg-brand-blue px-4 py-1.5 text-xs font-semibold text-white">검색</button>
          </div>
          <div className="ml-auto flex items-center gap-2">
            <HeaderButton to="/requirements/new" icon={<Bot size={15} />} label="AI 견적" />
            <HeaderButton to="/my/quotes" icon={<FileText size={15} />} label="내 견적함" />
            <HeaderButton to="/support/new" icon={<LifeBuoy size={15} />} label="AS 접수" />
            <HeaderButton to="/login" icon={<LogIn size={15} />} label="로그인" dark />
          </div>
        </div>
      </header>
      <PrimaryNav />
    </>
  );
}

function HeaderButton({ to, icon, label, dark }: { to: string; icon: ReactNode; label: string; dark?: boolean }) {
  return (
    <Link to={to} className={`flex h-9 items-center gap-1 rounded px-3 text-xs font-semibold ${dark ? 'bg-brand-navy text-white' : 'border border-slate-300 bg-white text-slate-700'}`}>
      {icon}
      {label}
    </Link>
  );
}
