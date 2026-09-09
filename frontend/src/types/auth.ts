export interface UserInfo {
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
  permissions?: string[];
}

export interface MeResponse {
  user: UserInfo;
  roles: RoleVO[];
  permissions: string[];
}

export interface LoginRequest {
  username: string;
  password: string;
  rememberMe?: boolean;
  captcha?: string;
  captchaKey?: string;
}

export interface LoginResponse {
  token: string;
  tokenType: string;
  expiresIn: number;
  userInfo: UserInfo;
}

export interface RefreshTokenResponse {
  token: string;
  tokenType: string;
  expiresIn: number;
}

export interface ChangePasswordRequest {
  oldPassword: string;
  newPassword: string;
}

export interface ResetPasswordRequest {
  userId: number;
  newPassword: string;
}

export interface RoleVO {
  id: number;
  code: string;
  name: string;
  description?: string;
  status: number;
  createTime: string;
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

export interface ModuleVO {
  id: number;
  code: string;
  name: string;
  description?: string;
  status: number;
  createTime: string;
}
