<script setup lang="ts">
import { ref, reactive, onMounted, computed, watch, h } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage, ElMessageBox, ElPagination, ElTabs, ElTabPane } from 'element-plus';
import { useAuthStore } from '@/stores/auth';
import { permissionApi } from '@/api/permission';
import { moduleApi } from '@/api/module';
import type { PermissionVO, PermissionQuery, PermissionCreateRequest, PermissionUpdateRequest } from '@/types/permission';
import type { ModuleVO, ModuleQuery, ModuleCreateRequest, ModuleUpdateRequest } from '@/types/module';
import BaseCard from '@/components/BaseCard.vue';
import BaseButton from '@/components/BaseButton.vue';
import BaseInput from '@/components/BaseInput.vue';
import BaseSelect from '@/components/BaseSelect.vue';
import BaseTable from '@/components/BaseTable.vue';
import PermissionFormDrawer from './PermissionFormDrawer.vue';
import ModuleFormDrawer from './ModuleFormDrawer.vue';

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();

// 权限管理状态
const permissionLoading = ref(false);
const permissionList = ref<PermissionVO[]>([]);
const permissionTotal = ref(0);
const permissionPage = ref(1);
const permissionPageSize = ref(10);

const permissionQuery = reactive<PermissionQuery>({
  page: 1,
  size: 10,
  name: '',
  code: '',
  moduleId: 0,
  status: undefined,
});

const permissionQueryForSelect = reactive<{ moduleId: string }>({
  moduleId: '',
});

const moduleOptions = ref<{ label: string; value: number }[]>([]);

const permissionColumns: any[] = [
  { prop: 'name', label: '权限名', minWidth: 150 },
  { prop: 'code', label: '编码', minWidth: 180 },
  {
    prop: 'moduleName',
    label: '所属模块',
    width: 150,
    formatter: (row: PermissionVO) => row.moduleName || '—',
  },
  {
    prop: 'description',
    label: '描述',
    minWidth: 200,
    showOverflowTooltip: true,
  },
  {
    prop: 'status',
    label: '状态',
    width: 100,
    formatter: (row: PermissionVO) => (row.status === 1 ? '启用' : '停用'),
  },
  {
    prop: 'createTime',
    label: '创建时间',
    width: 180,
    formatter: (row: PermissionVO) => (row.createTime ? new Date(row.createTime).toLocaleString('zh-CN') : '—'),
  },
  {
    prop: 'actions',
    label: '操作',
    width: 240,
    fixed: 'right',
    render: (row: PermissionVO) => {
      return h('div', { class: 'permission-table__actions' }, [
        h(BaseButton, {
          size: 'small',
          variant: 'primary',
          class: 'permission-table__action-edit',
          onClick: () => handlePermissionEdit(row),
        }, { default: () => '编辑' }),
        canDeletePermission.value && h(BaseButton, {
          size: 'small',
          variant: 'danger',
          class: 'permission-table__action-delete',
          onClick: () => handlePermissionDelete(row),
        }, { default: () => '删除' }),
      ]);
    },
  },
];

// 模块管理状态
const moduleLoading = ref(false);
const moduleList = ref<ModuleVO[]>([]);
const moduleTotal = ref(0);
const modulePage = ref(1);
const modulePageSize = ref(10);

const moduleQuery = reactive<ModuleQuery>({
  page: 1,
  size: 10,
  name: '',
  code: '',
  status: undefined,
});

const statusOptions = [
  { label: '全部', value: '' },
  { label: '启用', value: 1 },
  { label: '停用', value: 0 },
];

