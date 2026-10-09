import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';

// Mock @vueuse/core BEFORE any imports
vi.mock('@vueuse/core', () => ({
  useMediaQuery: () => ({ value: false }),
  useWindowSize: () => ({ width: 1024, height: 768 }),
}));

// Mock components BEFORE importing UserFormDrawer
vi.mock('@/components/BaseButton.vue', () => ({
  default: {
    name: 'BaseButton',
    props: ['variant', 'size', 'loading', 'disabled'],
    emits: ['click'],
    template:
      '<button class="base-button-mock" @click="$emit(\'click\', $event)" :disabled="disabled || loading" :class="{ \'is-loading\': loading }"><slot /></button>',
  },
}));

vi.mock('@/components/BaseInput.vue', () => ({
  default: {
    name: 'BaseInput',
    props: [
      'modelValue',
      'placeholder',
      'type',
      'clearable',
      'showPassword',
      'disabled',
      'readonly',
      'maxlength',
    ],
    emits: ['update:modelValue', 'blur', 'focus', 'change', 'clear'],
    template:
      '<input class="base-input-mock" :value="modelValue" :placeholder="placeholder" :type="type" :disabled="disabled" :readonly="readonly" :maxlength="maxlength" @input="$emit(\'update:modelValue\', $event.target.value)" @blur="$emit(\'blur\', $event)" @focus="$emit(\'focus\', $event)" @change="$emit(\'change\', $event.target.value)" />',
  },
}));

vi.mock('@/components/BaseSelect.vue', () => ({
  default: {
    name: 'BaseSelect',
    props: ['modelValue', 'options', 'placeholder', 'disabled', 'clearable', 'filterable'],
    emits: ['update:modelValue', 'change', 'blur', 'focus', 'clear'],
    template:
      '<select class="base-select-mock" :value="modelValue" :disabled="disabled" @input="$emit(\'update:modelValue\', $event.target.value)" @change="$emit(\'change\', $event.target.value)"><option v-for="opt in options" :key="opt.value" :value="opt.value">{{ opt.label }}</option></select>',
  },
}));

import { mount } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import UserFormDrawer from '@/views/users/UserFormDrawer.vue';
vi.mock('element-plus', async (importOriginal) => {
  const actual = (await importOriginal()) as any;
  return {
    ...actual,
    ElMessage: {
      success: vi.fn(),
      error: vi.fn(),
      warning: vi.fn(),
      info: vi.fn(),
    },
    ElMessageBox: {
      confirm: vi.fn().mockResolvedValue('confirm'),
      alert: vi.fn(),
      prompt: vi.fn(),
    },
  };
});

describe('UserFormDrawer - 用户表单抽屉', () => {
  let pinia: ReturnType<typeof createPinia>;

  const createWrapper = (
    props: { visible: boolean; mode: 'create' | 'edit'; initialData?: any } = {
      visible: true,
      mode: 'create',
    }
  ) => {
    const wrapper = mount(UserFormDrawer, {
      props,
      global: {
        plugins: [pinia],
        mocks: {
          $t: (key: string) => key,
        },
        stubs: {
          ElDrawer: {
            name: 'ElDrawer',
            props: ['modelValue', 'title', 'direction', 'size', 'beforeClose'],
            emits: ['update:modelValue', 'close'],
            template: `
              <div v-if="modelValue" class="el-drawer-mock">
                <div class="el-drawer__header"><span class="el-drawer__title">{{ title }}</span></div>
                <div class="el-drawer__body"><slot /></div>
                <div class="el-drawer__footer"><slot name="footer" /></div>
              </div>
            `,
          },
          ElForm: {
            name: 'ElForm',
            props: ['model', 'rules', 'labelWidth'],
            emits: ['validate'],
            template: '<form class="el-form-mock"><slot /></form>',
            methods: {
              validate(callback: (valid: boolean) => void) {
                callback(true);
              },
              clearValidate() {},
            },
          },
          ElFormItem: {
            name: 'ElFormItem',
            props: ['label', 'prop', 'rules'],
            template: '<div class="el-form-item-mock"><slot /></div>',
          },
        },
      },
    });
    return wrapper;
  };

  beforeEach(() => {
    vi.clearAllMocks();
    pinia = createPinia();
    setActivePinia(pinia);
  });

  afterEach(() => {
    vi.resetAllMocks();
  });

  describe('AC1 - 确认按钮不再因抽屉可见而禁用', () => {
    it('新增模式：打开抽屉时确认按钮可点击（非 loading、非 disabled）', async () => {
      const wrapper = createWrapper({ visible: true, mode: 'create' });
      await wrapper.vm.$nextTick();

      const confirmButton = wrapper.find('.base-button-mock:last-child'); // 最后一个按钮是确定
      expect(confirmButton.exists()).toBe(true);
      expect(confirmButton.attributes('disabled')).toBeUndefined();
      expect(confirmButton.classes()).not.toContain('is-loading');
    });

    it('编辑模式：打开抽屉时确认按钮可点击（非 loading、非 disabled）', async () => {
      const initialData = {
        id: 1,
        username: 'admin',
        nickname: '管理员',
        email: 'admin@example.com',
        phone: '13800138000',
        status: 1,
      };
      const wrapper = createWrapper({ visible: true, mode: 'edit', initialData });
      await wrapper.vm.$nextTick();

      const confirmButton = wrapper.find('.base-button-mock:last-child');
      expect(confirmButton.exists()).toBe(true);
      expect(confirmButton.attributes('disabled')).toBeUndefined();
      expect(confirmButton.classes()).not.toContain('is-loading');
    });
  });

  describe('AC2 - 提交期间按钮进入 loading 状态', () => {
    it('新增模式：点击确认按钮触发表单校验通过后，按钮立即进入 loading 状态，直到提交完成', async () => {
      const wrapper = createWrapper({ visible: true, mode: 'create' });
      await wrapper.vm.$nextTick();

      const confirmButton = wrapper.find('.base-button-mock:last-child');

      // 模拟表单验证通过
      const formRef = wrapper.vm.$refs.formRef as any;
      if (formRef) {
        formRef.validate = vi.fn((callback: (valid: boolean) => void) => callback(true));
      }

      // 点击确认按钮
      await confirmButton.trigger('click');
      await wrapper.vm.$nextTick();

      // 验证按钮进入 loading 状态
      expect(confirmButton.classes()).toContain('is-loading');
    });

    it('编辑模式：点击确认按钮触发表单校验通过后，按钮立即进入 loading 状态，直到提交完成', async () => {
      const initialData = {
        id: 1,
        username: 'admin',
        nickname: '管理员',
        email: 'admin@example.com',
        phone: '13800138000',
        status: 1,
      };
      const wrapper = createWrapper({ visible: true, mode: 'edit', initialData });
      await wrapper.vm.$nextTick();

      const confirmButton = wrapper.find('.base-button-mock:last-child');

      // 模拟表单验证通过
      const formRef = wrapper.vm.$refs.formRef as any;
      if (formRef) {
        formRef.validate = vi.fn((callback: (valid: boolean) => void) => callback(true));
      }

      // 点击确认按钮
      await confirmButton.trigger('click');
      await wrapper.vm.$nextTick();

      // 验证按钮进入 loading 状态
      expect(confirmButton.classes()).toContain('is-loading');
    });
  });
});
