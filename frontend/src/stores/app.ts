import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { storage } from '@/utils/storage';
import type { BreadcrumbItem } from '@/types/store';

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref<boolean>(storage.get('sidebarCollapsed') || false);
  const theme = ref<'light' | 'dark'>(storage.get('theme') || 'light');
  const breadcrumbs = ref<BreadcrumbItem[]>([]);
  const device = ref<'desktop' | 'tablet' | 'mobile'>('desktop');

  const isMobile = computed(() => device.value === 'mobile');

  const toggleSidebar = () => {
    sidebarCollapsed.value = !sidebarCollapsed.value;
    storage.set('sidebarCollapsed', sidebarCollapsed.value);
  };

  const setSidebarCollapsed = (collapsed: boolean) => {
    sidebarCollapsed.value = collapsed;
    storage.set('sidebarCollapsed', collapsed);
  };

  const toggleTheme = () => {
    theme.value = theme.value === 'light' ? 'dark' : 'light';
    storage.set('theme', theme.value);
    applyTheme(theme.value);
  };

  const setTheme = (newTheme: 'light' | 'dark') => {
    theme.value = newTheme;
    storage.set('theme', newTheme);
    applyTheme(newTheme);
  };

  const applyTheme = (newTheme: 'light' | 'dark') => {
    document.documentElement.setAttribute('data-theme', newTheme);
  };

  const setBreadcrumbs = (crumbs: BreadcrumbItem[]) => {
    breadcrumbs.value = crumbs;
  };

  const setDevice = (newDevice: 'desktop' | 'tablet' | 'mobile') => {
    device.value = newDevice;
  };

  const initTheme = () => {
    applyTheme(theme.value);
  };

  return {
    sidebarCollapsed,
    theme,
    breadcrumbs,
    device,
    isMobile,
    toggleSidebar,
    setSidebarCollapsed,
    toggleTheme,
    setTheme,
    setBreadcrumbs,
    setDevice,
    initTheme,
  };
});
