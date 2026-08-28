import type { NavigationGuardNext, RouteLocationNormalized } from 'vue-router';
import { useAuthStore } from '@/stores/auth';

export const authGuard = async (
  to: RouteLocationNormalized,
  from: RouteLocationNormalized,
  next: NavigationGuardNext
) => {
  const authStore = useAuthStore();

  if (!authStore.isAuthenticated) {
    next({ name: 'Login', query: { redirect: to.fullPath } });
    return;
  }

  next();
};

export const guestGuard = async (
  to: RouteLocationNormalized,
  from: RouteLocationNormalized,
  next: NavigationGuardNext
) => {
  const authStore = useAuthStore();

  if (authStore.isAuthenticated) {
    next({ name: 'Dashboard' });
    return;
  }

  next();
};

export const permissionGuard = (requiredPermissions: string[]) => {
  return async (
    to: RouteLocationNormalized,
    from: RouteLocationNormalized,
    next: NavigationGuardNext
  ) => {
    const authStore = useAuthStore();

    if (!authStore.isAuthenticated) {
      next({ name: 'Login', query: { redirect: to.fullPath } });
      return;
    }

    const hasPermission = requiredPermissions.some((p) => authStore.hasPermission(p));

    if (!hasPermission) {
      next({ name: 'Dashboard' });
      return;
    }

    next();
  };
};

export const roleGuard = (requiredRoles: string[]) => {
  return async (
    to: RouteLocationNormalized,
    from: RouteLocationNormalized,
    next: NavigationGuardNext
  ) => {
    const authStore = useAuthStore();

    if (!authStore.isAuthenticated) {
      next({ name: 'Login', query: { redirect: to.fullPath } });
      return;
    }

    const hasRole = requiredRoles.some((r) => authStore.hasRole(r));

    if (!hasRole) {
      next({ name: 'Dashboard' });
      return;
    }

    next();
  };
};
