import type { PageResult } from './api';

export interface ModuleVO {
  id: number;
  code: string;
  name: string;
  baseUrl: string;
  description?: string;
  status: number;
  createTime: string;
  permissions?: PermissionVO[];
}

export interface PermissionVO {
  id: number;
  code: string;
  name: string;
  moduleId: number;
  moduleName: string;
  description: string;
  status: number;
  createTime: string;
}

export interface ModuleQuery {
  page?: number;
  size?: number;
  code?: string;
  name?: string;
  status?: number;
}

export interface ModuleCreateRequest {
  code: string;
  name: string;
  baseUrl: string;
  description?: string;
  status?: number;
}

export interface ModuleUpdateRequest {
  name?: string;
  baseUrl?: string;
  description?: string;
  status?: number;
}

export interface AssignPermissionsRequest {
  permissionIds: number[];
}

export type ModulePageResult = PageResult<ModuleVO>;