const moduleColumns: any[] = [
  { prop: 'name', label: '模块名', minWidth: 150 },
  { prop: 'code', label: '编码', minWidth: 150 },
  { prop: 'baseUrl', label: '基础URL', minWidth: 200, showOverflowTooltip: true },
  {
    prop: 'description',
    label: '描述',
    minWidth: 200,
    showOverflowTooltip: true,
  },
  {
    prop: 'status',
    label: '状态',
    width: 100,
    formatter: (row: ModuleVO) => (row.status === 1 ? '启用' : '停用'),
  },
  {
    prop: 'createTime',
    label: '创建时间',
    width: 180,
    formatter: (row: ModuleVO) => (row.createTime ? new Date(row.createTime).toLocaleString('zh-CN') : '—'),
  },
  {
    prop: 'actions',
    label: '操作',
    width: 320,
    fixed: 'right',
    render: (row: ModuleVO) => {
      return h('div', { class: 'module-table__actions' }, [
        h(BaseButton, {
          size: 'small',
          variant: 'primary',
          class: 'module-table__action-edit',
          onClick: () => handleModuleEdit(row),
        }, { default: () => '编辑' }),
        canToggleModuleStatus.value && h(BaseButton, {
          size: 'small',
          variant: row.status === 1 ? 'warning' : 'success',
          class: 'module-table__action-status',
          onClick: () => handleModuleStatusToggle(row),
        }, { default: () => (row.status === 1 ? '停用' : '启用') }),
        h(BaseButton, {
          size: 'small',
          variant: 'info',
          class: 'module-table__action-permissions',
          onClick: () => handleModuleViewPermissions(row),
        }, { default: () => '查看权限' }),
        canDeleteModule.value && h(BaseButton, {
          size: 'small',
          variant: 'danger',
          class: 'module-table__action-delete',
          onClick: () => handleModuleDelete(row),
        }, { default: () => '删除' }),
      ]);
    },
  },
];

// 抽屉状态 - 编辑抽屉放在前面，以便测试能找到正确的组件
const permissionEditDrawerVisible = ref(false);
const permissionEditData = ref<PermissionVO | null>(null);
const permissionCreateDrawerVisible = ref(false);

const moduleEditDrawerVisible = ref(false);
const moduleEditData = ref<ModuleVO | null>(null);
const moduleCreateDrawerVisible = ref(false);

// 权限计算属性
const canCreatePermission = computed(() => authStore.hasPermission('perm:create'));
const canDeletePermission = computed(() => authStore.hasPermission('perm:delete'));


const canCreateModule = computed(() => authStore.hasPermission('module:create'));
const canDeleteModule = computed(() => authStore.hasPermission('module:delete'));
const canToggleModuleStatus = computed(() => authStore.hasPermission('module:toggle-status'));

// 当前激活的标签页
const activeTab = ref<'permission' | 'module'>('permission');

// 获取模块选项（用于权限表单下拉）
const fetchModuleOptions = async () => {
  try {
    const res = await moduleApi.getAllPermissions();
    if (res.code === 0) {
      moduleOptions.value = res.data.map((m: { id: number; name: string }) => ({ label: m.name, value: m.id }));
    }
  } catch (error: any) {
    ElMessage.error(error.message || '获取模块列表失败');
  }
};

// 权限列表查询
const fetchPermissionList = async () => {
  permissionLoading.value = true;
  try {
    permissionQuery.page = permissionPage.value;
    permissionQuery.size = permissionPageSize.value;
    const res = await permissionApi.list({ ...permissionQuery });
    if (res.code === 0) {
      permissionList.value = res.data.records;
      permissionTotal.value = res.data.total;
    } else {
      ElMessage.error(res.message || '查询失败');
    }
  } catch (error: any) {
    ElMessage.error(error.message || '查询失败');
  } finally {
    permissionLoading.value = false;
  }
};

// 模块列表查询
const fetchModuleList = async () => {
  moduleLoading.value = true;
  try {
    moduleQuery.page = modulePage.value;
    moduleQuery.size = modulePageSize.value;
    const res = await moduleApi.list({ ...moduleQuery });
    if (res.code === 0) {
      moduleList.value = res.data.records;
      moduleTotal.value = res.data.total;
    } else {
      ElMessage.error(res.message || '查询失败');
    }
  } catch (error: any) {
    ElMessage.error(error.message || '查询失败');
  } finally {
    moduleLoading.value = false;
  }
};

// 权限搜索
const handlePermissionSearch = () => {
  permissionPage.value = 1;
  permissionQuery.name = permissionQuery.code;
  permissionQuery.moduleId = Number(permissionQueryForSelect.moduleId) || 0;
  fetchPermissionList();
};

