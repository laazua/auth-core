<script setup lang="ts">
import { ref, reactive, onMounted, computed, watch, h } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox, ElPagination } from 'element-plus';
import { useAuthStore } from '@/stores/auth';
import { userApi } from '@/api/user';
import { roleApi } from '@/api/role';
import type { UserVO, UserQuery, UserCreateRequest, UserUpdateRequest, AssignRolesRequest } from '@/types/user';
import type { RoleVO } from '@/types/role';
import BaseCard from '@/components/BaseCard.vue';
import BaseButton from '@/components/BaseButton.vue';
import BaseInput from '@/components/BaseInput.vue';
import BaseSelect from '@/components/BaseSelect.vue';
import BaseTable from '@/components/BaseTable.vue';
import UserFormDrawer from './UserFormDrawer.vue';
import RoleAssignDrawer from './RoleAssignDrawer.vue';

const router = useRouter();
const authStore = useAuthStore();

const loading = ref(false);
const userList = ref<UserVO[]>([]);
const total = ref(0);
const page = ref(1);
const pageSize = ref(10);

const queryForm = reactive<UserQuery>({
  page: 1,
  size: 10,
  username: '',
  nickname: '',
  email: '',
  phone: '',
  status: '',
});

const statusOptions = [
  { label: '全部', value: '' },
  { label: '启用', value: 1 },
  { label: '停用', value: 0 },
];

const columns = [
  { prop: 'username', label: '用户名', minWidth: 120 },
  { prop: 'nickname', label: '昵称', minWidth: 100 },
  { prop: 'email', label: '邮箱', minWidth: 180 },
  { prop: 'phone', label: '手机号', minWidth: 130 },
  {
    prop: 'status',
    label: '状态',
    width: 100,
    formatter: (row: UserVO) => {
      const status = row.status;
      return status === 1 ? '启用' : '停用';
    },
  },
  {
    prop: 'roles',
    label: '角色',
    minWidth: 150,
    formatter: (row: UserVO) => {
      return row.roles?.map(r => r.name).join(', ') || '—';
    },
  },
  {
    prop: 'createTime',
    label: '创建时间',
    width: 180,
    formatter: (row: UserVO) => {
      return row.createTime ? new Date(row.createTime).toLocaleString('zh-CN') : '—';
    },
  },
  {
    prop: 'actions',
    label: '操作',
    width: 280,
    fixed: 'right',
    render: (row: UserVO) => {
      return h('div', { class: 'user-table__actions' }, [
        h(BaseButton, {
          size: 'small',
          variant: 'primary',
          class: 'user-table__action-edit',
          onClick: () => handleEdit(row),
        }, { default: () => '编辑' }),
        row.id !== 1 && h(BaseButton, {
          size: 'small',
          variant: row.status === 1 ? 'warning' : 'success',
          class: 'user-table__action-status',
          onClick: () => handleStatusToggle(row),
        }, { default: () => row.status === 1 ? '停用' : '启用' }),
        authStore.hasPermission('user:reset-password') && row.id !== 1 && h(BaseButton, {
          size: 'small',
          variant: 'danger',
          class: 'user-table__action-reset',
          onClick: () => handleResetPassword(row),
        }, { default: () => '重置密码' }),
        authStore.hasPermission('user:assign-role') && h(BaseButton, {
          size: 'small',
          variant: 'info',
          class: 'user-table__action-roles',
          onClick: () => handleRoleAssign(row),
        }, { default: () => '角色分配' }),
      ]);
    },
  },
];

const createDrawerVisible = ref(false);
const editDrawerVisible = ref(false);
const editUserData = ref<UserVO | null>(null);
const roleAssignDrawerVisible = ref(false);
const roleAssignUserId = ref<number | null>(null);

const canCreate = computed(() => authStore.hasPermission('user:create'));
const canResetPassword = computed(() => authStore.hasPermission('user:reset-password'));
const canAssignRole = computed(() => authStore.hasPermission('user:assign-role'));

const fetchUserList = async () => {
  loading.value = true;
  try {
    queryForm.page = page.value;
    queryForm.size = pageSize.value;
    const res = await userApi.list({ ...queryForm });
    if (res.code === 0) {
      userList.value = res.data.records;
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
  // Set all search fields to the same value for multi-field search
  queryForm.nickname = queryForm.username;
  queryForm.email = queryForm.username;
  queryForm.phone = queryForm.username;
  fetchUserList();
};

const handleReset = () => {
  queryForm.username = '';
  queryForm.nickname = '';
  queryForm.email = '';
  queryForm.phone = '';
  queryForm.status = '';
  page.value = 1;
  fetchUserList();
};

const handlePageChange = (newPage: number) => {
  page.value = newPage;
  fetchUserList();
};

const handleSizeChange = (newSize: number) => {
  pageSize.value = newSize;
  page.value = 1;
  fetchUserList();
};

const handleCreate = () => {
  createDrawerVisible.value = true;
};

const handleEdit = (row: UserVO) => {
  editUserData.value = row;
  editDrawerVisible.value = true;
};

const handleStatusToggle = async (row: UserVO) => {
  try {
    if (row.status === 1) {
      await userApi.disable(row.id);
    } else {
      await userApi.enable(row.id);
    }
    ElMessage.success('操作成功');
    fetchUserList();
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败');
  }
};

const handleResetPassword = async (row: UserVO) => {
  try {
    await ElMessageBox.confirm(`确定要重置用户"${row.username}"的密码吗？`, '确认重置', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    });
    const newPassword = Math.random().toString(36).slice(-8) + 'Aa1';
    await userApi.resetPassword(row.id, { newPassword });
    ElMessage.success(`重置成功，新密码：${newPassword}`);
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '重置失败');
    }
  }
};

