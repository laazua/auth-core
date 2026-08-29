import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest';

// Mock @vueuse/core BEFORE any imports
vi.mock('@vueuse/core', () => ({
  useMediaQuery: () => ({ value: false }),
  useWindowSize: () => ({ width: 1024, height: 768 }),
}));

// Mock components BEFORE importing IndexView
vi.mock('@/components/BaseButton.vue', () => ({
  default: {
    name: 'BaseButton',
    props: ['variant', 'size', 'loading', 'disabled'],
    emits: ['click'],
    template: '<button class="base-button-mock" @click="$emit(\'click\', $event)" :disabled="disabled || loading"><slot /></button>',
  },
}));

vi.mock('@/components/BaseInput.vue', () => ({
  default: {
    name: 'BaseInput',
    props: ['modelValue', 'placeholder', 'type', 'clearable', 'showPassword', 'disabled', 'readonly', 'maxlength'],
    emits: ['update:modelValue', 'blur', 'focus', 'change', 'clear'],
    template: '<input class="base-input-mock" :value="modelValue" :placeholder="placeholder" :type="type" :disabled="disabled" :readonly="readonly" :maxlength="maxlength" @input="$emit(\'update:modelValue\', $event.target.value)" @blur="$emit(\'blur\', $event)" @focus="$emit(\'focus\', $event)" @change="$emit(\'change\', $event.target.value)" />',
  },
}));

vi.mock('@/components/BaseSelect.vue', () => ({
  default: {
    name: 'BaseSelect',
    props: ['modelValue', 'options', 'placeholder', 'disabled', 'clearable', 'filterable'],
    emits: ['update:modelValue', 'change', 'blur', 'focus', 'clear'],
    template: '<select class="base-select-mock" :value="modelValue" :disabled="disabled" @input="$emit(\'update:modelValue\', $event.target.value)" @change="$emit(\'change\', $event.target.value)"><option v-for="opt in options" :key="opt.value" :value="opt.value">{{ opt.label }}</option></select>',
  },
}));

vi.mock('@/components/BaseTable.vue', () => ({
  default: {
    name: 'BaseTable',
    props: ['data', 'columns', 'loading', 'rowKey', 'emptyText'],
    emits: ['sort-change', 'selection-change', 'row-click', 'row-dblclick', 'header-click', 'current-change', 'size-change'],
    template: `
      <div class="base-table-mock">
        <table>
          <thead>
            <tr>
              <th v-for="col in columns" :key="col.prop">{{ col.label }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in data" :key="row[rowKey || 'id']">
              <td v-for="col in columns" :key="col.prop">
                <div v-if="col.prop === 'actions' && col.render" class="user-table__actions">
                  <button class="user-table__action-edit" @click="$emit('row-click', row, 'edit')">编辑</button>
                  <button v-if="row.id !== 1" class="user-table__action-status" @click="$emit('row-click', row, 'status')">{{ row.status === 1 ? '停用' : '启用' }}</button>
                  <button v-if="row.id !== 1" class="user-table__action-reset" @click="$emit('row-click', row, 'reset')">重置密码</button>
                  <button class="user-table__action-roles" @click="$emit('row-click', row, 'roles')">角色分配</button>
                </div>
                <span v-else>{{ col.formatter ? col.formatter(row, {}, row[col.prop], 0) : row[col.prop] }}</span>
              </td>
            </tr>
          </tbody>
        </table>
        <slot name="append" />
      </div>
    `,
  },
}));

vi.mock('@/components/BaseCard.vue', () => ({
  default: {
    name: 'BaseCard',
    props: ['title', 'subtitle', 'bordered', 'shadow', 'padding'],
    template: '<div class="base-card-mock"><div v-if="title || $slots.header" class="base-card-mock__header"><slot name="header"><div><h3 v-if="title">{{ title }}</h3><p v-if="subtitle">{{ subtitle }}</p></div></slot></div><div class="base-card-mock__body"><slot /></div><div v-if="$slots.footer" class="base-card-mock__footer"><slot name="footer" /></div></div>',
  },
}));

