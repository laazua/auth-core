import { http, type Result, type PageResult } from './http';
import type {
  ModuleVO,
  ModuleQuery,
  ModuleCreateRequest,
  ModuleUpdateRequest,
  AssignPermissionsRequest,
} from '@/types/module';

export const moduleApi = {
  list(params: ModuleQuery): Promise<Result<PageResult<ModuleVO>>> {
    return http.get('/modules', { params });
  },

  getById(id: number): Promise<Result<ModuleVO>> {
    return http.get(`/modules/${id}`);
  },

  create(data: ModuleCreateRequest): Promise<Result<ModuleVO>> {
    return http.post('/modules', data);
  },

  update(id: number, data: ModuleUpdateRequest): Promise<Result<ModuleVO>> {
    return http.put(`/modules/${id}`, data);
  },

  delete(id: number): Promise<Result<void>> {
    return http.del(`/modules/${id}`);
  },

  assignPermissions(id: number, data: AssignPermissionsRequest): Promise<Result<void>> {
    return http.post(`/modules/${id}/permissions`, data);
  },

  getAllPermissions(): Promise<Result<{ id: number; code: string; name: string }[]>> {
    return http.get('/modules/permissions/all');
  },
};
