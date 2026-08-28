<script setup lang="ts">
  import { computed } from 'vue';
  import { ElBreadcrumb, ElBreadcrumbItem } from 'element-plus';

  interface RouteRecord {
    path: string;
    meta?: {
      title?: string;
      hidden?: boolean;
      icon?: string;
    };
  }

  interface Props {
    routes?: RouteRecord[];
  }

  const props = withDefaults(defineProps<Props>(), {
    routes: () => [],
  });

  const breadcrumbItems = computed(() => {
    const items = props.routes
      .filter((r) => r.meta?.title && !r.meta?.hidden)
      .map((r) => ({
        path: r.path,
        title: r.meta!.title,
        icon: r.meta?.icon,
      }));

    return items;
  });
</script>

<template>
  <div v-if="breadcrumbItems.length > 0" class="breadcrumb">
    <el-breadcrumb separator="/">
      <el-breadcrumb-item :to="{ path: '/dashboard' }">
        <component :is="Monitor" class="breadcrumb__icon" />
        <span>首页</span>
      </el-breadcrumb-item>
      <el-breadcrumb-item v-for="(item, index) in breadcrumbItems" :key="item.path + index">
        <template v-if="index === breadcrumbItems.length - 1">
          <component :is="item.icon" v-if="item.icon" class="breadcrumb__icon" />
          <span>{{ item.title }}</span>
        </template>
        <template v-else>
          <component :is="item.icon" v-if="item.icon" class="breadcrumb__icon" />
          <span>{{ item.title }}</span>
        </template>
      </el-breadcrumb-item>
    </el-breadcrumb>
  </div>
</template>

<style scoped lang="scss">
  .breadcrumb {
    margin-bottom: 16px;
    font-size: 13px;

    :deep(.el-breadcrumb) {
      :deep(.el-breadcrumb__inner) {
        color: var(--color-text-regular);
        font-size: 13px;

        &:hover {
          color: var(--color-primary);
        }
      }

      :deep(.el-breadcrumb__inner.is-link) {
        color: var(--color-text-regular);

        &:hover {
          color: var(--color-primary);
        }
      }

      :deep(.el-breadcrumb__separator) {
        color: var(--color-text-placeholder);
      }

      :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
        color: var(--color-text-primary);
        font-weight: 500;
        pointer-events: none;
      }
    }
  }

  .breadcrumb__icon {
    margin-right: 4px;
    font-size: 13px;
  }
</style>