const handleRoleAssign = async (row: UserVO) => {
  roleAssignUserId.value = row.id;
  roleAssignDrawerVisible.value = true;
};

const handleCreateSubmit = async (data: UserCreateRequest) => {
  try {
    const { confirmPassword, ...createData } = data;
    await userApi.create(createData);
    ElMessage.success('创建成功');
    createDrawerVisible.value = false;
    fetchUserList();
  } catch (error: any) {
    ElMessage.error(error.message || '创建失败');
    throw error;
  }
};

const handleEditSubmit = async (data: UserUpdateRequest) => {
  if (!editUserData.value) return;
  try {
    await userApi.update(editUserData.value.id, data);
    ElMessage.success('更新成功');
    editDrawerVisible.value = false;
    editUserData.value = null;
    fetchUserList();
  } catch (error: any) {
    throw error;
  }
};

const handleRoleAssignSubmit = async (roleIds: number[]) => {
  if (!roleAssignUserId.value) return;
  try {
    await userApi.assignRoles(roleAssignUserId.value, { roleIds });
    ElMessage.success('分配成功');
    roleAssignDrawerVisible.value = false;
    roleAssignUserId.value = null;
    fetchUserList();
  } catch (error: any) {
    throw error;
  }
};

onMounted(() => {
  fetchUserList();
});
</script>

<template>
  <div class="user-management">
    <BaseCard>
      <template #header>
        <div class="user-toolbar">
          <div class="user-toolbar__search">
            <BaseInput
              v-model="queryForm.username"
              placeholder="用户名/昵称/邮箱/手机号"
              clearable
              @change="handleSearch"
            />
          </div>
          <div class="user-toolbar__filter">
            <BaseSelect
              v-model="queryForm.status"
              :options="statusOptions"
              placeholder="全部状态"
              style="width: 140px"
              @change="handleSearch"
            />
          </div>
          <div class="user-toolbar__actions">
            <BaseButton
              v-if="canCreate"
              class="user-toolbar__create"
              variant="primary"
              @click="handleCreate"
            >
              <template #default>新增用户</template>
            </BaseButton>
          </div>
        </div>
      </template>

      <div class="user-table-container">
        <BaseTable
          class="user-table"
          :data="userList"
          :columns="columns"
          :loading="loading"
          :row-key="'id'"
          empty-text="暂无用户数据"
        >
          <template #append>
            <div class="user-pagination">
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

    <UserFormDrawer
      :visible="createDrawerVisible"
      mode="create"
      @submit="handleCreateSubmit"
      @close="createDrawerVisible = false"
    />

    <UserFormDrawer
      :visible="editDrawerVisible"
      mode="edit"
      :initial-data="editUserData"
      @submit="handleEditSubmit"
      @close="() => { editDrawerVisible = false; editUserData = null; }"
    />

    <RoleAssignDrawer
      :visible="roleAssignDrawerVisible"
      :user-id="roleAssignUserId"
      @submit="handleRoleAssignSubmit"
      @close="() => { roleAssignDrawerVisible = false; roleAssignUserId = null; }"
    />
  </div>
</template>

<style scoped lang="scss">
.user-management {
  height: 100%;
  padding: 16px;
}

.user-toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
  padding: 8px 0;
  height: 56px;
}

.user-toolbar__search {
  flex: 1;
  min-width: 200px;
  max-width: 400px;
}

.user-toolbar__filter {
  min-width: 160px;
}

.user-toolbar__actions {
  margin-left: auto;
}

.user-table-container {
  overflow-x: auto;
  margin: -16px;
  padding: 16px;
}

.user-table {
  width: 100%;
  min-width: 900px;
}

.user-table__actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.user-pagination {
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
  .user-toolbar {
    flex-direction: column;
    align-items: stretch;
    height: auto;
    gap: 12px;
  }

  .user-toolbar__search {
    max-width: none;
  }

  .user-toolbar__filter {
    width: 100%;
  }

  .user-toolbar__actions {
    margin-left: 0;
    justify-content: flex-end;
  }
}
</style>