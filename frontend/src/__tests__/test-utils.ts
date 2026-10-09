import type { Result, PageResult } from '@/types/api';
import type { UserVO, RoleVO } from '@/types/user';
import type { PermissionVO, PermissionTreeNode } from '@/types/permission';
import type { ModuleVO } from '@/types/module';

export const createResult = <T>(data: T, code = 0, message = 'success'): Result<T> => ({
  code,
  message,
  data,
  timestamp: Date.now(),
});

export const createPageResult = <T>(
  records: T[],
  total: number,
  size = 10,
  current = 1
): PageResult<T> => ({
  records,
  total,
  size,
  current,
  pages: Math.ceil(total / size),
});

export const createUserVO = (overrides: Partial<UserVO> = {}): UserVO => ({
  id: 1,
  username: 'admin',
  nickname: '管理员',
  email: 'admin@example.com',
  phone: '13800138000',
  status: 1,
  createTime: '2024-01-01T00:00:00Z',
  ...overrides,
});

export const createRoleVO = (overrides: Partial<RoleVO> = {}): RoleVO => ({
  id: 1,
  code: 'admin',
  name: '管理员',
  status: 1,
  createTime: '2024-01-01T00:00:00Z',
  ...overrides,
});

export const createPermissionVO = (overrides: Partial<PermissionVO> = {}): PermissionVO => ({
  id: 1,
  code: 'user:view',
  name: '用户查看',
  moduleId: 1,
  moduleName: '用户管理',
  description: '查看用户列表',
  status: 1,
  createTime: '2024-01-01T00:00:00Z',
  ...overrides,
});

export const createPermissionTreeNode = (
  overrides: Partial<PermissionTreeNode> = {}
): PermissionTreeNode => ({
  id: 1,
  code: 'user:view',
  name: '用户查看',
  moduleId: 1,
  moduleName: '用户管理',
  description: '查看用户列表',
  status: 1,
  createTime: '2024-01-01T00:00:00Z',
  children: [],
  ...overrides,
});

export const createModuleVO = (overrides: Partial<ModuleVO> = {}): ModuleVO => ({
  id: 1,
  code: 'user',
  name: '用户管理',
  baseUrl: '/api/v1/user',
  description: '用户管理模块',
  status: 1,
  createTime: '2024-01-01T00:00:00Z',
  ...overrides,
});

export const mockElMessageBox = () => ({
  confirm: vi.fn().mockResolvedValue(undefined),
  alert: vi.fn().mockResolvedValue(undefined),
  prompt: vi.fn().mockResolvedValue(undefined),
});

export const mockElMessage = () => ({
  success: vi.fn(),
  error: vi.fn(),
  warning: vi.fn(),
  info: vi.fn(),
});
