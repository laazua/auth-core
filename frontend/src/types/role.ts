import type { PageResult } from './api';

export interface RoleVO {
  id: number;
  code: string;
  name: string;
  description?: string;
  status: number;
  createTime: string;
  permissions?: PermissionVO[];
}

export interface PermissionVO {
  id: number;
  code: string;
  name: string;
  type: number;
  parentId?: number;
  path?: string;
  component?: string;
  icon?: string;
  sort: number;
  status: number;
  createTime: string;
}

export interface RoleQuery {
  page?: number;
  size?: number;
  code?: string;
  name?: string;
  status?: number;
}

export interface RoleCreateRequest {
  code: string;
  name: string;
  description?: string;
  status?: number;
  permissionIds?: number[];
}

export interface RoleUpdateRequest {
  name?: string;
  description?: string;
  status?: number;
}

export interface AssignPermissionsRequest {
  permissionIds: number[];
}

export type RolePageResult = PageResult<RoleVO>;
