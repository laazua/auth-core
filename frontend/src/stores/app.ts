import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { storage } from '@/utils/storage';
import type { BreadcrumbItem } from '@/types/store';
import type { MenuConfig } from '@/composables/useMenu';

export interface TagView {
  path: string;
  title: string;
  icon?: string;
  fullPath: string;
  closable: boolean;
  affix?: boolean;
}

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref<boolean>(storage.get('sidebarCollapsed') || false);
  const sidebarOpened = ref<boolean>(false);
  const theme = ref<'light' | 'dark'>(storage.get('theme') || 'light');
  const breadcrumbs = ref<BreadcrumbItem[]>([]);
  const device = ref<'desktop' | 'tablet' | 'mobile'>('desktop');
  const menuList = ref<MenuConfig[]>([]);
  const tagsViewList = ref<TagView[]>([]);
  const cachedViews = ref<string[]>([]);
  const activeTag = ref<string>('/dashboard');

  const isMobile = computed(() => device.value === 'mobile');

  const toggleSidebar = () => {
    sidebarCollapsed.value = !sidebarCollapsed.value;
    storage.set('sidebarCollapsed', sidebarCollapsed.value);
  };

  const setSidebarCollapsed = (collapsed: boolean) => {
    sidebarCollapsed.value = collapsed;
    storage.set('sidebarCollapsed', collapsed);
  };

  const setSidebarOpened = (opened: boolean) => {
    sidebarOpened.value = opened;
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

  const setMenuList = (menus: MenuConfig[]) => {
    menuList.value = menus;
  };

  const addTag = (tag: TagView) => {
    if (tagsViewList.value.some((t) => t.fullPath === tag.fullPath)) {
      activeTag.value = tag.fullPath;
      return;
    }
    tagsViewList.value.push(tag);
    if (!tag.affix) {
      cachedViews.value.push(tag.fullPath);
    }
    activeTag.value = tag.fullPath;
    persistTags();
  };

  const removeTag = (fullPath: string) => {
    const index = tagsViewList.value.findIndex((t) => t.fullPath === fullPath);
    if (index === -1) return;

    const tag = tagsViewList.value[index];
    if (tag.affix) return;

    tagsViewList.value.splice(index, 1);
    cachedViews.value = cachedViews.value.filter((v) => v !== fullPath);

    if (activeTag.value === fullPath) {
      const nextTag = tagsViewList.value[index] || tagsViewList.value[index - 1];
      activeTag.value = nextTag?.fullPath || '/dashboard';
    }
    persistTags();
  };

  const removeOtherTags = (fullPath: string) => {
    tagsViewList.value = tagsViewList.value.filter((t) => t.fullPath === fullPath || t.affix);
    cachedViews.value = cachedViews.value.filter((v) => v === fullPath);
    activeTag.value = fullPath;
    persistTags();
  };

  const removeAllTags = () => {
    const affixTags = tagsViewList.value.filter((t) => t.affix);
    tagsViewList.value = affixTags;
    cachedViews.value = [];
    activeTag.value = affixTags[0]?.fullPath || '/dashboard';
    persistTags();
  };

  const setActiveTag = (fullPath: string) => {
    activeTag.value = fullPath;
  };

  const reorderTags = (fromIndex: number, toIndex: number) => {
    if (fromIndex < 0 || fromIndex >= tagsViewList.value.length) return;
    if (toIndex < 0 || toIndex >= tagsViewList.value.length) return;

    const [removed] = tagsViewList.value.splice(fromIndex, 1);
    tagsViewList.value.splice(toIndex, 0, removed);
    persistTags();
  };

  const persistTags = () => {
    storage.set('tagsViewList', tagsViewList.value);
    storage.set('cachedViews', cachedViews.value);
    storage.set('activeTag', activeTag.value);
  };

  const restoreTags = () => {
    const savedTags = storage.get('tagsViewList');
    const savedCached = storage.get('cachedViews');
    const savedActive = storage.get('activeTag');

    if (savedTags && Array.isArray(savedTags)) {
      tagsViewList.value = savedTags;
    }
    if (savedCached && Array.isArray(savedCached)) {
      cachedViews.value = savedCached;
    }
    if (savedActive) {
      activeTag.value = savedActive;
    }
  };

  return {
    sidebarCollapsed,
    sidebarOpened,
    theme,
    breadcrumbs,
    device,
    isMobile,
    menuList,
    tagsViewList,
    cachedViews,
    activeTag,
    toggleSidebar,
    setSidebarCollapsed,
    setSidebarOpened,
    toggleTheme,
    setTheme,
    applyTheme,
    setBreadcrumbs,
    setDevice,
    initTheme,
    setMenuList,
    addTag,
    removeTag,
    removeOtherTags,
    removeAllTags,
    setActiveTag,
    reorderTags,
    persistTags,
    restoreTags,
  };
});