import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { staticRoutes, layoutRoutes, generateRoutes } from '@/router/routes';

let dynamicRoutesLoaded = false;

const routes: RouteRecordRaw[] = [
  ...staticRoutes,
  ...layoutRoutes,
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFoundView.vue'),
    meta: { public: true, title: '404' },
  },
];

const router = createRouter({
  history: createWebHistory(import.meta.env.VITE_BASE_URL || '/'),
  routes,
  scrollBehavior: () => ({ top: 0 }),
});

router.beforeEach(async (to, _from, next) => {
  const authStore = useAuthStore();
  const requiresAuth = to.matched.some((record) => record.meta.requiresAuth);
  const isPublic = to.matched.some((record) => record.meta.public);

  console.log(
    '[Guard] to:',
    to.name,
    to.path,
    'requiresAuth:',
    requiresAuth,
    'isPublic:',
    isPublic,
    'isAuthenticated:',
    authStore.isAuthenticated,
    'dynamicRoutesLoaded:',
    dynamicRoutesLoaded,
    'roles:',
    authStore.roles
  );

  // 1. 未认证访问受保护路由 -> 重定向登录
  if (requiresAuth && !authStore.isAuthenticated) {
    console.log('[Guard] Redirect to Login');
    next({ name: 'Login', query: { redirect: to.fullPath } });
    return;
  }

  // 2. 已认证访问登录页 -> 重定向 dashboard
  if (isPublic && authStore.isAuthenticated && to.name === 'Login') {
    console.log('[Guard] Authenticated on Login page, redirect to Dashboard');
    next({ name: 'Dashboard' });
    return;
  }

  // 3. 首次登录后跳转 dashboard：直接放行，不加载动态路由
  //    动态路由将在后续导航中由 case 4 加载
  if (to.name === 'Dashboard' && authStore.isAuthenticated && !dynamicRoutesLoaded) {
    console.log('[Guard] First login redirect to Dashboard, allowing through');
    next();
    return;
  }

  // 4. 其他已认证场景且动态路由未加载 -> 加载动态路由
  if (authStore.isAuthenticated && !dynamicRoutesLoaded) {
    console.log('[Guard] Loading dynamic routes for roles:', authStore.roles);
    try {
      const roles = authStore.roles;
      const accessibleRoutes = generateRoutes(roles);
      accessibleRoutes.forEach((route) => router.addRoute(route));
      dynamicRoutesLoaded = true;
      // to 此时可能匹配 catch-all（name='NotFound'），展开 {...to} 会被 vue-router 按 name 优先解析回 404；
      // 显式只传 path/query/hash，按目标 path 重新匹配动态路由
      next({ path: to.path, query: to.query, hash: to.hash, replace: true });
      return;
    } catch (error) {
      console.error('Failed to load dynamic routes:', error);
      next(); // 兜底放行，避免卡死
    }
  }

  console.log('[Guard] Allowing through');
  next();
});

export default router;
