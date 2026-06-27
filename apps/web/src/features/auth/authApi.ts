import { api } from '../../lib/api';

export type LoginResponse = {
  token: string;
  user: {
    id: string;
    email: string;
    role: 'USER' | 'ADMIN';
  };
};

export function login(email: string, password: string) {
  return api<LoginResponse>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email, password })
  });
}

export function signup(name: string, email: string, password: string) {
  return api('/api/users', {
    method: 'POST',
    body: JSON.stringify({ name, email, password })
  });
}
