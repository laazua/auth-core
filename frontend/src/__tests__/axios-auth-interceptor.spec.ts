import { describe, it, expect, beforeEach, vi } from 'vitest';
import { setActivePinia, createPinia } from 'pinia';
import axios, { InternalAxiosRequestConfig } from 'axios';
import { http } from '@/api/http';
import { useAuthStore } from '@/stores/auth';

vi.mock('@/stores/auth', () => ({
  useAuthStore: vi.fn(),
}));

vi.mock('@/router', () => ({
  default: { push: vi.fn() },
}));

describe('Axios Auth Interceptor', () => {
  let authStore: ReturnType<typeof useAuthStore>;

  beforeEach(() => {
    setActivePinia(createPinia());
    authStore = { token: null } as ReturnType<typeof useAuthStore>;
    vi.mocked(useAuthStore).mockReturnValue(authStore as any);
  });

  it('adds Authorization header when token exists', async () => {
    authStore.token = 'test-token';
    const config: InternalAxiosRequestConfig = {
      headers: new axios.AxiosHeaders(),
      url: '/test',
    } as InternalAxiosRequestConfig;
    const handlers = http.interceptors.request.handlers as any[];
    expect(handlers).toBeDefined();
    expect(handlers.length).toBeGreaterThan(0);
    const requestInterceptor = handlers[0]?.fulfilled;
    expect(requestInterceptor).toBeDefined();
    const result = await requestInterceptor!(config);
    expect(result.headers.Authorization).toBe('Bearer test-token');
  });
});