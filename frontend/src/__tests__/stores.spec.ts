import { describe, it, expect, beforeEach, vi } from 'vitest';
import { setActivePinia, createPinia } from 'pinia';
import { useAuthStore } from '@/stores/auth';
import { useAppStore } from '@/stores/app';

vi.mock('@/utils/storage', () => ({
  storage: {
    get: vi.fn(),
    set: vi.fn(),
    remove: vi.fn(),
    clear: vi.fn(),
  },
}));

describe('Pinia Stores', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.clearAllMocks();
  });

  describe('auth store', () => {
    it('should set and get token correctly', () => {
      const store = useAuthStore();
      const token = 'test-jwt-token-123';

      store.setToken(token);

      expect(store.token).toBe(token);
      expect(store.isAuthenticated).toBe(true);
    });

    it('should clear token on logout', () => {
      const store = useAuthStore();
      store.setToken('token-to-clear');

      store.logout();

      expect(store.token).toBeNull();
      expect(store.userInfo).toBeNull();
      expect(store.roles).toEqual([]);
      expect(store.permissions).toEqual([]);
      expect(store.isAuthenticated).toBe(false);
    });

    it('should set and get user info', () => {
      const store = useAuthStore();
      const userInfo = {
        id: 1,
        username: 'testuser',
        nickname: 'Test User',
        email: 'test@example.com',
        phone: '13800138000',
        status: 1,
        createTime: '2024-01-01T00:00:00Z',
      };

      store.setUserInfo(userInfo);

      expect(store.userInfo).toEqual(userInfo);
    });

    it('should set roles and permissions', () => {
      const store = useAuthStore();
      const roles = ['admin', 'user'];
      const permissions = ['user:create', 'user:view', 'role:create'];

      store.setRoles(roles);
      store.setPermissions(permissions);

      expect(store.roles).toEqual(roles);
      expect(store.permissions).toEqual(permissions);
    });

    it('should check permission correctly', () => {
      const store = useAuthStore();
      store.setPermissions(['user:create', 'user:view']);

      expect(store.hasPermission('user:create')).toBe(true);
      expect(store.hasPermission('user:delete')).toBe(false);
      expect(store.hasPermission('user:*')).toBe(false);
    });

    it('should check role correctly', () => {
      const store = useAuthStore();
      store.setRoles(['admin', 'editor']);

      expect(store.hasRole('admin')).toBe(true);
      expect(store.hasRole('viewer')).toBe(false);
    });
  });

  describe('app store', () => {
    it('should toggle sidebar collapsed state', () => {
      const store = useAppStore();

      expect(store.sidebarCollapsed).toBe(false);

      store.toggleSidebar();

      expect(store.sidebarCollapsed).toBe(true);

      store.toggleSidebar();

      expect(store.sidebarCollapsed).toBe(false);
    });

    it('should set sidebar collapsed state directly', () => {
      const store = useAppStore();

      store.setSidebarCollapsed(true);

      expect(store.sidebarCollapsed).toBe(true);
    });

    it('should toggle theme between light and dark', () => {
      const store = useAppStore();

      expect(store.theme).toBe('light');

      store.toggleTheme();

      expect(store.theme).toBe('dark');

      store.toggleTheme();

      expect(store.theme).toBe('light');
    });

    it('should set theme directly', () => {
      const store = useAppStore();

      store.setTheme('dark');

      expect(store.theme).toBe('dark');
      expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
    });

    it('should manage breadcrumbs', () => {
      const store = useAppStore();
      const breadcrumbs = [
        { path: '/dashboard', title: '仪表盘' },
        { path: '/user', title: '用户管理' },
      ];

      store.setBreadcrumbs(breadcrumbs);

      expect(store.breadcrumbs).toEqual(breadcrumbs);
    });

    it('should track device type', () => {
      const store = useAppStore();

      store.setDevice('mobile');

      expect(store.device).toBe('mobile');
      expect(store.isMobile).toBe(true);

      store.setDevice('desktop');

      expect(store.device).toBe('desktop');
      expect(store.isMobile).toBe(false);
    });
  });
});
