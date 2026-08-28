import { http, type Result } from './http';
import type {
  LoginRequest,
  LoginResponse,
  UserInfo,
  ChangePasswordRequest,
  ResetPasswordRequest,
} from '@/types/auth';

export const authApi = {
  login(data: LoginRequest): Promise<Result<LoginResponse>> {
    return http.post('/auth/login', data);
  },

  logout(): Promise<Result<void>> {
    return http.post('/auth/logout');
  },

  me(): Promise<Result<UserInfo>> {
    return http.get('/auth/me');
  },

  check(permission: string): Promise<Result<boolean>> {
    return http.post('/auth/check', { permission });
  },

  changePassword(data: ChangePasswordRequest): Promise<Result<void>> {
    return http.post('/auth/change-password', data);
  },

  resetPassword(data: ResetPasswordRequest): Promise<Result<void>> {
    return http.post('/auth/reset-password', data);
  },

  refreshToken(): Promise<Result<{ token: string; tokenType: string; expiresIn: number }>> {
    return http.post('/auth/refresh');
  },
};
