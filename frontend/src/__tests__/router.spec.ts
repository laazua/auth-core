import { describe, it, expect, beforeEach, vi } from 'vitest';
import { createRouter, createWebHistory } from 'vue-router';
import { createPinia, setActivePinia } from 'pinia';
import { generateRoutes } from '@/router/routes';

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

    const userRoute = routes.find((r) => r.path === '/users');
    expect(userRoute).toBeDefined();

    const roleRoute = routes.find((r) => r.path === '/roles');
    expect(roleRoute).toBeDefined();
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
