import { describe, it, expect, beforeEach } from 'vitest';
import { setActivePinia, createPinia } from 'pinia';
import { useAuthStore } from '@/stores/auth';

// 不 mock @/utils/storage：本用例验证「setToken→storage.set→localStorage.setItem」全链路真实落盘，
// localStorage spy 已由 setup.ts 全局替换提供；mock storage 模块会掐断链路使断言恒红（R1 根因判定）

describe('Auth Store Token Persistence', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    localStorage.clear();
  });

  it('calls storage.set when setToken called with token', () => {
    const store = useAuthStore();
    store.setToken('test-jwt-token');
    expect(localStorage.setItem).toHaveBeenCalledWith('token', '"test-jwt-token"');
  });

  it('calls storage.remove when setToken(null) called', () => {
    const store = useAuthStore();
    store.setToken('test-jwt-token');
    store.setToken(null);
    expect(localStorage.removeItem).toHaveBeenCalledWith('token');
  });
});
