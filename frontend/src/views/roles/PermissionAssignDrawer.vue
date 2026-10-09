<script setup lang="ts">
  import { ref, computed, watch, nextTick } from 'vue';
  import { ElMessage, ElTree } from 'element-plus';
  import { permissionApi } from '@/api/permission';
  import { roleApi } from '@/api/role';
  import type { PermissionTreeNode } from '@/types/permission';
  import BaseButton from '@/components/BaseButton.vue';
  import BaseInput from '@/components/BaseInput.vue';

  interface Props {
    visible: boolean;
    roleId: number | null;
  }

  interface Emits {
    submit: [permissionIds: number[]];
    close: [];
  }

  const props = withDefaults(defineProps<Props>(), {
    visible: false,
    roleId: null,
  });

  const emit = defineEmits<Emits>();

  const loading = ref(false);
  const permissionTree = ref<PermissionTreeNode[]>([]);
  const searchQuery = ref('');
  const defaultExpandedKeys = ref<number[]>([]);
  const checkedKeys = ref<number[]>([]);
  const halfCheckedKeys = ref<number[]>([]);
  const treeRef = ref<InstanceType<typeof ElTree>>();

  const treeProps = computed(() => ({
    children: 'children',
    label: 'name',
    id: 'id',
    disabled: (data: PermissionTreeNode) => data.status === 0,
  })) as import('element-plus').TreeOptionProps;

  const filteredTree = computed(() => {
    if (!searchQuery.value) return permissionTree.value;

    const query = searchQuery.value.toLowerCase();

    const filterNode = (node: PermissionTreeNode): PermissionTreeNode | null => {
      const matches =
        node.name.toLowerCase().includes(query) || node.code.toLowerCase().includes(query);
      if (!node.children || node.children.length === 0) {
        return matches ? { ...node } : null;
      }

      const filteredChildren = node.children
        .map(filterNode)
        .filter((child): child is PermissionTreeNode => child !== null);

      if (matches || filteredChildren.length > 0) {
        return { ...node, children: filteredChildren };
      }
      return null;
    };

    return permissionTree.value
      .map(filterNode)
      .filter((node): node is PermissionTreeNode => node !== null);
  });

  const fetchPermissionTree = async () => {
    if (!props.roleId) return;
    loading.value = true;
    try {
      const res = await permissionApi.getTree();
      if (res.code === 0) {
        permissionTree.value = res.data;

        // Collect all node IDs for default expansion
        const collectIds = (nodes: PermissionTreeNode[], ids: number[]) => {
          nodes.forEach((node) => {
            ids.push(node.id);
            if (node.children?.length) {
              collectIds(node.children, ids);
            }
          });
        };
        const allIds: number[] = [];
        collectIds(res.data, allIds);
        defaultExpandedKeys.value = allIds;

        // Fetch role's current permissions
        await fetchRolePermissions();
      } else {
        ElMessage.error(res.message || '获取权限树失败');
      }
    } catch (error: any) {
      ElMessage.error(error.message || '获取权限树失败');
    } finally {
      loading.value = false;
    }
  };

  const fetchRolePermissions = async () => {
    if (!props.roleId) return;
    try {
      const res = await roleApi.getById(props.roleId);
      if (res.code === 0 && res.data.permissions) {
        const permissionIds = res.data.permissions.map((p) => p.id);
        checkedKeys.value = permissionIds;
        // Update half-checked keys based on children
        updateHalfCheckedKeys();
      }
    } catch (error: any) {
      ElMessage.error(error.message || '获取角色权限失败');
    }
  };

  const updateHalfCheckedKeys = () => {
    const halfChecked: number[] = [];

    const checkNode = (node: PermissionTreeNode): 'checked' | 'half' | 'unchecked' => {
      if (!node.children || node.children.length === 0) {
        return checkedKeys.value.includes(node.id) ? 'checked' : 'unchecked';
      }

      let checkedCount = 0;
      let halfCount = 0;

      node.children.forEach((child) => {
        const state = checkNode(child);
        if (state === 'checked') checkedCount++;
        else if (state === 'half') halfCount++;
      });

      const totalChildren = node.children.length;
      if (checkedCount === totalChildren) {
        return 'checked';
      } else if (checkedCount > 0 || halfCount > 0) {
        halfChecked.push(node.id);
        return 'half';
      }
      return 'unchecked';
    };

    permissionTree.value.forEach((node) => checkNode(node));
    halfCheckedKeys.value = halfChecked;
  };

  const handleCheckChange = (data: { checkedKeys: number[]; halfCheckedKeys: number[] }) => {
    checkedKeys.value = data.checkedKeys;
    halfCheckedKeys.value = data.halfCheckedKeys;
  };

  const expandAll = () => {
    if (treeRef.value) {
      // @ts-expect-error - ElTree instance type
      treeRef.value.expandedKeys = defaultExpandedKeys.value;
    }
  };

  const collapseAll = () => {
    if (treeRef.value) {
      // @ts-expect-error - ElTree instance type
      treeRef.value.expandedKeys = [];
    }
  };

  const handleSubmit = () => {
    emit('submit', [...checkedKeys.value]);
  };

  const handleClose = () => {
    emit('close');
  };

  watch(
    () => props.visible,
    async (newVal) => {
      if (newVal && props.roleId) {
        checkedKeys.value = [];
        halfCheckedKeys.value = [];
        searchQuery.value = '';
        await fetchPermissionTree();
        // Expand all by default after loading
        await nextTick();
        expandAll();
      } else {
        permissionTree.value = [];
        checkedKeys.value = [];
        halfCheckedKeys.value = [];
        searchQuery.value = '';
      }
    },
    { immediate: true }
  );

  watch(
    () => searchQuery.value,
    () => {
      // When searching, expand matching nodes
      nextTick(() => {
        if (searchQuery.value) {
          expandAll();
        }
      });
    }
  );

  defineOptions({
    name: 'PermissionAssignDrawer',
  });
