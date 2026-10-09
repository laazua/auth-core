import { describe, it, expect, beforeEach, vi } from 'vitest';
import { createRouter, createWebHistory, Router } from 'vue-router';
import { createPinia, setActivePinia } from 'pinia';
import { authGuard, guestGuard, permissionGuard, roleGuard } from '@/router/guards';
import { useAuthStore } from '@/stores/auth';
import { generateRoutes } from '@/router/routes';

vi.mock('@/stores/auth');

const createMockAuthStore = () => {
  const store = {
    isAuthenticated: false,
    token: null as string | null,
    roles: [] as string[],
    permissions: [] as string[],
    hasPermission: vi.fn(),
    hasRole: vi.fn(),
    hasAnyRole: vi.fn(),
    setToken: vi.fn((newToken: string | null) => {
      store.token = newToken;
      store.isAuthenticated = !!newToken;
    }),
    setRoles: vi.fn((newRoles: string[]) => {
      store.roles = newRoles;
    }),
    setPermissions: vi.fn((newPermissions: string[]) => {
      store.permissions = newPermissions;
    }),
  };
  return store;
};

const mockAuthStore = createMockAuthStore();

vi.mocked(useAuthStore).mockReturnValue(
  mockAuthStore as unknown as ReturnType<typeof useAuthStore>
);

const createTestRouter = (
  routes: Array<{ path: string; name: string; meta?: Record<string, unknown> }>
): Router => {
  return createRouter({
    history: createWebHistory(),
    routes: routes.map((r) => ({
      path: r.path,
      name: r.name,
      component: { template: '<div>' + r.name + '</div>' },
      meta: r.meta || {},
    })),
  });
};

