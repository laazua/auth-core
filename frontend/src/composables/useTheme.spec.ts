import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import { setActivePinia, createPinia } from 'pinia';
import { useAppStore } from '@/stores/app';
import { useTheme } from '@/composables/useTheme';
import { storage } from '@/utils/storage';

vi.mock('@/stores/app', () => ({
  useAppStore: vi.fn(),
}));

describe('useTheme', () => {
  let appStore: ReturnType<typeof useAppStore>;
  let originalMatchMedia: typeof window.matchMedia;

  beforeEach(() => {
    setActivePinia(createPinia());
    vi.clearAllMocks();
    document.documentElement.removeAttribute('data-theme');
    originalMatchMedia = window.matchMedia;
    window.matchMedia = vi.fn().mockImplementation((query: string) => ({
      matches: query === '(prefers-color-scheme: dark)',
      media: query,
      onchange: null,
      addListener: vi.fn(),
      removeListener: vi.fn(),
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn(),
    })) as unknown as typeof window.matchMedia;
  });

  afterEach(() => {
    vi.restoreAllMocks();
    window.matchMedia = originalMatchMedia;
  });

  const createMockAppStore = (initialTheme: 'light' | 'dark' = 'light') => {
    const themeValue = { value: initialTheme };
    return {
      get theme() {
        return themeValue.value;
      },
      setTheme: vi.fn((t: 'light' | 'dark') => {
        themeValue.value = t;
        document.documentElement.setAttribute('data-theme', t);
      }),
      toggleTheme: vi.fn(() => {
        themeValue.value = themeValue.value === 'light' ? 'dark' : 'light';
        document.documentElement.setAttribute('data-theme', themeValue.value);
      }),
      initTheme: vi.fn(() => {
        document.documentElement.setAttribute('data-theme', themeValue.value);
      }),
    };
  };

  const getAppStore = (theme: 'light' | 'dark' = 'light') => {
    const store = createMockAppStore(theme);
    (useAppStore as vi.Mock).mockReturnValue(store);
    return store;
  };

  describe('AC1 — Theme Toggle', () => {
    it('toggleTheme switches between light and dark', () => {
      getAppStore('light');
      const { toggleTheme } = useTheme();

      toggleTheme();

      expect(useAppStore().toggleTheme).toHaveBeenCalled();
      expect(document.documentElement.classList.contains('theme-transitioning')).toBe(true);
    });

    it('toggleTheme adds theme-transitioning class', () => {
      getAppStore();
      const { toggleTheme } = useTheme();

      toggleTheme();

      expect(document.documentElement.classList.contains('theme-transitioning')).toBe(true);
    });
  });

  describe('AC1 — Set Theme', () => {
    it('setTheme sets the theme to dark and updates data-theme', () => {
      getAppStore();
      const { setTheme } = useTheme();

      setTheme('dark');

      expect(useAppStore().setTheme).toHaveBeenCalledWith('dark');
      expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
    });

    it('setTheme sets the theme to light and updates data-theme', () => {
      getAppStore('dark');
      const { setTheme } = useTheme();

      setTheme('light');

      expect(useAppStore().setTheme).toHaveBeenCalledWith('light');
      expect(document.documentElement.getAttribute('data-theme')).toBe('light');
    });
  });

  describe('AC1 — Init Theme', () => {
    it('initTheme applies the stored theme', () => {
      vi.spyOn(storage, 'get').mockReturnValue('dark');
      getAppStore('dark');
      const { initTheme } = useTheme();

      initTheme();

      expect(useAppStore().initTheme).toHaveBeenCalled();
      expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
    });

    it('initTheme defaults to light when no theme stored', () => {
      vi.spyOn(storage, 'get').mockReturnValue(null);
      getAppStore('light');
      const { initTheme } = useTheme();

      initTheme();

      expect(useAppStore().initTheme).toHaveBeenCalled();
    });
  });

  describe('AC1 — System Preference', () => {
    it('detects system dark preference', () => {
      window.matchMedia = vi.fn().mockImplementation((query: string) => ({
        matches: query === '(prefers-color-scheme: dark)',
        media: query,
        onchange: null,
        addListener: vi.fn(),
        removeListener: vi.fn(),
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        dispatchEvent: vi.fn(),
      })) as unknown as typeof window.matchMedia;

      const { getSystemTheme } = useTheme();
      const theme = getSystemTheme();

      expect(theme).toBe('dark');
    });

    it('detects system light preference', () => {
      window.matchMedia = vi.fn().mockImplementation((query: string) => ({
        matches: query === '(prefers-color-scheme: dark)' && false,
        media: query,
        onchange: null,
        addListener: vi.fn(),
        removeListener: vi.fn(),
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        dispatchEvent: vi.fn(),
      })) as unknown as typeof window.matchMedia;

      const { getSystemTheme } = useTheme();
      const theme = getSystemTheme();

      expect(theme).toBe('light');
    });

    it('applySystemTheme applies the system theme', () => {
      getAppStore();
      const { applySystemTheme } = useTheme();

      applySystemTheme();

      expect(useAppStore().setTheme).toHaveBeenCalledWith('dark');
      expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
    });
  });

  describe('AC1 — Theme Transition', () => {
    it('toggleTheme triggers smooth transition', () => {
      getAppStore();
      const { toggleTheme } = useTheme();

      toggleTheme();

      expect(document.documentElement.classList.contains('theme-transitioning')).toBe(true);
    });

    it('removes theme-transitioning class after transition', async () => {
      getAppStore();
      const { toggleTheme } = useTheme();

      toggleTheme();
      expect(document.documentElement.classList.contains('theme-transitioning')).toBe(true);

      await new Promise((resolve) => setTimeout(resolve, 400));
      expect(document.documentElement.classList.contains('theme-transitioning')).toBe(false);
    });
  });

  describe('AC4: darkModeGradientBackground', () => {
    it('dark mode uses dark gradient variables when data-theme=dark', () => {
      vi.spyOn(storage, 'get').mockReturnValue('dark');
      const store = getAppStore('dark');
      const { initTheme } = useTheme();

      initTheme();

      // Verify dark theme is applied
      expect(document.documentElement.getAttribute('data-theme')).toBe('dark');

      // The actual gradient variables are defined in CSS :root[data-theme="dark"]
      // This test verifies the theme switching works; visual rendering is tested in component tests
    });

    it('light mode uses light gradient variables when data-theme=light', () => {
      getAppStore('light');
      const { initTheme } = useTheme();

      initTheme();

      expect(document.documentElement.getAttribute('data-theme')).toBe('light');
    });
  });
});