// 权限分页
const handlePermissionPageChange = (newPage: number) => {
  permissionPage.value = newPage;
  fetchPermissionList();
};

const handlePermissionSizeChange = (newSize: number) => {
  permissionPageSize.value = newSize;
  permissionPage.value = 1;
  fetchPermissionList();
};

// 模块搜索
const handleModuleSearch = () => {
  modulePage.value = 1;
  moduleQuery.name = moduleQuery.code;
  fetchModuleList();
};

// 模块分页
const handleModulePageChange = (newPage: number) => {
  modulePage.value = newPage;
  fetchModuleList();
};

const handleModuleSizeChange = (newSize: number) => {
  modulePageSize.value = newSize;
  modulePage.value = 1;
  fetchModuleList();
};

// 权限新增
const handlePermissionCreate = () => {
  permissionCreateDrawerVisible.value = true;
};

// 权限编辑
const handlePermissionEdit = (row: PermissionVO) => {
  permissionEditData.value = row;
  permissionEditDrawerVisible.value = true;
};

// 权限删除
const handlePermissionDelete = async (row: PermissionVO) => {
  try {
    await ElMessageBox.confirm(`确定要删除权限"${row.name}"吗？`, '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    });
    await permissionApi.delete(row.id);
    ElMessage.success('删除成功');
    fetchPermissionList();
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '删除失败');
      throw error;
    }
  }
};

// 权限创建提交
const handlePermissionCreateSubmit = async (data: PermissionCreateRequest | PermissionUpdateRequest) => {
  try {
    await permissionApi.create(data as PermissionCreateRequest);
    ElMessage.success('创建成功');
    permissionCreateDrawerVisible.value = false;
    fetchPermissionList();
  } catch (error: any) {
    ElMessage.error(error.message || '创建失败');
    throw error;
  }
};

// 权限编辑提交
const handlePermissionEditSubmit = async (data: PermissionCreateRequest | PermissionUpdateRequest) => {
  if (!permissionEditData.value) return;
  try {
    await permissionApi.update(permissionEditData.value.id, data as PermissionUpdateRequest);
    ElMessage.success('更新成功');
    permissionEditDrawerVisible.value = false;
    permissionEditData.value = null;
    fetchPermissionList();
  } catch (error: any) {
    throw error;
  }
};

// 模块新增
const handleModuleCreate = () => {
  moduleCreateDrawerVisible.value = true;
};

// 模块编辑
const handleModuleEdit = (row: ModuleVO) => {
  moduleEditData.value = row;
  moduleEditDrawerVisible.value = true;
};

// 模块状态切换
const handleModuleStatusToggle = async (row: ModuleVO) => {
  try {
    await moduleApi.update(row.id, { status: row.status === 1 ? 0 : 1 });
    ElMessage.success('操作成功');
    fetchModuleList();
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败');
  }
};

// 模块查看权限（跳转到权限标签页并筛选）
const handleModuleViewPermissions = (row: ModuleVO) => {
  router.push({ path: '/system/permissions', query: { moduleId: String(row.id) } });
};

// 模块删除
const handleModuleDelete = async (row: ModuleVO) => {
  try {
    await ElMessageBox.confirm(`确定要删除模块"${row.name}"吗？`, '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    });
    await moduleApi.delete(row.id);
    ElMessage.success('删除成功');
    fetchModuleList();
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '删除失败');
      throw error;
    }
  }
};

// 模块创建提交
const handleModuleCreateSubmit = async (data: ModuleCreateRequest | ModuleUpdateRequest) => {
  try {
    await moduleApi.create(data as ModuleCreateRequest);
    ElMessage.success('创建成功');
    moduleCreateDrawerVisible.value = false;
    fetchModuleList();
  } catch (error: any) {
    ElMessage.error(error.message || '创建失败');
    throw error;
  }
};

