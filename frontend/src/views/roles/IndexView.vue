<script setup lang="ts">
import { ref, reactive, onMounted, computed, h } from 'vue';
import { ElMessage, ElMessageBox, ElPagination } from 'element-plus';
import { useAuthStore } from '@/stores/auth';
import { roleApi } from '@/api/role';
import type { RoleVO, RoleQuery, RoleCreateRequest, RoleUpdateRequest } from '@/types/role';
import BaseCard from '@/components/BaseCard.vue';
import BaseButton from '@/components/BaseButton.vue';
import BaseInput from '@/components/BaseInput.vue';
import BaseSelect from '@/components/BaseSelect.vue';
import BaseTable from '@/components/BaseTable.vue';
import RoleFormDrawer from './RoleFormDrawer.vue';
import PermissionAssignDrawer from './PermissionAssignDrawer.vue';

const authStore = useAuthStore();

const loading = ref(false);
const roleList = ref<RoleVO[]>([]);
const total = ref(0);
const page = ref(1);
const pageSize = ref(10);

const queryForm = reactive<RoleQuery>({
  page: 1,
  size: 10,
  code: '',
  name: '',
  status: undefined,
});

const statusOptions = [
  { label: '全部', value: '' },
  { label: '启用', value: 1 },
  { label: '停用', value: 0 },
];

const columns = [
  { prop: 'name', label: '角色名', minWidth: 150 },
  { prop: 'code', label: '编码', minWidth: 150 },
  {
    prop: 'status',
    label: '状态',
    width: 100,
    formatter: (row: RoleVO) => {
      const status = row.status;
      return status === 1 ? '启用' : '停用';
    },
  },
  {
    prop: 'createTime',
    label: '创建时间',
    width: 180,
    formatter: (row: RoleVO) => {
      return row.createTime ? new Date(row.createTime).toLocaleString('zh-CN') : '—';
    },
  },
  {
    prop: 'actions',
    label: '操作',
    width: 320,
    fixed: 'right',
    render: (row: RoleVO) => {
      return h('div', { class: 'role-table__actions' }, [
        h(BaseButton, {
          size: 'small',
          variant: 'primary',
          class: 'role-table__action-edit',
          onClick: () => handleEdit(row),
        }, { default: () => '编辑' }),
        row.id !== 1 && h(BaseButton, {
          size: 'small',
          variant: row.status === 1 ? 'warning' : 'success',
          class: 'role-table__action-status',
          onClick: () => handleStatusToggle(row),
        }, { default: () => row.status === 1 ? '停用' : '启用' }),
        h(BaseButton, {
          size: 'small',
          variant: 'info',
          class: 'role-table__action-permissions',
          onClick: () => handlePermissionAssign(row),
        }, { default: () => '权限分配' }),
        authStore.hasPermission('role:delete') && row.id !== 1 && h(BaseButton, {
          size: 'small',
          variant: 'danger',
          class: 'role-table__action-delete',
          onClick: () => handleDelete(row),
        }, { default: () => '删除' }),
      ]);
    },
  },
];

const createDrawerVisible = ref(false);
const editDrawerVisible = ref(false);
const editRoleData = ref<RoleVO | null>(null);
const permissionAssignDrawerVisible = ref(false);
const permissionAssignRoleId = ref<number | null>(null);

const canCreate = computed(() => authStore.hasPermission('role:create'));
const canDelete = computed(() => authStore.hasPermission('role:delete'));
const canAssignPermission = computed(() => authStore.hasPermission('role:assign-permission'));

const fetchRoleList = async () => {
  loading.value = true;
  try {
    queryForm.page = page.value;
    queryForm.size = pageSize.value;
    const res = await roleApi.list({ ...queryForm });
    if (res.code === 0) {
      roleList.value = res.data.records;
      total.value = res.data.total;
    } else {
      ElMessage.error(res.message || '查询失败');
    }
  } catch (error: any) {
    ElMessage.error(error.message || '查询失败');
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  page.value = 1;
  queryForm.name = queryForm.code;
  fetchRoleList();
};

const handleReset = () => {
  queryForm.code = '';
  queryForm.name = '';
  queryForm.status = undefined;
  page.value = 1;
  fetchRoleList();
};

const handlePageChange = (newPage: number) => {
  page.value = newPage;
  fetchRoleList();
};

const handleSizeChange = (newSize: number) => {
  pageSize.value = newSize;
  page.value = 1;
  fetchRoleList();
};

const handleCreate = () => {
  createDrawerVisible.value = true;
};

const handleEdit = (row: RoleVO) => {
  editRoleData.value = row;
  editDrawerVisible.value = true;
};

const handleStatusToggle = async (row: RoleVO) => {
  try {
    await roleApi.update(row.id, { status: row.status === 1 ? 0 : 1 });
    ElMessage.success('操作成功');
    fetchRoleList();
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败');
  }
};

const handleDelete = async (row: RoleVO) => {
  try {
    await ElMessageBox.confirm(`确定要删除角色"${row.name}"吗？`, '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    });
    await roleApi.delete(row.id);
    ElMessage.success('删除成功');
    fetchRoleList();
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '删除失败');
      throw error;
    }
  }
};

