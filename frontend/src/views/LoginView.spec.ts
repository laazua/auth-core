import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import { mount } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import { createRouter, createWebHistory } from 'vue-router';
import LoginView from '@/views/LoginView.vue';
import { useAuthStore } from '@/stores/auth';
import { authApi } from '@/api/auth';
import { ElMessage } from 'element-plus';
import BaseButton from '@/components/BaseButton.vue';
import BaseInput from '@/components/BaseInput.vue';

vi.mock('@/api/auth');
vi.mock('element-plus', () => ({
  ElMessage: {
    success: vi.fn(),
    error: vi.fn(),
  },
}));
// 覆盖 setup.ts 的 vue-router 全局桩：本 spec 已装真实 router 插件，
// 组件须拿到真件（currentRoute/push），否则 LoginView.vue:75 读 currentRoute.value 抛错、导航 no-op（R1）
vi.mock(
  'vue-router',
  async (importOriginal) => (await importOriginal()) as typeof import('vue-router')
);

const mockAuthApi = vi.mocked(authApi);
const mockElMessage = vi.mocked(ElMessage);

const createWrapper = (routePath = '/login') => {
  const router = createRouter({
    history: createWebHistory(),
    routes: [
      { path: '/login', name: 'Login', component: LoginView, meta: { public: true } },
      {
        path: '/dashboard',
        name: 'Dashboard',
        component: { template: '<div>Dashboard</div>' },
        meta: { requiresAuth: true },
      },
      {
        path: '/403',
        name: 'Forbidden',
        component: { template: '<div>403</div>' },
        meta: { public: true },
      },
      {
        path: '/user',
        name: 'User',
        component: { template: '<div>User</div>' },
        meta: { requiresAuth: true },
      },
    ],
  });
  router.push(routePath);
  return mount(LoginView, {
    global: {
      plugins: [router],
      components: {
        BaseButton,
        BaseInput,
      },
      mocks: {
        $t: (key: string) => key,
      },
    },
  });
};

