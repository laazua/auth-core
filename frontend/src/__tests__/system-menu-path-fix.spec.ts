import { describe, it, expect } from 'vitest';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const useMenuPath = resolve(__dirname, '../composables/useMenu.ts');
const useMenuContent = readFileSync(useMenuPath, 'utf-8');

describe('SystemMenuPathFixSpec', () => {
  describe('userMenuPathFixed', () => {
    it('useMenu.ts 中"用户管理"菜单路径为 /system/users', () => {
      expect(useMenuContent).toContain("path: '/system/users'");
      expect(useMenuContent).toContain("title: '用户管理'");
      const systemIndex = useMenuContent.indexOf("path: '/system'");
      const userIndex = useMenuContent.indexOf("path: '/system/users'");
      expect(userIndex).toBeGreaterThan(systemIndex);
    });

    it('useMenu.ts 中不应存在旧路径 /system/user', () => {
      expect(useMenuContent).not.toContain("path: '/system/user'");
    });
  });

  describe('roleMenuPathFixed', () => {
    it('useMenu.ts 中"角色管理"菜单路径为 /system/roles', () => {
      expect(useMenuContent).toContain("path: '/system/roles'");
      expect(useMenuContent).toContain("title: '角色管理'");
      const systemIndex = useMenuContent.indexOf("path: '/system'");
      const roleIndex = useMenuContent.indexOf("path: '/system/roles'");
      expect(roleIndex).toBeGreaterThan(systemIndex);
    });

    it('useMenu.ts 中不应存在旧路径 /system/role', () => {
      expect(useMenuContent).not.toContain("path: '/system/role'");
    });
  });

  describe('permModuleMenuPathsUnchanged', () => {
    it('useMenu.ts 中"权限管理"菜单路径为 /system/permissions', () => {
      expect(useMenuContent).toContain("path: '/system/permissions'");
      expect(useMenuContent).toContain("title: '权限管理'");
      const systemIndex = useMenuContent.indexOf("path: '/system'");
      const permIndex = useMenuContent.indexOf("path: '/system/permissions'");
      expect(permIndex).toBeGreaterThan(systemIndex);
    });

    it('useMenu.ts 中"模块管理"菜单路径为 /system/modules', () => {
      expect(useMenuContent).toContain("path: '/system/modules'");
      expect(useMenuContent).toContain("title: '模块管理'");
      const systemIndex = useMenuContent.indexOf("path: '/system'");
      const moduleIndex = useMenuContent.indexOf("path: '/system/modules'");
      expect(moduleIndex).toBeGreaterThan(systemIndex);
    });
  });

  describe('web034SystemRouteMigration', () => {
    it('AC1 useMenu 用户/角色菜单路径为 /system/users 与 /system/roles 且旧顶级路径不存在', () => {
      expect(useMenuContent).toContain("path: '/system/users'");
      expect(useMenuContent).toContain("path: '/system/roles'");
      expect(useMenuContent).not.toContain("path: '/users'");
      expect(useMenuContent).not.toContain("path: '/roles'");
      const systemIndex = useMenuContent.indexOf("path: '/system'");
      const userIndex = useMenuContent.indexOf("path: '/system/users'");
      const roleIndex = useMenuContent.indexOf("path: '/system/roles'");
      expect(systemIndex).toBeGreaterThanOrEqual(0);
      expect(userIndex).toBeGreaterThan(systemIndex);
      expect(roleIndex).toBeGreaterThan(systemIndex);
    });
  });
});