const handlePermissionAssign = async (row: RoleVO) => {
  permissionAssignRoleId.value = row.id;
  permissionAssignDrawerVisible.value = true;
};

const handleCreateSubmit = async (data: RoleCreateRequest) => {
  try {
    await roleApi.create(data);
    ElMessage.success('创建成功');
    createDrawerVisible.value = false;
    fetchRoleList();
  } catch (error: any) {
    ElMessage.error(error.message || '创建失败');
    throw error;
  }
};

const handleEditSubmit = async (data: RoleUpdateRequest) => {
  if (!editRoleData.value) return;
  try {
    await roleApi.update(editRoleData.value.id, data);
    ElMessage.success('更新成功');
    editDrawerVisible.value = false;
    editRoleData.value = null;
    fetchRoleList();
  } catch (error: any) {
    throw error;
  }
};

const handlePermissionAssignSubmit = async (permissionIds: number[]) => {
  if (!permissionAssignRoleId.value) return;
  try {
    await roleApi.assignPermissions(permissionAssignRoleId.value, { permissionIds });
    ElMessage.success('分配成功');
    permissionAssignDrawerVisible.value = false;
    permissionAssignRoleId.value = null;
    fetchRoleList();
  } catch (error: any) {
    throw error;
  }
};

onMounted(() => {
  fetchRoleList();
});
</script>

<template>
  <div class="role-management">
    <BaseCard>
      <template #header>
        <div class="role-toolbar">
          <div class="role-toolbar__search">
            <BaseInput
              v-model="queryForm.code"
              placeholder="角色名/编码"
              clearable
              @change="handleSearch"
            />
          </div>
          <div class="role-toolbar__filter">
            <BaseSelect
              v-model="queryForm.status"
              :options="statusOptions"
              placeholder="全部状态"
              style="width: 140px"
              @change="handleSearch"
            />
          </div>
          <div class="role-toolbar__actions">
            <BaseButton
              v-if="canCreate"
              class="role-toolbar__create"
              variant="primary"
              @click="handleCreate"
            >
              <template #default>新增角色</template>
            </BaseButton>
          </div>
        </div>
      </template>

      <div class="role-table-container">
        <BaseTable
          class="role-table"
          :data="roleList"
          :columns="columns"
          :loading="loading"
          :row-key="'id'"
          empty-text="暂无角色数据"
        >
          <template #append>
            <div class="role-pagination">
              <ElPagination
                v-model:current-page="page"
                v-model:page-size="pageSize"
                :page-sizes="[10, 20, 50, 100]"
                :total="total"
                layout="total, sizes, prev, pager, next, jumper"
                @current-change="handlePageChange"
                @page-size-change="handleSizeChange"
              />
            </div>
          </template>
        </BaseTable>
      </div>
    </BaseCard>

    <RoleFormDrawer
      :visible="createDrawerVisible"
      mode="create"
      @submit="handleCreateSubmit"
      @close="createDrawerVisible = false"
    />

    <RoleFormDrawer
      :visible="editDrawerVisible"
      mode="edit"
      :initial-data="editRoleData"
      @submit="handleEditSubmit"
      @close="() => { editDrawerVisible = false; editRoleData = null; }"
    />

    <PermissionAssignDrawer
      :visible="permissionAssignDrawerVisible"
      :role-id="permissionAssignRoleId"
      @submit="handlePermissionAssignSubmit"
      @close="() => { permissionAssignDrawerVisible = false; permissionAssignRoleId = null; }"
    />
  </div>
</template>

<style scoped lang="scss">
.role-management {
  height: 100%;
  padding: 16px;
}

.role-toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
  padding: 8px 0;
  height: 56px;
}

.role-toolbar__search {
  flex: 1;
  min-width: 200px;
  max-width: 400px;
}

.role-toolbar__filter {
  min-width: 160px;
}

.role-toolbar__actions {
  margin-left: auto;
}

.role-table-container {
  overflow-x: auto;
  margin: -16px;
  padding: 16px;
}

.role-table {
  width: 100%;
  min-width: 900px;
}

.role-table__actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.role-pagination {
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
  .role-toolbar {
    flex-direction: column;
    align-items: stretch;
    height: auto;
    gap: 12px;
  }

  .role-toolbar__search {
    max-width: none;
  }

  .role-toolbar__filter {
    width: 100%;
  }

  .role-toolbar__actions {
    margin-left: 0;
    justify-content: flex-end;
  }
}
</style>