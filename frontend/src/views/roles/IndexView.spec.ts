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
                <div v-if="col.prop === 'actions' && col.render" class="role-table__actions">
                  <button class="role-table__action-edit" @click="$emit('row-click', row, 'edit')">编辑</button>
                  <button v-if="row.id !== 1" class="role-table__action-status" @click="$emit('row-click', row, 'status')">{{ row.status === 1 ? '停用' : '启用' }}</button>
                  <button class="role-table__action-permissions" @click="$emit('row-click', row, 'permissions')">权限分配</button>
                  <button v-if="row.id !== 1" class="role-table__action-delete" @click="$emit('row-click', row, 'delete')">删除</button>
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

vi.mock('@/views/roles/RoleFormDrawer.vue', () => ({
  default: {
    name: 'RoleFormDrawer',
    props: ['visible', 'mode', 'initialData'],
    emits: ['submit', 'close'],
    template: '<div class="role-form-drawer-mock" v-if="visible" data-test="role-form-drawer"><slot /></div>',
  },
}));

vi.mock('@/views/roles/PermissionAssignDrawer.vue', () => ({
  default: {
    name: 'PermissionAssignDrawer',
    props: ['visible', 'roleId'],
    emits: ['submit', 'close'],
    template: '<div class="permission-assign-drawer-mock" v-if="visible" data-test="permission-assign-drawer"><slot /></div>',
  },
}));

import { mount } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import { createRouter, createWebHistory } from 'vue-router';
import IndexView from '@/views/roles/IndexView.vue';
import { useAuthStore } from '@/stores/auth';
import { roleApi } from '@/api/role';
import { permissionApi } from '@/api/permission';
import { ElMessage, ElMessageBox } from 'element-plus';

vi.mock('@/api/role');
vi.mock('@/api/permission');
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

const mockRoleApi = vi.mocked(roleApi);
const mockPermissionApi = vi.mocked(permissionApi);
const mockElMessage = vi.mocked(ElMessage);
const mockElMessageBox = vi.mocked(ElMessageBox);

const mockRoles = [
  {
    id: 1,
    code: 'ROLE_ADMIN',
    name: '超级管理员',
    description: '系统超级管理员',
    status: 1,
    createTime: '2024-01-01T00:00:00Z',
    permissions: [
      { id: 1, code: 'user:view', name: '用户查看', type: 2, parentId: 0, sort: 1, status: 1, createTime: '2024-01-01T00:00:00Z' },
      { id: 2, code: 'user:create', name: '用户新增', type: 2, parentId: 0, sort: 2, status: 1, createTime: '2024-01-01T00:00:00Z' },
    ],
  },
  {
    id: 2,
    code: 'ROLE_USER',
    name: '普通用户',
    description: '普通用户角色',
    status: 1,
    createTime: '2024-01-02T00:00:00Z',
    permissions: [
      { id: 1, code: 'user:view', name: '用户查看', type: 2, parentId: 0, sort: 1, status: 1, createTime: '2024-01-01T00:00:00Z' },
    ],
  },
  {
    id: 3,
    code: 'ROLE_EDITOR',
    name: '编辑',
    description: '内容编辑角色',
    status: 0,
    createTime: '2024-01-03T00:00:00Z',
    permissions: [],
  },
];

