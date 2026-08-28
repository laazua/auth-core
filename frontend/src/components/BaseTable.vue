<script setup lang="ts">
  import { ref } from 'vue';
  import type { TableColumnCtx } from 'element-plus';

  interface Column<T = unknown> {
    prop: string;
    label: string;
    width?: string | number;
    minWidth?: string | number;
    fixed?: boolean | 'left' | 'right';
    sortable?: boolean;
    formatter?: (row: T, column: TableColumnCtx<T>, cellValue: unknown, index: number) => string;
    align?: 'left' | 'center' | 'right';
    headerAlign?: 'left' | 'center' | 'right';
    showOverflowTooltip?: boolean;
    render?: (row: T, column: TableColumnCtx<T>, cellValue: unknown, index: number) => unknown;
  }

  interface Props<T = unknown> {
    data: T[];
    columns: Column<T>[];
    border?: boolean;
    stripe?: boolean;
    highlightCurrentRow?: boolean;
    rowKey?: string;
    loading?: boolean;
    emptyText?: string;
    height?: string | number;
    maxHeight?: string | number;
    defaultSort?: { prop: string; order: 'ascending' | 'descending' };
  }

  withDefaults(defineProps<Props>(), {
    border: true,
    stripe: true,
    highlightCurrentRow: true,
    rowKey: 'id',
    loading: false,
    emptyText: '暂无数据',
  });

  const emit = defineEmits<{
    'sort-change': [column: Column, prop: string, order: string];
    'selection-change': [selection: unknown[]];
    'row-click': [row: unknown, column: Column, event: MouseEvent];
    'row-dblclick': [row: unknown, column: Column, event: MouseEvent];
    'header-click': [column: Column, event: MouseEvent];
  }>();

  const tableRef = ref();

  const handleSortChange = (column: Column, prop: string, order: string) => {
    emit('sort-change', column, prop, order);
  };

  const handleSelectionChange = (selection: unknown[]) => {
    emit('selection-change', selection);
  };

  const handleRowClick = (row: unknown, column: Column, event: MouseEvent) => {
    emit('row-click', row, column, event);
  };

  const handleRowDblclick = (row: unknown, column: Column, event: MouseEvent) => {
    emit('row-dblclick', row, column, event);
  };

  const handleHeaderClick = (column: Column, event: MouseEvent) => {
    emit('header-click', column, event);
  };
</script>

<template>
  <el-table
    ref="tableRef"
    v-bind="$attrs"
    :data="data"
    :border="border"
    :stripe="stripe"
    :highlight-current-row="highlightCurrentRow"
    :row-key="rowKey"
    :loading="loading"
    :empty-text="emptyText"
    :height="height"
    :max-height="maxHeight"
    :default-sort="defaultSort"
    @sort-change="handleSortChange"
    @selection-change="handleSelectionChange"
    @row-click="handleRowClick"
    @row-dblclick="handleRowDblclick"
    @header-click="handleHeaderClick"
  >
    <template v-for="column in columns" :key="column.prop">
      <el-table-column
        v-if="!column.render"
        :prop="column.prop"
        :label="column.label"
        :width="column.width"
        :min-width="column.minWidth"
        :fixed="column.fixed"
        :sortable="column.sortable"
        :align="column.align"
        :header-align="column.headerAlign"
        :show-overflow-tooltip="column.showOverflowTooltip"
        :formatter="column.formatter"
      />
      <el-table-column
        v-else
        :prop="column.prop"
        :label="column.label"
        :width="column.width"
        :min-width="column.minWidth"
        :fixed="column.fixed"
        :align="column.align"
        :header-align="column.headerAlign"
        :show-overflow-tooltip="column.showOverflowTooltip"
      >
        <template #default="scope">
          <component
            :is="column.render(scope.row, scope.column, scope.$index, scope.row[column.prop])"
          />
        </template>
      </el-table-column>
    </template>
    <slot />
  </el-table>
</template>

<style scoped lang="scss">
  .base-table {
    width: 100%;
  }

  :deep(.el-table) {
    font-size: 13px;
  }

  :deep(.el-table__header-wrapper) {
    background: var(--color-bg-page);
  }

  :deep(.el-table__header th) {
    font-weight: 600;
    color: var(--color-text-primary);
    background: var(--color-bg-page);
    border-bottom: 2px solid var(--color-border-light);
    padding: 10px 12px;
  }

  :deep(.el-table__row) {
    transition: background 0.1s ease-out;

    &:hover {
      background: var(--color-bg-hover);
    }

    &.is-current {
      background: rgba(64, 158, 255, 0.08);
    }
  }

  :deep(.el-table__cell) {
    padding: 10px 12px;
    border-bottom: 1px solid var(--color-border-lighter);
    color: var(--color-text-primary);
  }

  :deep(.el-table--border) {
    :deep(.el-table__cell) {
      border-right: 1px solid var(--color-border-lighter);
    }

    :deep(.el-table__header th) {
      border-right: 1px solid var(--color-border-light);
    }
  }

  :deep(.el-table__empty-block) {
    padding: 40px 0;
    color: var(--color-text-placeholder);
  }

  :deep(.el-table__expanded-cell) {
    padding: 20px;
    background: var(--color-bg-page);
  }

  :deep(.el-table__fixed) {
    :deep(.el-table__header-wrapper) {
      background: var(--color-bg-page);
    }
  }

  :deep(.el-table__fixed-right) {
    :deep(.el-table__header-wrapper) {
      background: var(--color-bg-page);
    }
  }

  :deep(.el-loading-mask) {
    background: rgba(255, 255, 255, 0.9) !important;

    [data-theme='dark'] & {
      background: rgba(29, 30, 31, 0.9) !important;
    }
  }

  :deep(.el-loading-spinner) {
    :deep(.el-loading-spinner__circular) {
      border-color: var(--color-primary) !important;
      border-top-color: transparent !important;
    }
  }
</style>
