<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { useTheme } from '@/composables/useTheme';
import { ElMessage } from 'element-plus';
import BaseCard from '@/components/BaseCard.vue';
import Breadcrumb from '@/components/Breadcrumb.vue';
import { Setting, SwitchButton, Monitor, Moon, Sunny } from '@element-plus/icons-vue';
import { ElSwitch, ElButton, ElDivider, ElSelect, ElOption } from 'element-plus';

const router = useRouter();
const authStore = useAuthStore();
const { isDark, toggleTheme } = useTheme();

const breadcrumbRoutes = [
  { path: '/dashboard', meta: { title: '首页', icon: 'Monitor' } },
  { path: '/settings', meta: { title: '设置' } },
];

const userPreferences = ref({
  language: 'zh-CN',
  notifications: true,
  compactMode: false,
});

const handleLogout = () => {
  authStore.logout();
  ElMessage.success('已退出登录');
  setTimeout(() => {
    router.push('/login');
  }, 500);
};
</script>

<template>
  <div class="settings-page">
    <Breadcrumb :routes="breadcrumbRoutes" />

    <div class="settings__container">
      <BaseCard class="settings__card" shadow="never">
        <template #header>
          <div class="settings__header">
            <Setting class="settings__header-icon" />
            <div>
              <h2 class="settings__title">设置</h2>
              <p class="settings__subtitle">管理您的账户偏好和界面设置</p>
            </div>
          </div>
        </template>

        <div class="settings__content">
          <div class="settings__section">
            <h3 class="settings__section-title">界面设置</h3>

            <div class="settings__item">
              <div class="settings__item-info">
                <div class="settings__item-icon">
                  <component :is="isDark ? Moon : Sunny" />
                </div>
                <div>
                  <p class="settings__item-title">深色模式</p>
                  <p class="settings__item-description">
                    切换亮色/暗色主题，支持跟随系统
                  </p>
                </div>
              </div>
              <ElSwitch
                v-model="isDark"
                @change="toggleTheme"
                :active-value="true"
                :inactive-value="false"
              />
            </div>

            <div class="settings__item">
              <div class="settings__item-info">
                <div class="settings__item-icon">
                  <Monitor />
                </div>
                <div>
                  <p class="settings__item-title">紧凑模式</p>
                  <p class="settings__item-description">减少界面间距，显示更多内容</p>
                </div>
              </div>
              <ElSwitch v-model="userPreferences.compactMode" />
            </div>
          </div>

          <ElDivider class="settings__divider" />

          <div class="settings__section">
            <h3 class="settings__section-title">通知设置</h3>

            <div class="settings__item">
              <div class="settings__item-info">
                <div class="settings__item-icon">
                  <SwitchButton />
                </div>
                <div>
                  <p class="settings__item-title">启用通知</p>
                  <p class="settings__item-description">接收系统消息和重要提醒</p>
                </div>
              </div>
              <ElSwitch v-model="userPreferences.notifications" />
            </div>
          </div>

          <ElDivider class="settings__divider" />

          <div class="settings__section">
            <h3 class="settings__section-title">语言设置</h3>

            <div class="settings__item">
              <div class="settings__item-info">
                <div class="settings__item-icon">
                  <Setting />
                </div>
                <div>
                  <p class="settings__item-title">界面语言</p>
                  <p class="settings__item-description">选择系统显示语言</p>
                </div>
              </div>
              <ElSelect v-model="userPreferences.language" placeholder="请选择语言" style="width: 200px">
                <ElOption label="中文（简体）" value="zh-CN" />
                <ElOption label="English" value="en-US" />
              </ElSelect>
            </div>
          </div>

          <ElDivider class="settings__divider" />

          <div class="settings__section">
            <h3 class="settings__section-title">账户安全</h3>

            <div class="settings__item">
              <div class="settings__item-info">
                <div class="settings__item-icon">
                  <Setting />
                </div>
                <div>
                  <p class="settings__item-title">修改密码</p>
                  <p class="settings__item-description">更新您的登录密码</p>
                </div>
              </div>
              <ElButton variant="primary" @click="router.push('/profile/password')">前往修改</ElButton>
            </div>

            <div class="settings__item">
              <div class="settings__item-info">
                <div class="settings__item-icon">
                  <SwitchButton />
                </div>
                <div>
                  <p class="settings__item-title">退出登录</p>
                  <p class="settings__item-description">安全退出当前账户</p>
                </div>
              </div>
              <ElButton variant="danger" @click="handleLogout">退出登录</ElButton>
            </div>
          </div>
        </div>
      </BaseCard>
    </div>
  </div>
</template>

<style scoped lang="scss">
.settings-page {
  padding: 24px;
  min-height: calc(100vh - 60px);
  background: var(--color-bg-page);
}

.settings__container {
  max-width: 720px;
  margin: 0 auto;
  width: 100%;
}

.settings__card {
  width: 100%;
}

.settings__header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.settings__header-icon {
  font-size: 20px;
  color: var(--color-primary);
  flex-shrink: 0;
  margin-top: 2px;
}

.settings__title {
  margin: 0 0 4px;
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-primary);
  line-height: 1.3;
}

.settings__subtitle {
  margin: 0;
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: 1.5;
}

.settings__content {
  padding-top: 8px;
}

.settings__section {
  padding: 16px 0;
}

.settings__section-title {
  margin: 0 0 16px;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary);
  padding-left: 4px;
}

.settings__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid var(--color-border-light);

  &:last-child {
    border-bottom: none;
  }
}

.settings__item-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.settings__item-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: var(--color-primary-bg);
  color: var(--color-primary);
  flex-shrink: 0;
}

.settings__item-title {
  margin: 0 0 4px;
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-primary);
  line-height: 1.4;
}

.settings__item-description {
  margin: 0;
  font-size: 12px;
  color: var(--color-text-secondary);
  line-height: 1.5;
}

.settings__divider {
  margin: 8px 0;
}

/* Responsive */
@media (max-width: 768px) {
  .settings-page {
    padding: 16px;
  }

  .settings__card {
    :deep(.base-card__header) {
      padding: 0 16px;
    }

    :deep(.base-card__body) {
      padding: 16px;
    }
  }

  .settings__header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}

@media (max-width: 480px) {
  .settings-page {
    padding: 0;
  }

  .settings__container {
    max-width: 100%;
  }

  .settings__card {
    border-radius: 0;
    border: none;
    :deep(.base-card__header) {
      border-bottom: 1px solid var(--color-border-light);
      height: auto;
      padding: 16px;
    }

    :deep(.base-card__body) {
      padding: 16px;
    }
  }
}

/* Dark mode adjustments */
[data-theme='dark'] .settings-page {
  background: var(--color-bg-page);
}

[data-theme='dark'] .settings__card {
  background: var(--color-bg);
  border-color: var(--color-border-light);
}

[data-theme='dark'] .settings__item {
  border-bottom-color: var(--color-border-light);
}
</style>