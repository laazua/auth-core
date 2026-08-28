import type { UserInfo } from './auth';

export interface AuthState {
  token: string | null;
  userInfo: UserInfo | null;
  roles: string[];
  permissions: string[];
  isAuthenticated: boolean;
}

export interface AppState {
  sidebarCollapsed: boolean;
  theme: 'light' | 'dark';
  breadcrumbs: BreadcrumbItem[];
  device: 'desktop' | 'tablet' | 'mobile';
  isMobile: boolean;
}

export interface BreadcrumbItem {
  path: string;
  title: string;
  icon?: string;
}
