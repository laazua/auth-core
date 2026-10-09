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
    template:
      '<button class="base-button-mock" @click="$emit(\'click\', $event)" :disabled="disabled || loading"><slot /></button>',
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

vi.mock('@/components/BaseTable.vue', () => ({
  default: {
    name: 'BaseTable',
    props: ['data', 'columns', 'loading', 'rowKey', 'emptyText'],
    emits: [
      'sort-change',
      'selection-change',
      'row-click',
      'row-dblclick',
      'header-click',
      'current-change',
      'size-change',
    ],
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
                <div v-if="col.prop === 'actions' && col.render" class="actions-mock">
                  <button class="action-edit" @click="$emit('row-click', row, 'edit')">编辑</button>
                  <button v-if="col.prop === 'actions' && col.label === '操作'" class="action-delete" @click="$emit('row-click', row, 'delete')">删除</button>
                  <button v-if="col.prop === 'actions' && col.label === '操作'" class="action-status" @click="$emit('row-click', row, 'status')">{{ row.status === 1 ? '停用' : '启用' }}</button>
                  <button v-if="col.prop === 'actions' && col.label === '操作'" class="action-permissions" @click="$emit('row-click', row, 'permissions')">查看权限</button>
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
    template:
      '<div class="base-card-mock"><div v-if="title || $slots.header" class="base-card-mock__header"><slot name="header"><div><h3 v-if="title">{{ title }}</h3><p v-if="subtitle">{{ subtitle }}</p></div></slot></div><div class="base-card-mock__body"><slot /></div><div v-if="$slots.footer" class="base-card-mock__footer"><slot name="footer" /></div></div>',
  },
}));

vi.mock('@/components/ElTabs.vue', () => ({
  default: {
    name: 'ElTabs',
    props: ['modelValue', 'type'],
    emits: ['update:modelValue', 'tab-click'],
    template:
      '<div class="el-tabs-mock"><div class="el-tabs__nav"><button v-for="tab in $slots.default()" :key="tab.props?.name" class="el-tabs__item" :class="{ \'is-active\': tab.props?.name === modelValue }" @click="$emit(\'update:modelValue\', tab.props?.name)">{{ tab.props?.label }}</button></div><div class="el-tabs__content"><slot /></div></div>',
  },
}));

vi.mock('@/components/ElTabPane.vue', () => ({
  default: {
    name: 'ElTabPane',
    props: ['label', 'name', 'disabled'],
    template:
      '<div class="el-tab-pane-mock" v-show="$attrs.name === $parent.modelValue"><slot /></div>',
  },
}));

vi.mock('@/views/system/PermissionFormDrawer.vue', () => ({
  default: {
    name: 'PermissionFormDrawer',
    props: ['visible', 'mode', 'initialData', 'moduleOptions'],
    emits: ['submit', 'close'],
    template:
      '<div class="permission-form-drawer-mock" v-if="visible" data-test="permission-form-drawer"><slot /></div>',
  },
}));

vi.mock('@/views/system/ModuleFormDrawer.vue', () => ({
  default: {
    name: 'ModuleFormDrawer',
    props: ['visible', 'mode', 'initialData'],
    emits: ['submit', 'close'],
    template:
      '<div class="module-form-drawer-mock" v-if="visible" data-test="module-form-drawer"><slot /></div>',
  },
}));

import { mount } from '@vue/test-utils';
import { createPinia, setActivePinia } from 'pinia';
import { createRouter, createWebHistory } from 'vue-router';
// 覆盖 setup.ts 的 vue-router 全局桩：本 spec 各用例均装真实 router 插件，
// 组件须拿到真件使 watch(route.path) 分支与 router.push 生效（R1 根因判定）
vi.mock(
  'vue-router',
  async (importOriginal) => (await importOriginal()) as typeof import('vue-router')
);
import IndexView from '@/views/system/IndexView.vue';
import { useAuthStore } from '@/stores/auth';
import { permissionApi } from '@/api/permission';
import { moduleApi } from '@/api/module';
import { ElMessage, ElMessageBox } from 'element-plus';

vi.mock('@/api/permission');
vi.mock('@/api/module');
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
    ElPagination: {
      name: 'ElPagination',
      props: ['currentPage', 'pageSize', 'pageSizes', 'total', 'layout'],
      emits: ['update:currentPage', 'update:pageSize', 'current-change', 'page-size-change'],
      template: '<div class="el-pagination-mock" data-test="pagination">Pagination</div>',
    },
    ElTabs: {
      name: 'ElTabs',
      props: ['modelValue', 'type'],
      emits: ['update:modelValue', 'tab-click'],
      template:
        '<div class="el-tabs-mock"><div class="el-tabs__nav"><button v-for="tab in $slots.default()" :key="tab.props?.name" class="el-tabs__item" :class="{ \'is-active\': tab.props?.name === modelValue }" @click="$emit(\'update:modelValue\', tab.props?.name)">{{ tab.props?.label }}</button></div><div class="el-tabs__content"><slot /></div></div>',
    },
    ElTabPane: {
      name: 'ElTabPane',
      props: ['label', 'name', 'disabled'],
      template:
        '<div class="el-tab-pane-mock" v-show="$attrs.name === $parent.modelValue"><slot /></div>',
    },
  };
});

const mockPermissionApi = vi.mocked(permissionApi);
const mockModuleApi = vi.mocked(moduleApi);
const mockElMessage = vi.mocked(ElMessage);
const mockElMessageBox = vi.mocked(ElMessageBox);

