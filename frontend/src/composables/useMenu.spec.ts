import { describe, it, expect, beforeEach, vi } from 'vitest';
import { setActivePinia, createPinia } from 'pinia';
import { useMenu } from '@/composables/useMenu';
import { useAuthStore } from '@/stores/auth';

vi.mock('@/stores/auth', () => ({
  useAuthStore: vi.fn(),
}));

describe('useMenu - Dynamic Menu Generation & Permission Filtering', () => {
  let authStore: ReturnType<typeof useAuthStore>;

  const mockAdminAuthStore = {
    roles: ['admin'],
    permissions: ['*'],
    hasPermission: vi.fn((p: string) => true),
    hasRole: vi.fn((r: string) => true),
    hasAnyRole: vi.fn((roles: string[]) => true),
    hasAnyPermission: vi.fn((perms: string[]) => true),
  };

  const mockUserAuthStore = {
    roles: ['user'],
    permissions: ['user:view', 'role:view', 'perm:view'],
    hasPermission: vi.fn((p: string) => ['user:view', 'role:view', 'perm:view'].includes(p)),
    hasRole: vi.fn((r: string) => r === 'user'),
    hasAnyRole: vi.fn((roles: string[]) => roles.includes('user')),
    hasAnyPermission: vi.fn((perms: string[]) => perms.some((p) => ['user:view', 'role:view', 'perm:view'].includes(p))),
  };

  const mockLimitedAuthStore = {
    roles: ['viewer'],
    permissions: ['user:view'],
    hasPermission: vi.fn((p: string) => p === 'user:view'),
    hasRole: vi.fn((r: string) => r === 'viewer'),
    hasAnyRole: vi.fn((roles: string[]) => roles.includes('viewer')),
    hasAnyPermission: vi.fn((perms: string[]) => perms.some((p) => p === 'user:view')),
  };

  beforeEach(() => {
    setActivePinia(createPinia());
    vi.clearAllMocks();
  });

  describe('AC2 - Menu Permission Filtering', () => {
    it('returns all menu items for admin user with * permission', () => {
      (useAuthStore as vi.Mock).mockReturnValue(mockAdminAuthStore);
      const { generateMenuTree } = useMenu();
      const menu = generateMenuTree();

      expect(menu.length).toBeGreaterThan(0);
      const systemModule = menu.find((m) => m.path === '/system');
      expect(systemModule).toBeDefined();
      expect(systemModule?.children?.length).toBe(4);
    });

    it('filters menu items based on user permissions for regular user', () => {
      (useAuthStore as vi.Mock).mockReturnValue(mockUserAuthStore);
      const { generateMenuTree } = useMenu();
      const menu = generateMenuTree();

      const systemModule = menu.find((m) => m.path === '/system');
      expect(systemModule).toBeDefined();
      expect(systemModule?.children?.length).toBe(3);
      const childPaths = systemModule?.children?.map((c) => c.path) || [];
      expect(childPaths).toContain('/users');
      expect(childPaths).toContain('/roles');
      expect(childPaths).toContain('/system/permissions');
      expect(childPaths).not.toContain('/system/modules');
    });

    it('hides entire module when user has no permissions for any child', () => {
      (useAuthStore as vi.Mock).mockReturnValue(mockLimitedAuthStore);
      const { generateMenuTree } = useMenu();
      const menu = generateMenuTree();

      const systemModule = menu.find((m) => m.path === '/system');
      expect(systemModule).toBeDefined();
      expect(systemModule?.children?.length).toBe(1);
      expect(systemModule?.children?.[0]?.path).toBe('/users');
    });

    it('includes dashboard for all authenticated users', () => {
      (useAuthStore as vi.Mock).mockReturnValue(mockLimitedAuthStore);
      const { generateMenuTree } = useMenu();
      const menu = generateMenuTree();

      const dashboard = menu.find((m) => m.path === '/dashboard');
      expect(dashboard).toBeDefined();
      expect(dashboard?.title).toBe('仪表盘');
    });

    it('handles nested menu structures correctly', () => {
      (useAuthStore as vi.Mock).mockReturnValue(mockAdminAuthStore);
      const { generateMenuTree } = useMenu();
      const menu = generateMenuTree();

      const systemModule = menu.find((m) => m.path === '/system');
      expect(systemModule?.children).toBeDefined();
      systemModule?.children?.forEach((child) => {
        expect(child.title).toBeDefined();
        expect(child.path).toBeDefined();
        expect(child.icon).toBeDefined();
      });
    });
  });

  describe('Menu Configuration', () => {
    it('returns menu config with correct structure', () => {
      (useAuthStore as vi.Mock).mockReturnValue(mockAdminAuthStore);
      const { getMenuConfig } = useMenu();
      const config = getMenuConfig();

      expect(Array.isArray(config)).toBe(true);
      expect(config.length).toBeGreaterThan(0);

      config.forEach((item) => {
        expect(item).toHaveProperty('path');
        expect(item).toHaveProperty('title');
        expect(item).toHaveProperty('icon');
      });
    });

    it('menu config includes module grouping', () => {
      (useAuthStore as vi.Mock).mockReturnValue(mockAdminAuthStore);
      const { getMenuConfig } = useMenu();
      const config = getMenuConfig();

      const systemModule = config.find((m) => m.path === '/system');
      expect(systemModule).toBeDefined();
      expect(systemModule?.children).toBeDefined();
      expect(Array.isArray(systemModule?.children)).toBe(true);
    });
  });

  describe('Permission Helper Functions', () => {
    it('filters menu items by permission', () => {
      (useAuthStore as vi.Mock).mockReturnValue(mockUserAuthStore);
      const { filterMenuByPermission } = useMenu();

      const testMenu = [
        { path: '/a', title: 'A', permissions: ['user:view'] },
        { path: '/b', title: 'B', permissions: ['b:read'] },
        { path: '/c', title: 'C', permissions: [] },
      ];

      const filtered = filterMenuByPermission(testMenu);
      expect(filtered.length).toBe(2);
      expect(filtered.map((m) => m.path)).toContain('/a');
      expect(filtered.map((m) => m.path)).toContain('/c');
    });

    it('handles empty permissions array', () => {
      (useAuthStore as vi.Mock).mockReturnValue(mockUserAuthStore);
      const { filterMenuByPermission } = useMenu();

      const testMenu = [
        { path: '/a', title: 'A', permissions: [] },
        { path: '/b', title: 'B', permissions: ['b:read'] },
      ];

      const filtered = filterMenuByPermission(testMenu);
      expect(filtered.length).toBe(1);
      expect(filtered[0].path).toBe('/a');
    });
  });
});

