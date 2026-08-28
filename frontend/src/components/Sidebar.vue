<script setup lang="ts">
  import { computed } from 'vue';
  import { useRoute } from 'vue-router';
  import { useAppStore } from '@/stores/app';
  import { useAuthStore } from '@/stores/auth';
  import { ElMenu, ElMenuItem, ElSubMenu } from 'element-plus';
  import {
    Monitor,
    Setting,
    User,
    UserFilled,
    Lock,
    Grid,
    Fold,
    Expand,
  } from '@element-plus/icons-vue';

  const emit = defineEmits<{
    toggle: [];
  }>();

  const route = useRoute();
  const appStore = useAppStore();
  const authStore = useAuthStore();

  const isCollapsed = computed(() => appStore.sidebarCollapsed);

  const menuItems = computed(() => [
    {
      index: '/dashboard',
      icon: Monitor,
      title: '仪表盘',
      roles: [],
    },
    {
      index: '/system',
      icon: Setting,
      title: '系统管理',
      roles: ['admin'],
      children: [
        { index: '/system/user', title: '用户管理', icon: User, permissions: ['user:read'] },
        { index: '/system/role', title: '角色管理', icon: UserFilled, permissions: ['role:read'] },
        {
          index: '/system/permission',
          title: '权限管理',
          icon: Lock,
          permissions: ['permission:read'],
        },
        { index: '/system/module', title: '模块管理', icon: Grid, permissions: ['module:read'] },
      ],
    },
  ]);

  const filteredMenuItems = computed(() => {
    return menuItems.value
      .filter((item) => {
        if (!item.roles.length) return true;
        return authStore.hasAnyRole(item.roles);
      })
      .map((item) => {
        if (item.children) {
          return {
            ...item,
            children: item.children.filter((child) => {
              if (!child.permissions.length) return true;
              return authStore.hasAnyPermission(child.permissions);
            }),
          };
        }
        return item;
      })
      .filter((item) => !item.children || item.children.length > 0);
  });

  const handleSelect = (_key: string) => {
    // router.push is handled by router prop on el-menu
  };

  const handleOpenChange = (_keys: string[]) => {
    // 手风琴模式：只保持一个子菜单展开
  };

  const toggleCollapse = () => {
    emit('toggle');
  };
</script>

