import { useAuthStore } from '@/stores/auth';

export interface MenuItem {
  path: string;
  title: string;
  icon?: string;
  roles?: string[];
  permissions?: string[];
  hidden?: boolean;
  external?: boolean;
  children?: MenuItem[];
  component?: string;
  redirect?: string;
  affix?: boolean;
  order?: number;
}

export interface MenuConfig extends MenuItem {
  children?: MenuConfig[];
}

const menuConfig: MenuConfig[] = [
  {
    path: '/dashboard',
    title: '仪表盘',
    icon: 'Monitor',
    order: 1,
    affix: true,
  },
  {
    path: '/system',
    title: '系统管理',
    icon: 'Setting',
    order: 2,
    children: [
      {
        path: '/system/users',
        title: '用户管理',
        icon: 'User',
        permissions: ['user:view'],
        order: 1,
      },
      {
        path: '/system/roles',
        title: '角色管理',
        icon: 'UserFilled',
        permissions: ['role:view'],
        order: 2,
      },
      {
        path: '/system/permissions',
        title: '权限管理',
        icon: 'Lock',
        permissions: ['perm:view'],
        order: 3,
      },
      {
        path: '/system/modules',
        title: '模块管理',
        icon: 'Grid',
        permissions: ['module:view'],
        order: 4,
      },
    ],
  },
];

export const useMenu = () => {
  const authStore = useAuthStore();

  const getMenuConfig = (): MenuConfig[] => {
    return menuConfig;
  };

  const hasPermission = (item: MenuConfig): boolean => {
    if (item.hidden) return false;

    if (item.roles && item.roles.length > 0) {
      if (!authStore.hasAnyRole(item.roles)) return false;
    }

    if (item.permissions && item.permissions.length > 0) {
      if (!authStore.hasAnyPermission(item.permissions)) return false;
    }

    return true;
  };

  const filterMenuByPermission = (items: MenuConfig[]): MenuConfig[] => {
    return items
      .filter((item) => hasPermission(item))
      .map((item) => {
        if (item.children && item.children.length > 0) {
          const filteredChildren = filterMenuByPermission(item.children);
          if (filteredChildren.length === 0) {
            return null;
          }
          return { ...item, children: filteredChildren };
        }
        return item;
      })
      .filter((item): item is MenuConfig => item !== null);
  };

  const generateMenuTree = (): MenuConfig[] => {
    const config = getMenuConfig();
    return filterMenuByPermission(config);
  };

  const sortMenu = (items: MenuConfig[]): MenuConfig[] => {
    return items
      .sort((a, b) => (a.order || 0) - (b.order || 0))
      .map((item) => {
        if (item.children && item.children.length > 0) {
          return { ...item, children: sortMenu(item.children) };
        }
        return item;
      });
  };

  const getSortedMenuTree = (): MenuConfig[] => {
    const tree = generateMenuTree();
    return sortMenu(tree);
  };

  const flatMenu = (items: MenuConfig[], result: MenuConfig[] = []): MenuConfig[] => {
    items.forEach((item) => {
      result.push(item);
      if (item.children && item.children.length > 0) {
        flatMenu(item.children, result);
      }
    });
    return result;
  };

  const getFlatMenuList = (): MenuConfig[] => {
    const tree = getSortedMenuTree();
    return flatMenu(tree);
  };

  const findMenuByPath = (path: string): MenuConfig | undefined => {
    const flatList = getFlatMenuList();
    return flatList.find((item) => item.path === path);
  };

  const getBreadcrumbItems = (path: string): MenuConfig[] => {
    const flatList = getFlatMenuList();
    const target = flatList.find((item) => item.path === path);
    if (!target) return [];

    const items: MenuConfig[] = [];
    let current: MenuConfig | undefined = target;

    while (current) {
      items.unshift(current);
      current = flatList.find((item) =>
        item.children?.some((child) => child.path === current?.path)
      );
    }

    return items;
  };

  return {
    getMenuConfig,
    generateMenuTree,
    getSortedMenuTree,
    getFlatMenuList,
    findMenuByPath,
    getBreadcrumbItems,
    filterMenuByPermission,
  };
};
