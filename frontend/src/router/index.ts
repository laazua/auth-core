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

  // 1. 未认证访问受保护路由 -> 重定向登录
  if (requiresAuth && !authStore.isAuthenticated) {
    next({ name: 'Login', query: { redirect: to.fullPath } });
    return;
  }

  // 2. 已认证访问登录页 -> 重定向 dashboard
  if (isPublic && authStore.isAuthenticated && to.name === 'Login') {
    next({ name: 'Dashboard' });
    return;
  }

  // 3. 首次登录后跳转 dashboard：直接放行，不加载动态路由
  //    判断依据：目标是 Dashboard 且动态路由未加载
  if (to.name === 'Dashboard' && authStore.isAuthenticated && !dynamicRoutesLoaded) {
    dynamicRoutesLoaded = true; // 标记已加载，避免重复
    next();
    return;
  }

  // 4. 其他已认证场景且动态路由未加载 -> 加载动态路由
  if (authStore.isAuthenticated && !dynamicRoutesLoaded) {
    try {
      const roles = authStore.roles;
      const accessibleRoutes = generateRoutes(roles);
      accessibleRoutes.forEach((route) => router.addRoute(route));
      dynamicRoutesLoaded = true;
      next({ ...to, replace: true });
      return;
    } catch (error) {
      console.error('Failed to load dynamic routes:', error);
      next(); // 兜底放行，避免卡死
    }
  }

  next();
});

export default router;