import { expect, test, type Page } from '@playwright/test';

const ticket = {
  id: 'support-ticket-001',
  status: 'OPEN',
  analysisStatus: 'NOT_STARTED',
  reviewStatus: 'NOT_REQUIRED',
  supportDecision: null,
  riskLevel: null,
  symptom: '게임 중 GPU 온도가 급격히 올라갑니다.',
  logUploadId: 'log-upload-001',
  assignedAdminId: null,
  causeCandidates: [],
  upgradeCandidates: [],
  adminNote: null,
  remoteSupportLink: null,
  remoteSupportStatus: null,
  safetyAdviceLevel: null,
  safetyNotices: [],
  visitSupportRequired: false,
  createdAt: '2026-07-06T01:00:00Z'
};

test('shows support chat launcher with AS intake guidance when no chat session exists', async ({ page }) => {
  await mockLoggedInUser(page);
  await mockQuoteDraft(page);
  await mockTicket(page);
  await page.route('**/api/support/chat-sessions/current', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ contact: null, messages: [], pollingIntervalMs: 5000 })
    });
  });

  await page.goto('/support/support-ticket-001');

  await expect(page.getByRole('button', { name: 'PC Agent 상담 열기' })).toBeVisible();
  await page.getByRole('button', { name: 'PC Agent 상담 열기' }).click();
  await expect(page.getByText('AS 접수 후 상담이 시작됩니다.')).toBeVisible();
  await expect(page.getByRole('link', { name: 'AS 접수로 이동' })).toHaveAttribute('href', '/support/new');
});

test('keeps support chat hidden on excluded support entry routes', async ({ page }) => {
  await mockLoggedInUser(page);
  await mockQuoteDraft(page);

  await page.goto('/support/new');

  await expect(page.getByRole('button', { name: 'PC Agent 상담 열기' })).toHaveCount(0);
});

test('keeps existing support chat message flow available for active sessions', async ({ page }) => {
  let postPayload: unknown;
  const initialSession = {
    contact: {
      id: 'chat-session-001',
      status: 'ACTIVE',
      supportRequestType: 'REMOTE',
      title: 'GPU 온도 상담',
      lastMessagePreview: '상담이 시작되었습니다.',
      adminUnreadCount: 0,
      userUnreadCount: 0,
      ticketId: 'support-ticket-001'
    },
    messages: [
      {
        id: 'message-001',
        role: 'SYSTEM',
        content: '접수되었습니다. 홈페이지에서 채팅 상담을 진행하세요.',
        createdAt: '2026-07-06T01:00:00Z'
      }
    ],
    pollingIntervalMs: 5000
  };
  await mockLoggedInUser(page);
  await mockQuoteDraft(page);
  await mockTicket(page);
  await page.route('**/api/support/chat-sessions/current', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(initialSession)
    });
  });
  await page.route('**/api/support/chat-sessions/chat-session-001/messages', async (route) => {
    if (route.request().method() === 'POST') {
      postPayload = route.request().postDataJSON();
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({
          ...initialSession,
          messages: [
            ...initialSession.messages,
            {
              id: 'message-002',
              role: 'USER',
              content: '지금 원격 확인이 가능할까요?',
              createdAt: '2026-07-06T01:01:00Z'
            }
          ]
        })
      });
      return;
    }
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(initialSession)
    });
  });

  await page.goto('/support/support-ticket-001');
  await page.getByRole('button', { name: 'PC Agent 상담 열기' }).click();
  await expect(page.getByText('접수되었습니다. 홈페이지에서 채팅 상담을 진행하세요.')).toBeVisible();
  await page.getByPlaceholder('메시지를 입력하세요').fill('지금 원격 확인이 가능할까요?');
  await page.getByRole('button', { name: '전송' }).click();

  await expect.poll(() => postPayload).toEqual({ content: '지금 원격 확인이 가능할까요?' });
});

async function mockLoggedInUser(page: Page) {
  await page.addInitScript(() => {
    localStorage.setItem('buildgraph.token', 'jwt-user-token');
    localStorage.setItem('buildgraph.authUser', JSON.stringify({
      id: 'user-001',
      email: 'user@example.com',
      name: '테스트 사용자',
      role: 'USER'
    }));
  });
  await page.route('**/api/auth/me', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        id: 'user-001',
        email: 'user@example.com',
        name: '테스트 사용자',
        role: 'USER'
      })
    });
  });
}

async function mockQuoteDraft(page: Page) {
  await page.route('**/api/quote-drafts/current', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        id: null,
        status: 'EMPTY',
        name: '빈 견적',
        items: [],
        totalPrice: 0,
        itemCount: 0
      })
    });
  });
}

async function mockTicket(page: Page) {
  await page.route(/\/api\/as-tickets\/[^/]+$/, async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(ticket)
    });
  });
}
