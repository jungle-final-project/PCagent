import { ReactNode } from 'react';
import { AppHeader } from './AppHeader';

export function Screen({ children }: { children: ReactNode }) {
  return (
    <div className="screen-shell">
      <AppHeader />
      <main className="mx-auto w-[1320px] py-6">{children}</main>
    </div>
  );
}
