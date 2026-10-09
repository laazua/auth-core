import { computed } from 'vue';
import { useAppStore } from '@/stores/app';
import { storage } from '@/utils/storage';

export function useTheme() {
  const appStore = useAppStore();

  const isDark = computed(() => appStore.theme === 'dark');

  const toggleTheme = () => {
    document.documentElement.classList.add('theme-transitioning');
    setTimeout(() => {
      document.documentElement.classList.remove('theme-transitioning');
    }, 350);
    appStore.toggleTheme();
  };

  const setTheme = (theme: 'light' | 'dark') => {
    document.documentElement.classList.add('theme-transitioning');
    setTimeout(() => {
      document.documentElement.classList.remove('theme-transitioning');
    }, 350);
    appStore.setTheme(theme);
  };

  const initTheme = () => {
    const savedTheme = (storage.get('theme') as 'light' | 'dark' | 'system' | null) || 'light';
    const systemTheme = getSystemTheme();
    const theme = savedTheme === 'system' ? systemTheme : savedTheme;
    appStore.initTheme();
    if (theme !== appStore.theme) {
      appStore.setTheme(theme as 'light' | 'dark');
    }
  };

  const getSystemTheme = (): 'light' | 'dark' => {
    if (typeof window === 'undefined') return 'light';
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  };

  const applySystemTheme = () => {
    const systemTheme = getSystemTheme();
    setTheme(systemTheme);
  };

  const isSystemTheme = computed(() => {
    const saved = storage.get('theme') as 'light' | 'dark' | 'system' | null;
    return saved === 'system';
  });

  return {
    isDark,
    toggleTheme,
    setTheme,
    initTheme,
    getSystemTheme,
    applySystemTheme,
    isSystemTheme,
  };
}