<template>
  <aside class="sidebar" :class="{ 'sidebar--collapsed': isCollapsed }">
    <div class="sidebar__header">
      <div v-if="!isCollapsed" class="sidebar__logo">
        <svg
          viewBox="0 0 32 32"
          fill="none"
          xmlns="http://www.w3.org/2000/svg"
          width="28"
          height="28"
        >
          <rect width="32" height="32" rx="8" fill="var(--color-primary)" />
          <path
            d="M8 16L14 22L24 10"
            stroke="white"
            stroke-width="2.5"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
        </svg>
        <span>Auth Core</span>
      </div>
      <div v-else class="sidebar__logo-collapsed">
        <svg
          viewBox="0 0 32 32"
          fill="none"
          xmlns="http://www.w3.org/2000/svg"
          width="28"
          height="28"
        >
          <rect width="32" height="32" rx="8" fill="var(--color-primary)" />
          <path
            d="M8 16L14 22L24 10"
            stroke="white"
            stroke-width="2.5"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
        </svg>
      </div>
    </div>
    <div class="sidebar__menu">
      <el-menu
        :default-active="route.path"
        :collapse="isCollapsed"
        :unique-opened="true"
        :collapse-transition="false"
        router
        class="sidebar__el-menu"
        @select="handleSelect"
        @open-change="handleOpenChange"
      >
        <template v-for="item in filteredMenuItems" :key="item.index">
          <el-sub-menu v-if="item.children && item.children.length > 0" :index="item.index">
            <template #title>
              <component :is="item.icon" class="sidebar__icon" />
              <span v-if="!isCollapsed" class="sidebar__title">{{ item.title }}</span>
            </template>
            <el-menu-item v-for="child in item.children" :key="child.index" :index="child.index">
              <component :is="child.icon" v-if="child.icon" class="sidebar__icon" />
              <span>{{ child.title }}</span>
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="item.index">
            <component :is="item.icon" class="sidebar__icon" />
            <span v-if="!isCollapsed" class="sidebar__title">{{ item.title }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </div>
    <div class="sidebar__footer">
      <button
        class="sidebar__toggle"
        :aria-label="isCollapsed ? '展开侧边栏' : '折叠侧边栏'"
        @click="toggleCollapse"
      >
        <component :is="isCollapsed ? Expand : Fold" />
      </button>
    </div>
  </aside>
</template>

<style scoped lang="scss">
  .sidebar {
    width: 260px;
    height: 100vh;
    background: var(--color-bg);
    border-right: 1px solid var(--color-border-light);
    display: flex;
    flex-direction: column;
    position: fixed;
    left: 0;
    top: 0;
    z-index: var(--z-index-fixed);
    transition: width 0.15s ease-out;
    overflow: hidden;

    &--collapsed {
      width: 64px;
    }
  }

  .sidebar__header {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 0 16px;
    border-bottom: 1px solid var(--color-border-light);
    flex-shrink: 0;
  }

  .sidebar__logo {
    display: flex;
    align-items: center;
    gap: 10px;
    color: var(--color-text-primary);
    white-space: nowrap;

    svg {
      flex-shrink: 0;
    }

    span {
      font-size: 16px;
      font-weight: 600;
    }
  }

  .sidebar__logo-collapsed {
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .sidebar__menu {
    flex: 1;
    overflow-y: auto;
    padding: 12px 8px;
  }

  .sidebar__el-menu {
    :deep(.el-menu) {
      border-right: none;
      background: transparent;

      :deep(.el-menu-item) {
        height: 40px;
        line-height: 40px;
        padding: 0 12px;
        border-radius: 6px;
        margin: 2px 4px;
        color: var(--color-text-regular);
        font-size: 13px;
        transition: all 0.1s ease-out;

        &:hover {
          background: var(--color-bg-hover);
          color: var(--color-primary);
        }

        &.is-active {
          background: rgba(64, 158, 255, 0.1);
          color: var(--color-primary);
          font-weight: 500;

          .sidebar__icon {
            color: var(--color-primary);
          }
        }

        .sidebar__icon {
          margin-right: 10px;
          font-size: 14px;
          color: var(--color-text-secondary);
          transition: color 0.1s ease-out;
          flex-shrink: 0;
        }
      }

      :deep(.el-sub-menu) {
        :deep(.el-sub-menu__title) {
          height: 40px;
          line-height: 40px;
          padding: 0 12px;
          border-radius: 6px;
          margin: 2px 4px;
          color: var(--color-text-regular);
          font-size: 13px;
          font-weight: 500;
          transition: all 0.1s ease-out;

          &:hover {
            background: var(--color-bg-hover);
            color: var(--color-primary);
          }

          .sidebar__icon {
            margin-right: 10px;
            font-size: 14px;
            color: var(--color-text-secondary);
            flex-shrink: 0;
          }

          :deep(.el-sub-menu__icon-arrow) {
            transition: transform 0.15s ease-out;
          }
        }

        &.is-opened {
          :deep(.el-sub-menu__title) {
            color: var(--color-primary);
            background: rgba(64, 158, 255, 0.1);

            :deep(.el-sub-menu__icon-arrow) {
              transform: rotate(90deg);
            }
          }
        }

        :deep(.el-menu--collapse) {
          :deep(.el-sub-menu__title) {
            padding: 0;
            justify-content: center;
          }
        }
      }

      :deep(.el-menu--collapse) {
        :deep(.el-menu-item, .el-sub-menu__title) {
          padding: 0;
          justify-content: center;
          margin: 2px 4px;
        }
      }
    }
  }

  .sidebar__icon {
    font-size: 14px;
  }

  .sidebar__title {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .sidebar__footer {
    padding: 12px;
    border-top: 1px solid var(--color-border-light);
    flex-shrink: 0;
  }

  .sidebar__toggle {
    width: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    height: 32px;
    border-radius: 6px;
    color: var(--color-text-secondary);
    background: transparent;
    transition: all 0.1s ease-out;

    &:hover {
      background: var(--color-bg-hover);
      color: var(--color-primary);
    }

    svg {
      width: 16px;
      height: 16px;
    }
  }

  @media (max-width: 767.98px) {
    .sidebar {
      transform: translateX(-100%);
      box-shadow: var(--color-shadow-heavy);

      &--collapsed {
        width: 260px;
        transform: translateX(0);
      }
    }
  }
</style>