const mockPermissions = [
  {
    id: 1,
    code: 'perm:user:view',
    name: '用户查看',
    moduleId: 1,
    moduleName: '用户管理',
    description: '查看用户列表',
    status: 1,
    createTime: '2024-01-01T00:00:00Z',
  },
  {
    id: 2,
    code: 'perm:user:create',
    name: '用户新增',
    moduleId: 1,
    moduleName: '用户管理',
    description: '创建新用户',
    status: 1,
    createTime: '2024-01-01T00:00:00Z',
  },
  {
    id: 3,
    code: 'perm:role:view',
    name: '角色查看',
    moduleId: 2,
    moduleName: '角色管理',
    description: '查看角色列表',
    status: 0,
    createTime: '2024-01-02T00:00:00Z',
  },
];

const mockModules = [
  {
    id: 1,
    code: 'USER_MGMT',
    name: '用户管理',
    baseUrl: 'http://user-service',
    description: '用户管理模块',
    status: 1,
    createTime: '2024-01-01T00:00:00Z',
  },
  {
    id: 2,
    code: 'ROLE_MGMT',
    name: '角色管理',
    baseUrl: 'http://role-service',
    description: '角色管理模块',
    status: 1,
    createTime: '2024-01-01T00:00:00Z',
  },
  {
    id: 3,
    code: 'CONTENT_MGMT',
    name: '内容管理',
    baseUrl: 'http://content-service',
    description: '内容管理模块',
    status: 0,
    createTime: '2024-01-02T00:00:00Z',
  },
];

const mockModuleOptions = [
  { id: 1, code: 'USER_MGMT', name: '用户管理' },
  { id: 2, code: 'ROLE_MGMT', name: '角色管理' },
  { id: 3, code: 'CONTENT_MGMT', name: '内容管理' },
];

