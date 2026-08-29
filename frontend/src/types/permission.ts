import type { PageResult } from './api';

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

export interface PermissionQuery {
  page?: number;
  size?: number;
  code?: string;
  name?: string;
  moduleId?: number;
  status?: number;
}

export interface PermissionCreateRequest {
  code: string;
  name: string;
  moduleId: number;
  description?: string;
  status?: number;
}

export interface PermissionUpdateRequest {
  name?: string;
  moduleId?: number;
  description?: string;
  status?: number;
}

export interface PermissionTreeNode extends PermissionVO {
  children: PermissionTreeNode[];
}

export type PermissionPageResult = PageResult<PermissionVO>;