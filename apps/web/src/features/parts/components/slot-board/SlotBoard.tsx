import { useEffect, useMemo, useRef, useState, type CSSProperties } from 'react';
import type { BuildGraphResolveResponse, PartCategory } from '../../../quote/aiSelection';
import type { QuoteDraftItem } from '../../types';
import {
  FALLBACK_EDGES,
  SLOT_BOARD_BG,
  SLOT_CONFIGS,
  isMultiItemCategory,
  isSlotBoardPercentPosition,
  isSlotCategory,
  slotConfigFor,
  slotLayoutWithPosition,
  type SlotBoardPosition,
  type SlotConfig,
  type SlotEdgeConfig
} from './slotBoardConfig';

type SlotBoardProps = {
  items: QuoteDraftItem[];
  selectedCategory: PartCategory | null;
  onSlotSelect: (category: PartCategory) => void;
  onRemoveItem: (partId: string) => void;
  isRemovePending: boolean;
  graph?: BuildGraphResolveResponse;
};

export function SlotBoard({ items, selectedCategory, onSlotSelect, onRemoveItem, isRemovePending, graph }: SlotBoardProps) {
  const statusByCategory = partStatusByCategory(graph);
  const slotPositions = useMemo(() => slotPositionsFromGraph(graph), [graph]);
  return (
    <div
      data-testid="slot-board"
      className="flex flex-col gap-3 rounded-lg border border-commerce-line bg-slate-900/[0.03] p-3 lg:relative lg:block lg:aspect-[16/10] lg:bg-[url('/slot-board/backgrounds/topology-board-bg.svg')] lg:bg-cover lg:bg-center lg:p-0"
      style={{ ['--slot-board-bg' as string]: `url(${SLOT_BOARD_BG})` }}
    >
      <SlotBoardEdges items={items} graph={graph} slotPositions={slotPositions} />
      {SLOT_CONFIGS.map((slot) => (
        <BoardSlot
          key={slot.category}
          slot={slot}
          layout={slotLayoutWithPosition(slot, slotPositions[slot.category])}
          items={items.filter((item) => item.category === slot.category)}
          problemStatus={statusByCategory.get(slot.category)}
          isSelected={selectedCategory === slot.category}
          onSelect={() => onSlotSelect(slot.category)}
          onRemoveItem={onRemoveItem}
          isRemovePending={isRemovePending}
        />
      ))}
    </div>
  );
}

function BoardSlot({
  slot,
  layout,
  items,
  problemStatus,
  isSelected,
  onSelect,
  onRemoveItem,
  isRemovePending
}: {
  slot: SlotConfig;
  layout: SlotConfig['layout'];
  items: QuoteDraftItem[];
  problemStatus?: 'PASS' | 'WARN' | 'FAIL';
  isSelected: boolean;
  onSelect: () => void;
  onRemoveItem: (partId: string) => void;
  isRemovePending: boolean;
}) {
  const filled = items.length > 0;
  const primaryItem = items[0];
  const lineTotal = items.reduce((sum, item) => sum + item.lineTotal, 0);
  // 문제 상태는 장착된 슬롯에만 표시한다. 숨기지 않고 강조한다.
  const slotStatus = filled ? problemStatus ?? 'PASS' : 'NONE';
  const isFlashing = useAttachFlash(items);
  const layoutVars: CSSProperties = {
    ['--sx' as string]: `${layout.x}%`,
    ['--sy' as string]: `${layout.y}%`,
    ['--sw' as string]: `${layout.w}%`,
    ['--sh' as string]: `${layout.h}%`
  };
  const borderClass = isSelected
    ? 'border-2 border-brand-blue ring-4 ring-blue-100'
    : slotStatus === 'FAIL'
      ? 'border-2 border-red-500 ring-4 ring-red-100'
      : slotStatus === 'WARN'
        ? 'border-2 border-amber-400 ring-4 ring-amber-100'
        : filled
          ? 'border border-commerce-line hover:border-commerce-ink'
          : 'border-2 border-dashed border-slate-300 bg-blue-50/40 hover:border-brand-blue';

  return (
    <div
      data-testid={`slot-${slot.category}`}
      data-selected={isSelected ? 'true' : 'false'}
      data-status={slotStatus}
      data-flash={isFlashing ? 'true' : 'false'}
      style={layoutVars}
      className={`group relative min-h-[104px] rounded-lg bg-white/95 p-2.5 text-left shadow-sm transition lg:absolute lg:left-[var(--sx)] lg:top-[var(--sy)] lg:h-[var(--sh)] lg:w-[var(--sw)] lg:min-h-0 ${borderClass} ${
        isFlashing ? 'slot-attach-flash' : ''
      } ${!filled && !isSelected ? 'slot-empty-pulse' : ''}`}
    >
      <button
        type="button"
        aria-label={`${slot.label} 슬롯 열기`}
        aria-pressed={isSelected}
        onClick={onSelect}
        className="absolute inset-0 z-0 h-full w-full rounded-lg focus:outline-none focus-visible:ring-2 focus-visible:ring-brand-blue"
      />
      <div className="pointer-events-none relative z-10 flex h-full flex-col gap-1 overflow-hidden">
        <div className="flex items-center justify-between gap-1">
          <span className="flex items-center gap-1.5 text-[11px] font-black text-slate-600">
            <img src={slot.glyph} alt="" aria-hidden="true" className={`h-6 w-6 ${filled ? '' : 'opacity-40'}`} />
            {slot.label}
            {slotStatus === 'FAIL' ? (
              <span className="rounded border border-red-200 bg-red-50 px-1.5 py-0.5 text-[10px] font-black text-red-700">안 맞음</span>
            ) : slotStatus === 'WARN' ? (
              <span className="rounded border border-amber-200 bg-amber-50 px-1.5 py-0.5 text-[10px] font-black text-amber-700">간섭 주의</span>
            ) : null}
          </span>
          {filled && !isMultiItemCategory(slot.category) ? (
            <button
              type="button"
              aria-label={`${primaryItem.name} 견적에서 제거`}
              disabled={isRemovePending}
              onClick={() => onRemoveItem(primaryItem.partId)}
              className="pointer-events-auto rounded border border-commerce-line bg-white px-1.5 py-0.5 text-[10px] font-black text-slate-500 opacity-0 transition group-hover:opacity-100 focus-visible:opacity-100 hover:border-commerce-sale hover:text-commerce-sale disabled:cursor-wait"
            >
              빼기
            </button>
          ) : null}
        </div>
        {filled ? (
          <>
            <div className="line-clamp-2 text-xs font-black leading-4 text-commerce-ink">
              {items.length > 1 ? `${primaryItem.name} 외 ${items.length - 1}개` : primaryItem.name}
            </div>
            <div className="mt-auto flex items-end justify-between gap-1">
              <span className="text-xs font-black text-brand-blue">{lineTotal.toLocaleString()}원</span>
              {slot.miniSlots ? <MiniSlotRow slot={slot} items={items} /> : null}
            </div>
          </>
        ) : (
          <div className="flex flex-1 flex-col justify-center gap-1">
            <span className="text-xs font-black text-brand-blue">+ 부품 선택</span>
            {slot.miniSlots ? <MiniSlotRow slot={slot} items={items} /> : null}
          </div>
        )}
      </div>
    </div>
  );
}