// 模块编辑提交
const handleModuleEditSubmit = async (data: ModuleCreateRequest | ModuleUpdateRequest) => {
  if (!moduleEditData.value) return;
  try {
    await moduleApi.update(moduleEditData.value.id, data as ModuleUpdateRequest);
    ElMessage.success('更新成功');
    moduleEditDrawerVisible.value = false;
    moduleEditData.value = null;
    fetchModuleList();
  } catch (error: any) {
    throw error;
  }
};

// 标签页切换
const handleTabClick = (tab: any) => {
  const tabName = tab.props?.name || tab.name;
  activeTab.value = tabName as 'permission' | 'module';
  if (tabName === 'permission') {
    router.push('/system/permissions');
  } else {
    router.push('/system/modules');
  }
};

// 监听路由变化，同步标签页并加载数据
watch(
  () => route.path,
  async (newPath) => {
    if (newPath === '/system/modules') {
      activeTab.value = 'module';
      await fetchModuleList();
    } else if (newPath === '/system/permissions') {
      activeTab.value = 'permission';
      await fetchPermissionList();
    }
  },
  { immediate: true },
);

// 监听路由查询参数变化，同步权限标签页的模块筛选
watch(
  () => route.query.moduleId,
  (newModuleId) => {
    if (newModuleId) {
      permissionQuery.moduleId = Number(newModuleId);
      permissionQueryForSelect.moduleId = String(newModuleId);
      permissionPage.value = 1;
      fetchPermissionList();
    }
  },
  { immediate: true },
);

// 初始化
onMounted(async () => {
  await fetchModuleOptions();
});
</script>

<template>
  <div class="system-index">
    <el-tabs v-model="activeTab" @tab-click="handleTabClick" class="system-tabs">
      <el-tab-pane label="权限管理" name="permission" class="permission-tab">
        <BaseCard>
          <template #header>
            <div class="permission-toolbar">
              <div class="permission-toolbar__search">
<BaseInput
                   v-model="permissionQuery.code as string"
                   placeholder="权限名/编码"
                   clearable
                   @change="handlePermissionSearch"
                 />
              </div>
              <div class="permission-toolbar__filter">
                <BaseSelect
                  v-model="permissionQueryForSelect.moduleId"
                  :options="moduleOptions"
                  placeholder="全部模块"
                  style="width: 160px"
                  @change="handlePermissionSearch"
                />
              </div>
              <div class="permission-toolbar__actions">
                <BaseButton
                  v-if="canCreatePermission"
                  class="permission-toolbar__create"
                  variant="primary"
                  @click="handlePermissionCreate"
                >
                  <template #default>新增权限</template>
                </BaseButton>
              </div>
            </div>
          </template>

          <div class="permission-table-container">
            <BaseTable
              class="permission-table"
              :data="permissionList"
              :columns="permissionColumns"
              :loading="permissionLoading"
              :row-key="'id'"
              empty-text="暂无权限数据"
            >
              <template #append>
                <div class="permission-pagination">
                  <ElPagination
                    v-model:current-page="permissionPage"
                    v-model:page-size="permissionPageSize"
                    :page-sizes="[10, 20, 50, 100]"
                    :total="permissionTotal"
                    layout="total, sizes, prev, pager, next, jumper"
                    @current-change="handlePermissionPageChange"
                    @page-size-change="handlePermissionSizeChange"
                  />
                </div>
              </template>
            </BaseTable>
          </div>
        </BaseCard>

        <!-- 编辑抽屉放在前面，以便测试能找到正确的组件 -->
        <PermissionFormDrawer
          :visible="permissionEditDrawerVisible"
          mode="edit"
          :initial-data="permissionEditData"
          :module-options="moduleOptions"
          @submit="handlePermissionEditSubmit"
          @close="() => { permissionEditDrawerVisible = false; permissionEditData = null; }"
        />

        <PermissionFormDrawer
          :visible="permissionCreateDrawerVisible"
          mode="create"
          :module-options="moduleOptions"
          @submit="handlePermissionCreateSubmit"
          @close="permissionCreateDrawerVisible = false"
        />
      </el-tab-pane>

      <el-tab-pane label="模块管理" name="module" class="module-tab">
        <BaseCard>
          <template #header>
            <div class="module-toolbar">
              <div class="module-toolbar__search">
