import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import type { UserInfo } from '@/types/auth';
import { storage } from '@/utils/storage';

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>((storage.get('token') as string) || null);
  const userInfo = ref<UserInfo | null>((storage.get('userInfo') as UserInfo) || null);
  const roles = ref<string[]>((storage.get('roles') as string[]) || []);
  const permissions = ref<string[]>((storage.get('permissions') as string[]) || []);

  const isAuthenticated = computed(() => !!token.value);

  const setToken = (newToken: string | null) => {
    token.value = newToken;
    if (newToken) {
      storage.set('token', newToken);
    } else {
      storage.remove('token');
    }
  };

  const setUserInfo = (info: UserInfo | null) => {
    userInfo.value = info;
    if (info) {
      storage.set('userInfo', info);
    } else {
      storage.remove('userInfo');
    }
  };

  const setRoles = (newRoles: string[]) => {
    roles.value = newRoles;
    storage.set('roles', newRoles);
  };

  const setPermissions = (newPermissions: string[]) => {
    permissions.value = newPermissions;
    storage.set('permissions', newPermissions);
  };

  const hasPermission = (permission: string): boolean => {
    if (!permissions.value.length) return false;
    if (permissions.value.includes('*')) return true;
    if (permissions.value.includes(permission)) return true;

    const parts = permission.split(':');
    if (parts.length >= 2) {
      const prefix = parts[0];
      return permissions.value.some((p) => p === `${prefix}:*`);
    }
    return false;
  };

  const hasRole = (role: string): boolean => {
    return roles.value.includes(role);
  };

  const hasAnyRole = (roleList: string[]): boolean => {
    return roleList.some((role) => roles.value.includes(role));
  };

  const hasAnyPermission = (permissionList: string[]): boolean => {
    return permissionList.some((permission) => hasPermission(permission));
  };

  const login = (
    loginToken: string,
    user: UserInfo,
    userRoles: string[],
    userPermissions: string[]
  ) => {
    setToken(loginToken);
    setUserInfo(user);
    setRoles(userRoles);
    setPermissions(userPermissions);
  };

  const logout = () => {
    setToken(null);
    setUserInfo(null);
    setRoles([]);
    setPermissions([]);
    storage.clear();
  };

  const refreshAuth = (user: UserInfo, userRoles: string[], userPermissions: string[]) => {
    setUserInfo(user);
    setRoles(userRoles);
    setPermissions(userPermissions);
  };

  return {
    token,
    userInfo,
    roles,
    permissions,
    isAuthenticated,
    setToken,
    setUserInfo,
    setRoles,
    setPermissions,
    hasPermission,
    hasRole,
    hasAnyRole,
    hasAnyPermission,
    login,
    logout,
    refreshAuth,
  };
});