// 장착/교체로 슬롯 구성이 바뀌면 잠깐 flash 상태를 켠다. 애니메이션 자체는 CSS가 담당하고
// prefers-reduced-motion에서는 CSS에서 꺼진다.
function useAttachFlash(items: QuoteDraftItem[]) {
  const signature = items.map((item) => `${item.partId}:${item.quantity}`).join('|');
  const previousSignature = useRef<string | null>(null);
  const [isFlashing, setIsFlashing] = useState(false);

  useEffect(() => {
    const previous = previousSignature.current;
    previousSignature.current = signature;
    if (previous === null || previous === signature || signature === '') {
      return;
    }
    setIsFlashing(true);
    const timer = window.setTimeout(() => setIsFlashing(false), 900);
    return () => window.clearTimeout(timer);
  }, [signature]);

  return isFlashing;
}

type SlotEdgeStatus = 'PASS' | 'WARN' | 'FAIL' | 'PENDING' | 'BASE';

type ResolvedSlotEdge = {
  config: SlotEdgeConfig;
  status: SlotEdgeStatus;
  label: string;
  summary?: string;
};

const EDGE_STROKES: Record<SlotEdgeStatus, { stroke: string; dash?: string }> = {
  PASS: { stroke: '#16a34a' },
  WARN: { stroke: '#d97706' },
  FAIL: { stroke: '#ef4444', dash: '7 5' },
  PENDING: { stroke: '#94a3b8', dash: '4 6' },
  BASE: { stroke: '#64748b' }
};

const EDGE_LABEL_CLASSES: Record<SlotEdgeStatus, string> = {
  PASS: 'border-emerald-200 bg-emerald-50 text-emerald-700',
  WARN: 'border-amber-200 bg-amber-50 text-amber-700',
  FAIL: 'border-red-200 bg-red-50 text-red-700',
  PENDING: 'border-slate-200 bg-white text-slate-400',
  BASE: 'border-slate-200 bg-white text-slate-600'
};