describe('IndexView - 权限/模块管理页', () => {
  let authStore: ReturnType<typeof useAuthStore>;
  let pinia: ReturnType<typeof createPinia>;

  const createWrapper = async (
    permissions: string[] = [
      'perm:view',
      'perm:create',
      'perm:update',
      'perm:delete',
      'module:view',
      'module:create',
      'module:update',
      'module:delete',
      'module:toggle-status',
    ],
    initialRoute = '/system/permissions'
  ) => {
    authStore.setPermissions(permissions);

    const router = createRouter({
      history: createWebHistory(),
      routes: [
        {
          path: '/',
          name: 'Home',
          component: { template: '<div>Home</div>' },
          meta: { requiresAuth: true },
        },
        {
          path: '/system/permissions',
          name: 'SystemPermissions',
          component: IndexView,
          meta: { requiresAuth: true, permissions: ['perm:view'] },
        },
        {
          path: '/system/modules',
          name: 'SystemModules',
          component: IndexView,
          meta: { requiresAuth: true, permissions: ['module:view'] },
        },
        {
          path: '/login',
          name: 'Login',
          component: { template: '<div>Login</div>' },
          meta: { public: true },
        },
      ],
    });
    router.push(initialRoute);
    // 实证：push 早于 install 且不 await 时 currentRoute 永停在 START（path='/' 20ms 不动），
    // watch(route.path,{immediate}) 分支不进 → 先 await isReady 再 mount 使路由在 setup 期即落定
    await router.isReady();

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
    authStore.login(
      'mock-token',
      {
        id: 1,
        username: 'admin',
        nickname: '管理员',
        email: '',
        phone: '',
        status: 1,
        createTime: '',
      },
      ['admin'],
      [
        'perm:view',
        'perm:create',
        'perm:update',
        'perm:delete',
        'module:view',
        'module:create',
        'module:update',
        'module:delete',
        'module:toggle-status',
        '*',
      ]
    );

    mockPermissionApi.list.mockResolvedValue({
      code: 0,
      message: 'success',
      timestamp: Date.now(),
      data: {
        records: mockPermissions,
        total: 3,
        size: 10,
        current: 1,
        pages: 1,
      },
    });

    mockModuleApi.list.mockResolvedValue({
      code: 0,
      message: 'success',
      timestamp: Date.now(),
      data: {
        records: mockModules,
        total: 3,
        size: 10,
        current: 1,
        pages: 1,
      },
    });

    mockModuleApi.getAllPermissions.mockResolvedValue({
      code: 0,
      message: 'success',
      timestamp: Date.now(),
      data: mockModuleOptions,
    });
  });

  afterEach(() => {
    vi.resetAllMocks();
  });

  describe('Component Rendering', () => {
    it('renders permission management tab by default at /system/permissions', async () => {
      const wrapper = await createWrapper();
      await wrapper.vm.$nextTick();

      expect(wrapper.find('.system-index').exists()).toBe(true);
      expect(wrapper.find('.permission-tab').exists()).toBe(true);
    });

    it('renders module management tab at /system/modules', async () => {
      const wrapper = await createWrapper([], '/system/modules');
      await wrapper.vm.$nextTick();

      expect(wrapper.find('.system-index').exists()).toBe(true);
      expect(wrapper.find('.module-tab').exists()).toBe(true);
    });

    it('shows tabs for permission and module management', async () => {
      const wrapper = await createWrapper();
      await wrapper.vm.$nextTick();

      expect(wrapper.find('.el-tabs-mock').exists()).toBe(true);
      expect(wrapper.text()).toContain('权限管理');
      expect(wrapper.text()).toContain('模块管理');
    });
  });

  describe('AC1 - 权限列表分页查询与多条件筛选', () => {
    it('loads first page by default on mount', async () => {
      const wrapper = await createWrapper();
      await wrapper.vm.$nextTick();

      expect(mockPermissionApi.list).toHaveBeenCalledWith(
        expect.objectContaining({ page: 1, size: 10 })
      );
    });

    it('filters permissions by search keyword', async () => {
      const wrapper = await createWrapper();
      await wrapper.vm.$nextTick();

      const searchInput = wrapper.find('input[placeholder*="权限名/编码"]');
      await searchInput.setValue('用户');
      await wrapper.vm.$nextTick();

      expect(mockPermissionApi.list).toHaveBeenCalledWith(
        expect.objectContaining({
          page: 1,
          size: 10,
          name: '用户',
          code: '用户',
        })
      );
    });

    it('filters permissions by module', async () => {
      const wrapper = await createWrapper();
      await wrapper.vm.$nextTick();

      // moduleOptions 由 onMounted 异步装载（实证跨一个宏任务才就绪），先等就绪再交互；
      // 两面板同在 DOM（v-show），按 .permission-tab 限定防选中模块面 select
      await new Promise((resolve) => setTimeout(resolve, 20));
      await wrapper.vm.$nextTick();

      const moduleSelect = wrapper.find('.permission-tab select');
      await moduleSelect.setValue('1');
      await wrapper.vm.$nextTick();

      expect(mockPermissionApi.list).toHaveBeenLastCalledWith(
        expect.objectContaining({
          page: 1,
          size: 10,
          // 口径同步：handlePermissionSearch 经 Number() 转换（IndexView.vue:259），实参为 number
          moduleId: 1,
        })
      );
    });

    it('changes page correctly', async () => {
      mockPermissionApi.list.mockResolvedValue({
        code: 0,
        message: 'success',
        timestamp: Date.now(),
        data: {
          records: mockPermissions.slice(0, 1),
          total: 3,
          size: 1,
          current: 2,
          pages: 3,
        },
      });

      const wrapper = await createWrapper();
      await wrapper.vm.$nextTick();

      (wrapper.vm as any).handlePermissionPageChange(2);
      await wrapper.vm.$nextTick();

      expect(mockPermissionApi.list).toHaveBeenLastCalledWith(
        expect.objectContaining({ page: 2, size: 10 })
      );
    });

    it('changes page size correctly', async () => {
      mockPermissionApi.list.mockResolvedValue({
        code: 0,
        message: 'success',
        timestamp: Date.now(),
        data: {
          records: mockPermissions,
          total: 3,
          size: 20,
          current: 1,
          pages: 1,
        },
      });

      const wrapper = await createWrapper();
      await wrapper.vm.$nextTick();

      (wrapper.vm as any).handlePermissionSizeChange(20);
      await wrapper.vm.$nextTick();

      expect(mockPermissionApi.list).toHaveBeenLastCalledWith(
        expect.objectContaining({ page: 1, size: 20 })
      );
    });
  });

  describe('AC2 - 权限创建唯一校验 + 模块必选', () => {
    it('opens create drawer when clicking create button', async () => {
      const wrapper = await createWrapper();
      await wrapper.vm.$nextTick();

      const createButton = wrapper.find('.permission-toolbar__create');
      await createButton.trigger('click');
      await wrapper.vm.$nextTick();

      // 模板内 edit/create 两实例同名（edit 在前），须按 mode 定位 create 实例
      const createDrawer = wrapper
        .findAllComponents({ name: 'PermissionFormDrawer' })
        .find((c) => c.props('mode') === 'create');
      expect(createDrawer, '应存在 create 模式抽屉实例').toBeDefined();
      expect(createDrawer!.props('visible')).toBe(true);
    });

    it('creates permission successfully with valid form', async () => {
      const newPermission = {
        id: 4,
        code: 'perm:new',
        name: '新权限',
        moduleId: 1,
        moduleName: '用户管理',
        description: '新权限描述',
        status: 1,
        createTime: '2024-01-04T00:00:00Z',
      };

      mockPermissionApi.create.mockResolvedValue({
        code: 0,
        message: 'success',
        timestamp: Date.now(),
        data: newPermission,
      });

      mockPermissionApi.list
        .mockResolvedValueOnce({
          code: 0,
          message: 'success',
          timestamp: Date.now(),
          data: {
            records: mockPermissions,
            total: 3,
            size: 10,
            current: 1,
            pages: 1,
          },
        })
        .mockResolvedValueOnce({
          code: 0,
          message: 'success',
          timestamp: Date.now(),
          data: {
            records: [...mockPermissions, newPermission],
            total: 4,
            size: 10,
            current: 1,
            pages: 1,
          },
        });

      const wrapper = await createWrapper();
      await new Promise((resolve) => setTimeout(resolve, 10));
      await wrapper.vm.$nextTick();

      await (wrapper.vm as any).handlePermissionCreateSubmit({
        code: 'perm:new',
        name: '新权限',
        moduleId: 1,
        description: '新权限描述',
      });
      await wrapper.vm.$nextTick();

      expect(mockPermissionApi.create).toHaveBeenCalledWith({
        code: 'perm:new',
        name: '新权限',
        moduleId: 1,
        description: '新权限描述',
      });
      expect(mockElMessage.success).toHaveBeenCalledWith('创建成功');
      expect(mockPermissionApi.list).toHaveBeenCalledTimes(2);
    });

    it('shows error when permission name already exists (code=1201)', async () => {
      const error = new Error('权限名已存在');
      (error as any).response = { data: { code: 1201, message: '权限名已存在' } };
      mockPermissionApi.create.mockRejectedValue(error);

      const wrapper = await createWrapper();
      await new Promise((resolve) => setTimeout(resolve, 10));
      await wrapper.vm.$nextTick();

      await expect(
        (wrapper.vm as any).handlePermissionCreateSubmit({
          code: 'perm:new',
          name: '用户查看',
          moduleId: 1,
          description: '新权限描述',
        })
      ).rejects.toThrow();

      expect(mockElMessage.error).toHaveBeenCalledWith('权限名已存在');
    });

    it('shows error when permission code already exists (code=1202)', async () => {
      const error = new Error('权限编码已存在');
      (error as any).response = { data: { code: 1202, message: '权限编码已存在' } };
      mockPermissionApi.create.mockRejectedValue(error);

      const wrapper = await createWrapper();
      await new Promise((resolve) => setTimeout(resolve, 10));
      await wrapper.vm.$nextTick();

      await expect(
        (wrapper.vm as any).handlePermissionCreateSubmit({
          code: 'perm:user:read',
          name: '新权限',
          moduleId: 1,
          description: '新权限描述',
        })
      ).rejects.toThrow();

      expect(mockElMessage.error).toHaveBeenCalledWith('权限编码已存在');
    });

    it('shows error when module_id is missing', async () => {
      const error = new Error('模块ID不能为空');
      (error as any).response = { data: { code: 400, message: '模块ID不能为空' } };
      mockPermissionApi.create.mockRejectedValue(error);

      const wrapper = await createWrapper();
      await new Promise((resolve) => setTimeout(resolve, 10));
      await wrapper.vm.$nextTick();

      await expect(
        (wrapper.vm as any).handlePermissionCreateSubmit({
          code: 'perm:new',
          name: '新权限',
          moduleId: 0,
          description: '新权限描述',
        })
      ).rejects.toThrow();

      expect(mockElMessage.error).toHaveBeenCalledWith('模块ID不能为空');
    });
  });

  describe('AC3 - 权限更新 code 不可改', () => {
    it('opens edit drawer with permission data when clicking edit', async () => {
      const wrapper = await createWrapper();
      await new Promise((resolve) => setTimeout(resolve, 10));
      await wrapper.vm.$nextTick();

      (wrapper.vm as any).handlePermissionEdit(mockPermissions[0]);
      await wrapper.vm.$nextTick();

      const drawer = wrapper.findComponent({ name: 'PermissionFormDrawer' });
      expect(drawer.props('visible')).toBe(true);
      expect(drawer.props('mode')).toBe('edit');
      expect(drawer.props('initialData')).toEqual(mockPermissions[0]);
    });

    it('updates permission successfully without changing code', async () => {
      const updatedPermission = {
        ...mockPermissions[0],
        name: '更新后的用户查看',
        description: '更新后描述',
        moduleId: 2,
      };

      mockPermissionApi.update.mockResolvedValue({
        code: 0,
        message: 'success',
        timestamp: Date.now(),
        data: updatedPermission,
      });

      mockPermissionApi.list
        .mockResolvedValueOnce({
          code: 0,
          message: 'success',
          timestamp: Date.now(),
          data: {
            records: mockPermissions,
            total: 3,
            size: 10,
            current: 1,
            pages: 1,
          },
        })
        .mockResolvedValueOnce({
          code: 0,
          message: 'success',
          timestamp: Date.now(),
          data: {
            records: [updatedPermission, ...mockPermissions.slice(1)],
            total: 3,
            size: 10,
            current: 1,
            pages: 1,
          },
        });

      const wrapper = await createWrapper();
      await new Promise((resolve) => setTimeout(resolve, 10));
      await wrapper.vm.$nextTick();

      (wrapper.vm as any).handlePermissionEdit(mockPermissions[0]);
      await wrapper.vm.$nextTick();

      await (wrapper.vm as any).handlePermissionEditSubmit({
        name: '更新后的用户查看',
        description: '更新后描述',
        moduleId: 2,
      });
      await wrapper.vm.$nextTick();

      expect(mockPermissionApi.update).toHaveBeenCalledWith(1, {
        name: '更新后的用户查看',
        description: '更新后描述',
        moduleId: 2,
      });
      expect(mockElMessage.success).toHaveBeenCalledWith('更新成功');
    });
  });

  describe('AC4 - 权限删除引用保护', () => {
    it('rejects delete when permission has role references (code=1203)', async () => {
      const error = new Error('该权限已被角色引用，无法删除');
      (error as any).response = { data: { code: 1203, message: '该权限已被角色引用，无法删除' } };
      mockPermissionApi.delete.mockRejectedValue(error);

      const wrapper = await createWrapper();
      await new Promise((resolve) => setTimeout(resolve, 10));
      await wrapper.vm.$nextTick();

      await expect(
        (wrapper.vm as any).handlePermissionDelete(mockPermissions[0])
      ).rejects.toThrow();

      expect(mockElMessageBox.confirm).toHaveBeenCalled();
      expect(mockPermissionApi.delete).toHaveBeenCalledWith(1);
      expect(mockElMessage.error).toHaveBeenCalledWith('该权限已被角色引用，无法删除');
    });

    it('deletes permission successfully when no references', async () => {
      mockPermissionApi.delete.mockResolvedValue({
        code: 0,
        message: 'success',
        timestamp: Date.now(),
        data: undefined,
      });

      mockPermissionApi.list
        .mockResolvedValueOnce({
          code: 0,
          message: 'success',
          timestamp: Date.now(),
          data: {
            records: mockPermissions,
            total: 3,
            size: 10,
            current: 1,
            pages: 1,
          },
        })
        .mockResolvedValueOnce({
          code: 0,
          message: 'success',
          timestamp: Date.now(),
          data: {
            records: mockPermissions.slice(1),
            total: 2,
            size: 10,
            current: 1,
            pages: 1,
          },
        });

      const wrapper = await createWrapper();
      await new Promise((resolve) => setTimeout(resolve, 10));
      await wrapper.vm.$nextTick();

      await (wrapper.vm as any).handlePermissionDelete(mockPermissions[2]);
      await wrapper.vm.$nextTick();

      expect(mockPermissionApi.delete).toHaveBeenCalledWith(3);
      expect(mockElMessage.success).toHaveBeenCalledWith('删除成功');
      expect(mockPermissionApi.list).toHaveBeenCalledTimes(2);
    });
  });

  describe('AC5 - 模块列表分页查询与条件筛选', () => {
    it('loads first page by default on mount at /system/modules', async () => {
      const wrapper = await createWrapper([], '/system/modules');
      await wrapper.vm.$nextTick();

      expect(mockModuleApi.list).toHaveBeenCalledWith(
        expect.objectContaining({ page: 1, size: 10 })
      );
    });

    it('filters modules by search keyword', async () => {
      const wrapper = await createWrapper([], '/system/modules');
      await wrapper.vm.$nextTick();

      const searchInput = wrapper.find('input[placeholder*="模块名/编码"]');
      await searchInput.setValue('用户');
      await wrapper.vm.$nextTick();

      expect(mockModuleApi.list).toHaveBeenCalledWith(
        expect.objectContaining({
          page: 1,
          size: 10,
          name: '用户',
          code: '用户',
        })
      );
    });

    it('filters modules by status', async () => {
      const wrapper = await createWrapper([], '/system/modules');
      await wrapper.vm.$nextTick();

      // 两面板同在 DOM（v-show），按 .module-tab 限定防选中权限面 select
      const statusSelect = wrapper.find('.module-tab select');
      await statusSelect.setValue('1');
      await wrapper.vm.$nextTick();

      expect(mockModuleApi.list).toHaveBeenLastCalledWith(
        expect.objectContaining({
          page: 1,
          size: 10,
          status: '1',
        })
      );
    });

    it('changes page correctly', async () => {
      mockModuleApi.list.mockResolvedValue({
        code: 0,
        message: 'success',
        timestamp: Date.now(),
        data: {
          records: mockModules.slice(0, 1),
          total: 3,
          size: 1,
          current: 2,
          pages: 3,
        },
      });

      const wrapper = await createWrapper([], '/system/modules');
      await wrapper.vm.$nextTick();

      (wrapper.vm as any).handleModulePageChange(2);
      await wrapper.vm.$nextTick();

      expect(mockModuleApi.list).toHaveBeenLastCalledWith(
        expect.objectContaining({ page: 2, size: 10 })
      );
    });

    it('changes page size correctly', async () => {
      mockModuleApi.list.mockResolvedValue({
        code: 0,
        message: 'success',
        timestamp: Date.now(),
        data: {
          records: mockModules,
          total: 3,
          size: 20,
          current: 1,
          pages: 1,
        },
      });

      const wrapper = await createWrapper([], '/system/modules');
      await wrapper.vm.$nextTick();

      (wrapper.vm as any).handleModuleSizeChange(20);
      await wrapper.vm.$nextTick();

      expect(mockModuleApi.list).toHaveBeenLastCalledWith(
        expect.objectContaining({ page: 1, size: 20 })
      );
    });
  });

  describe('AC6 - 模块创建唯一校验', () => {
    it('opens create drawer when clicking create button', async () => {
      // 按钮 v-if=canCreateModule（IndexView.vue:581），须授予 module:view/create 才渲染
      const wrapper = await createWrapper(['module:view', 'module:create'], '/system/modules');
      await wrapper.vm.$nextTick();

      const createButton = wrapper.find('.module-toolbar__create');
      await createButton.trigger('click');
      await wrapper.vm.$nextTick();

      // 模板内 edit/create 两实例同名（edit 在前），须按 mode 定位 create 实例
      const createDrawer = wrapper
        .findAllComponents({ name: 'ModuleFormDrawer' })
        .find((c) => c.props('mode') === 'create');
      expect(createDrawer, '应存在 create 模式抽屉实例').toBeDefined();
      expect(createDrawer!.props('visible')).toBe(true);
    });

    it('creates module successfully with valid form', async () => {
      const newModule = {
        id: 4,
        code: 'NEW_MOD',
        name: '新模块',
        baseUrl: 'http://new-service',
        description: '新模块描述',
        status: 1,
        createTime: '2024-01-04T00:00:00Z',
      };

      mockModuleApi.create.mockResolvedValue({
        code: 0,
        message: 'success',
        timestamp: Date.now(),
        data: newModule,
      });

      mockModuleApi.list
        .mockResolvedValueOnce({
          code: 0,
          message: 'success',
          timestamp: Date.now(),
          data: {
            records: mockModules,
            total: 3,
            size: 10,
            current: 1,
            pages: 1,
          },
        })
        .mockResolvedValueOnce({
          code: 0,
          message: 'success',
          timestamp: Date.now(),
          data: {
            records: [...mockModules, newModule],
            total: 4,
            size: 10,
            current: 1,
            pages: 1,
          },
        });

      const wrapper = await createWrapper([], '/system/modules');
      await new Promise((resolve) => setTimeout(resolve, 10));
      await wrapper.vm.$nextTick();

      await (wrapper.vm as any).handleModuleCreateSubmit({
        code: 'NEW_MOD',
        name: '新模块',
        baseUrl: 'http://new-service',
        description: '新模块描述',
        status: 1,
      });
      await wrapper.vm.$nextTick();

      expect(mockModuleApi.create).toHaveBeenCalledWith({
        code: 'NEW_MOD',
        name: '新模块',
        baseUrl: 'http://new-service',
        description: '新模块描述',
        status: 1,
      });
      expect(mockElMessage.success).toHaveBeenCalledWith('创建成功');
      expect(mockModuleApi.list).toHaveBeenCalledTimes(2);
    });

    it('shows error when module name already exists (code=1301)', async () => {
      const error = new Error('模块名已存在');
      (error as any).response = { data: { code: 1301, message: '模块名已存在' } };
      mockModuleApi.create.mockRejectedValue(error);

      const wrapper = await createWrapper([], '/system/modules');
      await new Promise((resolve) => setTimeout(resolve, 10));
      await wrapper.vm.$nextTick();

      await expect(
        (wrapper.vm as any).handleModuleCreateSubmit({
          code: 'NEW_MOD',
          name: '用户管理',
          baseUrl: 'http://new-service',
          description: '新模块描述',
          status: 1,
        })
      ).rejects.toThrow();

      expect(mockElMessage.error).toHaveBeenCalledWith('模块名已存在');
    });

    it('shows error when module code already exists (code=1302)', async () => {
      const error = new Error('模块编码已存在');
      (error as any).response = { data: { code: 1302, message: '模块编码已存在' } };
      mockModuleApi.create.mockRejectedValue(error);

      const wrapper = await createWrapper([], '/system/modules');
      await new Promise((resolve) => setTimeout(resolve, 10));
      await wrapper.vm.$nextTick();

      await expect(
        (wrapper.vm as any).handleModuleCreateSubmit({
          code: 'USER_MGMT',
          name: '新模块',
          baseUrl: 'http://new-service',
          description: '新模块描述',
          status: 1,
        })
      ).rejects.toThrow();

      expect(mockElMessage.error).toHaveBeenCalledWith('模块编码已存在');
    });
  });

  describe('AC7 - 模块更新 code 不可改 + 级联查询权限', () => {
    it('opens edit drawer with module data when clicking edit', async () => {
      const wrapper = await createWrapper([], '/system/modules');
      await new Promise((resolve) => setTimeout(resolve, 10));
      await wrapper.vm.$nextTick();

      (wrapper.vm as any).handleModuleEdit(mockModules[0]);
      await wrapper.vm.$nextTick();

      const drawer = wrapper.findComponent({ name: 'ModuleFormDrawer' });
      expect(drawer.props('visible')).toBe(true);
      expect(drawer.props('mode')).toBe('edit');
      expect(drawer.props('initialData')).toEqual(mockModules[0]);
    });

    it('updates module successfully without changing code', async () => {
      const updatedModule = {
        ...mockModules[0],
        name: '更新后的用户管理',
        status: 0,
      };

      mockModuleApi.update.mockResolvedValue({
        code: 0,
        message: 'success',
        timestamp: Date.now(),
        data: updatedModule,
      });

      mockModuleApi.list
        .mockResolvedValueOnce({
          code: 0,
          message: 'success',
          timestamp: Date.now(),
          data: {
            records: mockModules,
            total: 3,
            size: 10,
            current: 1,
            pages: 1,
          },
        })
        .mockResolvedValueOnce({
          code: 0,
          message: 'success',
          timestamp: Date.now(),
          data: {
            records: [updatedModule, ...mockModules.slice(1)],
            total: 3,
            size: 10,
            current: 1,
            pages: 1,
          },
        });

      const wrapper = await createWrapper([], '/system/modules');
      await new Promise((resolve) => setTimeout(resolve, 10));
      await wrapper.vm.$nextTick();

      (wrapper.vm as any).handleModuleEdit(mockModules[0]);
      await wrapper.vm.$nextTick();

      await (wrapper.vm as any).handleModuleEditSubmit({
        name: '更新后的用户管理',
        status: 0,
      });
      await wrapper.vm.$nextTick();

      expect(mockModuleApi.update).toHaveBeenCalledWith(1, {
        name: '更新后的用户管理',
        status: 0,
      });
      expect(mockElMessage.success).toHaveBeenCalledWith('更新成功');
    });

    it('navigates to permission tab with module filter when clicking view permissions', async () => {
      const testRouter = createRouter({
        history: createWebHistory(),
        routes: [
          {
            path: '/system/permissions',
            name: 'SystemPermissions',
            component: IndexView,
            meta: { requiresAuth: true, permissions: ['permission:read'] },
          },
          {
            path: '/system/modules',
            name: 'SystemModules',
            component: IndexView,
            meta: { requiresAuth: true, permissions: ['module:read'] },
          },
          {
            path: '/login',
            name: 'Login',
            component: { template: '<div>Login</div>' },
            meta: { public: true },
          },
        ],
      });
      testRouter.push('/system/modules');

      const pushSpy = vi.spyOn(testRouter, 'push');

      const pinia = createPinia();
      setActivePinia(pinia);
      authStore = useAuthStore();
      authStore.login(
        'mock-token',
        {
          id: 1,
          username: 'admin',
          nickname: '管理员',
          email: '',
          phone: '',
          status: 1,
          createTime: '',
        },
        ['admin'],
        ['permission:read', 'module:read', '*']
      );

      const wrapper = mount(IndexView, {
        global: {
          plugins: [pinia, testRouter],
          mocks: { $t: (key: string) => key },
        },
      });

      await new Promise((resolve) => setTimeout(resolve, 10));
      await wrapper.vm.$nextTick();

      await (wrapper.vm as any).handleModuleViewPermissions(mockModules[0]);

      expect(pushSpy).toHaveBeenCalledWith({
        path: '/system/permissions',
        query: { moduleId: '1' },
      });
      pushSpy.mockRestore();
    });
  });

  describe('AC8 - 模块启停用、删除引用保护', () => {
    it('toggles module status successfully', async () => {
      mockModuleApi.update.mockResolvedValue({
        code: 0,
        message: 'success',
        timestamp: Date.now(),
        data: { ...mockModules[0], status: 0 },
      });

      mockModuleApi.list
        .mockResolvedValueOnce({
          code: 0,
          message: 'success',
          timestamp: Date.now(),
          data: {
            records: mockModules,
            total: 3,
            size: 10,
            current: 1,
            pages: 1,
          },
        })
        .mockResolvedValueOnce({
          code: 0,
          message: 'success',
          timestamp: Date.now(),
          data: {
            records: mockModules.map((m) => (m.id === 1 ? { ...m, status: 0 } : m)),
            total: 3,
            size: 10,
            current: 1,
            pages: 1,
          },
        });

      const wrapper = await createWrapper([], '/system/modules');
      await new Promise((resolve) => setTimeout(resolve, 10));
      await wrapper.vm.$nextTick();

      await (wrapper.vm as any).handleModuleStatusToggle(mockModules[0]);
      await wrapper.vm.$nextTick();

      expect(mockModuleApi.update).toHaveBeenCalledWith(1, { status: 0 });
      expect(mockElMessage.success).toHaveBeenCalledWith('操作成功');
      // 口径同步：路由修复后 watch(immediate) 初载 + 变更后刷新 = 2 次（旧期望 1 次系初载缺失的坏桩口径）
      expect(mockModuleApi.list).toHaveBeenCalledTimes(2);
    });

    it('rejects delete when module has permissions (code=1303)', async () => {
      const error = new Error('该模块下存在权限，无法删除');
      (error as any).response = { data: { code: 1303, message: '该模块下存在权限，无法删除' } };
      mockModuleApi.delete.mockRejectedValue(error);

      const wrapper = await createWrapper([], '/system/modules');
      await new Promise((resolve) => setTimeout(resolve, 10));
      await wrapper.vm.$nextTick();

      await expect((wrapper.vm as any).handleModuleDelete(mockModules[0])).rejects.toThrow();

      expect(mockElMessageBox.confirm).toHaveBeenCalled();
      expect(mockModuleApi.delete).toHaveBeenCalledWith(1);
      expect(mockElMessage.error).toHaveBeenCalledWith('该模块下存在权限，无法删除');
    });

    it('deletes module successfully when no permissions', async () => {
      mockModuleApi.delete.mockResolvedValue({
        code: 0,
        message: 'success',
        timestamp: Date.now(),
        data: undefined,
      });

      mockModuleApi.list.mockResolvedValueOnce({
        code: 0,
        message: 'success',
        timestamp: Date.now(),
        data: {
          records: mockModules.slice(1),
          total: 2,
          size: 10,
          current: 1,
          pages: 1,
        },
      });

      const wrapper = await createWrapper([], '/system/modules');
      await new Promise((resolve) => setTimeout(resolve, 10));
      await wrapper.vm.$nextTick();

      await (wrapper.vm as any).handleModuleDelete(mockModules[2]);
      await wrapper.vm.$nextTick();

      expect(mockModuleApi.delete).toHaveBeenCalledWith(3);
      expect(mockElMessage.success).toHaveBeenCalledWith('删除成功');
      // 口径同步：路由修复后 watch(immediate) 初载 + 变更后刷新 = 2 次（旧期望 1 次系初载缺失的坏桩口径）
      expect(mockModuleApi.list).toHaveBeenCalledTimes(2);
    });
  });

  describe('AC9 - 标签页切换状态保持', () => {
    it('preserves permission tab state when switching tabs', async () => {
      const router = createRouter({
        history: createWebHistory(),
        routes: [
          {
            path: '/system/permissions',
            name: 'SystemPermissions',
            component: IndexView,
            meta: { requiresAuth: true, permissions: ['permission:read'] },
          },
          {
            path: '/system/modules',
            name: 'SystemModules',
            component: IndexView,
            meta: { requiresAuth: true, permissions: ['module:read'] },
          },
          {
            path: '/login',
            name: 'Login',
            component: { template: '<div>Login</div>' },
            meta: { public: true },
          },
        ],
      });
      router.push('/system/permissions');

      const pinia = createPinia();
      setActivePinia(pinia);
      authStore = useAuthStore();
      authStore.login(
        'mock-token',
        {
          id: 1,
          username: 'admin',
          nickname: '管理员',
          email: '',
          phone: '',
          status: 1,
          createTime: '',
        },
        ['admin'],
        ['permission:read', 'module:read', '*']
      );

      mockPermissionApi.list
        .mockResolvedValueOnce({
          code: 0,
          message: 'success',
          timestamp: Date.now(),
          data: { records: mockPermissions, total: 3, size: 10, current: 1, pages: 1 },
        })
        .mockResolvedValueOnce({
          code: 0,
          message: 'success',
          timestamp: Date.now(),
          data: { records: mockPermissions.slice(0, 1), total: 3, size: 1, current: 2, pages: 3 },
        });

      const wrapper = mount(IndexView, {
        global: {
          plugins: [pinia, router],
          mocks: { $t: (key: string) => key },
        },
      });

      await new Promise((resolve) => setTimeout(resolve, 10));
      await wrapper.vm.$nextTick();

      // Search and go to page 2 on permission tab
      const searchInput = wrapper.find('input[placeholder*="权限名/编码"]');
      await searchInput.setValue('user');
      await wrapper.vm.$nextTick();

      (wrapper.vm as any).handlePermissionPageChange(2);
      await wrapper.vm.$nextTick();

      // Switch to module tab
      await router.push('/system/modules');
      await wrapper.vm.$nextTick();

      // Switch back to permission tab
      await router.push('/system/permissions');
      await wrapper.vm.$nextTick();

      // State should be preserved: search keyword "user" and page 2
      expect((wrapper.vm as any).permissionQuery.name).toBe('user');
      expect((wrapper.vm as any).permissionQuery.code).toBe('user');
      expect((wrapper.vm as any).permissionPage).toBe(2);
    });
  });

  describe('Permission Control', () => {
    it('hides create button when no permission:create', async () => {
      const wrapper = await createWrapper(['perm:view', 'perm:update', 'perm:delete']);
      await new Promise((resolve) => setTimeout(resolve, 10));
      await wrapper.vm.$nextTick();

      expect((wrapper.vm as any).canCreatePermission).toBe(false);
      expect(wrapper.find('.permission-toolbar__create').exists()).toBe(false);
    });

    it('hides delete button when no permission:delete', async () => {
      const wrapper = await createWrapper(['perm:view', 'perm:create', 'perm:update']);
      await new Promise((resolve) => setTimeout(resolve, 10));
      await wrapper.vm.$nextTick();

      expect((wrapper.vm as any).canDeletePermission).toBe(false);
    });

    it('hides module create button when no module:create', async () => {
      const wrapper = await createWrapper(
        ['perm:view', 'module:view', 'module:update', 'module:delete'],
        '/system/modules'
      );
      await new Promise((resolve) => setTimeout(resolve, 10));
      await wrapper.vm.$nextTick();

      expect((wrapper.vm as any).canCreateModule).toBe(false);
      expect(wrapper.find('.module-toolbar__create').exists()).toBe(false);
    });

    it('hides module delete button when no module:delete', async () => {
      const wrapper = await createWrapper(
        ['perm:view', 'module:view', 'module:create', 'module:update'],
        '/system/modules'
      );
      await new Promise((resolve) => setTimeout(resolve, 10));
      await wrapper.vm.$nextTick();

      expect((wrapper.vm as any).canDeleteModule).toBe(false);
    });
  });

  describe('Responsive Design', () => {
    it('has responsive table container for mobile', async () => {
      const wrapper = await createWrapper();
      await wrapper.vm.$nextTick();

      expect(wrapper.find('.permission-table-container').exists()).toBe(true);
    });
  });

  describe('web/038e 模块操作列「进入」按钮（AC2）', () => {
    const modulePermissions = [
      'module:view',
      'module:create',
      'module:update',
      'module:delete',
      'module:toggle-status',
    ];

    const buildRow = (overrides: Record<string, unknown> = {}) => ({
      id: 1,
      code: 'news',
      name: '新闻服务',
      baseUrl: 'http://news',
      description: null,
      status: 1,
      createTime: '2024-01-01T00:00:00Z',
      ...overrides,
    });

    const getEnterNode = (vm: Record<string, any>, row: Record<string, unknown>) => {
      const columns = vm.moduleColumns as Array<{ prop: string; render?: (r: unknown) => { children?: Array<Record<string, any>> } }>;
      const actions = columns.find((c) => c.prop === 'actions');
      expect(actions, '应存在操作列定义').toBeDefined();
      const vnode = actions!.render!(row);
      return vnode.children?.find((c) => String(c.props?.class ?? '').includes('module-table__action-enter'));
    };

    it('moduleEnterButtonNavigatesWhenReady', async () => {
      // Arrange: baseUrl 非空且 status=1 的模块行
      const wrapper = await createWrapper(modulePermissions, '/system/modules');
      await wrapper.vm.$nextTick();
      const vm = wrapper.vm as any;

      // Act: 直调操作列 render（BaseTable 为 stub，render 走直调路径）
      const enterNode = getEnterNode(vm, buildRow());

      // Assert: 按钮存在、可点、点击导航至 iframe 路由
      expect(enterNode, '操作列应渲染「进入」按钮').toBeDefined();
      expect(enterNode!.props.disabled).toBeFalsy();
      const pushSpy = vi.spyOn(vm.router, 'push');
      enterNode!.props.onClick();
      expect(pushSpy).toHaveBeenCalledWith('/workspace/module/news');
    });

    it('moduleEnterButtonDisabledWhenBaseUrlMissing', async () => {
      // Arrange: baseUrl 缺失 / 停用 两种不可点情形
      const wrapper = await createWrapper(modulePermissions, '/system/modules');
      await wrapper.vm.$nextTick();
      const vm = wrapper.vm as any;

      // Act & Assert: baseUrl 为空 → disabled + 提示
      const noBase = getEnterNode(vm, buildRow({ baseUrl: null }));
      expect(noBase, 'baseUrl 为空也应渲染「进入」按钮').toBeDefined();
      expect(noBase!.props.disabled, 'baseUrl 为空应 disabled').toBe(true);
      expect(noBase!.props.title).toBe('未配置服务地址');

      // Act & Assert: status≠1 → disabled + 提示
      const stopped = getEnterNode(vm, buildRow({ code: 'off', status: 0 }));
      expect(stopped, '停用模块也应渲染「进入」按钮').toBeDefined();
      expect(stopped!.props.disabled, '停用模块应 disabled').toBe(true);
      expect(stopped!.props.title).toBe('未配置服务地址');
    });
  });
});
