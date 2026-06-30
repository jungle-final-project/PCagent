import { expect, test } from '@playwright/test';

async function openHomeAsUser(page: import('@playwright/test').Page) {
  await page.addInitScript(() => {
    localStorage.setItem('buildgraph.token', 'jwt-user-token');
    sessionStorage.clear();
  });
  await page.route('**/api/auth/me', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        id: 'user-1004',
        email: 'user@example.com',
        name: '테스트 사용자',
        role: 'USER'
      })
    });
  });
  await page.goto('/');
}

async function mockSelfQuoteApis(page: import('@playwright/test').Page) {
  await page.route('**/api/quote-drafts/current**', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        id: 'draft-home-ai-test',
        status: 'ACTIVE',
        name: '셀프 견적',
        items: [],
        totalPrice: 0,
        itemCount: 0
      })
    });
  });

  await page.route('**/api/parts**', async (route) => {
    const url = new URL(route.request().url());
    if (url.pathname.includes('/price-history')) {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          partId: 'ai-gpu-balanced',
          partName: 'AI 균형 RTX 5070',
          currentPrice: 890000,
          days: 3650,
          source: 'NAVER_SHOPPING_SEARCH',
          items: [],
          summary: {
            sampleCount: 0,
            currentPrice: 890000,
            minPrice: 890000,
            maxPrice: 890000,
            firstPrice: 890000,
            lastPrice: 890000,
            changeAmount: 0,
            changeRatePercent: 0
          }
        })
      });
      return;
    }

    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        items: [],
        page: 0,
        size: 20,
        total: 0
      })
    });
  });
}

test('renders a single shopping home without the old hero prompt flow', async ({ page }) => {
  await openHomeAsUser(page);
  const main = page.getByRole('main');

  await expect(main.getByRole('textbox', { name: '원하는 PC 사양 입력' })).toHaveCount(0);
  await expect(main.getByRole('heading', { name: '오늘의 PC 부품 특가' })).toBeVisible();
  await expect(main.getByRole('heading', { name: '부품 바로가기' })).toBeVisible();
  await expect(main.getByRole('heading', { name: '오늘의 추천 견적' })).toBeVisible();
  await expect(main.getByRole('heading', { name: '인기 부품 랭킹' })).toBeVisible();

  for (const label of ['CPU', '메인보드', 'RAM', 'GPU', 'SSD', '파워', '케이스', '쿨러']) {
    await expect(main.getByRole('link', { name: label, exact: true })).toBeVisible();
  }
});

test('opens the collapsed chatbot and shows three recommendation tabs', async ({ page }) => {
  await openHomeAsUser(page);

  await expect(page.getByTestId('ai-chatbot-panel')).toHaveCount(0);
  await page.getByRole('button', { name: 'AI 견적 챗봇 열기' }).click();

  await expect(page.getByTestId('ai-chatbot-panel')).toBeVisible();
  await expect(page.getByRole('textbox', { name: 'AI 챗봇에게 PC 사양 질문' })).toBeVisible();
  await expect(page.getByRole('button', { name: '가성비' })).toBeVisible();
  await expect(page.getByRole('button', { name: '균형' })).toBeVisible();
  await expect(page.getByRole('button', { name: '고성능' })).toBeVisible();
  await expect(page.getByRole('button', { name: '이 조합으로 셀프 견적 보기' })).toBeVisible();
});

test('selects a chatbot recommendation and carries it to self quote as a separate AI panel', async ({ page }) => {
  await mockSelfQuoteApis(page);
  await openHomeAsUser(page);

  await page.getByRole('button', { name: 'AI 견적 챗봇 열기' }).click();
  await page.getByRole('textbox', { name: 'AI 챗봇에게 PC 사양 질문' }).fill('200만원 안에서 QHD 게임용 PC 추천해줘');
  await page.getByRole('button', { name: '질문 보내기' }).click();
  await page.getByRole('button', { name: '균형' }).click();
  await page.getByRole('button', { name: '이 조합으로 셀프 견적 보기' }).click();

  await expect(page).toHaveURL('/self-quote');
  await expect(page.getByTestId('ai-selected-build-panel')).toBeVisible();
  await expect(page.getByRole('heading', { name: 'AI 선택 조합' })).toBeVisible();
  await expect(page.getByText('균형 추천 조합')).toBeVisible();
  await expect(page.getByText('수동 장바구니와 별도 데모 상태')).toBeVisible();
});

test('keeps shared header and navigation destinations unchanged', async ({ page }) => {
  await openHomeAsUser(page);
  const header = page.locator('header');
  const nav = page.getByRole('navigation');

  await expect(header.getByRole('link', { name: 'AI 견적' })).toHaveAttribute('href', '/requirements/new');
  await expect(header.getByRole('link', { name: '내 견적함' })).toHaveAttribute('href', '/my/quotes');
  await expect(header.getByRole('link', { name: 'AS 접수' })).toHaveAttribute('href', '/support/new');
  await expect(nav.getByRole('link', { name: '홈' })).toHaveAttribute('href', '/');
  await expect(nav.getByRole('link', { name: '셀프 견적' })).toHaveAttribute('href', '/self-quote');
  await expect(nav.getByRole('link', { name: '관리자' })).toHaveAttribute('href', '/admin');
});

test('keeps the unified home usable on mobile width', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await openHomeAsUser(page);
  const main = page.getByRole('main');

  await expect(main.getByRole('heading', { name: '오늘의 PC 부품 특가' })).toBeVisible();
  await expect(main.getByRole('heading', { name: '부품 바로가기' })).toBeVisible();
  await page.getByRole('button', { name: 'AI 견적 챗봇 열기' }).click();
  await expect(page.getByTestId('ai-chatbot-panel')).toBeVisible();

  const hasBodyOverflow = await page.evaluate(() => document.documentElement.scrollWidth > window.innerWidth + 1);
  expect(hasBodyOverflow).toBe(false);
});
