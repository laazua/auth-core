import type { PageResult } from './api';

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
  children?: PermissionVO[];
}

export interface PermissionQuery {
  page?: number;
  size?: number;
  code?: string;
  name?: string;
  type?: number;
  status?: number;
}

export interface PermissionCreateRequest {
  code: string;
  name: string;
  type: number;
  parentId?: number;
  path?: string;
  component?: string;
  icon?: string;
  sort?: number;
  status?: number;
}

export interface PermissionUpdateRequest {
  name?: string;
  type?: number;
  parentId?: number;
  path?: string;
  component?: string;
  icon?: string;
  sort?: number;
  status?: number;
}

export interface PermissionTreeNode extends PermissionVO {
  children: PermissionTreeNode[];
}

export type PermissionPageResult = PageResult<PermissionVO>;