describe('LoginView', () => {
  let authStore: ReturnType<typeof useAuthStore>;

  beforeEach(() => {
    setActivePinia(createPinia());
    vi.clearAllMocks();
    mockAuthApi.me.mockResolvedValue({
      code: 0,
      message: 'success',
      timestamp: Date.now(),
      data: {
        user: {
          id: 1,
          username: 'admin',
          nickname: '管理员',
          email: 'admin@example.com',
          phone: '13800138000',
          status: 1,
          createTime: '2024-01-01T00:00:00Z',
        },
        roles: [
          { id: 1, code: 'admin', name: 'Admin', status: 1, createTime: '2024-01-01T00:00:00Z' },
        ],
        permissions: ['*'],
      },
    });
    authStore = useAuthStore();
    authStore.logout();
  });

  afterEach(() => {
    vi.resetAllMocks();
  });

  describe('Component Rendering (AC1)', () => {
    it('renders login page correctly with brand and form sections', async () => {
      const wrapper = createWrapper();
      expect(wrapper.find('.login-page').exists()).toBe(true);
      expect(wrapper.find('.login__brand-title').text()).toBe('Auth Core');
      expect(wrapper.find('.login__brand-slogan').text()).toBe('通用权限管理系统');
      expect(wrapper.find('.login__title').text()).toBe('欢迎登录');
      expect(wrapper.find('.login__subtitle').text()).toBe('请输入您的账号信息');
      expect(wrapper.find('input[placeholder="请输入用户"]').exists()).toBe(true);
      expect(wrapper.find('input[placeholder="请输入密码"]').exists()).toBe(true);
      expect(wrapper.find('.login__remember').exists()).toBe(true);
      expect(wrapper.find('.login__forgot').exists()).toBe(true);
      expect(wrapper.find('.login__submit').exists()).toBe(true);
    });

    it('has correct validation rules defined', () => {
      const wrapper = createWrapper();
      const vm = wrapper.vm as any;

      expect(vm.rules.username).toBeDefined();
      expect(vm.rules.username.some((r: any) => r.required && r.message === '请输入用户')).toBe(
        true
      );
      expect(vm.rules.username.some((r: any) => r.min === 3 && r.max === 20)).toBe(true);

      expect(vm.rules.password).toBeDefined();
      expect(vm.rules.password.some((r: any) => r.required && r.message === '请输入密码')).toBe(
        true
      );
      expect(vm.rules.password.some((r: any) => r.min === 6 && r.max === 30)).toBe(true);
    });

    it('shows brand features list', () => {
      const wrapper = createWrapper();
      const features = wrapper.findAll('.login__feature');
      expect(features.length).toBe(4);
      expect(features[0].find('.login__feature-title').text()).toBe('仪表盘概览');
      expect(features[1].find('.login__feature-title').text()).toBe('权限管理');
      expect(features[2].find('.login__feature-title').text()).toBe('认证授权');
      expect(features[3].find('.login__feature-title').text()).toBe('模块化架构');
    });
  });

  describe('Login Success Redirect (AC2)', () => {
    it('calls authApi.login with correct credentials', async () => {
      const wrapper = createWrapper();
      const vm = wrapper.vm as any;

      mockAuthApi.login.mockResolvedValue({
        message: 'success',
        timestamp: Date.now(),
        code: 0,
        data: {
          token: 'mock-jwt-token',
          tokenType: 'Bearer',
          expiresIn: 3600,
          userInfo: {
            id: 1,
            username: 'admin',
            nickname: '管理员',
            email: 'admin@example.com',
            phone: '13800138000',
            status: 1,
            createTime: '2024-01-01T00:00:00Z',
          },
        },
      });

      vm.formRef = {
        validate: vi.fn().mockResolvedValue(undefined),
      };
      vm.loginForm.username = 'admin';
      vm.loginForm.password = 'admin123456';

      await vm.handleLogin();

      expect(mockAuthApi.login).toHaveBeenCalledWith({
        username: 'admin',
        password: 'admin123456',
        rememberMe: false,
      });
    });

    it('handles successful login response', async () => {
      const wrapper = createWrapper();
      const vm = wrapper.vm as any;

      mockAuthApi.login.mockResolvedValue({
        message: 'success',
        timestamp: Date.now(),
        code: 0,
        data: {
          token: 'mock-jwt-token',
          tokenType: 'Bearer',
          expiresIn: 3600,
          userInfo: {
            id: 1,
            username: 'admin',
            nickname: '管理员',
            email: 'admin@example.com',
            phone: '13800138000',
            status: 1,
            createTime: '2024-01-01T00:00:00Z',
          },
        },
      });

      vm.formRef = {
        validate: vi.fn().mockResolvedValue(undefined),
      };
      vm.loginForm.username = 'admin';
      vm.loginForm.password = 'admin123456';

      await vm.handleLogin();

      expect(mockElMessage.success).toHaveBeenCalledWith('登录成功');
      expect(vm.loading).toBe(false);
      expect(vm.errorMessage).toBe('');
    });

    it('stores token before calling me() on successful login', async () => {
      const wrapper = createWrapper();
      const vm = wrapper.vm as any;
      const authStore = useAuthStore();

      mockAuthApi.login.mockResolvedValue({
        message: 'success',
        timestamp: Date.now(),
        code: 0,
        data: {
          token: 'mock-jwt-token',
          tokenType: 'Bearer',
          expiresIn: 3600,
          userInfo: {
            id: 1,
            username: 'admin',
            nickname: '管理员',
            email: 'admin@example.com',
            phone: '13800138000',
            status: 1,
            createTime: '2024-01-01T00:00:00Z',
          },
        },
      });

      vm.formRef = {
        validate: vi.fn().mockResolvedValue(undefined),
      };
      vm.loginForm.username = 'admin';
      vm.loginForm.password = 'admin123456';

      await vm.handleLogin();

      expect(authStore.token).toBe('mock-jwt-token');
      expect(mockAuthApi.me).toHaveBeenCalled();
      expect(mockAuthApi.login.mock.calls[0][0]).toEqual({
        username: 'admin',
        password: 'admin123456',
        rememberMe: false,
      });
    });

    it('sets loading state during login process', async () => {
      const wrapper = createWrapper();
      const vm = wrapper.vm as any;

      mockAuthApi.login.mockResolvedValue({
        message: 'success',
        timestamp: Date.now(),
        code: 0,
        data: {
          token: 'mock-jwt-token',
          tokenType: 'Bearer',
          expiresIn: 3600,
          userInfo: {
            id: 1,
            username: 'admin',
            nickname: '管理员',
            email: 'admin@example.com',
            phone: '13800138000',
            status: 1,
            createTime: '2024-01-01T00:00:00Z',
          },
        },
      });

      vm.formRef = {
        validate: vi.fn().mockResolvedValue(undefined),
      };
      vm.loginForm.username = 'admin';
      vm.loginForm.password = 'admin123456';

      expect(vm.loading).toBe(false);

      await vm.handleLogin();

      expect(vm.loading).toBe(false);
    });

    it('computes correct redirect path from query param', async () => {
      const router = createRouter({
        history: createWebHistory(),
        routes: [
          { path: '/login', name: 'Login', component: LoginView, meta: { public: true } },
          {
            path: '/dashboard',
            name: 'Dashboard',
            component: { template: '<div>Dashboard</div>' },
            meta: { requiresAuth: true },
          },
          {
            path: '/user',
            name: 'User',
            component: { template: '<div>User</div>' },
            meta: { requiresAuth: true },
          },
        ],
      });
      router.push('/login?redirect=/user');
      await router.isReady();

      const wrapper = mount(LoginView, {
        global: {
          plugins: [router],
          components: {
            BaseButton,
            BaseInput,
          },
        },
      });
      const vm = wrapper.vm as any;

      const redirect = vm.$route.query.redirect || '/dashboard';
      expect(redirect).toBe('/user');
    });

    it('defaults to dashboard when no redirect param', () => {
      const wrapper = createWrapper();
      const vm = wrapper.vm as any;

      const redirect = vm.$route.query.redirect || '/dashboard';
      expect(redirect).toBe('/dashboard');
    });
  });

  describe('Login Failure Error (AC3)', () => {
    it('shows error message on 401 unauthorized', async () => {
      const wrapper = createWrapper();
      const vm = wrapper.vm as any;

      mockAuthApi.login.mockRejectedValue(new Error('用户名或密码错误'));

      vm.formRef = {
        validate: vi.fn().mockResolvedValue(undefined),
      };
      vm.loginForm.username = 'admin';
      vm.loginForm.password = 'wrongpassword';

      await vm.handleLogin();

      expect(mockElMessage.error).toHaveBeenCalledWith('用户名或密码错误');
      expect(vm.errorMessage).toBe('用户名或密码错误');
      expect(vm.loading).toBe(false);
    });

    it('shows error message on 403 account disabled', async () => {
      const wrapper = createWrapper();
      const vm = wrapper.vm as any;

      mockAuthApi.login.mockRejectedValue(new Error('账号已停用'));

      vm.formRef = {
        validate: vi.fn().mockResolvedValue(undefined),
      };
      vm.loginForm.username = 'disabled';
      vm.loginForm.password = 'password123';

      await vm.handleLogin();

      expect(mockElMessage.error).toHaveBeenCalledWith('账号已停用');
      expect(vm.errorMessage).toBe('账号已停用');
    });

    it('resets loading state after failure', async () => {
      const wrapper = createWrapper();
      const vm = wrapper.vm as any;

      mockAuthApi.login.mockRejectedValue(new Error('用户名或密码错误'));

      vm.formRef = {
        validate: vi.fn().mockResolvedValue(undefined),
      };
      vm.loginForm.username = 'admin';
      vm.loginForm.password = 'wrongpassword';

      await vm.handleLogin();

      expect(vm.loading).toBe(false);
    });

    it('stops login if validation fails', async () => {
      const wrapper = createWrapper();
      const vm = wrapper.vm as any;

      vm.formRef = {
        validate: vi.fn().mockRejectedValue(new Error('Validation failed')),
      };
      vm.loginForm.username = '';
      vm.loginForm.password = '';

      await vm.handleLogin();

      expect(mockAuthApi.login).not.toHaveBeenCalled();
    });
  });

  describe('Responsive Layout', () => {
    it('has responsive CSS classes for mobile/desktop', () => {
      const wrapper = createWrapper();
      expect(wrapper.find('.login-container').exists()).toBe(true);
      expect(wrapper.find('.login__brand').exists()).toBe(true);
      expect(wrapper.find('.login__form-wrapper').exists()).toBe(true);
    });
  });

  describe('Password Visibility (AC1/AC2/AC4)', () => {
    it('renders password masked with toggle rendered by default (AC1)', () => {
      // Arrange
      const wrapper = createWrapper();

      // Act
      const passwordInput = wrapper.find('input[placeholder="请输入密码"]');
      const baseInputs = wrapper.findAllComponents({ name: 'BaseInput' });
      const passwordBaseInput = baseInputs[1];

      // Assert
      expect(passwordInput.attributes('type')).toBe('password');
      expect(passwordBaseInput.props('showPassword')).toBe(true);
      expect(passwordBaseInput.props('type')).toBe('password');
      expect(passwordBaseInput.find('.base-input__suffix').exists()).toBe(true);
    });

    it('toggle switches password type to text and back (AC2)', async () => {
      // Arrange
      const wrapper = createWrapper();
      const baseInputs = wrapper.findAllComponents({ name: 'BaseInput' });
      const passwordBaseInput = baseInputs[1];
      const toggleButton = passwordBaseInput.find('.base-input__suffix');
      expect(toggleButton.exists()).toBe(true);

      // Act & Assert: 掩码 → 明文
      await toggleButton.trigger('click');
      expect(passwordBaseInput.find('input').attributes('type')).toBe('text');

      // Act & Assert: 明文 → 掩码
      await toggleButton.trigger('click');
      expect(passwordBaseInput.find('input').attributes('type')).toBe('password');
    });

    it('toggle preserves password value and login payload (AC4)', async () => {
      // Arrange
      const wrapper = createWrapper();
      const vm = wrapper.vm as any;
      const baseInputs = wrapper.findAllComponents({ name: 'BaseInput' });
      const passwordBaseInput = baseInputs[1];
      await passwordBaseInput.find('input').setValue('admin123456');

      // Act: 切换显隐
      const toggleButton = passwordBaseInput.find('.base-input__suffix');
      expect(toggleButton.exists()).toBe(true);
      await toggleButton.trigger('click');

      // Assert: 切换不改变已输入密码值
      expect(vm.loginForm.password).toBe('admin123456');

      // Act: 切换后提交登录
      vm.formRef = { validate: vi.fn().mockResolvedValue(undefined) };
      vm.loginForm.username = 'admin';
      mockAuthApi.login.mockResolvedValue({
        code: 0,
        message: 'success',
        timestamp: Date.now(),
        data: {
          token: 'mock-jwt-token',
          tokenType: 'Bearer',
          expiresIn: 3600,
          userInfo: {
            id: 1,
            username: 'admin',
            nickname: '管理员',
            email: 'admin@example.com',
            phone: '13800138000',
            status: 1,
            createTime: '2024-01-01T00:00:00Z',
          },
        },
      });
      await vm.handleLogin();

      // Assert: 登录 payload 与切换前一致
      expect(mockAuthApi.login).toHaveBeenCalledWith({
        username: 'admin',
        password: 'admin123456',
        rememberMe: false,
      });
    });
  });

  describe('Enter Key Support', () => {
    it('triggers login API call on Enter key', async () => {
      const wrapper = createWrapper();
      const vm = wrapper.vm as any;

      vm.formRef = {
        validate: vi.fn().mockResolvedValue(undefined),
      };
      vm.loginForm.username = 'admin';
      vm.loginForm.password = 'admin123456';

      mockAuthApi.login.mockResolvedValue({
        code: 0,
        message: 'success',
        timestamp: Date.now(),
        data: {
          token: 'mock-jwt-token',
          tokenType: 'Bearer',
          expiresIn: 3600,
          userInfo: {
            id: 1,
            username: 'admin',
            nickname: '管理员',
            email: 'admin@example.com',
            phone: '13800138000',
            status: 1,
            createTime: '2024-01-01T00:00:00Z',
          },
        },
      });

      vm.handleKeyUp({ key: 'Enter' } as KeyboardEvent);

      // Wait for async handleLogin to complete
      await new Promise((resolve) => setTimeout(resolve, 10));

      expect(mockAuthApi.login).toHaveBeenCalledWith({
        username: 'admin',
        password: 'admin123456',
        rememberMe: false,
      });
    });

    it('does not trigger login on non-Enter keys', async () => {
      const wrapper = createWrapper();
      const vm = wrapper.vm as any;

      vm.handleKeyUp({ key: 'Tab' } as KeyboardEvent);
      vm.handleKeyUp({ key: 'Escape' } as KeyboardEvent);

      // Wait a bit to ensure no async calls
      await new Promise((resolve) => setTimeout(resolve, 10));

      expect(mockAuthApi.login).not.toHaveBeenCalled();
    });
  });

  describe('Login Redirect Fix (web/026)', () => {
    it('redirects to dashboard on successful login with correct credentials', async () => {
      const wrapper = createWrapper();
      const vm = wrapper.vm as any;

      mockAuthApi.login.mockResolvedValue({
        code: 0,
        message: 'success',
        timestamp: Date.now(),
        data: {
          token: 'mock-jwt-token',
          tokenType: 'Bearer',
          expiresIn: 3600,
          userInfo: {
            id: 1,
            username: 'admin',
            nickname: '管理员',
            email: 'admin@example.com',
            phone: '13800138000',
            status: 1,
            createTime: '2024-01-01T00:00:00Z',
          },
        },
      });

      vm.formRef = {
        validate: vi.fn().mockResolvedValue(undefined),
      };
      vm.loginForm.username = 'admin';
      vm.loginForm.password = 'admin123456';

      await vm.handleLogin();

      expect(mockAuthApi.login).toHaveBeenCalled();
      expect(mockAuthApi.me).toHaveBeenCalled();
      expect(mockElMessage.success).toHaveBeenCalledWith('登录成功');
    });

    it('shows error message when me() API fails with 404', async () => {
      const wrapper = createWrapper();
      const vm = wrapper.vm as any;

      mockAuthApi.login.mockResolvedValue({
        code: 0,
        message: 'success',
        timestamp: Date.now(),
        data: {
          token: 'mock-token',
          tokenType: 'Bearer',
          expiresIn: 3600,
          userInfo: {
            id: 1,
            username: 'admin',
            nickname: '管理员',
            email: 'admin@example.com',
            phone: '13800138000',
            status: 1,
            createTime: '2024-01-01T00:00:00Z',
          },
        },
      });

      mockAuthApi.me.mockRejectedValue(new Error('404 Not Found'));

      vm.formRef = { validate: vi.fn().mockResolvedValue(undefined) };
      vm.loginForm.username = 'admin';
      vm.loginForm.password = 'admin123456';

      await vm.handleLogin();

      expect(mockElMessage.error).toHaveBeenCalledWith(expect.stringContaining('接口地址错误'));
      expect(vm.errorMessage).toContain('接口地址错误');
    });

    it('shows error message on login failure (wrong password)', async () => {
      const wrapper = createWrapper();
      const vm = wrapper.vm as any;

      mockAuthApi.login.mockRejectedValue(new Error('用户名或密码错误'));

      vm.formRef = { validate: vi.fn().mockResolvedValue(undefined) };
      vm.loginForm.username = 'admin';
      vm.loginForm.password = 'wrongpassword';

      await vm.handleLogin();

      expect(mockElMessage.error).toHaveBeenCalledWith('用户名或密码错误');
      expect(vm.errorMessage).toBe('用户名或密码错误');
    });

    it('shows error message on account disabled', async () => {
      const wrapper = createWrapper();
      const vm = wrapper.vm as any;

      mockAuthApi.login.mockRejectedValue(new Error('账号已停用'));

      vm.formRef = { validate: vi.fn().mockResolvedValue(undefined) };
      vm.loginForm.username = 'disabled';
      vm.loginForm.password = 'password123';

      await vm.handleLogin();

      expect(mockElMessage.error).toHaveBeenCalledWith('账号已停用');
      expect(vm.errorMessage).toBe('账号已停用');
    });
  });

  describe('Login Redirect (web/028)', () => {
    it('redirects to dashboard after successful login', async () => {
      const router = createRouter({
        history: createWebHistory(),
        routes: [
          { path: '/login', name: 'Login', component: LoginView, meta: { public: true } },
          {
            path: '/dashboard',
            name: 'Dashboard',
            component: { template: '<div>Dashboard</div>' },
            meta: { requiresAuth: true },
          },
        ],
      });
      router.push('/login?redirect=/dashboard');
      await router.isReady();

      const wrapper = mount(LoginView, {
        global: { plugins: [router], components: { BaseButton, BaseInput } },
      });
      const vm = wrapper.vm as any;

      mockAuthApi.login.mockResolvedValue({
        code: 0,
        message: 'success',
        timestamp: Date.now(),
        data: {
          token: 'mock-jwt-token',
          tokenType: 'Bearer',
          expiresIn: 3600,
          userInfo: {
            id: 1,
            username: 'admin',
            nickname: '管理员',
            email: 'admin@example.com',
            phone: '13800138000',
            status: 1,
            createTime: '2024-01-01T00:00:00Z',
          },
        },
      });

      vm.formRef = { validate: vi.fn().mockResolvedValue(undefined) };
      vm.loginForm.username = 'admin';
      vm.loginForm.password = 'admin123456';

      await vm.handleLogin();

      // 验证路由跳转
      expect(router.currentRoute.value.name).toBe('Dashboard');
    });
  });
});