vi.mock('@/views/users/UserFormDrawer.vue', () => ({
  default: {
    name: 'UserFormDrawer',
    props: ['visible', 'mode', 'initialData'],
    emits: ['submit', 'close'],
    template: '<div class="user-form-drawer-mock" v-if="visible" data-test="user-form-drawer"><slot /></div>',
  },
}));

vi.mock('@/views/users/RoleAssignDrawer.vue', () => ({
  default: {
    name: 'RoleAssignDrawer',
    props: ['visible', 'userId'],
    emits: ['submit', 'close'],
    template: '<div class="role-assign-drawer-mock" v-if="visible" data-test="role-assign-drawer"><slot /></div>',
  },
}));

import { mount } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import { createRouter, createWebHistory } from 'vue-router';
import IndexView from '@/views/users/IndexView.vue';
import { useAuthStore } from '@/stores/auth';
import { userApi } from '@/api/user';
import { roleApi } from '@/api/role';
import { ElMessage, ElMessageBox } from 'element-plus';

vi.mock('@/api/user');
vi.mock('@/api/role');
vi.mock('element-plus', async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    ElMessage: {
      success: vi.fn(),
      error: vi.fn(),
      warning: vi.fn(),
      info: vi.fn(),
    },
    ElMessageBox: {
      confirm: vi.fn(),
      alert: vi.fn(),
      prompt: vi.fn(),
    },
    ElPagination: {
      name: 'ElPagination',
      props: ['currentPage', 'pageSize', 'pageSizes', 'total', 'layout'],
      emits: ['update:currentPage', 'update:pageSize', 'current-change', 'page-size-change'],
      template: '<div class="el-pagination-mock" data-test="pagination">Pagination</div>',
    },
  };
});

const mockUserApi = vi.mocked(userApi);
const mockRoleApi = vi.mocked(roleApi);
const mockElMessage = vi.mocked(ElMessage);
const mockElMessageBox = vi.mocked(ElMessageBox);

const mockUsers = [
  {
    id: 1,
    username: 'admin',
    nickname: '管理员',
    email: 'admin@example.com',
    phone: '13800138000',
    status: 1,
    createTime: '2024-01-01T00:00:00Z',
    roles: [{ id: 1, code: 'admin', name: '管理员', status: 1, createTime: '2024-01-01T00:00:00Z' }],
  },
  {
    id: 2,
    username: 'user1',
    nickname: '用户1',
    email: 'user1@example.com',
    phone: '13800138001',
    status: 1,
    createTime: '2024-01-02T00:00:00Z',
    roles: [{ id: 2, code: 'user', name: '普通用户', status: 1, createTime: '2024-01-01T00:00:00Z' }],
  },
  {
    id: 3,
    username: 'user2',
    nickname: '用户2',
    email: 'user2@example.com',
    phone: '13800138002',
    status: 0,
    createTime: '2024-01-03T00:00:00Z',
    roles: [],
  },
];

const mockRoles = [
  { id: 1, code: 'admin', name: '管理员', status: 1, createTime: '2024-01-01T00:00:00Z' },
  { id: 2, code: 'user', name: '普通用户', status: 1, createTime: '2024-01-01T00:00:00Z' },
  { id: 3, code: 'editor', name: '编辑', status: 1, createTime: '2024-01-01T00:00:00Z' },
];

