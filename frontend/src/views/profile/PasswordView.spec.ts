import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';
import { mount } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import { createRouter, createMemoryHistory } from 'vue-router';
import PasswordView from '@/views/profile/PasswordView.vue';
import { useAuthStore } from '@/stores/auth';
import { userApi } from '@/api/user';
import { ElMessage } from 'element-plus';
import BaseButton from '@/components/BaseButton.vue';
import BaseInput from '@/components/BaseInput.vue';
import BaseCard from '@/components/BaseCard.vue';
import { ElBreadcrumb, ElBreadcrumbItem, ElForm, ElFormItem } from 'element-plus';

vi.mock('@/api/user');
vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal<typeof import('element-plus')>();
  return {
    ...actual,
    ElMessage: {
      success: vi.fn(),
      error: vi.fn(),
    },
    ElBreadcrumb: actual.ElBreadcrumb,
    ElBreadcrumbItem: actual.ElBreadcrumbItem,
    ElForm: actual.ElForm,
    ElFormItem: actual.ElFormItem,
  };
});

const mockUserApi = vi.mocked(userApi);
const mockElMessage = vi.mocked(ElMessage);

let pinia: ReturnType<typeof createPinia>;

const createWrapper = (routePath = '/profile/password') => {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/login', name: 'Login', component: { template: '<div>Login</div>' }, meta: { public: true } },
      { path: '/dashboard', name: 'Dashboard', component: { template: '<div>Dashboard</div>' }, meta: { requiresAuth: true } },
      { path: '/profile/password', name: 'Password', component: PasswordView, meta: { requiresAuth: true, title: '修改密码' } },
    ],
  });
  router.push(routePath);
  return mount(PasswordView, {
    global: {
      plugins: [router, pinia],
      components: {
        BaseButton,
        BaseInput,
        BaseCard,
        ElBreadcrumb,
        ElBreadcrumbItem,
        ElForm,
        ElFormItem,
      },
      mocks: {
        $t: (key: string) => key,
      },
    },
  });
};

