import { type FormEvent, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Bot, CheckCircle2, Cpu, Send, ShoppingCart, Sparkles, X, Zap } from 'lucide-react';
import {
  type AiBuildTier,
  defaultAiBuild,
  getAiRecommendedBuilds,
  saveSelectedAiBuild
} from '../aiSelection';

type AiBuildAssistantProps = {
  surface?: 'home' | 'self-quote';
};

export function AiBuildAssistant({ surface = 'home' }: AiBuildAssistantProps) {
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [prompt, setPrompt] = useState('');
  const [submittedPrompt, setSubmittedPrompt] = useState('QHD 게임과 개발을 같이 할 PC 추천');
  const [activeTier, setActiveTier] = useState<AiBuildTier>('balanced');
  const [assistantMessage, setAssistantMessage] = useState('원하는 예산과 용도를 입력하면 가성비, 균형, 고성능 조합으로 바로 비교해드릴게요.');

  const builds = useMemo(() => getAiRecommendedBuilds(submittedPrompt), [submittedPrompt]);
  const activeBuild = builds.find((build) => build.tier === activeTier) ?? builds[0] ?? defaultAiBuild();

  function submitPrompt(event: FormEvent) {
    event.preventDefault();
    const nextPrompt = prompt.trim();
    if (!nextPrompt) return;

    setSubmittedPrompt(nextPrompt);
    const nextTier = inferTier(nextPrompt);
    setActiveTier(nextTier);
    setAssistantMessage('입력한 조건에 맞춰 3가지 조합을 다시 정리했습니다. 탭을 눌러 가격과 부품 구성을 비교해보세요.');
    setPrompt('');
  }

  function selectBuild() {
    saveSelectedAiBuild(activeBuild);
    setOpen(false);
    navigate('/self-quote');
  }

  if (!open) {
    return (
      <button
        type="button"
        aria-label="AI 견적 챗봇 열기"
        data-testid="ai-chatbot-launcher"
        onClick={() => setOpen(true)}
        className="fixed bottom-5 right-5 z-50 flex h-16 w-16 items-center justify-center rounded-2xl border border-slate-900 bg-slate-950 text-white shadow-2xl transition hover:-translate-y-0.5 hover:bg-slate-800 focus:outline-none focus:ring-4 focus:ring-blue-200"
      >
        <span className="relative grid h-11 w-11 place-items-center rounded-xl bg-white text-slate-950">
          <Bot size={26} />
          <span className="absolute -right-1 -top-1 h-3 w-3 rounded-full border-2 border-slate-950 bg-emerald-400" />
        </span>
      </button>
    );
  }

  return (
    <section
      data-testid="ai-chatbot-panel"
      className="fixed bottom-5 right-4 z-50 w-[min(calc(100vw-2rem),440px)] overflow-hidden rounded-xl border border-slate-900 bg-white shadow-2xl"
    >
      <div className="bg-slate-950 px-4 py-3 text-white">
        <div className="flex items-center justify-between gap-3">
          <div className="flex min-w-0 items-center gap-3">
            <div className="grid h-10 w-10 shrink-0 place-items-center rounded-lg bg-white text-slate-950">
              <Bot size={22} />
            </div>
            <div className="min-w-0">
              <h2 className="truncate text-sm font-black">BuildGraph AI 챗봇</h2>
              <p className="truncate text-xs text-white/70">{surface === 'home' ? '추천 조합 비교' : '셀프견적 보조'}</p>
            </div>
          </div>
          <button
            type="button"
            aria-label="AI 견적 챗봇 닫기"
            onClick={() => setOpen(false)}
            className="grid h-9 w-9 place-items-center rounded-md border border-white/15 text-white/80 hover:bg-white/10 hover:text-white"
          >
            <X size={17} />
          </button>
        </div>
      </div>

      <div className="max-h-[72vh] overflow-y-auto p-4">
        <div className="rounded-lg border border-blue-100 bg-blue-50 p-3 text-sm leading-6 text-slate-700">
          <div className="mb-1 flex items-center gap-2 text-xs font-black text-brand-blue">
            <Sparkles size={14} />
            프론트 데모 추천
          </div>
          {assistantMessage}
        </div>

        <form onSubmit={submitPrompt} className="mt-3 rounded-lg border border-commerce-line bg-slate-50 p-2">
          <label className="sr-only" htmlFor="ai-build-chat-input">AI 챗봇에게 PC 사양 질문</label>
          <div className="flex gap-2">
            <input
              id="ai-build-chat-input"
              aria-label="AI 챗봇에게 PC 사양 질문"
              value={prompt}
              onChange={(event) => setPrompt(event.target.value)}
              placeholder="예: 200만원 QHD 게임용으로 추천해줘"
              className="min-w-0 flex-1 bg-transparent px-2 text-sm font-medium text-slate-900 outline-none placeholder:text-slate-400"
            />
            <button
              type="submit"
              aria-label="질문 보내기"
              disabled={!prompt.trim()}
              className="grid h-10 w-10 place-items-center rounded-md bg-commerce-ink text-white disabled:bg-slate-300"
            >
              <Send size={17} />
            </button>
          </div>
        </form>

        <div className="mt-4 grid grid-cols-3 gap-2" role="group" aria-label="추천 조합 탭">
          {builds.map((build) => {
            const selected = build.tier === activeBuild.tier;
            return (
              <button
                key={build.id}
                type="button"
                onClick={() => setActiveTier(build.tier)}
                className={`min-h-12 rounded-lg border px-2 py-2 text-sm font-black transition focus:outline-none focus:ring-4 focus:ring-blue-100 ${selected ? 'border-slate-950 bg-slate-950 text-white' : 'border-commerce-line bg-white text-slate-700 hover:border-commerce-ink hover:text-commerce-ink'}`}
              >
                {build.label}
              </button>
            );
          })}
        </div>

        <article className="mt-3 rounded-xl border border-commerce-line bg-white p-4">
          <div className="mb-3 flex flex-wrap items-center gap-2">
            {activeBuild.badges.map((badge) => (
              <span key={badge} className="rounded bg-slate-100 px-2 py-1 text-[11px] font-black text-slate-700">{badge}</span>
            ))}
          </div>
          <h3 className="text-lg font-black text-commerce-ink">{activeBuild.title}</h3>
          <p className="mt-2 break-keep text-xs leading-5 text-slate-500">{activeBuild.summary}</p>
          <div className="mt-4 flex items-end gap-2">
            <span className="text-2xl font-black tracking-tight text-commerce-sale">{activeBuild.totalPrice.toLocaleString()}원</span>
            <span className="pb-1 text-xs font-bold text-commerce-green">호환성 검토 대상</span>
          </div>

          <div className="mt-4 space-y-2">
            {activeBuild.items.slice(0, 4).map((item) => (
              <div key={item.partId} className="flex items-center justify-between gap-3 rounded-lg bg-slate-50 px-3 py-2 text-xs">
                <div className="min-w-0">
                  <div className="font-black text-slate-900">{item.category}</div>
                  <div className="truncate text-slate-500">{item.name}</div>
                </div>
                <div className="font-black text-slate-900">{item.price.toLocaleString()}원</div>
              </div>
            ))}
          </div>

          <div className="mt-4 grid grid-cols-2 gap-2 text-xs">
            <div className="rounded-lg border border-commerce-line bg-slate-50 p-3">
              <div className="flex items-center gap-1 font-black text-slate-900"><Cpu size={14} /> 8개 부품</div>
              <div className="mt-1 text-slate-500">수동 장바구니와 분리</div>
            </div>
            <div className="rounded-lg border border-commerce-line bg-slate-50 p-3">
              <div className="flex items-center gap-1 font-black text-slate-900"><Zap size={14} /> Tool 대상</div>
              <div className="mt-1 text-slate-500">호환성/전력 확인</div>
            </div>
          </div>

          <button
            type="button"
            onClick={selectBuild}
            className="mt-4 flex w-full min-h-11 items-center justify-center gap-2 rounded-lg bg-commerce-ink px-4 py-3 text-sm font-black text-white transition hover:bg-slate-700 focus:outline-none focus:ring-4 focus:ring-blue-100"
          >
            <ShoppingCart size={17} />
            이 조합으로 셀프 견적 보기
          </button>
        </article>

        <div className="mt-3 flex items-center gap-2 text-[11px] font-bold text-slate-500">
          <CheckCircle2 size={14} className="text-commerce-green" />
          실제 API 저장 없이 화면 상태만 저장합니다.
        </div>
      </div>
    </section>
  );
}

function inferTier(prompt: string): AiBuildTier {
  const normalized = prompt.toLowerCase();
  if (normalized.includes('300') || normalized.includes('cuda') || normalized.includes('ai') || normalized.includes('고성능')) {
    return 'performance';
  }
  if (normalized.includes('150') || normalized.includes('가성비') || normalized.includes('저렴')) {
    return 'budget';
  }
  return 'balanced';
}
