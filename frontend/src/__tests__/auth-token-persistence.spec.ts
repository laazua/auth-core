import { describe, it, expect, beforeEach, vi } from 'vitest';
import { setActivePinia, createPinia } from 'pinia';
import { useAuthStore } from '@/stores/auth';

vi.mock('@/utils/storage');

describe('Auth Store Token Persistence', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    (localStorage as any).clear();
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