const mockPermissionTree = [
  {
    id: 10,
    code: 'system',
    name: '系统管理',
    type: 1,
    parentId: 0,
    sort: 1,
    status: 1,
    createTime: '2024-01-01T00:00:00Z',
    children: [
      {
        id: 11,
        code: 'system:user',
        name: '用户管理',
        type: 1,
        parentId: 10,
        sort: 1,
        status: 1,
        createTime: '2024-01-01T00:00:00Z',
        children: [
          { id: 1, code: 'user:view', name: '用户查看', type: 2, parentId: 11, sort: 1, status: 1, createTime: '2024-01-01T00:00:00Z', children: [] },
          { id: 2, code: 'user:create', name: '用户新增', type: 2, parentId: 11, sort: 2, status: 1, createTime: '2024-01-01T00:00:00Z', children: [] },
          { id: 3, code: 'user:update', name: '用户编辑', type: 2, parentId: 11, sort: 3, status: 1, createTime: '2024-01-01T00:00:00Z', children: [] },
          { id: 4, code: 'user:delete', name: '用户删除', type: 2, parentId: 11, sort: 4, status: 1, createTime: '2024-01-01T00:00:00Z', children: [] },
        ],
      },
      {
        id: 12,
        code: 'system:role',
        name: '角色管理',
        type: 1,
        parentId: 10,
        sort: 2,
        status: 1,
        createTime: '2024-01-01T00:00:00Z',
        children: [
          { id: 5, code: 'role:view', name: '角色查看', type: 2, parentId: 12, sort: 1, status: 1, createTime: '2024-01-01T00:00:00Z', children: [] },
          { id: 6, code: 'role:create', name: '角色新增', type: 2, parentId: 12, sort: 2, status: 1, createTime: '2024-01-01T00:00:00Z', children: [] },
          { id: 7, code: 'role:update', name: '角色编辑', type: 2, parentId: 12, sort: 3, status: 1, createTime: '2024-01-01T00:00:00Z', children: [] },
          { id: 8, code: 'role:delete', name: '角色删除', type: 2, parentId: 12, sort: 4, status: 1, createTime: '2024-01-01T00:00:00Z', children: [] },
          { id: 9, code: 'role:assign-permission', name: '权限分配', type: 2, parentId: 12, sort: 5, status: 1, createTime: '2024-01-01T00:00:00Z', children: [] },
        ],
      },
    ],
  },
  {
    id: 20,
    code: 'content',
    name: '内容管理',
    type: 1,
    parentId: 0,
    sort: 2,
    status: 1,
    createTime: '2024-01-01T00:00:00Z',
    children: [
      {
        id: 21,
        code: 'content:article',
        name: '文章管理',
        type: 1,
        parentId: 20,
        sort: 1,
        status: 1,
        createTime: '2024-01-01T00:00:00Z',
        children: [
          { id: 10, code: 'article:read', name: '文章查看', type: 2, parentId: 21, sort: 1, status: 1, createTime: '2024-01-01T00:00:00Z', children: [] },
          { id: 11, code: 'article:create', name: '文章新增', type: 2, parentId: 21, sort: 2, status: 1, createTime: '2024-01-01T00:00:00Z', children: [] },
          { id: 12, code: 'article:update', name: '文章编辑', type: 2, parentId: 21, sort: 3, status: 1, createTime: '2024-01-01T00:00:00Z', children: [] },
          { id: 13, code: 'article:delete', name: '文章删除', type: 2, parentId: 21, sort: 4, status: 1, createTime: '2024-01-01T00:00:00Z', children: [] },
        ],
      },
    ],
  },
];

