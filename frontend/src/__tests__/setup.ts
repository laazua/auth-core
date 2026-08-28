import { vi } from 'vitest';
import { config } from '@vue/test-utils';
import { createPinia } from 'pinia';

vi.mock('vue-router', async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    useRouter: () => ({
      push: vi.fn(),
      replace: vi.fn(),
      go: vi.fn(),
      back: vi.fn(),
      forward: vi.fn(),
    }),
    useRoute: () => ({
      path: '/',
      name: 'Dashboard',
      params: {},
      query: {},
      meta: {},
    }),
  };
});

vi.mock('pinia', async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    defineStore: actual.defineStore,
    createPinia: actual.createPinia,
    useStore: actual.useStore,
    storeToRefs: actual.storeToRefs,
  };
});

config.global.mocks = {
  $t: (key: string) => key,
};

const pinia = createPinia();
config.global.plugins = [pinia];

Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: vi.fn().mockImplementation((query) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn(),
  })),
});

Object.defineProperty(window, 'localStorage', {
  writable: true,
  value: {
    getItem: vi.fn(),
    setItem: vi.fn(),
    removeItem: vi.fn(),
    clear: vi.fn(),
  },
});
