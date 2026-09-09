<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { roleApi } from '@/api/role';
import type { RoleVO } from '@/types/role';
import BaseButton from '@/components/BaseButton.vue';
import BaseInput from '@/components/BaseInput.vue';

interface RoleWithSelection extends RoleVO {
  selected?: boolean;
}

interface Props {
  visible: boolean;
  userId: number | null;
}

interface Emits {
  submit: [roleIds: number[]];
  close: [];
}

const props = withDefaults(defineProps<Props>(), {
  visible: false,
  userId: null,
});

const emit = defineEmits<Emits>();

const loading = ref(false);
const availableRoles = ref<RoleWithSelection[]>([]);
const assignedRoleIds = ref<number[]>([]);
const searchQuery = ref('');
const allSelected = ref(false);

const filteredAvailableRoles = computed(() => {
  if (!searchQuery.value) return availableRoles.value;
  const query = searchQuery.value.toLowerCase();
  return availableRoles.value.filter(
    r => !assignedRoleIds.value.includes(r.id) &&
    (r.name.toLowerCase().includes(query) || r.code.toLowerCase().includes(query))
  );
});

const filteredAssignedRoles = computed(() => {
  if (!searchQuery.value) return availableRoles.value.filter(r => assignedRoleIds.value.includes(r.id));
  const query = searchQuery.value.toLowerCase();
  return availableRoles.value.filter(
    r => assignedRoleIds.value.includes(r.id) &&
    (r.name.toLowerCase().includes(query) || r.code.toLowerCase().includes(query))
  );
});

const fetchRoles = async () => {
  if (!props.userId) return;
  loading.value = true;
  try {
    const res = await roleApi.list({ page: 1, size: 100 });
    if (res.code === 0) {
      availableRoles.value = res.data.records;
    } else {
      ElMessage.error(res.message || '获取角色列表失败');
    }
  } catch (error: any) {
    ElMessage.error(error.message || '获取角色列表失败');
  } finally {
    loading.value = false;
  }
};

const moveToAssigned = () => {
  const selected = filteredAvailableRoles.value.filter(r => r.selected);
  selected.forEach(r => {
    if (!assignedRoleIds.value.includes(r.id)) {
      assignedRoleIds.value.push(r.id);
    }
    r.selected = false;
  });
  updateAllSelected();
};

const moveToAvailable = () => {
  const selected = filteredAssignedRoles.value.filter(r => r.selected);
  selected.forEach(r => {
    const index = assignedRoleIds.value.indexOf(r.id);
    if (index > -1) {
      assignedRoleIds.value.splice(index, 1);
    }
    r.selected = false;
  });
  updateAllSelected();
};

const moveAllToAssigned = () => {
  filteredAvailableRoles.value.forEach(r => {
    if (!assignedRoleIds.value.includes(r.id)) {
      assignedRoleIds.value.push(r.id);
    }
  });
  updateAllSelected();
};

const moveAllToAvailable = () => {
  assignedRoleIds.value = [];
  updateAllSelected();
};

const toggleAllAvailable = () => {
  filteredAvailableRoles.value.forEach(r => {
    r.selected = allSelected.value;
  });
};

const toggleAllAssigned = () => {
  filteredAssignedRoles.value.forEach(r => {
    r.selected = allSelected.value;
  });
};

const updateAllSelected = () => {
  allSelected.value = filteredAvailableRoles.value.length > 0 &&
    filteredAvailableRoles.value.every(r => r.selected);
};

const handleSubmit = () => {
  emit('submit', [...assignedRoleIds.value]);
};

const handleClose = () => {
  emit('close');
};

watch(
  () => props.visible,
  async (newVal) => {
    if (newVal && props.userId) {
      assignedRoleIds.value = [];
      searchQuery.value = '';
      allSelected.value = false;
      await fetchRoles();
    }
  },
  { immediate: true }
);

defineOptions({
  name: 'RoleAssignDrawer',
});
</script>

