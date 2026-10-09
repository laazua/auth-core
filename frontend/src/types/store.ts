import type { UserInfo } from './auth';
import type { MenuConfig } from '@/composables/useMenu';

export interface AuthState {
  token: string | null;
  userInfo: UserInfo | null;
  roles: string[];
  permissions: string[];
  isAuthenticated: boolean;
}

export interface TagView {
  path: string;
  title: string;
  icon?: string;
  fullPath: string;
  closable: boolean;
  affix?: boolean;
}

export interface AppState {
  sidebarCollapsed: boolean;
  sidebarOpened: boolean;
  theme: 'light' | 'dark';
  breadcrumbs: BreadcrumbItem[];
  device: 'desktop' | 'tablet' | 'mobile';
  isMobile: boolean;
  menuList: MenuConfig[];
  tagsViewList: TagView[];
  cachedViews: string[];
  activeTag: string;
}

export interface BreadcrumbItem {
  path: string;
  title: string;
  icon?: string;
}