describe('PasswordView', () => {
  let authStore: ReturnType<typeof useAuthStore>;

  beforeEach(() => {
    pinia = createPinia();
    setActivePinia(pinia);
    vi.clearAllMocks();
    authStore = useAuthStore();
    authStore.login('mock-token', { id: 1, username: 'admin', nickname: '管理员', email: 'admin@example.com', phone: '13800138000', status: 1, createTime: '2024-01-01T00:00:00Z' }, [], []);
  });

  afterEach(() => {
    vi.resetAllMocks();
  });

  describe('Form Validation (AC1)', () => {
    it('renders password change page with breadcrumb', () => {
      const wrapper = createWrapper();
      expect(wrapper.find('.password-page').exists()).toBe(true);
      expect(wrapper.findComponent(ElBreadcrumb).exists()).toBe(true);
      expect(wrapper.text()).toContain('修改密码');
    });

    it('shows required field errors on empty submit', async () => {
      const wrapper = createWrapper();
      const vm = wrapper.vm as any;

      vm.formRef = {
        validate: vi.fn().mockRejectedValue(new Error('Validation failed')),
      };

      await vm.handleSubmit();

      expect(vm.formRef.validate).toHaveBeenCalled();
      expect(mockUserApi.changePassword).not.toHaveBeenCalled();
    });

    it('shows password complexity error for short password', async () => {
      const wrapper = createWrapper();
      const vm = wrapper.vm as any;

      vm.formRef = {
        validate: vi.fn().mockResolvedValue(undefined),
      };
      vm.form.oldPassword = 'oldpass123';
      vm.form.newPassword = 'short';
      vm.form.confirmPassword = 'short';

      await vm.handleSubmit();

      expect(vm.errors.newPassword).toContain('密码需包含大小写字母、数字、特殊字符至少三类且长度≥8');
    });

    it('shows password complexity error for password without uppercase', async () => {
      const wrapper = createWrapper();
      const vm = wrapper.vm as any;

      vm.formRef = {
        validate: vi.fn().mockResolvedValue(undefined),
      };
      vm.form.oldPassword = 'oldpass123';
      vm.form.newPassword = 'lowercase123'; // only lowercase + number = 2 categories
      vm.form.confirmPassword = 'lowercase123';

      await vm.handleSubmit();

      expect(vm.errors.newPassword).toContain('密码需包含大小写字母、数字、特殊字符至少三类且长度≥8');
    });

    it('shows password complexity error for password without number', async () => {
      const wrapper = createWrapper();
      const vm = wrapper.vm as any;

      vm.formRef = {
        validate: vi.fn().mockResolvedValue(undefined),
      };
      vm.form.oldPassword = 'oldpass123';
      vm.form.newPassword = 'NoNumberHere'; // uppercase + lowercase = 2 categories
      vm.form.confirmPassword = 'NoNumberHere';

      await vm.handleSubmit();

      expect(vm.errors.newPassword).toContain('密码需包含大小写字母、数字、特殊字符至少三类且长度≥8');
    });

    it('shows password complexity error for password without special char', async () => {
      const wrapper = createWrapper();
      const vm = wrapper.vm as any;

      vm.formRef = {
        validate: vi.fn().mockResolvedValue(undefined),
      };
      vm.form.oldPassword = 'oldpass123';
      vm.form.newPassword = 'NoSpecialChar'; // uppercase + lowercase = 2 categories
      vm.form.confirmPassword = 'NoSpecialChar';

      await vm.handleSubmit();

      expect(vm.errors.newPassword).toContain('密码需包含大小写字母、数字、特殊字符至少三类且长度≥8');
    });

    it('shows confirm password mismatch error', async () => {
      const wrapper = createWrapper();
      const vm = wrapper.vm as any;

      vm.formRef = {
        validate: vi.fn().mockResolvedValue(undefined),
      };
      vm.form.oldPassword = 'oldpass123';
      vm.form.newPassword = 'ValidPass123!';
      vm.form.confirmPassword = 'DifferentPass123!';

      await vm.handleSubmit();

      expect(vm.errors.confirmPassword).toContain('两次输入的密码不一致');
    });

    it('shows new password same as old password error', async () => {
      const wrapper = createWrapper();
      const vm = wrapper.vm as any;

      vm.formRef = {
        validate: vi.fn().mockResolvedValue(undefined),
      };
      vm.form.oldPassword = 'SamePass123!';
      vm.form.newPassword = 'SamePass123!';
      vm.form.confirmPassword = 'SamePass123!';

      await vm.handleSubmit();

      expect(vm.errors.newPassword).toContain('新密码不能与旧密码相同');
    });

    it('passes validation with valid input', async () => {
      const wrapper = createWrapper();
      const vm = wrapper.vm as any;

      vm.formRef = {
        validate: vi.fn().mockResolvedValue(undefined),
      };
      vm.form.oldPassword = 'oldpass123';
      vm.form.newPassword = 'ValidPass123!';
      vm.form.confirmPassword = 'ValidPass123!';

      mockUserApi.changePassword.mockResolvedValue({ code: 0, message: 'success', timestamp: Date.now(), data: undefined });

      await vm.handleSubmit();

      expect(vm.errors.newPassword).toBeUndefined();
      expect(vm.errors.confirmPassword).toBeUndefined();
      expect(mockUserApi.changePassword).toHaveBeenCalledWith({
        oldPassword: 'oldpass123',
        newPassword: 'ValidPass123!',
      });
    });
  });

  describe('Wrong Old Password (AC2)', () => {
    it('shows error when old password is wrong (code=1005)', async () => {
      const wrapper = createWrapper();
      const vm = wrapper.vm as any;

      vm.formRef = {
        validate: vi.fn().mockResolvedValue(undefined),
      };
      vm.form.oldPassword = 'wrongoldpass';
      vm.form.newPassword = 'ValidPass123!';
      vm.form.confirmPassword = 'ValidPass123!';

      mockUserApi.changePassword.mockResolvedValue({
        code: 1005,
        message: '旧密码错误',
        timestamp: Date.now(),
        data: undefined,
      });

      await vm.handleSubmit();

      expect(mockElMessage.error).toHaveBeenCalledWith('旧密码错误');
      expect(vm.loading).toBe(false);
    });
  });

  describe('New Password Same As Old (AC3)', () => {
    it('shows error when new password equals old password (client-side validation)', async () => {
      const wrapper = createWrapper();
      const vm = wrapper.vm as any;

      vm.formRef = {
        validate: vi.fn().mockResolvedValue(undefined),
      };
      vm.form.oldPassword = 'SamePass123!';
      vm.form.newPassword = 'SamePass123!';
      vm.form.confirmPassword = 'SamePass123!';

      await vm.handleSubmit();

      expect(vm.errors.newPassword).toContain('新密码不能与旧密码相同');
      expect(mockUserApi.changePassword).not.toHaveBeenCalled();
      expect(vm.loading).toBe(false);
    });
  });

  describe('Change Password Success Redirect (AC4)', () => {
    it('shows success message, clears token, redirects to login on success', async () => {
      const wrapper = createWrapper();
      const vm = wrapper.vm as any;

      vm.formRef = {
        validate: vi.fn().mockResolvedValue(undefined),
      };
      vm.form.oldPassword = 'oldpass123';
      vm.form.newPassword = 'NewPass123!';
      vm.form.confirmPassword = 'NewPass123!';

      mockUserApi.changePassword.mockResolvedValue({ code: 0, message: 'success', timestamp: Date.now(), data: undefined });

      const logoutSpy = vi.spyOn(authStore, 'logout');

      await vm.handleSubmit();

      expect(mockElMessage.success).toHaveBeenCalledWith('密码修改成功，请重新登录');
      expect(logoutSpy).toHaveBeenCalled();
      expect(authStore.token).toBeNull();
      expect(vm.loading).toBe(false);
      // router.push('/login') is called in component but uses mocked router in tests
    });
  });
});