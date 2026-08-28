import { describe, it, expect, beforeEach, vi } from 'vitest';
import { createRouter, createWebHistory } from 'vue-router';
import { createPinia, setActivePinia } from 'pinia';

const mockAuthStore = {
  isAuthenticated: false,
  token: null,
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

    router.beforeEach(async (to, from, next) => {
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
