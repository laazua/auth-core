<script setup lang="ts">
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAppStore } from '@/stores/app';
import { useAuthStore } from '@/stores/auth';
import { useTheme } from '@/composables/useTheme';
import { ElDropdown, ElDropdownMenu, ElDropdownItem } from 'element-plus';
import {
  User,
  Setting,
  Lock,
  SwitchButton,
  Bell,
  FullScreen,
  Fold,
  Expand,
} from '@element-plus/icons-vue';

interface Props {
  sidebarCollapsed?: boolean;
  sidebarOpened?: boolean;
}

interface Emits {
  toggle: [];
  'open-change': [boolean];
}

const props = withDefaults(defineProps<Props>(), {
  sidebarCollapsed: false,
  sidebarOpened: false,
});

const emit = defineEmits<Emits>();

const route = useRoute();
const router = useRouter();
const appStore = useAppStore();
const authStore = useAuthStore();
const { isDark, toggleTheme } = useTheme();

const isCollapsed = computed(() => props.sidebarCollapsed ?? appStore.sidebarCollapsed);
const userInfo = computed(() => authStore.userInfo);

const avatarUrl = computed(() => {
  return (
    userInfo.value?.avatar ||
    `https://ui-avatars.com/api/?name=${encodeURIComponent(userInfo.value?.nickname || 'User')}&background=1D4ED8&color=fff`
  );
});

const dropdownItems = [
  { label: '个人中心', icon: User, command: 'profile' },
  { label: '设置', icon: Setting, command: 'settings' },
  { label: '修改密码', icon: Lock, command: 'password' },
  { label: '退出登录', icon: SwitchButton, command: 'logout', divided: true },
];

const handleDropdownCommand = (command: string) => {
  switch (command) {
    case 'profile':
      router.push('/profile');
      break;
    case 'settings':
      router.push('/settings');
      break;
    case 'password':
      router.push('/profile/password');
      break;
    case 'logout':
      authStore.logout();
      router.push('/login');
      break;
  }
};

const toggleFullscreen = () => {
  if (!document.fullscreenElement) {
    document.documentElement.requestFullscreen();
  } else {
    document.exitFullscreen();
  }
};

const handleSidebarToggle = () => {
  emit('toggle');
};
</script>

<template>
  <header class="header">
    <div class="header__left">
      <button
        class="header__toggle"
        :aria-label="isCollapsed ? '展开侧边栏' : '折叠侧边栏'"
        @click="handleSidebarToggle"
      >
        <component :is="isCollapsed ? Expand : Fold" />
      </button>
      <h1 v-if="!isCollapsed" class="header__title">{{ route.meta.title || '仪表盘' }}</h1>
    </div>
    <div class="header__right">
      <button class="header__theme-toggle" :aria-label="isDark ? '切换亮色模式' : '切换暗色模式'" @click="toggleTheme">
        <span class="header__theme-toggle-track" :class="{ 'is-dark': isDark }">
          <span class="header__theme-toggle-thumb" />
        </span>
        <span class="header__theme-toggle-label">{{ isDark ? '暗色' : '亮色' }}</span>
      </button>
      <el-dropdown trigger="click">
        <button class="header__action" aria-label="消息通知">
          <component :is="Bell" />
          <span class="header__badge">3</span>
        </button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item disabled class="header__dropdown-header">消息通知</el-dropdown-item>
            <el-dropdown-item divided>暂无新消息</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
      <el-dropdown trigger="click">
        <button class="header__action" aria-label="全屏切换" @click="toggleFullscreen">
          <component :is="FullScreen" />
        </button>
      </el-dropdown>
      <el-dropdown trigger="click" @command="handleDropdownCommand">
        <div class="header__avatar-wrapper">
          <img :src="avatarUrl" :alt="userInfo?.nickname || '用户'" class="header__avatar" />
          <span v-if="!isCollapsed" class="header__username">{{
            userInfo?.nickname || userInfo?.username
          }}</span>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item disabled class="header__dropdown-header">
              <div class="header__user-info">
                <img :src="avatarUrl" :alt="userInfo?.nickname" class="header__dropdown-avatar" />
                <div>
                  <p class="header__dropdown-name">
                    {{ userInfo?.nickname || userInfo?.username }}
                  </p>
                  <p class="header__dropdown-email">{{ userInfo?.email }}</p>
                </div>
              </div>
            </el-dropdown-item>
            <el-dropdown-item
              v-for="item in dropdownItems"
              :key="item.command"
              :divided="item.divided"
              :command="item.command"
            >
              <component :is="item.icon" class="header__dropdown-icon" />
              <span>{{ item.label }}</span>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<style scoped lang="scss">