</script>

<template>
  <el-drawer
    v-model="props.visible"
    title="权限分配"
    direction="rtl"
    size="520px"
    :before-close="handleClose"
    @close="handleClose"
  >
    <div class="permission-assign-drawer">
      <div class="permission-assign__search">
        <BaseInput
          v-model="searchQuery"
          placeholder="搜索权限名称/编码"
          clearable
          style="width: 100%"
          prefix-icon="Search"
        />
      </div>

      <div v-loading="loading" class="permission-assign__tree-container">
        <el-tree
          ref="treeRef"
          :data="filteredTree"
          :props="treeProps"
          :default-expanded-keys="defaultExpandedKeys"
          :show-checkbox="true"
          :check-strictly="false"
          :checked-keys="checkedKeys"
          :half-checked-keys="halfCheckedKeys"
          highlight-current
          default-expand-all
          class="permission-tree"
          @check-change="handleCheckChange"
        >
          <template #default="{ data }">
            <span class="permission-tree__node">
              <span class="permission-tree__name">{{ data.name }}</span>
              <span class="permission-tree__code">{{ data.code }}</span>
            </span>
          </template>
        </el-tree>
        <div v-if="filteredTree.length === 0 && !loading" class="permission-tree__empty">
          {{ searchQuery ? '无匹配权限' : '暂无权限数据' }}
        </div>
      </div>

      <div class="permission-assign__actions">
        <BaseButton size="small" variant="default" @click="expandAll"> 展开全部 </BaseButton>
        <BaseButton size="small" variant="default" @click="collapseAll"> 折叠全部 </BaseButton>
      </div>
    </div>

    <template #footer>
      <div class="permission-assign__footer">
        <BaseButton variant="default" @click="handleClose">取消</BaseButton>
        <BaseButton variant="primary" :loading="loading" @click="handleSubmit">确定</BaseButton>
      </div>
    </template>
  </el-drawer>
</template>

<style scoped lang="scss">
  .permission-assign-drawer {
    height: 100%;
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .permission-assign__search {
    padding: 0 4px;
  }

  .permission-assign__tree-container {
    flex: 1;
    overflow: hidden;
    background: var(--color-bg);
    border: 1px solid var(--color-border);
    border-radius: 4px;
  }

  .permission-tree {
    height: 100%;
    max-height: 400px;
  }

  .permission-tree__node {
    display: flex;
    align-items: center;
    gap: 8px;
    width: 100%;
  }

  .permission-tree__name {
    flex: 1;
    font-size: 13px;
    font-weight: 500;
    color: var(--color-text-primary);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .permission-tree__code {
    font-size: 12px;
    color: var(--color-text-secondary);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    min-width: 120px;
  }

  .permission-tree__empty {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 100%;
    min-height: 200px;
    color: var(--color-text-placeholder);
    font-size: 13px;
  }

  .permission-assign__actions {
    display: flex;
    justify-content: flex-end;
    gap: 8px;
    padding: 4px 8px;
  }

  .permission-assign__footer {
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
    display: flex;
    flex-direction: column;
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

  :deep(.el-tree) {
    height: 100%;
  }

  :deep(.el-tree-node__content) {
    height: 32px;
    line-height: 32px;
  }

  :deep(.el-tree-node__expand-icon) {
    color: var(--color-text-secondary);
  }

  :deep(.el-tree-node__content:hover) {
    background-color: var(--color-bg-hover);
  }

  :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
    background-color: var(--color-primary);
    border-color: var(--color-primary);
  }

  :deep(.el-checkbox__input.is-indeterminate .el-checkbox__inner) {
    background-color: var(--color-primary);
    border-color: var(--color-primary);
  }

  @media (max-width: 768px) {
    .permission-assign__tree-container {
      max-height: 300px;
    }
  }
</style>
