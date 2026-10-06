import { describe, it, expect, vi } from 'vitest';
import { mount, flushPromises } from '@vue/test-utils';
import { createRouter, createMemoryHistory } from 'vue-router';
import ModuleIframeView from '@/views/ModuleIframeView.vue';
import { staticRoutes, generateRoutes } from '@/router/routes';
import DefaultLayout from '@/layouts/DefaultLayout.vue';

const mockRouter = {
  push: vi.fn(),
  replace: vi.fn(),
  go: vi.fn(),
  back: vi.fn(),
  forward: vi.fn(),
  addRoute: vi.fn(),
};

vi.mock('vue-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('vue-router')>();
  return {
    ...actual,
    useRouter: () => mockRouter,
    useRoute: () => ({ path: '/workspace/module/news', params: { code: 'news' }, query: {}, meta: {} }),
  };
});

const createAppRouter = () =>
  createRouter({
    history: createMemoryHistory(),
    routes: [...staticRoutes, ...generateRoutes([])] as never[],
  });

describe('ModuleIframeView — 模块 iframe 内嵌视图（web/038e）', () => {
  describe('AC1 iframe 路由与视图', () => {
    it('rendersIframeWithGatewaySrcAndLoadingState', async () => {
      // Given: 布局链路由（DefaultLayout 父 + ModuleWorkspace 子）
      const router = createAppRouter();
      const matched = router.resolve('/workspace/module/news').matched;
      expect(matched.length, '/workspace/module/:code 应为两层').toBe(2);
      expect(matched[1].name, '子记录 name').toBe('ModuleWorkspace');
      const parent = (await (matched[0].components!.default as () => Promise<{ default: unknown }>)()).default;
      expect(parent, '父记录组件必须是 DefaultLayout').toBe(DefaultLayout);

      // When: 挂载视图（route params code=news）
      const wrapper = mount(ModuleIframeView);
      await flushPromises();

      // Then: iframe src 精确相对路径含尾斜杠、无 host
      const iframe = wrapper.find('iframe');
      expect(iframe.exists()).toBe(true);
      expect(iframe.attributes('src')).toBe('/api/v1/gateway/news/');

      // Then: 初始加载态可见，load 事件后消失
      expect(wrapper.find('.module-iframe__loading').exists(), '初始应显示加载态').toBe(true);
      await iframe.trigger('load');
      expect(wrapper.find('.module-iframe__loading').exists(), 'load 后加载态应消失').toBe(false);
    });
  });

  describe('AC3 038d 点卡导航目标闭环', () => {
    it('workspaceRouteResolvesInsideLayoutNot404', async () => {
      // Given/When: 解析 038d 点卡导航目标
      const router = createAppRouter();
      const matched = router.resolve('/workspace/module/news').matched;

      // Then: 命中两层布局链而非 catch-all NotFound
      expect(matched.length, '应 depth=2 而非 catch-all').toBe(2);
      expect(matched[1].name).toBe('ModuleWorkspace');
      expect(matched[1].name, '不得是 NotFound').not.toBe('NotFound');
      const parent = (await (matched[0].components!.default as () => Promise<{ default: unknown }>)()).default;
      expect(parent, '父记录组件必须是 DefaultLayout').toBe(DefaultLayout);
    });
  });
});
