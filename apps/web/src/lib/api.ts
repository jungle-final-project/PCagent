export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '';
export const AUTH_CHANGED_EVENT = 'buildgraph-auth-change';
const ACCESS_TOKEN_KEY = 'buildgraph.token';
const REFRESH_TOKEN_KEY = 'buildgraph.refreshToken';

type ErrorResponseBody = {
  code?: unknown;
  message?: unknown;
};

export type AuthChangedDetail = {
  user?: unknown;
};

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly path: string,
    public readonly code?: string,
    message?: string
  ) {
    super(message ?? `API ${status}: ${path}`);
  }
}

export async function api<T>(path: string, init?: RequestInit): Promise<T> {
  const token = localStorage.getItem(ACCESS_TOKEN_KEY);
  const headers = new Headers(init?.headers);
  if (!(init?.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json');
  }
  if (token) headers.set('Authorization', `Bearer ${token}`);

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers
  });

  if (!response.ok) {
    const errorBody = await readErrorResponse(response);
    throw new ApiError(response.status, path, errorBody.code, errorBody.message);
  }

  return response.json() as Promise<T>;
}

async function readErrorResponse(response: Response) {
  try {
    const body = await response.json() as ErrorResponseBody;
    return {
      code: typeof body.code === 'string' ? body.code : undefined,
      message: typeof body.message === 'string' ? body.message : undefined
    };
  } catch {
    return {};
  }
}

function dispatchAuthChanged(detail?: AuthChangedDetail) {
  window.dispatchEvent(new CustomEvent<AuthChangedDetail>(AUTH_CHANGED_EVENT, { detail }));
}

export function saveAuthTokens(accessToken: string, refreshToken: string, user?: unknown) {
  localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
  localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
  dispatchAuthChanged({ user });
}

export function saveToken(token: string) {
  localStorage.setItem(ACCESS_TOKEN_KEY, token);
  dispatchAuthChanged();
}

export function getToken() {
  return localStorage.getItem(ACCESS_TOKEN_KEY);
}

export function getRefreshToken() {
  return localStorage.getItem(REFRESH_TOKEN_KEY);
}

export function clearToken() {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
  dispatchAuthChanged();
}
