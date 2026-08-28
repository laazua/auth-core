import { http, type Result, type PageResult } from './http';
import type {
  UserVO,
  UserQuery,
  UserCreateRequest,
  UserUpdateRequest,
  AssignRolesRequest,
  ChangePasswordRequest,
  ResetPasswordRequest,
} from '@/types/user';

export const userApi = {
  list(params: UserQuery): Promise<Result<PageResult<UserVO>>> {
    return http.get('/users', { params });
  },

  getById(id: number): Promise<Result<UserVO>> {
    return http.get(`/users/${id}`);
  },

  create(data: UserCreateRequest): Promise<Result<UserVO>> {
    return http.post('/users', data);
  },

  update(id: number, data: UserUpdateRequest): Promise<Result<UserVO>> {
    return http.put(`/users/${id}`, data);
  },

  delete(id: number): Promise<Result<void>> {
    return http.del(`/users/${id}`);
  },

  enable(id: number): Promise<Result<void>> {
    return http.post(`/users/${id}/enable`);
  },

  disable(id: number): Promise<Result<void>> {
    return http.post(`/users/${id}/disable`);
  },

  assignRoles(id: number, data: AssignRolesRequest): Promise<Result<void>> {
    return http.post(`/users/${id}/roles`, data);
  },

  changePassword(data: ChangePasswordRequest): Promise<Result<void>> {
    return http.post('/auth/change-password', data);
  },

  resetPassword(userId: number, data: ResetPasswordRequest): Promise<Result<void>> {
    return http.post(`/users/${userId}/reset-password`, data);
  },
};
