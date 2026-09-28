import { describe, it, expect, beforeEach, vi } from 'vitest';
import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';
import { createPinia, setActivePinia } from 'pinia';
import { generateRoutes, staticRoutes, layoutRoutes } from '@/router/routes';

const mockAuthStore = {
  isAuthenticated: false,
  token: null as string | null,
  roles: [] as string[],
  permissions: [] as string[],
};

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => mockAuthStore,
}));

const mockRoutes = [
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
  { path: '/:pathMatch(.*)*', name: 'NotFound', component: { template: '<div>404</div>' } },
];

describe('Router Guards', () => {
  let router: ReturnType<typeof createRouter>;

  beforeEach(() => {
    setActivePinia(createPinia());
    vi.resetAllMocks();
    mockAuthStore.isAuthenticated = false;
    mockAuthStore.token = null;

    router = createRouter({
      history: createWebHistory(),
      routes: mockRoutes,
    });

    router.beforeEach(async (to, _from, next) => {
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

      next();
    });
  });

  it('should redirect to login when accessing protected route without token', async () => {
    mockAuthStore.isAuthenticated = false;
    mockAuthStore.token = null;

    await router.push('/dashboard');
    await router.isReady();

    expect(router.currentRoute.value.name).toBe('Login');
    expect(router.currentRoute.value.query.redirect).toBe('/dashboard');
  });

  it('should allow access to protected route with valid token', async () => {
    mockAuthStore.isAuthenticated = true;
    mockAuthStore.token = 'valid-token';

    await router.push('/dashboard');
    await router.isReady();

    expect(router.currentRoute.value.name).toBe('Dashboard');
  });

  it('should redirect to dashboard when accessing login with valid token', async () => {
    mockAuthStore.isAuthenticated = true;
    mockAuthStore.token = 'valid-token';

    await router.push('/login');
    await router.isReady();

    expect(router.currentRoute.value.name).toBe('Dashboard');
  });

  it('should allow access to login without token', async () => {
    mockAuthStore.isAuthenticated = false;
    mockAuthStore.token = null;

    await router.push('/login');
    await router.isReady();

    expect(router.currentRoute.value.name).toBe('Login');
  });

  it('should redirect to 404 for unknown routes', async () => {
    mockAuthStore.isAuthenticated = true;
    mockAuthStore.token = 'valid-token';

    await router.push('/unknown-path');
    await router.isReady();

    expect(router.currentRoute.value.name).toBe('NotFound');
  });
});

describe('Route Access Control - AC1: systemRouteAllowsPermissionBasedAccess', () => {
  it('generates routes without admin role when user has permissions', () => {
    mockAuthStore.roles = ['user'];
    mockAuthStore.permissions = ['user:view', 'role:view', 'perm:view', 'module:view'];

    const routes = generateRoutes(mockAuthStore.roles);

    const systemRoute = routes.find((r) => r.path === '/system');
    expect(systemRoute).toBeDefined();
    expect(systemRoute?.meta?.roles).toBeUndefined();

    const systemChildren = (systemRoute as { children?: { path: string }[] }).children ?? [];
    const childPaths = systemChildren.map((c) => c.path);
    expect(childPaths).toContain('users');
    expect(childPaths).toContain('roles');
    expect(routes.find((r) => r.path === '/users')).toBeUndefined();
    expect(routes.find((r) => r.path === '/roles')).toBeUndefined();
  });

  it('generates routes with admin role still works', () => {
    mockAuthStore.roles = ['admin'];
    mockAuthStore.permissions = ['*'];

    const routes = generateRoutes(mockAuthStore.roles);

    const systemRoute = routes.find((r) => r.path === '/system');
    expect(systemRoute).toBeDefined();
    expect(systemRoute?.meta?.roles).toBeUndefined();
  });
});

describe('AC1: Dynamic Route Loading After Dashboard Navigation', () => {
  it('Dashboard navigation does not set dynamicRoutesLoaded flag before routes are loaded', async () => {
    mockAuthStore.isAuthenticated = true;
    mockAuthStore.roles = ['admin'];

    const testRouter = createRouter({
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
        { path: '/:pathMatch(.*)*', name: 'NotFound', component: { template: '<div>404</div>' } },
      ],
    });

    let dynamicRoutesLoaded = false;
    testRouter.beforeEach(async (to, _from, next) => {
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

      if (to.name === 'Dashboard' && mockAuthStore.isAuthenticated && !dynamicRoutesLoaded) {
        next();
        return;
      }

      if (mockAuthStore.isAuthenticated && !dynamicRoutesLoaded) {
        dynamicRoutesLoaded = true;
        next({ ...to, replace: true });
        return;
      }

      next();
    });

    await testRouter.push('/dashboard');
    await testRouter.isReady();

    expect(dynamicRoutesLoaded).toBe(false);
  });

  it('subsequent navigation to /users loads dynamic routes after Dashboard', async () => {
    mockAuthStore.isAuthenticated = true;
    mockAuthStore.roles = ['admin'];

    const testRouter = createRouter({
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
          path: '/users',
          name: 'Users',
          component: { template: '<div>Users</div>' },
          meta: { requiresAuth: true },
        },
        { path: '/:pathMatch(.*)*', name: 'NotFound', component: { template: '<div>404</div>' } },
      ],
    });

    let dynamicRoutesLoaded = false;
    testRouter.beforeEach(async (to, _from, next) => {
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

      if (to.name === 'Dashboard' && mockAuthStore.isAuthenticated && !dynamicRoutesLoaded) {
        next();
        return;
      }

      if (mockAuthStore.isAuthenticated && !dynamicRoutesLoaded) {
        dynamicRoutesLoaded = true;
        next({ ...to, replace: true });
        return;
      }

      next();
    });

    await testRouter.push('/dashboard');
    await testRouter.isReady();

    expect(dynamicRoutesLoaded).toBe(false);

    await testRouter.push('/users');
    await testRouter.isReady();

    expect(dynamicRoutesLoaded).toBe(true);
    expect(testRouter.currentRoute.value.name).toBe('Users');
  });
});