.header {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: var(--color-bg-header);
  border-bottom: 1px solid var(--color-border-light);
  position: sticky;
  top: 0;
  z-index: var(--z-index-sticky);
  flex-shrink: 0;
  backdrop-filter: blur(var(--glass-blur));
  -webkit-backdrop-filter: blur(var(--glass-blur));
}

.header__left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header__toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  color: var(--color-text-secondary);
  background: transparent;
  transition: all var(--transition-duration-base) var(--transition-timing);

  &:hover {
    background: var(--color-bg-hover);
    color: var(--color-primary);
  }

  svg {
    width: 18px;
    height: 18px;
  }
}

.header__title {
  margin: 0;
  font-size: 16px;
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  letter-spacing: -0.2px;
}

.header__right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header__theme-toggle {
  display: flex;
  align-items: center;
  gap: 8px;
  width: auto;
  height: 32px;
  padding: 0 12px;
  border: 1px solid var(--color-border-light);
  background: var(--color-bg);
  cursor: pointer;
  border-radius: 16px;
  transition: all var(--transition-duration-base) var(--transition-timing);

  &:hover {
    border-color: var(--color-primary);
    background: var(--color-primary-bg);
  }
}

.header__theme-toggle-track {
  position: relative;
  width: 36px;
  height: 18px;
  border-radius: 9px;
  background: var(--color-border);
  transition: background var(--transition-duration-base) var(--transition-timing-smooth);
  flex-shrink: 0;

  &.is-dark {
    background: var(--color-primary);
  }
}

.header__theme-toggle-thumb {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: var(--color-bg);
  box-shadow: var(--color-shadow-sm);
  transition: transform var(--transition-duration-base) var(--transition-timing-bounce);

  .is-dark & {
    transform: translateX(18px);
  }
}

.header__theme-toggle-label {
  font-size: 12px;
  font-weight: var(--font-weight-medium);
  color: var(--color-text-secondary);
  white-space: nowrap;
}

.header__action {
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  color: var(--color-text-secondary);
  background: transparent;
  transition: all var(--transition-duration-base) var(--transition-timing);

  &:hover {
    background: var(--color-bg-hover);
    color: var(--color-primary);
  }

  svg {
    width: 18px;
    height: 18px;
  }
}

.header__badge {
  position: absolute;
  top: 2px;
  right: 2px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  font-size: 10px;
  font-weight: var(--font-weight-bold);
  color: white;
  background: var(--color-error);
  border-radius: 8px;
  text-align: center;
  line-height: 16px;
  box-shadow: 0 0 0 2px var(--color-bg);
}

.header__avatar-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 12px 4px 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: all var(--transition-duration-base) var(--transition-timing);
  border: 1px solid transparent;

  &:hover {
    background: var(--color-bg-hover);
    border-color: var(--color-border-light);
  }
}

.header__avatar {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  object-fit: cover;
  border: 2px solid var(--color-border-light);
  transition: border-color var(--transition-duration-base) var(--transition-timing);
}

.header__username {
  font-size: 13px;
  font-weight: var(--font-weight-medium);
  color: var(--color-text-primary);
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.header__dropdown-header {
  padding: 12px !important;
}

.header__user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header__dropdown-avatar {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  object-fit: cover;
}

.header__dropdown-name {
  margin: 0;
  font-size: 13px;
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
}

.header__dropdown-email {
  margin: 2px 0 0;
  font-size: 12px;
  color: var(--color-text-secondary);
}

.header__dropdown-icon {
  width: 16px;
  height: 16px;
  margin-right: 8px;
  flex-shrink: 0;
  color: var(--color-text-secondary);
}
</style>