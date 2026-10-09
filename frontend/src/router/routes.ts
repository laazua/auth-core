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
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/profile/IndexView.vue'),
        meta: { requiresAuth: true, title: '个人中心', hidden: true },
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/settings/IndexView.vue'),
        meta: { requiresAuth: true, title: '设置', hidden: true },
      },
      {
        path: 'profile/password',
        name: 'Password',
        component: () => import('@/views/profile/PasswordView.vue'),
        meta: { requiresAuth: true, title: '修改密码', hidden: true },
      },
    ],
  },
];

export const asyncRoutes: AppRouteRecordRaw[] = [
  {
    path: '/mymodules',
    component: () => import('@/layouts/DefaultLayout.vue'),
    meta: { requiresAuth: true, title: '我的模块', icon: 'Files' },
    children: [
      {
        path: '',
        name: 'MyModules',
        component: () => import('@/views/MyModulesView.vue'),
        meta: { requiresAuth: true, title: '我的模块', icon: 'Files' },
      },
    ],
  },
  {
    path: '/workspace/module/:code',
    component: () => import('@/layouts/DefaultLayout.vue'),
    meta: { requiresAuth: true, title: '模块工作区', hidden: true },
    children: [
      {
        path: '',
        name: 'ModuleWorkspace',
        component: () => import('@/views/ModuleIframeView.vue'),
        meta: { requiresAuth: true, title: '模块工作区', hidden: true },
      },
    ],
  },
  {
    path: '/system',
    name: 'System',
    component: () => import('@/layouts/DefaultLayout.vue'),
    redirect: '/system/permissions',
    meta: { requiresAuth: true, title: '系统管理', icon: 'Setting' },
    children: [
      {
        path: 'users',
        name: 'UsersIndex',
        component: () => import('@/views/users/IndexView.vue'),
        meta: { requiresAuth: true, title: '用户管理', icon: 'User', permissions: ['user:view'] },
      },
      {
        path: 'roles',
        name: 'RolesIndex',
        component: () => import('@/views/roles/IndexView.vue'),
        meta: {
          requiresAuth: true,
          title: '角色管理',
          icon: 'UserFilled',
          permissions: ['role:view'],
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
          permissions: ['perm:view'],
        },
      },
      {
        path: 'modules',
        name: 'Modules',
        component: () => import('@/views/system/IndexView.vue'),
        meta: { requiresAuth: true, title: '模块管理', icon: 'Grid', permissions: ['module:view'] },
      },
    ],
  },
];

export const generateRoutes = (roles: string[]): RouteRecordRaw[] => {
  const accessibleRoutes = asyncRoutes.filter((route) => {
    if (!route.meta?.roles) return true;
    return route.meta.roles.some((role: string) => roles.includes(role));
  });

  const routes: RouteRecordRaw[] = [
    ...layoutRoutes,
    ...(accessibleRoutes as RouteRecordRaw[]),
    { path: '/:pathMatch(.*)*', redirect: '/404' },
  ];
  return routes;
};
