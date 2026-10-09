<script setup lang="ts">
  export interface Column {
    prop: string;
    label: string;
    width?: string | number;
    minWidth?: string | number;
    fixed?: boolean | 'left' | 'right';
    sortable?: boolean;
    formatter?: (...args: any[]) => string;
    align?: 'left' | 'center' | 'right';
    headerAlign?: 'left' | 'center' | 'right';
    showOverflowTooltip?: boolean;
    render?: (...args: any[]) => unknown;
  }

  interface Props {
    data: unknown[];
    columns: Column[];
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
    'sort-change': [column: unknown, prop: string, order: string];
    'selection-change': [selection: unknown[]];
    'row-click': [row: unknown, column: unknown, event: MouseEvent];
    'row-dblclick': [row: unknown, column: unknown, event: MouseEvent];
    'header-click': [column: unknown, event: MouseEvent];
  }>();

  // 类型断言移入 script：模板内联 as 联合类型经 prettier 换行后 vue 模板解析器无法解析（RED-2 101→波后 106 异动实证）
  const toFormatter = (formatter?: Column['formatter']) =>
    formatter as
      ((row: unknown, column: unknown, cellValue: unknown, index: number) => string) | undefined;

  const handleSortChange = (data: {
    column: unknown;
    prop: string | null;
    order: 'ascending' | 'descending' | null;
  }) => {
    emit('sort-change', data.column, data.prop ?? '', data.order ?? '');
  };

  const handleSelectionChange = (selection: unknown[]) => {
    emit('selection-change', selection);
  };

  const handleRowClick = (row: unknown, column: unknown, event: MouseEvent) => {
    emit('row-click', row, column, event);
  };

  const handleRowDblclick = (row: unknown, column: unknown, event: MouseEvent) => {
    emit('row-dblclick', row, column, event);
  };

  const handleHeaderClick = (column: unknown, event: MouseEvent) => {
    emit('header-click', column, event);
  };
</script>

<template>
  <el-table
    ref="tableRef"
    v-bind="$attrs"
    :data="data as unknown as Record<string, unknown>[]"
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
        :formatter="toFormatter(column.formatter)"
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

  :deep(.el-loading-spinner) {
    :deep(.el-loading-spinner__circular) {
      border-color: var(--color-primary) !important;
      border-top-color: transparent !important;
    }
  }
</style>