<template>
  <el-drawer
    v-model="props.visible"
    title="角色分配"
    direction="rtl"
    size="700px"
    :before-close="handleClose"
    @close="handleClose"
  >
    <div class="role-assign-drawer">
      <div class="role-assign__search">
        <BaseInput
          v-model="searchQuery"
          placeholder="搜索角色名称/编码"
          clearable
          style="width: 100%"
        />
      </div>

      <div class="role-assign__content">
        <div class="role-assign__panel">
          <div class="role-assign__panel-header">
            <span>可用角色 ({{ filteredAvailableRoles.length }})</span>
            <div class="role-assign__panel-actions">
              <BaseButton
                size="small"
                variant="default"
                @click="toggleAllAvailable"
              >
                {{ allSelected ? '取消全选' : '全选' }}
              </BaseButton>
            </div>
          </div>
          <div class="role-assign__list" v-loading="loading">
            <div
              v-for="role in filteredAvailableRoles"
              :key="role.id"
              class="role-assign__item"
              :class="{ 'role-assign__item--selected': role.selected }"
              @click="role.selected = !role.selected"
            >
              <input
                type="checkbox"
                :checked="role.selected"
                @click.stop
                class="role-assign__checkbox"
              />
              <div class="role-assign__info">
                <span class="role-assign__name">{{ role.name }}</span>
                <span class="role-assign__code">{{ role.code }}</span>
              </div>
            </div>
            <div v-if="filteredAvailableRoles.length === 0" class="role-assign__empty">
              {{ searchQuery ? '无匹配角色' : '暂无可用角色' }}
            </div>
          </div>
        </div>

        <div class="role-assign__actions">
          <BaseButton
            size="small"
            variant="default"
            @click="moveToAssigned"
            :disabled="!filteredAvailableRoles.some(r => r.selected)"
          >
            >>
          </BaseButton>
          <BaseButton
            size="small"
            variant="default"
            @click="moveAllToAssigned"
            :disabled="filteredAvailableRoles.length === 0"
          >
            >>
          </BaseButton>
          <BaseButton
            size="small"
            variant="default"
            @click="moveToAvailable"
            :disabled="!filteredAssignedRoles.some(r => r.selected)"
          >
            <<
          </BaseButton>
          <BaseButton
            size="small"
            variant="default"
            @click="moveAllToAvailable"
            :disabled="assignedRoleIds.length === 0"
          >
            <<
          </BaseButton>
        </div>

        <div class="role-assign__panel">
          <div class="role-assign__panel-header">
            <span>已分配角色 ({{ filteredAssignedRoles.length }})</span>
            <div class="role-assign__panel-actions">
              <BaseButton
                size="small"
                variant="default"
                @click="toggleAllAssigned"
              >
                {{ allSelected ? '取消全选' : '全选' }}
              </BaseButton>
            </div>
          </div>
          <div class="role-assign__list" v-loading="loading">
            <div
              v-for="role in filteredAssignedRoles"
              :key="role.id"
              class="role-assign__item"
              :class="{ 'role-assign__item--selected': role.selected }"
              @click="role.selected = !role.selected"
            >
              <input
                type="checkbox"
                :checked="role.selected"
                @click.stop
                class="role-assign__checkbox"
              />
              <div class="role-assign__info">
                <span class="role-assign__name">{{ role.name }}</span>
                <span class="role-assign__code">{{ role.code }}</span>
              </div>
            </div>
            <div v-if="filteredAssignedRoles.length === 0" class="role-assign__empty">
              {{ searchQuery ? '无匹配角色' : '暂无已分配角色' }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <div class="role-assign__footer">
        <BaseButton variant="default" @click="handleClose">取消</BaseButton>
        <BaseButton variant="primary" @click="handleSubmit" :loading="loading">确定</BaseButton>
      </div>
    </template>
  </el-drawer>
</template>

<style scoped lang="scss">
.role-assign-drawer {
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.role-assign__search {
  padding: 0 4px;
}

.role-assign__content {
  flex: 1;
  display: flex;
  gap: 16px;
  overflow: hidden;
}

.role-assign__panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: 4px;
  overflow: hidden;
  min-width: 0;
}

.role-assign__panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: var(--color-bg-page);
  border-bottom: 1px solid var(--color-border-light);
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-primary);
}

.role-assign__panel-actions {
  display: flex;
  gap: 8px;
}

.role-assign__list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
  max-height: 300px;
}

.role-assign__item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.1s ease-out;

  &:hover {
    background: var(--color-bg-hover);
  }

  &--selected {
    background: rgba(64, 158, 255, 0.08);
  }
}

.role-assign__checkbox {
  width: 16px;
  height: 16px;
  accent-color: var(--color-primary);
  flex-shrink: 0;
}

.role-assign__info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.role-assign__name {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.role-assign__code {
  font-size: 12px;
  color: var(--color-text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.role-assign__empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: 120px;
  color: var(--color-text-placeholder);
  font-size: 13px;
}

.role-assign__actions {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 12px;
  padding: 0 8px;
  min-width: 64px;
}

.role-assign__footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 16px;
  margin-top: 8px;
  border-top: 1px solid var(--color-border-light);
}

:deep(.el-drawer__body) {
  padding: 0 24px 24px;
  overflow: hidden;
}

:deep(.el-drawer__header) {
  padding: 16px 24px;
  border-bottom: 1px solid var(--color-border-light);
}

:deep(.el-drawer__title) {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
}

@media (max-width: 768px) {
  .role-assign__content {
    flex-direction: column;
  }

  .role-assign__actions {
    flex-direction: row;
    justify-content: center;
    gap: 8px;
  }

  .role-assign__panel {
    min-height: 200px;
  }
}
</style>