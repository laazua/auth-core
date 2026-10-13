import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';

vi.mock('@vueuse/core', () => ({
  useMediaQuery: () => ({ value: false }),
  useWindowSize: () => ({ width: 1024, height: 768 }),
}));

vi.mock('@/components/BaseButton.vue', () => ({
  default: {
    name: 'BaseButton',
    props: ['variant', 'size', 'loading', 'disabled'],
    emits: ['click'],
    template:
      '<button class="base-button-mock" @click="$emit(\'click\', $event)" :disabled="disabled || loading"><slot /></button>',
  },
}));

vi.mock('@/components/BaseInput.vue', () => ({
  default: {
    name: 'BaseInput',
    props: ['modelValue', 'placeholder', 'type', 'clearable', 'disabled', 'maxlength'],
    emits: ['update:modelValue'],
    template:
      '<input class="base-input-mock" :value="modelValue" :placeholder="placeholder" @input="$emit(\'update:modelValue\', $event.target.value)" />',
  },
}));

const mockGetById = vi.fn();
vi.mock('@/api/user', () => ({
  userApi: {
    getById: (...args: unknown[]) => mockGetById(...args),
  },
}));

const mockRoleList = vi.fn();
vi.mock('@/api/role', () => ({
  roleApi: {
    list: (...args: unknown[]) => mockRoleList(...args),
  },
}));

import { mount, flushPromises } from '@vue/test-utils';
import RoleAssignDrawer from '@/views/users/RoleAssignDrawer.vue';

describe('RoleAssignDrawer', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockGetById.mockResolvedValue({
      code: 0,
      message: 'success',
      data: {
        id: 1,
        username: 'admin',
        roles: [{ id: 2, name: '管理员', code: 'ROLE_ADMIN', status: 1 }],
      },
    });
    mockRoleList.mockResolvedValue({
      code: 0,
      message: 'success',
      data: {
        records: [
          { id: 2, name: '管理员', code: 'ROLE_ADMIN', status: 1 },
          { id: 3, name: '普通用户', code: 'ROLE_USER', status: 1 },
        ],
        total: 2,
      },
    });
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('打开抽屉时按userId回显已分配角色', async () => {
    const wrapper = mount(RoleAssignDrawer, {
      props: { visible: true, userId: 1 },
      global: {
        stubs: {
          'el-drawer': { template: '<div><slot /></div>' },
          'el-button': { template: '<button><slot /></button>' },
          'el-input': { template: '<input />' },
          'el-checkbox': { template: '<input type="checkbox" />' },
        },
      },
    });

    await flushPromises();

    expect(mockGetById).toHaveBeenCalledWith(1);
    const vm = wrapper.vm as unknown as { assignedRoleIds: number[] };
    expect(vm.assignedRoleIds).toEqual([2]);
  });
});
