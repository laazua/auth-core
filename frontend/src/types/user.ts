import type { PageResult } from './api';

export interface UserVO {
  id: number;
  username: string;
  nickname: string;
  email: string;
  phone: string;
  avatar?: string;
  status: number;
  createTime: string;
  updateTime?: string;
  lastLoginTime?: string;
  roles?: RoleVO[];
}

export interface RoleVO {
  id: number;
  code: string;
  name: string;
  description?: string;
  status: number;
  createTime: string;
}

export interface UserQuery {
  page?: number;
  size?: number;
  username?: string;
  nickname?: string;
  phone?: string;
  email?: string;
  status?: number;
  createTimeStart?: string;
  createTimeEnd?: string;
}

export interface UserCreateRequest {
  username: string;
  password: string;
  nickname: string;
  email: string;
  phone: string;
  status?: number;
  roleIds?: number[];
  confirmPassword?: string;
}

export interface UserUpdateRequest {
  nickname?: string;
  email?: string;
  phone?: string;
  status?: number;
  avatar?: string;
}

export interface AssignRolesRequest {
  roleIds: number[];
}

export interface ChangePasswordRequest {
  oldPassword: string;
  newPassword: string;
}

export interface ResetPasswordRequest {
  newPassword: string;
}

export type UserPageResult = PageResult<UserVO>;