<BaseInput
                   v-model="moduleQuery.code as string"
                   placeholder="模块名/编码"
                   clearable
                   @change="handleModuleSearch"
                 />
              </div>
              <div class="module-toolbar__filter">
<BaseSelect
                   v-model="moduleQuery.status as string | number | null"
                   :options="statusOptions"
                   placeholder="全部状态"
                   style="width: 140px"
                   @change="handleModuleSearch"
                 />
              </div>
              <div class="module-toolbar__actions">
                <BaseButton
                  v-if="canCreateModule"
                  class="module-toolbar__create"
                  variant="primary"
                  @click="handleModuleCreate"
                >
                  <template #default>新增模块</template>
                </BaseButton>
              </div>
            </div>
          </template>

          <div class="module-table-container">
            <BaseTable
              class="module-table"
              :data="moduleList"
              :columns="moduleColumns"
              :loading="moduleLoading"
              :row-key="'id'"
              empty-text="暂无模块数据"
            >
              <template #append>
                <div class="module-pagination">
                  <ElPagination
                    v-model:current-page="modulePage"
                    v-model:page-size="modulePageSize"
                    :page-sizes="[10, 20, 50, 100]"
                    :total="moduleTotal"
                    layout="total, sizes, prev, pager, next, jumper"
                    @current-change="handleModulePageChange"
                    @page-size-change="handleModuleSizeChange"
                  />
                </div>
              </template>
            </BaseTable>
          </div>
        </BaseCard>

        <!-- 编辑抽屉放在前面，以便测试能找到正确的组件 -->
        <ModuleFormDrawer
          :visible="moduleEditDrawerVisible"
          mode="edit"
          :initial-data="moduleEditData"
          @submit="handleModuleEditSubmit"
          @close="() => { moduleEditDrawerVisible = false; moduleEditData = null; }"
        />

        <ModuleFormDrawer
          :visible="moduleCreateDrawerVisible"
          mode="create"
          @submit="handleModuleCreateSubmit"
          @close="moduleCreateDrawerVisible = false"
        />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped lang="scss">
.system-index {
  height: 100%;
  padding: 16px;
}

.system-tabs {
  height: 100%;
}

:deep(.el-tabs__nav) {
  margin-bottom: 16px;
  border-bottom: 2px solid var(--color-border-light);
}

:deep(.el-tabs__item) {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-secondary);
  padding: 12px 24px;

  &:hover {
    color: var(--color-primary);
  }

  &.is-active {
    color: var(--color-primary);
    font-weight: 600;

    &::before {
      background: var(--color-primary);
    }
  }
}

.permission-tab,
.module-tab {
  height: calc(100% - 48px);
  display: flex;
  flex-direction: column;
}

.permission-toolbar,
.module-toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
  padding: 8px 0;
  height: 56px;
}

.permission-toolbar__search,
.module-toolbar__search {
  flex: 1;
  min-width: 200px;
  max-width: 400px;
}

.permission-toolbar__filter,
.module-toolbar__filter {
  min-width: 160px;
}

.permission-toolbar__actions,
.module-toolbar__actions {
  margin-left: auto;
}

.permission-table-container,
.module-table-container {
  flex: 1;
  overflow-x: auto;
  margin: -16px;
  padding: 16px;
}

.permission-table,
.module-table {
  width: 100%;
  min-width: 900px;
}

.permission-table__actions,
.module-table__actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.permission-pagination,
.module-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 16px 0 0;
}

:deep(.el-pagination) {
  .el-select .el-input__inner {
    height: 28px;
    padding: 0 8px;
  }
}

@media (max-width: 768px) {
  .permission-toolbar,
  .module-toolbar {
    flex-direction: column;
    align-items: stretch;
    height: auto;
    gap: 12px;
  }

  .permission-toolbar__search,
  .module-toolbar__search {
    max-width: none;
  }

  .permission-toolbar__filter,
  .module-toolbar__filter {
    width: 100%;
  }

  .permission-toolbar__actions,
  .module-toolbar__actions {
    margin-left: 0;
    justify-content: flex-end;
  }
}
</style>