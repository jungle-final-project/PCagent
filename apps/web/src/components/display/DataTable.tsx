import { ReactNode } from 'react';

type Row = Record<string, string | number | ReactNode>;

export function DataTable({ columns, rows }: { columns: string[]; rows: Row[] }) {
  return (
    <div className="overflow-hidden rounded border border-slate-200">
      <table className="w-full border-collapse bg-white text-left text-xs">
        <thead className="bg-slate-100 text-slate-600">
          <tr>
            {columns.map((column) => <th key={column} className="border-b border-slate-200 px-3 py-3 font-bold">{column}</th>)}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, index) => (
            <tr key={index} className="border-b border-slate-100 last:border-0">
              {columns.map((column) => <td key={column} className="px-3 py-3 text-slate-700">{row[column]}</td>)}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