describe('Menu Config - AC2: menuPointsToFunctionalPages', () => {
  const mockAdminAuthStore = {
    roles: ['admin'],
    permissions: ['*'],
    hasPermission: vi.fn((p: string) => true),
    hasRole: vi.fn((r: string) => true),
    hasAnyRole: vi.fn((roles: string[]) => true),
    hasAnyPermission: vi.fn((perms: string[]) => true),
  };

  beforeEach(() => {
    setActivePinia(createPinia());
    vi.clearAllMocks();
    (useAuthStore as vi.Mock).mockReturnValue(mockAdminAuthStore);
  });

  it('menu items point to correct functional pages', () => {
    const { getMenuConfig } = useMenu();
    const config = getMenuConfig();

    const systemModule = config.find((m) => m.path === '/system');
    expect(systemModule).toBeDefined();

    const childPaths = systemModule?.children?.map((c) => c.path) || [];

    // 用户管理 -> /users (实际功能页)
    expect(childPaths).toContain('/users');
    // 角色管理 -> /roles (实际功能页)
    expect(childPaths).toContain('/roles');
    // 权限管理 -> /system/permissions
    expect(childPaths).toContain('/system/permissions');
    // 模块管理 -> /system/modules
    expect(childPaths).toContain('/system/modules');

    // 旧路径不应存在
    expect(childPaths).not.toContain('/system/user');
    expect(childPaths).not.toContain('/system/role');
  });
});