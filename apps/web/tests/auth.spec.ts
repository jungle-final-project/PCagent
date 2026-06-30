import { expect, test } from '@playwright/test';

test('shows login API error message and does not save tokens', async ({ page }) => {
  await page.route('**/api/auth/login', async (route) => {
    expect(JSON.parse(route.request().postData() ?? '{}')).toEqual({
      email: 'user@example.com',
      password: 'wrong-password'
    });
    await route.fulfill({
      status: 401,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 'UNAUTHORIZED',
        message: '이메일 또는 비밀번호가 올바르지 않습니다.'
      })
    });
  });

  await page.goto('/login');
  await page.getByLabel('이메일').fill('user@example.com');
  await page.getByLabel('비밀번호').fill('wrong-password');
  await page.getByRole('button', { name: '로그인' }).click();

  await expect(page.getByText('이메일 또는 비밀번호가 올바르지 않습니다.')).toBeVisible();
  expect(await page.evaluate(() => localStorage.getItem('buildgraph.token'))).toBeNull();
  expect(await page.evaluate(() => localStorage.getItem('buildgraph.refreshToken'))).toBeNull();
});

test('updates header from login response before auth me finishes', async ({ page }) => {
  await page.route('**/api/auth/login', async (route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        accessToken: 'jwt-fast-user',
        refreshToken: 'refresh-fast-user',
        user: {
          id: '00000000-0000-4000-8000-000000001088',
          email: 'fast@example.com',
          name: 'Fast User',
          role: 'USER'
        }
      })
    });
  });
  await page.route('**/api/auth/me', async (route) => {
    await new Promise((resolve) => setTimeout(resolve, 2_000));
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({
        id: '00000000-0000-4000-8000-000000001088',
        email: 'fast@example.com',
        name: 'Fast User',
        role: 'USER'
      })
    });
  });

  await page.goto('/login');
  await page.getByLabel('이메일').fill('fast@example.com');
  await page.getByLabel('비밀번호').fill('passw0rd!');
  await page.getByRole('button', { name: '로그인' }).click();

  await expect(page.getByText('로그인됨 · fast@example.com · USER')).toBeVisible({ timeout: 500 });
  await expect(page.getByText('Fast User')).toBeVisible({ timeout: 500 });
});

test('submits signup form with the OpenAPI user payload', async ({ page }) => {
  await page.route('**/api/users', async (route) => {
    expect(JSON.parse(route.request().postData() ?? '{}')).toEqual({
      name: '홍길동',
      email: 'new-user@example.com',
      password: 'passw0rd!',
      termsAccepted: true,
      marketingAccepted: false
    });
    await route.fulfill({
      status: 201,
      contentType: 'application/json',
      body: JSON.stringify({
        id: '00000000-0000-4000-8000-000000001099',
        email: 'new-user@example.com',
        name: '홍길동',
        role: 'USER'
      })
    });
  });

  await page.goto('/signup');
  await page.getByLabel('이름').fill('홍길동');
  await page.getByLabel('이메일').fill('new-user@example.com');
  await page.getByLabel('비밀번호', { exact: true }).fill('passw0rd!');
  await page.getByLabel('비밀번호 확인').fill('passw0rd!');
  await page.getByLabel('서비스 이용약관 및 로그 업로드 정책 확인').check();
  await page.getByRole('button', { name: '회원가입' }).click();

  await expect(page).toHaveURL('/login');
});

test('shows signup API error message', async ({ page }) => {
  await page.route('**/api/users', async (route) => {
    await route.fulfill({
      status: 409,
      contentType: 'application/json',
      body: JSON.stringify({
        code: 'DUPLICATE_RESOURCE',
        message: '이미 가입된 이메일입니다.'
      })
    });
  });

  await page.goto('/signup');
  await page.getByLabel('이름').fill('홍길동');
  await page.getByLabel('이메일').fill('user@example.com');
  await page.getByLabel('비밀번호', { exact: true }).fill('passw0rd!');
  await page.getByLabel('비밀번호 확인').fill('passw0rd!');
  await page.getByLabel('서비스 이용약관 및 로그 업로드 정책 확인').check();
  await page.getByRole('button', { name: '회원가입' }).click();

  await expect(page.getByText('이미 가입된 이메일입니다.')).toBeVisible();
  await expect(page).toHaveURL('/signup');
});