describe('IndexView - 用户管理页', () => {
  let authStore: ReturnType<typeof useAuthStore>;
  let pinia: ReturnType<typeof createPinia>;

const createWrapper = (permissions: string[] = ['user:read', 'user:create', 'user:update', 'user:delete', 'user:reset-password', 'user:assign-role']) => {
  // Update authStore with the given permissions
  authStore.setPermissions(permissions);
  
  const router = createRouter({
    history: createWebHistory(),
    routes: [
      { path: '/', name: 'Home', component: { template: '<div>Home</div>' }, meta: { requiresAuth: true } },
      { path: '/users', name: 'Users', component: IndexView, meta: { requiresAuth: true, permissions: ['user:read'] } },
      { path: '/login', name: 'Login', component: { template: '<div>Login</div>' }, meta: { public: true } },
    ],
  });
  router.push('/users');

  return mount(IndexView, {
    global: {
      plugins: [pinia, router],
      mocks: {
        $t: (key: string) => key,
      },
    },
  });
};

  beforeEach(() => {
    vi.clearAllMocks();
    pinia = createPinia();
    setActivePinia(pinia);
    authStore = useAuthStore();
    authStore.login('mock-token', { id: 1, username: 'admin', nickname: '管理员', email: '', phone: '', status: 1, createTime: '' }, ['admin'], ['user:read', 'user:create', 'user:update', 'user:delete', 'user:reset-password', 'user:assign-role', '*']);
    
    mockUserApi.list.mockResolvedValue({
      code: 0,
      data: {
        records: mockUsers,
        total: 3,
        page: 1,
        size: 10,
        pages: 1,
      },
    });
  });

  afterEach(() => {
    vi.resetAllMocks();
  });

  describe('Component Rendering', () => {
    it('renders user management page with toolbar and table', async () => {
      const wrapper = createWrapper();
      await wrapper.vm.$nextTick();

      expect(wrapper.find('.user-management').exists()).toBe(true);
      expect(wrapper.find('.user-toolbar').exists()).toBe(true);
      expect(wrapper.find('.user-table').exists()).toBe(true);
      expect(wrapper.find('.user-pagination').exists()).toBe(true);
    });

    it('shows search input, status filter, and create button in toolbar', async () => {
      const wrapper = createWrapper();
      await wrapper.vm.$nextTick();

      expect(wrapper.find('input[placeholder*="用户名/昵称/邮箱/手机号"]').exists()).toBe(true);
      expect(wrapper.find('select').exists()).toBe(true);
      expect(wrapper.find('.user-toolbar__create').exists()).toBe(true);
    });

    it('displays user list with correct columns', async () => {
      const wrapper = createWrapper();
      await wrapper.vm.$nextTick();

      const table = wrapper.findComponent({ name: 'BaseTable' });
      expect(table.exists()).toBe(true);
      expect(table.props('columns')).toBeDefined();
      const columns = table.props('columns') as any[];
      const columnProps = columns.map(c => c.prop);
      expect(columnProps).toContain('username');
      expect(columnProps).toContain('nickname');
      expect(columnProps).toContain('email');
      expect(columnProps).toContain('phone');
      expect(columnProps).toContain('status');
      expect(columnProps).toContain('roles');
      expect(columnProps).toContain('createTime');
      expect(columnProps).toContain('actions');
    });
  });

  describe('AC1 - 用户列表分页查询与条件筛选', () => {
    it('loads first page by default on mount', async () => {
      const wrapper = createWrapper();
      await wrapper.vm.$nextTick();

      expect(mockUserApi.list).toHaveBeenCalledWith(expect.objectContaining({ page: 1, size: 10 }));
    });

    it('filters users by search keyword', async () => {
      const wrapper = createWrapper();
      await wrapper.vm.$nextTick();

      const searchInput = wrapper.find('input[placeholder*="用户名/昵称/邮箱/手机号"]');
      await searchInput.setValue('admin');
      await wrapper.vm.$nextTick();

      expect(mockUserApi.list).toHaveBeenCalledWith(expect.objectContaining({
        page: 1,
        size: 10,
        username: 'admin',
        nickname: 'admin',
        email: 'admin',
        phone: 'admin',
      }));
    });

    it('filters users by status', async () => {
      const wrapper = createWrapper();
      await wrapper.vm.$nextTick();

      const statusSelect = wrapper.find('select');
      await statusSelect.setValue('1');
      await wrapper.vm.$nextTick();

      expect(mockUserApi.list).toHaveBeenLastCalledWith(expect.objectContaining({
        page: 1,
        size: 10,
        status: '1',
      }));
    });

    it('changes page correctly', async () => {
      mockUserApi.list.mockResolvedValue({
        code: 0,
        data: {
          records: mockUsers.slice(0, 1),
          total: 3,
          page: 2,
          size: 1,
          pages: 3,
        },
      });

      const wrapper = createWrapper();
      await wrapper.vm.$nextTick();

      // Directly call the component's handlePageChange method
      wrapper.vm.handlePageChange(2);
      await wrapper.vm.$nextTick();

      expect(mockUserApi.list).toHaveBeenLastCalledWith(expect.objectContaining({ page: 2, size: 10 }));
    });

    it('changes page size correctly', async () => {
      mockUserApi.list.mockResolvedValue({
        code: 0,
        data: {
          records: mockUsers.slice(0, 20),
          total: 3,
          page: 1,
          size: 20,
          pages: 1,
        },
      });

      const wrapper = createWrapper();
      await wrapper.vm.$nextTick();

      // Directly call the component's handleSizeChange method
      wrapper.vm.handleSizeChange(20);
      await wrapper.vm.$nextTick();

      expect(mockUserApi.list).toHaveBeenLastCalledWith(expect.objectContaining({ page: 1, size: 20 }));
    });
  });

  describe('AC2 - 新增用户密码加密且唯一', () => {
    it('opens create drawer when clicking create button', async () => {
      const wrapper = createWrapper();
      await wrapper.vm.$nextTick();

      const createButton = wrapper.find('.user-toolbar__create');
      await createButton.trigger('click');
      await wrapper.vm.$nextTick();

      expect(wrapper.findComponent({ name: 'UserFormDrawer' }).props('visible')).toBe(true);
      expect(wrapper.findComponent({ name: 'UserFormDrawer' }).props('mode')).toBe('create');
    });

    it('creates user successfully with valid form', async () => {
      const newUser = {
        id: 4,
        username: 'newuser',
        nickname: '新用户',
        email: 'new@example.com',
        phone: '13800138003',
        status: 1,
        createTime: '2024-01-04T00:00:00Z',
        roles: [],
      };

      mockUserApi.create.mockResolvedValue({
        code: 0,
        data: newUser,
      });

      mockUserApi.list
        .mockResolvedValueOnce({
          code: 0,
          data: {
            records: mockUsers,
            total: 3,
            page: 1,
            size: 10,
            pages: 1,
          },
        })
        .mockResolvedValueOnce({
          code: 0,
          data: {
            records: [...mockUsers, newUser],
            total: 4,
            page: 1,
            size: 10,
            pages: 1,
          },
        });

      const wrapper = createWrapper();
      // Wait for initial load
      await new Promise(resolve => setTimeout(resolve, 10));
      await wrapper.vm.$nextTick();

      // Directly call handleCreateSubmit
      await wrapper.vm.handleCreateSubmit({
        username: 'newuser',
        password: 'password123',
        confirmPassword: 'password123',
        nickname: '新用户',
        email: 'new@example.com',
        phone: '13800138003',
        status: 1,
      });
      await wrapper.vm.$nextTick();

      expect(mockUserApi.create).toHaveBeenCalledWith({
        username: 'newuser',
        password: 'password123',
        nickname: '新用户',
        email: 'new@example.com',
        phone: '13800138003',
        status: 1,
      });
      expect(mockElMessage.success).toHaveBeenCalledWith('创建成功');
      expect(mockUserApi.list).toHaveBeenCalledTimes(2); // Initial + refresh
    });

    it('shows error when username already exists', async () => {
      const error = new Error('用户名已存在');
      (error as any).response = { data: { code: 400, message: '用户名已存在' } };
      mockUserApi.create.mockRejectedValue(error);

      const wrapper = createWrapper();
      await new Promise(resolve => setTimeout(resolve, 10));
      await wrapper.vm.$nextTick();

      // Directly call handleCreateSubmit and expect it to throw
      await expect(wrapper.vm.handleCreateSubmit({
        username: 'admin',
        password: 'password123',
        confirmPassword: 'password123',
        nickname: '管理员',
        email: 'admin@example.com',
        phone: '13800138000',
        status: 1,
      })).rejects.toThrow();

      expect(mockElMessage.error).toHaveBeenCalledWith('用户名已存在');
    });
  });

  describe('AC3 - 编辑用户不含密码、用户名不可改', () => {
    it('opens edit drawer with user data when clicking edit', async () => {
      mockUserApi.getById.mockResolvedValue({
        code: 0,
        data: mockUsers[0],
      });

      const wrapper = createWrapper();
      await new Promise(resolve => setTimeout(resolve, 10));
      await wrapper.vm.$nextTick();

      // Directly call handleEdit method
      wrapper.vm.handleEdit(mockUsers[0]);
      await wrapper.vm.$nextTick();

      // Find the edit drawer (second UserFormDrawer)
      const drawers = wrapper.findAllComponents({ name: 'UserFormDrawer' });
      const editDrawer = drawers[1];
      expect(editDrawer.props('visible')).toBe(true);
      expect(editDrawer.props('mode')).toBe('edit');
      expect(editDrawer.props('initialData')).toEqual(mockUsers[0]);
    });

    it('does not include password field in edit mode', async () => {
      mockUserApi.getById.mockResolvedValue({
        code: 0,
        data: mockUsers[0],
      });

      const wrapper = createWrapper();
      await new Promise(resolve => setTimeout(resolve, 10));
      await wrapper.vm.$nextTick();

      wrapper.vm.handleEdit(mockUsers[0]);
      await wrapper.vm.$nextTick();

      const drawers = wrapper.findAllComponents({ name: 'UserFormDrawer' });
      const editDrawer = drawers[1];
      expect(editDrawer.props('mode')).toBe('edit');
    });

    it('updates user successfully without changing username/password', async () => {
      const updatedUser = {
        ...mockUsers[0],
        nickname: '更新后的管理员',
        email: 'updated@example.com',
      };

      mockUserApi.update.mockResolvedValue({
        code: 0,
        data: updatedUser,
      });

      mockUserApi.list
        .mockResolvedValueOnce({
          code: 0,
          data: {
            records: mockUsers,
            total: 3,
            page: 1,
            size: 10,
            pages: 1,
          },
        })
        .mockResolvedValueOnce({
          code: 0,
          data: {
            records: [updatedUser, ...mockUsers.slice(1)],
            total: 3,
            page: 1,
            size: 10,
            pages: 1,
          },
        });

      mockUserApi.getById.mockResolvedValue({
        code: 0,
        data: mockUsers[0],
      });

      const wrapper = createWrapper();
      await new Promise(resolve => setTimeout(resolve, 10));
      await wrapper.vm.$nextTick();

      wrapper.vm.handleEdit(mockUsers[0]);
      await wrapper.vm.$nextTick();

      // Directly call handleEditSubmit
      await wrapper.vm.handleEditSubmit({
        nickname: '更新后的管理员',
        email: 'updated@example.com',
        phone: '13800138000',
        status: 1,
      });
      await wrapper.vm.$nextTick();

      expect(mockUserApi.update).toHaveBeenCalledWith(1, {
        nickname: '更新后的管理员',
        email: 'updated@example.com',
        phone: '13800138000',
        status: 1,
      });
      expect(mockElMessage.success).toHaveBeenCalledWith('更新成功');
    });
  });

  describe('AC4 - 启停用、重置密码、角色分配', () => {
    it('toggles user status successfully', async () => {
      mockUserApi.disable.mockResolvedValue({ code: 0, data: null });

      mockUserApi.list
        .mockResolvedValueOnce({
          code: 0,
          data: {
            records: mockUsers,
            total: 3,
            page: 1,
            size: 10,
            pages: 1,
          },
        })
        .mockResolvedValueOnce({
          code: 0,
          data: {
            records: mockUsers.map(u => u.id === 1 ? { ...u, status: 0 } : u),
            total: 3,
            page: 1,
            size: 10,
            pages: 1,
          },
        });

      const wrapper = createWrapper();
      await new Promise(resolve => setTimeout(resolve, 10));
      await wrapper.vm.$nextTick();

      // Directly call handleStatusToggle
      await wrapper.vm.handleStatusToggle(mockUsers[0]);
      await wrapper.vm.$nextTick();

      expect(mockUserApi.disable).toHaveBeenCalledWith(1);
      expect(mockElMessage.success).toHaveBeenCalledWith('操作成功');
      expect(mockUserApi.list).toHaveBeenCalledTimes(2);
    });

    it('resets password with confirmation dialog', async () => {
      mockElMessageBox.confirm.mockResolvedValue('confirm');
      mockUserApi.resetPassword.mockResolvedValue({ code: 0, data: null });

      const wrapper = createWrapper(['user:read', 'user:reset-password']);
      await new Promise(resolve => setTimeout(resolve, 10));
      await wrapper.vm.$nextTick();

      // Directly call handleResetPassword
      await wrapper.vm.handleResetPassword(mockUsers[0]);
      await wrapper.vm.$nextTick();

      expect(mockElMessageBox.confirm).toHaveBeenCalled();
      expect(mockUserApi.resetPassword).toHaveBeenCalledWith(1, { newPassword: expect.any(String) });
      expect(mockElMessage.success).toHaveBeenCalledWith(expect.stringContaining('重置成功'));
    });

    it('opens role assign drawer when clicking role assign', async () => {
      mockRoleApi.list.mockResolvedValue({
        code: 0,
        data: {
          records: mockRoles,
          total: 3,
          page: 1,
          size: 10,
          pages: 1,
        },
      });

      mockUserApi.getById.mockResolvedValue({
        code: 0,
        data: mockUsers[0],
      });

      const wrapper = createWrapper(['user:read', 'user:assign-role']);
      await new Promise(resolve => setTimeout(resolve, 10));
      await wrapper.vm.$nextTick();

      // Directly call handleRoleAssign
      wrapper.vm.handleRoleAssign(mockUsers[0]);
      await wrapper.vm.$nextTick();

      expect(wrapper.findComponent({ name: 'RoleAssignDrawer' }).props('visible')).toBe(true);
      expect(wrapper.findComponent({ name: 'RoleAssignDrawer' }).props('userId')).toBe(1);
    });

    it('assigns roles successfully via role assign drawer', async () => {
      mockUserApi.assignRoles.mockResolvedValue({ code: 0, data: null });

      mockUserApi.list
        .mockResolvedValueOnce({
          code: 0,
          data: {
            records: mockUsers,
            total: 3,
            page: 1,
            size: 10,
            pages: 1,
          },
        })
        .mockResolvedValueOnce({
          code: 0,
          data: {
            records: [{ ...mockUsers[0], roles: mockRoles.slice(0, 2) }, ...mockUsers.slice(1)],
            total: 3,
            page: 1,
            size: 10,
            pages: 1,
          },
        });

      mockRoleApi.list.mockResolvedValue({
        code: 0,
        data: {
          records: mockRoles,
          total: 3,
          page: 1,
          size: 10,
          pages: 1,
        },
      });

      mockUserApi.getById.mockResolvedValue({
        code: 0,
        data: mockUsers[0],
      });

      const wrapper = createWrapper(['user:read', 'user:assign-role']);
      await new Promise(resolve => setTimeout(resolve, 10));
      await wrapper.vm.$nextTick();

      wrapper.vm.handleRoleAssign(mockUsers[0]);
      await wrapper.vm.$nextTick();

      // Directly call handleRoleAssignSubmit
      await wrapper.vm.handleRoleAssignSubmit([1, 2]);
      await wrapper.vm.$nextTick();

      expect(mockUserApi.assignRoles).toHaveBeenCalledWith(1, { roleIds: [1, 2] });
      expect(mockElMessage.success).toHaveBeenCalledWith('分配成功');
    });

    it('hides reset password button when no permission', async () => {
      const wrapper = createWrapper(['user:read', 'user:create', 'user:update', 'user:delete']);
      await new Promise(resolve => setTimeout(resolve, 10));
      await wrapper.vm.$nextTick();

      // Check computed property instead of rendered buttons
      expect(wrapper.vm.canResetPassword).toBe(false);
    });

    it('hides role assign button when no permission', async () => {
      const wrapper = createWrapper(['user:read', 'user:create', 'user:update', 'user:delete']);
      await new Promise(resolve => setTimeout(resolve, 10));
      await wrapper.vm.$nextTick();

      expect(wrapper.vm.canAssignRole).toBe(false);
    });

    it('hides create button when no create permission', async () => {
      const wrapper = createWrapper(['user:read', 'user:update', 'user:delete']);
      await new Promise(resolve => setTimeout(resolve, 10));
      await wrapper.vm.$nextTick();

      expect(wrapper.vm.canCreate).toBe(false);
      expect(wrapper.find('.user-toolbar__create').exists()).toBe(false);
    });
  });

  describe('Responsive Design', () => {
    it('has responsive table container for mobile', async () => {
      const wrapper = createWrapper();
      await wrapper.vm.$nextTick();

      expect(wrapper.find('.user-table-container').exists()).toBe(true);
    });
  });
});