// 기본 topology 관계선은 graph API 없이 항상 렌더링되고,
// graph 응답이 있으면 카테고리 쌍이 일치하는 edge의 라벨/상태만 덧입힌다.
function SlotBoardEdges({
  items,
  graph,
  slotPositions
}: {
  items: QuoteDraftItem[];
  graph?: BuildGraphResolveResponse;
  slotPositions: Partial<Record<PartCategory, SlotBoardPosition>>;
}) {
  const filledCategories = new Set(items.map((item) => item.category));
  const edges: ResolvedSlotEdge[] = FALLBACK_EDGES.map((config) => {
    if (!filledCategories.has(config.from) || !filledCategories.has(config.to)) {
      return { config, status: 'PENDING', label: config.label, summary: '부품 선택 후 계산됩니다.' };
    }
    const graphEdge = findGraphEdge(graph, config.from, config.to);
    if (graphEdge) {
      return {
        config,
        status: graphEdge.status,
        label: graphEdge.label || config.label,
        summary: graphEdge.summary
      };
    }
    return { config, status: 'BASE', label: config.label };
  });

  return (
    <div data-testid="slot-board-edges" aria-hidden="true" className="pointer-events-none absolute inset-0 hidden lg:block">
      <svg viewBox="0 0 100 100" preserveAspectRatio="none" className="h-full w-full">
        {edges.map((edge) => {
          const { from, to } = edgeEndpoints(edge.config, slotPositions);
          const style = EDGE_STROKES[edge.status];
          return (
            <g key={`${edge.config.from}-${edge.config.to}`}>
              <line
                x1={from.x}
                y1={from.y}
                x2={to.x}
                y2={to.y}
                stroke={style.stroke}
                strokeWidth={2.5}
                strokeDasharray={style.dash}
                vectorEffect="non-scaling-stroke"
              />
              <circle cx={from.x} cy={from.y} r={0.6} fill={style.stroke} />
              <circle cx={to.x} cy={to.y} r={0.6} fill={style.stroke} />
            </g>
          );
        })}
      </svg>
      {edges.map((edge) => {
        const { from, to } = edgeEndpoints(edge.config, slotPositions);
        const midX = (from.x + to.x) / 2;
        const midY = (from.y + to.y) / 2;
        return (
          <span
            key={`label-${edge.config.from}-${edge.config.to}`}
            data-testid={`slot-edge-${edge.config.from}-${edge.config.to}`}
            data-status={edge.status}
            title={edge.summary}
            style={{ left: `${midX}%`, top: `${midY}%` }}
            className={`absolute -translate-x-1/2 -translate-y-1/2 whitespace-nowrap rounded-full border px-2 py-0.5 text-[10px] font-black shadow-sm ${EDGE_LABEL_CLASSES[edge.status]}`}
          >
            {edge.label}
          </span>
        );
      })}
    </div>
  );
}

function edgeEndpoints(config: SlotEdgeConfig, slotPositions: Partial<Record<PartCategory, SlotBoardPosition>>) {
  const fromSlot = slotConfigFor(config.from);
  const toSlot = slotConfigFor(config.to);
  const fromLayout = fromSlot ? slotLayoutWithPosition(fromSlot, slotPositions[config.from]) : undefined;
  const toLayout = toSlot ? slotLayoutWithPosition(toSlot, slotPositions[config.to]) : undefined;
  const center = (layout?: { x: number; y: number; w: number; h: number }) => ({
    x: (layout?.x ?? 0) + (layout?.w ?? 0) / 2,
    y: (layout?.y ?? 0) + (layout?.h ?? 0) / 2
  });
  return { from: center(fromLayout), to: center(toLayout) };
}

function slotPositionsFromGraph(graph?: BuildGraphResolveResponse) {
  const positions: Partial<Record<PartCategory, SlotBoardPosition>> = {};
  graph?.nodes.forEach((node) => {
    const category = typeof node.category === 'string' ? node.category : null;
    if (node.type === 'PART' && isSlotCategory(category) && isSlotBoardPercentPosition(category, node.position)) {
      positions[category] = node.position;
    }
  });
  return positions;
}

function partStatusByCategory(graph?: BuildGraphResolveResponse) {
  const statusMap = new Map<string, 'PASS' | 'WARN' | 'FAIL'>();
  graph?.nodes.forEach((node) => {
    if (node.type === 'PART' && node.category) {
      statusMap.set(node.category, node.status);
    }
  });
  return statusMap;
}

function findGraphEdge(graph: BuildGraphResolveResponse | undefined, from: PartCategory, to: PartCategory) {
  if (!graph) {
    return undefined;
  }
  const categoryByNodeId = new Map(graph.nodes.map((node) => [node.id, node.category]));
  return graph.edges.find((edge) => {
    const sourceCategory = categoryByNodeId.get(edge.source);
    const targetCategory = categoryByNodeId.get(edge.target);
    return (sourceCategory === from && targetCategory === to) || (sourceCategory === to && targetCategory === from);
  });
}

function MiniSlotRow({ slot, items }: { slot: SlotConfig; items: QuoteDraftItem[] }) {
  const total = slot.miniSlots ?? 0;
  const fillCount = slot.miniFillBy === 'quantity'
    ? items.reduce((sum, item) => sum + item.quantity, 0)
    : items.length;
  const overflow = Math.max(0, fillCount - total);

  return (
    <span className="flex items-center gap-1" aria-label={`${slot.label} 시각 슬롯 ${Math.min(fillCount, total)}/${total}`}>
      {Array.from({ length: total }).map((_, index) => (
        <span
          key={index}
          data-mini-slot-filled={index < fillCount ? 'true' : 'false'}
          className={`h-2.5 w-2.5 rounded-sm ${index < fillCount ? 'bg-brand-blue' : 'border border-dashed border-slate-300 bg-white'}`}
        />
      ))}
      {overflow > 0 ? <span className="text-[10px] font-black text-slate-500">+{overflow}</span> : null}
    </span>
  );
}