describe('Router Guards', () => {
  let router: Router;

  beforeEach(() => {
    setActivePinia(createPinia());
    vi.clearAllMocks();

    mockAuthStore.isAuthenticated = false;
    mockAuthStore.token = null;
    mockAuthStore.roles = [];
    mockAuthStore.permissions = [];
    mockAuthStore.hasPermission.mockReturnValue(false);
    mockAuthStore.hasRole.mockReturnValue(false);
    mockAuthStore.hasAnyRole.mockReturnValue(false);
    mockAuthStore.setToken.mockClear();
    mockAuthStore.setRoles.mockClear();
    mockAuthStore.setPermissions.mockClear();

    router = createTestRouter([
      { path: '/login', name: 'Login', meta: { public: true } },
      { path: '/403', name: 'Forbidden', meta: { public: true } },
      { path: '/dashboard', name: 'Dashboard', meta: { requiresAuth: true } },
      { path: '/user', name: 'User', meta: { requiresAuth: true, permissions: ['user:view'] } },
      { path: '/role', name: 'Role', meta: { requiresAuth: true, permissions: ['role:view'] } },
      { path: '/admin', name: 'Admin', meta: { requiresAuth: true, roles: ['admin'] } },
    ]);
  });

  const setupAuthGuard = (r: Router) => {
    r.beforeEach(async (to, _from, next) => {
      const requiresAuth = to.matched.some((record) => record.meta.requiresAuth);
      const isPublic = to.matched.some((record) => record.meta.public);

      if (requiresAuth && !mockAuthStore.isAuthenticated) {
        next({ name: 'Login', query: { redirect: to.fullPath } });
        return;
      }

      if (isPublic && mockAuthStore.isAuthenticated && to.name === 'Login') {
        next({ name: 'Dashboard' });
        return;
      }

      // Check permissions
      const requiredPermissions = to.matched.flatMap(
        (record) => (record.meta.permissions as string[]) || []
      );
      if (requiresAuth && requiredPermissions.length > 0) {
        const hasPermission = requiredPermissions.some((p) => mockAuthStore.hasPermission(p));
        if (!hasPermission) {
          next({ name: 'Forbidden' });
          return;
        }
      }

      // Check roles
      const requiredRoles = to.matched.flatMap((record) => (record.meta.roles as string[]) || []);
      if (requiresAuth && requiredRoles.length > 0) {
        const hasRole = requiredRoles.some((r) => mockAuthStore.hasRole(r));
        if (!hasRole) {
          next({ name: 'Forbidden' });
          return;
        }
      }

      next();
    });
  };

  describe('Auth Guard (AC4)', () => {
    it('redirects to login with redirect query when accessing protected route without token', async () => {
      mockAuthStore.isAuthenticated = false;
      mockAuthStore.token = null;

      setupAuthGuard(router);
      await router.push('/dashboard');
      await router.isReady();

      expect(router.currentRoute.value.name).toBe('Login');
      expect(router.currentRoute.value.query.redirect).toBe('/dashboard');
    });

    it('redirects to login with redirect query for user route', async () => {
      mockAuthStore.isAuthenticated = false;
      mockAuthStore.token = null;

      setupAuthGuard(router);
      await router.push('/user');
      await router.isReady();

      expect(router.currentRoute.value.name).toBe('Login');
      expect(router.currentRoute.value.query.redirect).toBe('/user');
    });

    it('allows access to protected route with valid token', async () => {
      mockAuthStore.isAuthenticated = true;
      mockAuthStore.token = 'valid-token';

      setupAuthGuard(router);
      await router.push('/dashboard');
      await router.isReady();

      expect(router.currentRoute.value.name).toBe('Dashboard');
    });

    it('allows access to login without token', async () => {
      mockAuthStore.isAuthenticated = false;
      mockAuthStore.token = null;

      setupAuthGuard(router);
      await router.push('/login');
      await router.isReady();

      expect(router.currentRoute.value.name).toBe('Login');
    });
  });

  describe('Logged In User Redirect (AC5)', () => {
    it('redirects to dashboard when logged in user accesses login page', async () => {
      mockAuthStore.isAuthenticated = true;
      mockAuthStore.token = 'valid-token';

      setupAuthGuard(router);
      await router.push('/login');
      await router.isReady();

      expect(router.currentRoute.value.name).toBe('Dashboard');
    });

    it('redirects to dashboard when logged in user accesses login with redirect query', async () => {
      mockAuthStore.isAuthenticated = true;
      mockAuthStore.token = 'valid-token';

      setupAuthGuard(router);
      await router.push('/login?redirect=/user');
      await router.isReady();

      expect(router.currentRoute.value.name).toBe('Dashboard');
    });
  });

  describe('Permission Guard (AC6)', () => {
    it('redirects to 403 when user lacks required permission', async () => {
      mockAuthStore.isAuthenticated = true;
      mockAuthStore.token = 'valid-token';
      mockAuthStore.permissions = ['role:view'];
      mockAuthStore.hasPermission.mockImplementation((p: string) =>
        mockAuthStore.permissions.includes(p)
      );

      setupAuthGuard(router);
      await router.push('/user');
      await router.isReady();

      expect(router.currentRoute.value.name).toBe('Forbidden');
    });

    it('allows access when user has required permission', async () => {
      mockAuthStore.isAuthenticated = true;
      mockAuthStore.token = 'valid-token';
      mockAuthStore.permissions = ['user:view', 'role:view'];
      mockAuthStore.hasPermission.mockImplementation((p: string) =>
        mockAuthStore.permissions.includes(p)
      );

      setupAuthGuard(router);
      await router.push('/user');
      await router.isReady();

      expect(router.currentRoute.value.name).toBe('User');
    });

    it('redirects to 403 when user lacks required role', async () => {
      mockAuthStore.isAuthenticated = true;
      mockAuthStore.token = 'valid-token';
      mockAuthStore.roles = ['user'];
      mockAuthStore.hasRole.mockImplementation((r: string) => mockAuthStore.roles.includes(r));

      setupAuthGuard(router);
      await router.push('/admin');
      await router.isReady();

      expect(router.currentRoute.value.name).toBe('Forbidden');
    });

    it('allows access when user has required role', async () => {
      mockAuthStore.isAuthenticated = true;
      mockAuthStore.token = 'valid-token';
      mockAuthStore.roles = ['admin', 'user'];
      mockAuthStore.hasRole.mockImplementation((r: string) => mockAuthStore.roles.includes(r));

      setupAuthGuard(router);
      await router.push('/admin');
      await router.isReady();

      expect(router.currentRoute.value.name).toBe('Admin');
    });

    it('redirects to login first when accessing permission route without token', async () => {
      mockAuthStore.isAuthenticated = false;
      mockAuthStore.token = null;

      setupAuthGuard(router);
      await router.push('/user');
      await router.isReady();

      expect(router.currentRoute.value.name).toBe('Login');
      expect(router.currentRoute.value.query.redirect).toBe('/user');
    });
  });

  describe('Guard Functions', () => {
    it('authGuard redirects unauthenticated to login', async () => {
      mockAuthStore.isAuthenticated = false;

      const to = {
        name: 'Dashboard',
        fullPath: '/dashboard',
        matched: [{ meta: { requiresAuth: true } }],
      };
      const from = { name: 'Login' };
      const next = vi.fn();

      await authGuard(to as any, from as any, next);

      expect(next).toHaveBeenCalledWith({ name: 'Login', query: { redirect: '/dashboard' } });
    });

    it('authGuard allows authenticated user', async () => {
      mockAuthStore.isAuthenticated = true;

      const to = { name: 'Dashboard', matched: [{ meta: { requiresAuth: true } }] };
      const from = { name: 'Login' };
      const next = vi.fn();

      await authGuard(to as any, from as any, next);

      expect(next).toHaveBeenCalledWith();
    });

    it('guestGuard redirects authenticated user to dashboard', async () => {
      mockAuthStore.isAuthenticated = true;

      const to = { name: 'Login' };
      const from = { name: 'Dashboard' };
      const next = vi.fn();

      await guestGuard(to as any, from as any, next);

      expect(next).toHaveBeenCalledWith({ name: 'Dashboard' });
    });

    it('guestGuard allows unauthenticated user', async () => {
      mockAuthStore.isAuthenticated = false;

      const to = { name: 'Login' };
      const from = { name: 'Dashboard' };
      const next = vi.fn();

      await guestGuard(to as any, from as any, next);

      expect(next).toHaveBeenCalledWith();
    });

    it('permissionGuard redirects to 403 when no permission', async () => {
      mockAuthStore.isAuthenticated = true;
      mockAuthStore.hasPermission.mockReturnValue(false);

      const guard = permissionGuard(['user:view']);
      const to = {
        name: 'User',
        matched: [{ meta: { requiresAuth: true, permissions: ['user:view'] } }],
      };
      const from = { name: 'Dashboard' };
      const next = vi.fn();

      await guard(to as any, from as any, next);

      expect(next).toHaveBeenCalledWith({ name: 'Forbidden' });
    });

    it('permissionGuard allows when has permission', async () => {
      mockAuthStore.isAuthenticated = true;
      mockAuthStore.hasPermission.mockReturnValue(true);

      const guard = permissionGuard(['user:view']);
      const to = {
        name: 'User',
        matched: [{ meta: { requiresAuth: true, permissions: ['user:view'] } }],
      };
      const from = { name: 'Dashboard' };
      const next = vi.fn();

      await guard(to as any, from as any, next);

      expect(next).toHaveBeenCalledWith();
    });

    it('roleGuard redirects to 403 when no role', async () => {
      mockAuthStore.isAuthenticated = true;
      mockAuthStore.hasRole.mockReturnValue(false);

      const guard = roleGuard(['admin']);
      const to = { name: 'Admin', matched: [{ meta: { requiresAuth: true, roles: ['admin'] } }] };
      const from = { name: 'Dashboard' };
      const next = vi.fn();

      await guard(to as any, from as any, next);

      expect(next).toHaveBeenCalledWith({ name: 'Forbidden' });
    });

    it('roleGuard allows when has role', async () => {
      mockAuthStore.isAuthenticated = true;
      mockAuthStore.hasRole.mockReturnValue(true);

      const guard = roleGuard(['admin']);
      const to = { name: 'Admin', matched: [{ meta: { requiresAuth: true, roles: ['admin'] } }] };
      const from = { name: 'Dashboard' };
      const next = vi.fn();

      await guard(to as any, from as any, next);

      expect(next).toHaveBeenCalledWith();
    });
  });

  describe('Login Redirect with Dynamic Routes (web/026)', () => {
    let router: Router;
    let authStore: ReturnType<typeof useAuthStore>;
    let dynamicRoutesLoaded: boolean;

    const createTestRouterWithGuards = () => {
      dynamicRoutesLoaded = false;
      const r = createRouter({
        history: createWebHistory(),
        routes: [
          {
            path: '/login',
            name: 'Login',
            component: { template: '<div>Login</div>' },
            meta: { public: true },
          },
          {
            path: '/dashboard',
            name: 'Dashboard',
            component: { template: '<div>Dashboard</div>' },
            meta: { requiresAuth: true },
          },
          {
            path: '/user',
            name: 'User',
            component: { template: '<div>User</div>' },
            meta: { requiresAuth: true },
          },
        ],
      });

      r.beforeEach(async (to, _from, next) => {
        const store = useAuthStore();
        const requiresAuth = to.matched.some((record) => record.meta.requiresAuth);
        const isPublic = to.matched.some((record) => record.meta.public);

        if (requiresAuth && !store.isAuthenticated) {
          next({ name: 'Login', query: { redirect: to.fullPath } });
          return;
        }

        if (isPublic && store.isAuthenticated && to.name === 'Login') {
          next({ name: 'Dashboard' });
          return;
        }

        if (store.isAuthenticated && !dynamicRoutesLoaded) {
          try {
            const roles = store.roles;
            const accessibleRoutes = generateRoutes(roles);
            accessibleRoutes.forEach((route) => {
              r.addRoute(route);
            });
            dynamicRoutesLoaded = true;
            next({ ...to, replace: true });
            return;
          } catch (error) {
            console.error('Failed to load dynamic routes:', error);
          }
        }

        next();
      });

      return r;
    };

    beforeEach(() => {
      setActivePinia(createPinia());
      vi.clearAllMocks();

      // Reset mock auth store state
      mockAuthStore.isAuthenticated = false;
      mockAuthStore.token = null;
      mockAuthStore.roles = [];
      mockAuthStore.permissions = [];
      mockAuthStore.setToken.mockClear();
      mockAuthStore.setRoles.mockClear();
      mockAuthStore.setPermissions.mockClear();

      router = createTestRouterWithGuards();
      authStore = useAuthStore();
    });

    it('allows redirect to dashboard after login without blocking on dynamicRoutesLoaded', async () => {
      authStore.setToken('mock-token');
      authStore.setRoles(['admin']);
      authStore.setPermissions(['*']);

      await router.push('/login?redirect=/dashboard');
      await router.push('/dashboard');

      expect(router.currentRoute.value.name).toBe('Dashboard');
    });
  });
});