describe('web-034 系统管理路由迁移', () => {
  it('AC2 generateRoutes 注册 /system/users 与 /system/roles 并移除顶级 /users /roles 与占位 user /role', () => {
    const routes = generateRoutes(['admin']);

    const systemRoute = routes.find((r) => r.path === '/system');
    expect(systemRoute).toBeDefined();
    expect(systemRoute?.redirect).toBe('/system/permissions');

    const systemChildren = (systemRoute as { children?: RouteRecordRaw[] }).children ?? [];
    const childPaths = systemChildren.map((c) => c.path);
    expect(childPaths).toContain('users');
    expect(childPaths).toContain('roles');
    expect(childPaths).toContain('permissions');
    expect(childPaths).toContain('modules');
    expect(childPaths).not.toContain('user');
    expect(childPaths).not.toContain('role');

    expect(routes.find((r) => r.path === '/users')).toBeUndefined();
    expect(routes.find((r) => r.path === '/roles')).toBeUndefined();

    const usersChild = systemChildren.find((c) => c.path === 'users');
    expect(usersChild).toBeDefined();
    expect(usersChild?.name).toBe('UsersIndex');
    expect(String(usersChild?.component)).toContain('views/users/IndexView.vue');

    const rolesChild = systemChildren.find((c) => c.path === 'roles');
    expect(rolesChild).toBeDefined();
    expect(rolesChild?.name).toBe('RolesIndex');
    expect(String(rolesChild?.component)).toContain('views/roles/IndexView.vue');
  });

  it('AC3 push /system/users 与 /system/roles 可达且旧 /users /roles 落 404', async () => {
    const testRouter = createRouter({
      history: createWebHistory(),
      routes: [...staticRoutes, ...layoutRoutes, ...generateRoutes(['admin'])],
    });

    await testRouter.push('/system/users');
    expect(testRouter.currentRoute.value.matched.some((r) => r.path === '/system/users')).toBe(
      true
    );
    expect(testRouter.currentRoute.value.name).not.toBe('NotFound');

    await testRouter.push('/system/roles');
    expect(testRouter.currentRoute.value.matched.some((r) => r.path === '/system/roles')).toBe(
      true
    );
    expect(testRouter.currentRoute.value.name).not.toBe('NotFound');

    await testRouter.push('/users');
    expect(testRouter.currentRoute.value.name).toBe('NotFound');

    await testRouter.push('/roles');
    expect(testRouter.currentRoute.value.name).toBe('NotFound');
  });
});

describe('web-035 动态路由首载导航', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    mockAuthStore.isAuthenticated = true;
    mockAuthStore.token = 'valid-token';
    mockAuthStore.roles = ['admin'];
    mockAuthStore.permissions = ['*'];
  });

  const loadFreshRouter = async () => {
    vi.resetModules();
    const mod = await import('@/router');
    return mod.default;
  };

  it('AC1 登录后首次导航 /system/users 与 /system/permissions 一次即达不落 404', async () => {
    const router = await loadFreshRouter();

    await router.push('/dashboard');
    expect(router.currentRoute.value.name).toBe('Dashboard');

    await router.push('/system/users');
    expect(router.currentRoute.value.name).toBe('UsersIndex');
    expect(router.currentRoute.value.matched.some((r) => r.path === '/system/users')).toBe(true);

    await router.push('/system/permissions');
    expect(router.currentRoute.value.name).toBe('Permissions');
    expect(router.currentRoute.value.matched.some((r) => r.path === '/system/permissions')).toBe(
      true
    );
  });

  it('AC2 首载导航保留目标 query 参数', async () => {
    const router = await loadFreshRouter();

    await router.push('/dashboard');
    await router.push({ path: '/system/users', query: { page: '2' } });

    expect(router.currentRoute.value.name).toBe('UsersIndex');
    expect(router.currentRoute.value.fullPath).toBe('/system/users?page=2');
  });

  it('AC3 动态路由加载完成后二次导航直达', async () => {
    const router = await loadFreshRouter();

    await router.push('/dashboard');
    await router.push('/system/users');

    await router.push('/system/roles');
    expect(router.currentRoute.value.name).toBe('RolesIndex');
    expect(router.currentRoute.value.matched.some((r) => r.path === '/system/roles')).toBe(true);
  });
});
