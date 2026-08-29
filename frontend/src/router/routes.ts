import type { RouteRecordRaw } from 'vue-router';
import type { AppRouteRecordRaw } from '@/types/router';

export const staticRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { public: true, title: '登录', hidden: true },
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/ForbiddenView.vue'),
    meta: { public: true, title: '403', hidden: true },
  },
  {
    path: '/404',
    name: 'NotFound',
    component: () => import('@/views/NotFoundView.vue'),
    meta: { public: true, title: '404', hidden: true },
  },
  {
    path: '/redirect',
    component: () => import('@/layouts/DefaultLayout.vue'),
    meta: { hidden: true },
    children: [
      {
        path: '/redirect/:path(.*)',
        name: 'Redirect',
        component: () => import('@/views/RedirectView.vue'),
        meta: { hidden: true },
      },
    ],
  },
];

export const layoutRoutes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('@/layouts/DefaultLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/DashboardView.vue'),
        meta: { requiresAuth: true, title: '仪表盘', icon: 'Monitor', affix: true },
      },
    ],
  },
];

export const asyncRoutes: AppRouteRecordRaw[] = [
  {
    path: '/system',
    name: 'System',
    component: () => import('@/layouts/DefaultLayout.vue'),
    redirect: '/system/user',
    meta: { requiresAuth: true, title: '系统管理', icon: 'Setting', roles: ['admin'] },
    children: [
      {
        path: 'user',
        name: 'User',
        component: () => import('@/views/system/UserView.vue'),
        meta: { requiresAuth: true, title: '用户管理', icon: 'User', permissions: ['user:read'] },
      },
      {
        path: 'role',
        name: 'Role',
        component: () => import('@/views/system/RoleView.vue'),
        meta: {
          requiresAuth: true,
          title: '角色管理',
          icon: 'UserFilled',
          permissions: ['role:read'],
        },
      },
      {
        path: 'permissions',
        name: 'Permissions',
        component: () => import('@/views/system/IndexView.vue'),
        meta: {
          requiresAuth: true,
          title: '权限管理',
          icon: 'Lock',
          permissions: ['permission:read'],
        },
      },
      {
        path: 'modules',
        name: 'Modules',
        component: () => import('@/views/system/IndexView.vue'),
        meta: { requiresAuth: true, title: '模块管理', icon: 'Grid', permissions: ['module:read'] },
      },
    ],
  },
  {
    path: '/users',
    name: 'Users',
    component: () => import('@/layouts/DefaultLayout.vue'),
    meta: { requiresAuth: true, title: '用户管理', icon: 'User', permissions: ['user:read'] },
    children: [
      {
        path: '',
        name: 'UsersIndex',
        component: () => import('@/views/users/IndexView.vue'),
        meta: { requiresAuth: true, title: '用户管理', permissions: ['user:read'] },
      },
    ],
  },
  {
    path: '/roles',
    name: 'Roles',
    component: () => import('@/layouts/DefaultLayout.vue'),
    meta: { requiresAuth: true, title: '角色管理', icon: 'UserFilled', permissions: ['role:read'] },
    children: [
      {
        path: '',
        name: 'RolesIndex',
        component: () => import('@/views/roles/IndexView.vue'),
        meta: { requiresAuth: true, title: '角色管理', permissions: ['role:read'] },
      },
    ],
  },
];

export const generateRoutes = (roles: string[]): RouteRecordRaw[] => {
  const accessibleRoutes = asyncRoutes.filter((route) => {
    if (!route.meta?.roles) return true;
    return route.meta.roles.some((role) => roles.includes(role));
  });

  return [...layoutRoutes, ...accessibleRoutes, { path: '/:pathMatch(.*)*', redirect: '/404' }];
};