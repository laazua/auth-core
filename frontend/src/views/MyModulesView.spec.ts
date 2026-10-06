import { describe, it, expect, beforeEach, vi } from 'vitest';
import { mount, flushPromises, type VueWrapper } from '@vue/test-utils';
import { createRouter, createWebHistory, type Router } from 'vue-router';
import { setActivePinia, createPinia } from 'pinia';
import type { Mock } from 'vitest';
import MyModulesView from '@/views/MyModulesView.vue';
import { moduleApi } from '@/api/module';
import { useMenu } from '@/composables/useMenu';
import { useAuthStore } from '@/stores/auth';
import { staticRoutes, generateRoutes } from '@/router/routes';

vi.mock('@/api/module');
vi.mock('@/stores/auth', () => ({
  useAuthStore: vi.fn(),
}));

const zeroPermissionStore = {
  roles: [] as string[],
  permissions: [] as string[],
  hasPermission: vi.fn(() => false),
  hasRole: vi.fn(() => false),
  hasAnyRole: vi.fn(() => false),
  hasAnyPermission: vi.fn(() => false),
};

const sampleModules = [
  { id: 1, name: '新闻服务', code: 'news', baseUrl: 'http://news', description: '新闻聚合服务', status: 1 },
  { id: 2, name: '报表服务', code: 'report', baseUrl: 'http://report', description: '报表分析平台', status: 1 },
];

const createViewRouter = (): Router =>
  createRouter({
    history: createWebHistory(),
    routes: [
      { path: '/', name: 'MyModules', component: MyModulesView },
      { path: '/workspace/module/:code', name: 'ModuleWorkspace', component: { template: '<div />' } },
    ],
  });

const mountView = async (): Promise<VueWrapper> => {
  const router = createViewRouter();
  router.push('/');
  await router.isReady();
  const wrapper = mount(MyModulesView, { global: { plugins: [router] } });
  await flushPromises();
  return wrapper;
};

describe('MyModulesView — 我的模块菜单页（web/038d）', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.clearAllMocks();
  });

  describe('AC1 路由与菜单全员可见', () => {
    it('myModulesMenuAndRouteVisibleWithoutPermission', async () => {
      // Given: 零权限已登录用户
      (useAuthStore as unknown as Mock).mockReturnValue(zeroPermissionStore);

      // When: 读取合并路由表与过滤后菜单树
      const allRoutes = [...staticRoutes, ...generateRoutes([])];
      const { getSortedMenuTree } = useMenu();

      // Then: 路由存在且不绑权限
      const route = allRoutes.find((r) => r.path === '/mymodules');
      expect(route, '应存在 /mymodules 路由').toBeDefined();
      expect(route?.meta?.requiresAuth).toBe(true);
      expect(route?.meta).not.toHaveProperty('permissions');
      expect(route?.meta).not.toHaveProperty('roles');

      // Then: 零权限用户菜单仍含「我的模块」
      const item = getSortedMenuTree().find((i) => i.path === '/mymodules');
      expect(item, '零权限用户菜单应含 /mymodules').toBeDefined();
      expect(item?.title).toBe('我的模块');
    });
  });

  describe('AC2 列表渲染与空态', () => {
    it('rendersAccessibleModuleCards', async () => {
      // Given: getAccessibles 数据源存在且返回 2 项
      expect(typeof moduleApi.getAccessibles, 'moduleApi.getAccessibles 应为函数').toBe('function');
      vi.mocked(moduleApi.getAccessibles).mockResolvedValue({
        code: 0,
        message: 'success',
        timestamp: Date.now(),
        data: sampleModules,
      });

      // When: 挂载页面并等待数据
      const wrapper = await mountView();

      // Then: 渲染 2 张卡片且三字段文本均出现
      const cards = wrapper.findAll('.module-card');
      expect(cards).toHaveLength(2);
      expect(wrapper.text()).toContain('新闻服务');
      expect(wrapper.text()).toContain('news');
      expect(wrapper.text()).toContain('新闻聚合服务');
      expect(wrapper.text()).toContain('报表服务');
      expect(wrapper.text()).toContain('report');
      expect(wrapper.text()).toContain('报表分析平台');
    });

    it('rendersEmptyStateWhenNoModules', async () => {
      // Given: 用户无任何可访问模块
      vi.mocked(moduleApi.getAccessibles).mockResolvedValue({
        code: 0,
        message: 'success',
        timestamp: Date.now(),
        data: [],
      });

      // When: 挂载页面并等待数据
      const wrapper = await mountView();

      // Then: 显示空态文案且无卡片
      expect(wrapper.text()).toContain('暂无可访问模块');
      expect(wrapper.findAll('.module-card')).toHaveLength(0);
    });
  });

  describe('AC3 点卡导航', () => {
    it('navigatesToModuleWorkspaceOnCardClick', async () => {
      // Given: 渲染含 news 模块的页面
      vi.mocked(moduleApi.getAccessibles).mockResolvedValue({
        code: 0,
        message: 'success',
        timestamp: Date.now(),
        data: [sampleModules[0]],
      });
      const wrapper = await mountView();
      const pushSpy = vi.spyOn(wrapper.vm.$.appContext.config.globalProperties.$router as Router, 'push');

      // When: 点击卡片
      await wrapper.find('.module-card').trigger('click');

      // Then: 导航至工作区路由目标（目标路由由 038e 注册）
      expect(pushSpy).toHaveBeenCalledWith('/workspace/module/news');
    });
  });
});
