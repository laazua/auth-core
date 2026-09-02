import { http, type Result, type PageResult } from './http';
import type {
  PermissionVO,
  PermissionQuery,
  PermissionCreateRequest,
  PermissionUpdateRequest,
  PermissionTreeNode,
} from '@/types/permission';

export const permissionApi = {
  list(params: PermissionQuery): Promise<Result<PageResult<PermissionVO>>> {
    return http.get('/permissions', { params });
  },

  getTree(): Promise<Result<PermissionTreeNode[]>> {
    return http.get('/permissions/tree');
  },

  getById(id: number): Promise<Result<PermissionVO>> {
    return http.get(`/permissions/${id}`);
  },

  create(data: PermissionCreateRequest): Promise<Result<PermissionVO>> {
    return http.post('/permissions', data);
  },

  update(id: number, data: PermissionUpdateRequest): Promise<Result<PermissionVO>> {
    return http.put(`/permissions/${id}`, data);
  },

  delete(id: number): Promise<Result<void>> {
    return http.delete(`/permissions/${id}`);
  },
};
