import { http, type Result, type PageResult } from './http';
import type {
  RoleVO,
  RoleQuery,
  RoleCreateRequest,
  RoleUpdateRequest,
  AssignPermissionsRequest,
} from '@/types/role';

export const roleApi = {
  list(params: RoleQuery): Promise<Result<PageResult<RoleVO>>> {
    return http.get('/roles', { params });
  },

  getById(id: number): Promise<Result<RoleVO>> {
    return http.get(`/roles/${id}`);
  },

  create(data: RoleCreateRequest): Promise<Result<RoleVO>> {
    return http.post('/roles', data);
  },

  update(id: number, data: RoleUpdateRequest): Promise<Result<RoleVO>> {
    return http.put(`/roles/${id}`, data);
  },

  delete(id: number): Promise<Result<void>> {
    return http.delete(`/roles/${id}`);
  },

  assignPermissions(id: number, data: AssignPermissionsRequest): Promise<Result<void>> {
    return http.post(`/roles/${id}/permissions`, data);
  },

  getAllPermissions(): Promise<Result<{ id: number; code: string; name: string }[]>> {
    return http.get('/roles/permissions/all');
  },
};
