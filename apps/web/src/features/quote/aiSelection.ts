export const AI_SELECTED_BUILD_STORAGE_KEY = 'buildgraph.ai.selectedBuild';
export const AI_SELECTED_BUILD_CHANGED_EVENT = 'buildgraph.ai.selectedBuildChanged';

export type AiBuildTier = 'budget' | 'balanced' | 'performance';

export type AiBuildItem = {
  partId: string;
  category: string;
  name: string;
  manufacturer: string;
  quantity: number;
  price: number;
  note: string;
};

export type AiRecommendedBuild = {
  id: string;
  tier: AiBuildTier;
  label: string;
  title: string;
  summary: string;
  totalPrice: number;
  badges: string[];
  items: AiBuildItem[];
};

export type AiSelectedBuild = Omit<AiRecommendedBuild, 'label' | 'badges'> & {
  selectedAt: string;
};

const aiBuilds: Record<AiBuildTier, AiRecommendedBuild> = {
  budget: {
    id: 'ai-budget',
    tier: 'budget',
    label: '가성비',
    title: '가성비 추천 조합',
    summary: '150만원대에서 FHD 게임, 학습, 문서 작업을 안정적으로 처리하는 데모 조합입니다.',
    totalPrice: 1490000,
    badges: ['150만원대', 'FHD', '업그레이드 여지'],
    items: [
      { partId: 'ai-cpu-budget', category: 'CPU', name: 'Ryzen 5 가성비 CPU', manufacturer: 'AMD', quantity: 1, price: 260000, note: '게임과 일반 작업 균형' },
      { partId: 'ai-board-budget', category: 'MOTHERBOARD', name: 'B850M 기본형 메인보드', manufacturer: 'ASRock', quantity: 1, price: 165000, note: 'AM5 업그레이드 여지' },
      { partId: 'ai-ram-budget', category: 'RAM', name: 'DDR5 32GB 메모리', manufacturer: 'Samsung', quantity: 1, price: 120000, note: '멀티태스킹 기본 용량' },
      { partId: 'ai-gpu-budget', category: 'GPU', name: 'RTX 5060 Ti 가성비 GPU', manufacturer: 'NVIDIA', quantity: 1, price: 560000, note: 'FHD 고주사율 중심' },
      { partId: 'ai-storage-budget', category: 'STORAGE', name: 'NVMe 1TB SSD', manufacturer: 'SK hynix', quantity: 1, price: 105000, note: '게임과 작업 저장공간' },
      { partId: 'ai-psu-budget', category: 'PSU', name: 'ATX 3.1 750W 파워', manufacturer: 'Micronics', quantity: 1, price: 118000, note: '중급 GPU 여유 전력' },
      { partId: 'ai-case-budget', category: 'CASE', name: '메쉬 미들타워 케이스', manufacturer: 'darkFlash', quantity: 1, price: 96000, note: '흡기 중심 구조' },
      { partId: 'ai-cooler-budget', category: 'COOLER', name: '싱글타워 공랭 쿨러', manufacturer: 'PCCOOLER', quantity: 1, price: 66000, note: '기본 소음 억제' }
    ]
  },
  balanced: {
    id: 'ai-balanced',
    tier: 'balanced',
    label: '균형',
    title: '균형 추천 조합',
    summary: 'QHD 게임과 개발 작업을 함께 고려한 데모 조합입니다.',
    totalPrice: 1980000,
    badges: ['QHD', '개발 병행', '호환성 여유'],
    items: [
      { partId: 'ai-cpu-balanced', category: 'CPU', name: 'Ryzen 7 AI 균형 CPU', manufacturer: 'AMD', quantity: 1, price: 420000, note: '게임과 개발 균형' },
      { partId: 'ai-board-balanced', category: 'MOTHERBOARD', name: 'B850 Wi-Fi 메인보드', manufacturer: 'MSI', quantity: 1, price: 245000, note: '확장성과 무선 연결' },
      { partId: 'ai-ram-balanced', category: 'RAM', name: 'DDR5 32GB 튜닝 메모리', manufacturer: 'TeamGroup', quantity: 1, price: 148000, note: 'IDE, 브라우저 병행' },
      { partId: 'ai-gpu-balanced', category: 'GPU', name: 'AI 균형 RTX 5070', manufacturer: 'NVIDIA', quantity: 1, price: 890000, note: 'QHD 게임 핵심 부품' },
      { partId: 'ai-storage-balanced', category: 'STORAGE', name: 'NVMe 2TB SSD', manufacturer: 'Samsung', quantity: 1, price: 185000, note: '프로젝트와 게임 동시 저장' },
      { partId: 'ai-psu-balanced', category: 'PSU', name: 'ATX 3.1 850W 골드 파워', manufacturer: 'Corsair', quantity: 1, price: 175000, note: '피크 전력 여유' },
      { partId: 'ai-case-balanced', category: 'CASE', name: '쿨링 강화 미들타워', manufacturer: 'Fractal', quantity: 1, price: 132000, note: 'GPU 장착 여유' },
      { partId: 'ai-cooler-balanced', category: 'COOLER', name: '듀얼타워 공랭 쿨러', manufacturer: 'DeepCool', quantity: 1, price: 85000, note: '소음과 온도 균형' }
    ]
  },
  performance: {
    id: 'ai-performance',
    tier: 'performance',
    label: '고성능',
    title: '고성능 추천 조합',
    summary: 'AI CUDA 실습, QHD 고주사율, 장시간 작업을 우선한 데모 조합입니다.',
    totalPrice: 2980000,
    badges: ['CUDA', '고주사율', '전력 여유'],
    items: [
      { partId: 'ai-cpu-performance', category: 'CPU', name: 'Ryzen 9 고성능 CPU', manufacturer: 'AMD', quantity: 1, price: 680000, note: '멀티코어 작업 우선' },
      { partId: 'ai-board-performance', category: 'MOTHERBOARD', name: 'X870E 확장형 메인보드', manufacturer: 'ASUS', quantity: 1, price: 410000, note: '고성능 부품 확장' },
      { partId: 'ai-ram-performance', category: 'RAM', name: 'DDR5 64GB 메모리', manufacturer: 'G.Skill', quantity: 1, price: 285000, note: 'AI/개발 작업 여유' },
      { partId: 'ai-gpu-performance', category: 'GPU', name: 'RTX 5080 CUDA GPU', manufacturer: 'NVIDIA', quantity: 1, price: 1360000, note: 'CUDA와 QHD 고주사율' },
      { partId: 'ai-storage-performance', category: 'STORAGE', name: 'NVMe 2TB 고성능 SSD', manufacturer: 'WD', quantity: 1, price: 220000, note: '대용량 프로젝트' },
      { partId: 'ai-psu-performance', category: 'PSU', name: 'ATX 3.1 1000W 골드 파워', manufacturer: 'Seasonic', quantity: 1, price: 260000, note: '전력 피크 대응' },
      { partId: 'ai-case-performance', category: 'CASE', name: '하이플로우 풀타워 케이스', manufacturer: 'Lian Li', quantity: 1, price: 190000, note: '대형 GPU 장착 여유' },
      { partId: 'ai-cooler-performance', category: 'COOLER', name: '360mm 수랭 쿨러', manufacturer: 'ARCTIC', quantity: 1, price: 175000, note: '장시간 부하 대응' }
    ]
  }
};

