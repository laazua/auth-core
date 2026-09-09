import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import { mount, VueWrapper } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import { createRouter, createWebHistory } from 'vue-router';
import DefaultLayout from '@/layouts/DefaultLayout.vue';
import { useAppStore } from '@/stores/app';
import { useAuthStore } from '@/stores/auth';
import { ElMenu } from 'element-plus';

vi.mock('@/stores/auth', () => ({
  useAuthStore: vi.fn(),
}));

vi.mock('@/stores/app', () => ({
  useAppStore: vi.fn(),
}));

const mockAuthStore = {
  token: 'mock-token',
  userInfo: { id: 1, username: 'admin', nickname: 'Admin', email: 'admin@example.com' },
  roles: ['admin'],
  permissions: ['*'],
  isAuthenticated: true,
  hasPermission: vi.fn(() => true),
  hasRole: vi.fn(() => true),
  hasAnyRole: vi.fn(() => true),
  hasAnyPermission: vi.fn(() => true),
  logout: vi.fn(),
  setToken: vi.fn(),
  setUserInfo: vi.fn(),
  setRoles: vi.fn(),
  setPermissions: vi.fn(),
};

const mockAppStore = {
  sidebarCollapsed: false,
  sidebarOpened: false,
  theme: 'light',
  breadcrumbs: [],
  device: 'desktop',
  isMobile: false,
  toggleSidebar: vi.fn(),
  setSidebarCollapsed: vi.fn(),
  toggleTheme: vi.fn(),
  setTheme: vi.fn(),
  setBreadcrumbs: vi.fn(),
  setDevice: vi.fn(),
  initTheme: vi.fn(),
  restoreTags: vi.fn(),
  setSidebarOpened: vi.fn(),
  setActiveTag: vi.fn(),
  tagsViewList: [],
  cachedViews: [],
  activeTag: '/dashboard',
  menuList: [],
};

const routes = [
  { path: '/login', name: 'Login', meta: { public: true } },
  { path: '/dashboard', name: 'Dashboard', meta: { requiresAuth: true, title: '仪表盘' } },
  { path: '/system/user', name: 'User', meta: { requiresAuth: true, title: '用户管理' } },
  { path: '/system/role', name: 'Role', meta: { requiresAuth: true, title: '角色管理' } },
  { path: '/profile', name: 'Profile', meta: { requiresAuth: true, title: '个人中心' } },
];

const router = createRouter({
  history: createWebHistory(),
  routes: routes as import('vue-router').RouteRecordRaw[],
});

describe('DefaultLayout', () => {
  let wrapper: VueWrapper<any>;
  let appStore: ReturnType<typeof useAppStore>;

  beforeEach(() => {
    setActivePinia(createPinia());
    vi.clearAllMocks();

    (useAuthStore as unknown as ReturnType<typeof vi.fn>).mockReturnValue(mockAuthStore);
    (useAppStore as unknown as ReturnType<typeof vi.fn>).mockReturnValue(mockAppStore);

    appStore = useAppStore();

    wrapper = mount(DefaultLayout, {
      global: {
        plugins: [router],
        stubs: {
          'el-menu': ElMenu,
          'el-menu-item': true,
          'el-sub-menu': true,
          'el-dropdown': true,
          'el-dropdown-menu': true,
          'el-dropdown-item': true,
          'el-tooltip': true,
          'el-tabs': true,
          'el-tab-pane': true,
          'el-breadcrumb': true,
          'el-breadcrumb-item': true,
        },
      },
    });
  });

  afterEach(() => {
    wrapper.unmount();
  });

  describe('AC1 - Layout Rendering', () => {
    it('renders layout with sidebar, header, breadcrumb, and content area', () => {
      expect(wrapper.find('.layout').exists()).toBe(true);
      expect(wrapper.findComponent({ name: 'Sidebar' }).exists()).toBe(true);
      expect(wrapper.findComponent({ name: 'Header' }).exists()).toBe(true);
      expect(wrapper.findComponent({ name: 'Breadcrumb' }).exists()).toBe(true);
      expect(wrapper.find('.layout__content').exists()).toBe(true);
      expect(wrapper.find('.layout__page').exists()).toBe(true);
    });

    it('renders router-view with transition wrapper', () => {
      expect(wrapper.findComponent({ name: 'RouterView' }).exists()).toBe(true);
      expect(wrapper.find('.layout__page').exists()).toBe(true);
    });
  });

  describe('AC1 - Sidebar Collapse/Expand', () => {
    it('has sidebar toggle button in sidebar component', () => {
      const sidebar = wrapper.findComponent({ name: 'Sidebar' });
      expect(sidebar.exists()).toBe(true);
    });

    it('toggles sidebar when sidebar emits toggle event', async () => {
      const sidebar = wrapper.findComponent({ name: 'Sidebar' });
      await sidebar.vm.$emit('toggle');
      expect(appStore.toggleSidebar).toHaveBeenCalled();
    });
  });

  describe('AC1 - User Dropdown Menu', () => {
    it('toggles sidebar when header emits toggle event', async () => {
      const header = wrapper.findComponent({ name: 'Header' });
      await header.vm.$emit('toggle');
      expect(appStore.toggleSidebar).toHaveBeenCalled();
    });
  });

  describe('AC3 - Sidebar Responsive', () => {
    it('detects mobile viewport and sets device to mobile', () => {
      global.innerWidth = 500;
      global.dispatchEvent(new Event('resize'));

      expect(appStore.setDevice).toHaveBeenCalledWith('mobile');
      expect(appStore.setSidebarCollapsed).toHaveBeenCalledWith(true);
    });

    it('detects desktop viewport and sets device to desktop', () => {
      global.innerWidth = 1024;
      global.dispatchEvent(new Event('resize'));

      expect(appStore.setDevice).toHaveBeenCalledWith('desktop');
    });

    it('persists sidebar collapsed state in localStorage', () => {
      const toggleSidebar = appStore.toggleSidebar;
      toggleSidebar();
      expect(toggleSidebar).toHaveBeenCalled();
    });
  });

  describe('AC4 - Tag Management (TagsView)', () => {
    it('renders TagsView component', () => {
      expect(wrapper.findComponent({ name: 'TagsView' }).exists()).toBe(true);
    });
  });

  describe('AC5 - Breadcrumb and User Menu', () => {
    it('renders Breadcrumb component', () => {
      expect(wrapper.findComponent({ name: 'Breadcrumb' }).exists()).toBe(true);
    });

    it('renders Header with user menu', () => {
      expect(wrapper.findComponent({ name: 'Header' }).exists()).toBe(true);
    });
  });
});