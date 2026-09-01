<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import { useAppStore } from '@/stores/app';
import { ElMenu, ElMenuItem, ElSubMenu } from 'element-plus';
import { Fold, Expand } from '@element-plus/icons-vue';
import { useMenu, type MenuConfig } from '@/composables/useMenu';

interface Props {
  collapsed?: boolean;
  opened?: boolean;
}

interface Emits {
  toggle: [];
  'open-change': [boolean];
}

const props = withDefaults(defineProps<Props>(), {
  collapsed: false,
  opened: false,
});

const emit = defineEmits<Emits>();

const route = useRoute();
const appStore = useAppStore();
const { getSortedMenuTree } = useMenu();

const isCollapsed = computed(() => props.collapsed ?? appStore.sidebarCollapsed);
const menuItems = computed<MenuConfig[]>(() => getSortedMenuTree());

const handleSelect = (_key: string) => {
  if (appStore.isMobile) {
    emit('open-change', false);
  }
};

const handleOpenChange = (opened: boolean) => {
  emit('open-change', opened);
};

const toggleCollapse = () => {
  emit('toggle');
};
</script>

<template>
  <aside class="sidebar" :class="{ 'sidebar--collapsed': isCollapsed }">
    <div class="sidebar__header">
      <div v-if="!isCollapsed" class="sidebar__logo">
        <div class="sidebar__logo-mark" />
        <span>Auth Core</span>
      </div>
      <div v-else class="sidebar__logo-collapsed">
        <div class="sidebar__logo-mark sidebar__logo-mark--sm" />
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
        <template v-for="item in menuItems" :key="item.path">
          <el-sub-menu v-if="item.children && item.children.length > 0" :index="item.path">
            <template #title>
              <component :is="item.icon" class="sidebar__icon" />
              <span v-if="!isCollapsed" class="sidebar__title">{{ item.title }}</span>
            </template>
            <el-menu-item v-for="child in item.children" :key="child.path" :index="child.path">
              <component :is="child.icon" v-if="child.icon" class="sidebar__icon" />
              <span>{{ child.title }}</span>
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="item.path">
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
  transition: width 0.2s var(--transition-timing-smooth), background var(--transition-duration-slow) var(--transition-timing-smooth);
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
  gap: 12px;
  color: var(--color-text-primary);
  white-space: nowrap;

  span {
    font-size: 16px;
    font-weight: var(--font-weight-semibold);
    letter-spacing: -0.2px;
  }
}

.sidebar__logo-mark {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: var(--color-primary);
  flex-shrink: 0;
  box-shadow: 0 2px 8px var(--color-primary-glow);
}

.sidebar__logo-mark--sm {
  width: 24px;
  height: 24px;
  margin: 0 auto;
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
      border-radius: 8px;
      margin: 2px 4px;
      color: var(--color-text-secondary);
      font-size: 13px;
      font-weight: var(--font-weight-medium);
      transition: all var(--transition-duration-base) var(--transition-timing);
      border: 1px solid transparent;

      &:hover {
        background: var(--color-bg-hover);
        color: var(--color-primary);
      }

      &.is-active {
        background: var(--color-primary-bg);
        color: var(--color-primary);
        font-weight: var(--font-weight-semibold);
        border-color: var(--color-border);
        box-shadow: none;

        .sidebar__icon {
          color: var(--color-primary);
        }
      }

      .sidebar__icon {
        margin-right: 12px;
        font-size: 16px;
        color: var(--color-text-placeholder);
        transition: color var(--transition-duration-base) var(--transition-timing);
        flex-shrink: 0;
      }
    }

    :deep(.el-sub-menu) {
      :deep(.el-sub-menu__title) {
        height: 40px;
        line-height: 40px;
        padding: 0 12px;
        border-radius: 8px;
        margin: 2px 4px;
        color: var(--color-text-secondary);
        font-size: 13px;
        font-weight: var(--font-weight-medium);
        transition: all var(--transition-duration-base) var(--transition-timing);
        border: 1px solid transparent;

        &:hover {
          background: var(--color-bg-hover);
          color: var(--color-primary);
        }

        .sidebar__icon {
          margin-right: 12px;
          font-size: 16px;
          color: var(--color-text-placeholder);
          flex-shrink: 0;
        }

        :deep(.el-sub-menu__icon-arrow) {
          transition: transform var(--transition-duration-base) var(--transition-timing-smooth);
        }
      }

      &.is-opened {
        :deep(.el-sub-menu__title) {
          color: var(--color-primary);
          background: var(--color-primary-bg);

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
  font-size: 16px;
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
  border-radius: 8px;
  color: var(--color-text-secondary);
  background: transparent;
  border: 1px solid var(--color-border-light);
  transition: all var(--transition-duration-base) var(--transition-timing);

  &:hover {
    background: var(--color-bg-hover);
    color: var(--color-primary);
    border-color: var(--color-primary);
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