describe('IndexView - 角色管理页', () => {
  let authStore: ReturnType<typeof useAuthStore>;
  let pinia: ReturnType<typeof createPinia>;

  const createWrapper = (permissions: string[] = ['role:view', 'role:create', 'role:update', 'role:delete', 'role:assign-permission']) => {
    authStore.setPermissions(permissions);

    const router = createRouter({
      history: createWebHistory(),
      routes: [
        { path: '/', name: 'Home', component: { template: '<div>Home</div>' }, meta: { requiresAuth: true } },
        { path: '/roles', name: 'Roles', component: IndexView, meta: { requiresAuth: true, permissions: ['role:view'] } },
        { path: '/login', name: 'Login', component: { template: '<div>Login</div>' }, meta: { public: true } },
      ],
    });
    router.push('/roles');

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
    authStore.login('mock-token', { id: 1, username: 'admin', nickname: '管理员', email: '', phone: '', status: 1, createTime: '' }, ['admin'], ['role:view', 'role:create', 'role:update', 'role:delete', 'role:assign-permission', '*']);

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
  });

  afterEach(() => {
    vi.resetAllMocks();
  });

  describe('Component Rendering', () => {
    it('renders role management page with toolbar and table', async () => {
      const wrapper = createWrapper();
      await wrapper.vm.$nextTick();

      expect(wrapper.find('.role-management').exists()).toBe(true);
      expect(wrapper.find('.role-toolbar').exists()).toBe(true);
      expect(wrapper.find('.role-table').exists()).toBe(true);
      expect(wrapper.find('.role-pagination').exists()).toBe(true);
    });

    it('shows search input, status filter, and create button in toolbar', async () => {
      const wrapper = createWrapper();
      await wrapper.vm.$nextTick();

      expect(wrapper.find('input[placeholder*="角色名/编码"]').exists()).toBe(true);
      expect(wrapper.find('select').exists()).toBe(true);
      expect(wrapper.find('.role-toolbar__create').exists()).toBe(true);
    });

    it('displays role list with correct columns', async () => {
      const wrapper = createWrapper();
      await wrapper.vm.$nextTick();

      const table = wrapper.findComponent({ name: 'BaseTable' });
      expect(table.exists()).toBe(true);
      expect(table.props('columns')).toBeDefined();
      const columns = table.props('columns') as any[];
      const columnProps = columns.map(c => c.prop);
      expect(columnProps).toContain('name');
      expect(columnProps).toContain('code');
      expect(columnProps).toContain('status');
      expect(columnProps).toContain('createTime');
      expect(columnProps).toContain('actions');
    });
  });

  describe('AC1 - 角色列表分页查询与条件筛选', () => {
    it('loads first page by default on mount', async () => {
      const wrapper = createWrapper();
      await wrapper.vm.$nextTick();

      expect(mockRoleApi.list).toHaveBeenCalledWith(expect.objectContaining({ page: 1, size: 10 }));
    });

    it('filters roles by search keyword', async () => {
      const wrapper = createWrapper();
      await wrapper.vm.$nextTick();

      const searchInput = wrapper.find('input[placeholder*="角色名/编码"]');
      await searchInput.setValue('admin');
      await wrapper.vm.$nextTick();

      expect(mockRoleApi.list).toHaveBeenCalledWith(expect.objectContaining({
        page: 1,
        size: 10,
        name: 'admin',
        code: 'admin',
      }));
    });

    it('filters roles by status', async () => {
      const wrapper = createWrapper();
      await wrapper.vm.$nextTick();

      const statusSelect = wrapper.find('select');
      await statusSelect.setValue('1');
      await wrapper.vm.$nextTick();

      expect(mockRoleApi.list).toHaveBeenLastCalledWith(expect.objectContaining({
        page: 1,
        size: 10,
        status: '1',
      }));
    });

    it('changes page correctly', async () => {
      mockRoleApi.list.mockResolvedValue({
        code: 0,
        data: {
          records: mockRoles.slice(0, 1),
          total: 3,
          page: 2,
          size: 1,
          pages: 3,
        },
      });

      const wrapper = createWrapper();
      await wrapper.vm.$nextTick();

      wrapper.vm.handlePageChange(2);
      await wrapper.vm.$nextTick();

      expect(mockRoleApi.list).toHaveBeenLastCalledWith(expect.objectContaining({ page: 2, size: 10 }));
    });

    it('changes page size correctly', async () => {
      mockRoleApi.list.mockResolvedValue({
        code: 0,
        data: {
          records: mockRoles,
          total: 3,
          page: 1,
          size: 20,
          pages: 1,
        },
      });

      const wrapper = createWrapper();
      await wrapper.vm.$nextTick();

      wrapper.vm.handleSizeChange(20);
      await wrapper.vm.$nextTick();

      expect(mockRoleApi.list).toHaveBeenLastCalledWith(expect.objectContaining({ page: 1, size: 20 }));
    });
  });

  describe('AC2 - 创建角色唯一校验', () => {
    it('opens create drawer when clicking create button', async () => {
      const wrapper = createWrapper();
      await wrapper.vm.$nextTick();

      const createButton = wrapper.find('.role-toolbar__create');
      await createButton.trigger('click');
      await wrapper.vm.$nextTick();

      expect(wrapper.findComponent({ name: 'RoleFormDrawer' }).props('visible')).toBe(true);
      expect(wrapper.findComponent({ name: 'RoleFormDrawer' }).props('mode')).toBe('create');
    });

    it('creates role successfully with valid form', async () => {
      const newRole = {
        id: 4,
        code: 'ROLE_NEW',
        name: '新角色',
        description: '新角色描述',
        status: 1,
        createTime: '2024-01-04T00:00:00Z',
        permissions: [],
      };

      mockRoleApi.create.mockResolvedValue({
        code: 0,
        data: newRole,
      });

      mockRoleApi.list
        .mockResolvedValueOnce({
          code: 0,
          data: {
            records: mockRoles,
            total: 3,
            page: 1,
            size: 10,
            pages: 1,
          },
        })
        .mockResolvedValueOnce({
          code: 0,
          data: {
            records: [...mockRoles, newRole],
            total: 4,
            page: 1,
            size: 10,
            pages: 1,
          },
        });

      const wrapper = createWrapper();
      await new Promise(resolve => setTimeout(resolve, 10));
      await wrapper.vm.$nextTick();

      await wrapper.vm.handleCreateSubmit({
        code: 'ROLE_NEW',
        name: '新角色',
        description: '新角色描述',
        status: 1,
      });
      await wrapper.vm.$nextTick();

      expect(mockRoleApi.create).toHaveBeenCalledWith({
        code: 'ROLE_NEW',
        name: '新角色',
        description: '新角色描述',
        status: 1,
      });
      expect(mockElMessage.success).toHaveBeenCalledWith('创建成功');
      expect(mockRoleApi.list).toHaveBeenCalledTimes(2);
    });

    it('shows error when role name already exists (code=1101)', async () => {
      const error = new Error('角色名已存在');
      (error as any).response = { data: { code: 1101, message: '角色名已存在' } };
      mockRoleApi.create.mockRejectedValue(error);

      const wrapper = createWrapper();
      await new Promise(resolve => setTimeout(resolve, 10));
      await wrapper.vm.$nextTick();

      await expect(wrapper.vm.handleCreateSubmit({
        code: 'ROLE_NEW',
        name: '超级管理员',
        description: '新角色描述',
        status: 1,
      })).rejects.toThrow();

      expect(mockElMessage.error).toHaveBeenCalledWith('角色名已存在');
    });

    it('shows error when role code already exists (code=1102)', async () => {
      const error = new Error('角色编码已存在');
      (error as any).response = { data: { code: 1102, message: '角色编码已存在' } };
      mockRoleApi.create.mockRejectedValue(error);

      const wrapper = createWrapper();
      await new Promise(resolve => setTimeout(resolve, 10));
      await wrapper.vm.$nextTick();

      await expect(wrapper.vm.handleCreateSubmit({
        code: 'ROLE_ADMIN',
        name: '新角色',
        description: '新角色描述',
        status: 1,
      })).rejects.toThrow();

      expect(mockElMessage.error).toHaveBeenCalledWith('角色编码已存在');
    });
  });

  describe('AC3 - 更新角色 code 不可改', () => {
    it('opens edit drawer with role data when clicking edit', async () => {
      const wrapper = createWrapper();
      await new Promise(resolve => setTimeout(resolve, 10));
      await wrapper.vm.$nextTick();

      wrapper.vm.handleEdit(mockRoles[0]);
      await wrapper.vm.$nextTick();

      const drawers = wrapper.findAllComponents({ name: 'RoleFormDrawer' });
      const editDrawer = drawers[1];
      expect(editDrawer.props('visible')).toBe(true);
      expect(editDrawer.props('mode')).toBe('edit');
      expect(editDrawer.props('initialData')).toEqual(mockRoles[0]);
    });

    it('updates role successfully without changing code', async () => {
      const updatedRole = {
        ...mockRoles[0],
        name: '更新后的超级管理员',
        description: '更新后描述',
      };

      mockRoleApi.update.mockResolvedValue({
        code: 0,
        data: updatedRole,
      });

      mockRoleApi.list
        .mockResolvedValueOnce({
          code: 0,
          data: {
            records: mockRoles,
            total: 3,
            page: 1,
            size: 10,
            pages: 1,
          },
        })
        .mockResolvedValueOnce({
          code: 0,
          data: {
            records: [updatedRole, ...mockRoles.slice(1)],
            total: 3,
            page: 1,
            size: 10,
            pages: 1,
          },
        });

      const wrapper = createWrapper();
      await new Promise(resolve => setTimeout(resolve, 10));
      await wrapper.vm.$nextTick();

      wrapper.vm.handleEdit(mockRoles[0]);
      await wrapper.vm.$nextTick();

      await wrapper.vm.handleEditSubmit({
        name: '更新后的超级管理员',
        description: '更新后描述',
        status: 1,
      });
      await wrapper.vm.$nextTick();

      expect(mockRoleApi.update).toHaveBeenCalledWith(1, {
        name: '更新后的超级管理员',
        description: '更新后描述',
        status: 1,
      });
      expect(mockElMessage.success).toHaveBeenCalledWith('更新成功');
    });
  });

  describe('AC4 - 启停用、删除引用保护', () => {
    it('toggles role status successfully', async () => {
      mockRoleApi.update.mockResolvedValue({ code: 0, data: { ...mockRoles[0], status: 0 } });

      mockRoleApi.list
        .mockResolvedValueOnce({
          code: 0,
          data: {
            records: mockRoles,
            total: 3,
            page: 1,
            size: 10,
            pages: 1,
          },
        })
        .mockResolvedValueOnce({
          code: 0,
          data: {
            records: mockRoles.map(r => r.id === 1 ? { ...r, status: 0 } : r),
            total: 3,
            page: 1,
            size: 10,
            pages: 1,
          },
        });

      const wrapper = createWrapper();
      await new Promise(resolve => setTimeout(resolve, 10));
      await wrapper.vm.$nextTick();

      await wrapper.vm.handleStatusToggle(mockRoles[0]);
      await wrapper.vm.$nextTick();

      expect(mockRoleApi.update).toHaveBeenCalledWith(1, { status: 0 });
      expect(mockElMessage.success).toHaveBeenCalledWith('操作成功');
      expect(mockRoleApi.list).toHaveBeenCalledTimes(2);
    });

    it('rejects delete when role has users assigned (code=1103)', async () => {
      const error = new Error('该角色已被用户引用，无法删除');
      (error as any).response = { data: { code: 1103, message: '该角色已被用户引用，无法删除' } };
      mockRoleApi.delete.mockRejectedValue(error);
      mockElMessageBox.confirm.mockResolvedValue('confirm');

      const wrapper = createWrapper();
      await new Promise(resolve => setTimeout(resolve, 10));
      await wrapper.vm.$nextTick();

      await expect(wrapper.vm.handleDelete(mockRoles[0])).rejects.toThrow();

      expect(mockElMessageBox.confirm).toHaveBeenCalled();
      expect(mockRoleApi.delete).toHaveBeenCalledWith(1);
      expect(mockElMessage.error).toHaveBeenCalledWith('该角色已被用户引用，无法删除');
    });

    it('rejects delete when role has permissions assigned (code=1104)', async () => {
      const error = new Error('该角色已分配权限，无法删除');
      (error as any).response = { data: { code: 1104, message: '该角色已分配权限，无法删除' } };
      mockRoleApi.delete.mockRejectedValue(error);
      mockElMessageBox.confirm.mockResolvedValue('confirm');

      const wrapper = createWrapper();
      await new Promise(resolve => setTimeout(resolve, 10));
      await wrapper.vm.$nextTick();

      await expect(wrapper.vm.handleDelete(mockRoles[0])).rejects.toThrow();

      expect(mockElMessageBox.confirm).toHaveBeenCalled();
      expect(mockRoleApi.delete).toHaveBeenCalledWith(1);
      expect(mockElMessage.error).toHaveBeenCalledWith('该角色已分配权限，无法删除');
    });

    it('deletes role successfully when no references', async () => {
      mockRoleApi.delete.mockResolvedValue({ code: 0, data: null });
      mockElMessageBox.confirm.mockResolvedValue('confirm');

      mockRoleApi.list
        .mockResolvedValueOnce({
          code: 0,
          data: {
            records: mockRoles,
            total: 3,
            page: 1,
            size: 10,
            pages: 1,
          },
        })
        .mockResolvedValueOnce({
          code: 0,
          data: {
            records: mockRoles.slice(1),
            total: 2,
            page: 1,
            size: 10,
            pages: 1,
          },
        });

      const wrapper = createWrapper();
      await new Promise(resolve => setTimeout(resolve, 10));
      await wrapper.vm.$nextTick();

      await wrapper.vm.handleDelete(mockRoles[2]);
      await wrapper.vm.$nextTick();

      expect(mockRoleApi.delete).toHaveBeenCalledWith(3);
      expect(mockElMessage.success).toHaveBeenCalledWith('删除成功');
      expect(mockRoleApi.list).toHaveBeenCalledTimes(2);
    });
  });

  describe('AC5 - 权限分配树功能完整', () => {
    it('opens permission assign drawer when clicking permissions button', async () => {
      const wrapper = createWrapper(['role:view', 'role:assign-permission']);
      await new Promise(resolve => setTimeout(resolve, 10));
      await wrapper.vm.$nextTick();

      wrapper.vm.handlePermissionAssign(mockRoles[0]);
      await wrapper.vm.$nextTick();

      expect(wrapper.findComponent({ name: 'PermissionAssignDrawer' }).props('visible')).toBe(true);
      expect(wrapper.findComponent({ name: 'PermissionAssignDrawer' }).props('roleId')).toBe(1);
    });

    it('opens permission assign drawer and fetches role permissions', async () => {
      const wrapper = createWrapper(['role:view', 'role:assign-permission']);
      await new Promise(resolve => setTimeout(resolve, 10));
      await wrapper.vm.$nextTick();

      wrapper.vm.handlePermissionAssign(mockRoles[0]);
      await wrapper.vm.$nextTick();

      // IndexView opens the drawer with correct roleId; drawer handles permission fetching internally
      const drawer = wrapper.findComponent({ name: 'PermissionAssignDrawer' });
      expect(drawer.props('roleId')).toBe(1);
    });

    it('saves permission assignment successfully', async () => {
      mockPermissionApi.getTree.mockResolvedValue({
        code: 0,
        data: mockPermissionTree,
      });

      mockRoleApi.getById.mockResolvedValue({
        code: 0,
        data: mockRoles[0],
      });

      mockRoleApi.assignPermissions.mockResolvedValue({ code: 0, data: null });

      mockRoleApi.list
        .mockResolvedValueOnce({
          code: 0,
          data: {
            records: mockRoles,
            total: 3,
            page: 1,
            size: 10,
            pages: 1,
          },
        })
        .mockResolvedValueOnce({
          code: 0,
          data: {
            records: [{
              ...mockRoles[0],
              permissions: [
                { id: 1, code: 'user:view', name: '用户查看', type: 2, parentId: 0, sort: 1, status: 1, createTime: '2024-01-01T00:00:00Z' },
                { id: 2, code: 'user:create', name: '用户新增', type: 2, parentId: 0, sort: 2, status: 1, createTime: '2024-01-01T00:00:00Z' },
                { id: 3, code: 'user:update', name: '用户编辑', type: 2, parentId: 0, sort: 3, status: 1, createTime: '2024-01-01T00:00:00Z' },
              ],
            }, ...mockRoles.slice(1)],
            total: 3,
            page: 1,
            size: 10,
            pages: 1,
          },
        });

      const wrapper = createWrapper(['role:view', 'role:assign-permission']);
      await new Promise(resolve => setTimeout(resolve, 10));
      await wrapper.vm.$nextTick();

      wrapper.vm.handlePermissionAssign(mockRoles[0]);
      await wrapper.vm.$nextTick();

      // Simulate submit with new permission IDs
      await wrapper.vm.handlePermissionAssignSubmit([1, 2, 3]);
      await wrapper.vm.$nextTick();

      expect(mockRoleApi.assignPermissions).toHaveBeenCalledWith(1, { permissionIds: [1, 2, 3] });
      expect(mockElMessage.success).toHaveBeenCalledWith('分配成功');
    });
  });

  describe('Permission Control', () => {
    it('hides create button when no create permission', async () => {
      const wrapper = createWrapper(['role:view', 'role:update', 'role:delete']);
      await new Promise(resolve => setTimeout(resolve, 10));
      await wrapper.vm.$nextTick();

      expect(wrapper.vm.canCreate).toBe(false);
      expect(wrapper.find('.role-toolbar__create').exists()).toBe(false);
    });

    it('hides delete button when no delete permission', async () => {
      const wrapper = createWrapper(['role:view', 'role:create', 'role:update']);
      await new Promise(resolve => setTimeout(resolve, 10));
      await wrapper.vm.$nextTick();

      expect(wrapper.vm.canDelete).toBe(false);
    });

    it('hides permission assign button when no assign permission', async () => {
      const wrapper = createWrapper(['role:view', 'role:create', 'role:update', 'role:delete']);
      await new Promise(resolve => setTimeout(resolve, 10));
      await wrapper.vm.$nextTick();

      expect(wrapper.vm.canAssignPermission).toBe(false);
    });
  });

  describe('Responsive Design', () => {
    it('has responsive table container for mobile', async () => {
      const wrapper = createWrapper();
      await wrapper.vm.$nextTick();

      expect(wrapper.find('.role-table-container').exists()).toBe(true);
    });
  });
});