export function getAiRecommendedBuilds(prompt: string): AiRecommendedBuild[] {
  const normalized = prompt.toLowerCase();
  if (normalized.includes('300') || normalized.includes('cuda') || normalized.includes('ai') || normalized.includes('고성능')) {
    return [aiBuilds.performance, aiBuilds.balanced, aiBuilds.budget];
  }
  if (normalized.includes('150') || normalized.includes('가성비') || normalized.includes('저렴')) {
    return [aiBuilds.budget, aiBuilds.balanced, aiBuilds.performance];
  }
  return [aiBuilds.balanced, aiBuilds.budget, aiBuilds.performance];
}

export function defaultAiBuild() {
  return aiBuilds.balanced;
}

export function toSelectedAiBuild(build: AiRecommendedBuild): AiSelectedBuild {
  const { label: _label, badges: _badges, ...selectedBuild } = build;
  return {
    ...selectedBuild,
    selectedAt: new Date().toISOString()
  };
}

export function saveSelectedAiBuild(build: AiRecommendedBuild) {
  if (typeof window === 'undefined') return;
  const selectedBuild = toSelectedAiBuild(build);
  window.sessionStorage.setItem(AI_SELECTED_BUILD_STORAGE_KEY, JSON.stringify(selectedBuild));
  window.dispatchEvent(new Event(AI_SELECTED_BUILD_CHANGED_EVENT));
}

export function readSelectedAiBuild(): AiSelectedBuild | null {
  if (typeof window === 'undefined') return null;
  try {
    const raw = window.sessionStorage.getItem(AI_SELECTED_BUILD_STORAGE_KEY);
    return raw ? JSON.parse(raw) as AiSelectedBuild : null;
  } catch {
    return null;
  }
}

export function clearSelectedAiBuild() {
  if (typeof window === 'undefined') return;
  window.sessionStorage.removeItem(AI_SELECTED_BUILD_STORAGE_KEY);
  window.dispatchEvent(new Event(AI_SELECTED_BUILD_CHANGED_EVENT));
}
