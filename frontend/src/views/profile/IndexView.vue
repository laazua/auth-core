<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { userApi } from '@/api/user';
import { ElMessage } from 'element-plus';
import BaseCard from '@/components/BaseCard.vue';
import Breadcrumb from '@/components/Breadcrumb.vue';
import { User, Lock, Setting } from '@element-plus/icons-vue';
import { ElButton } from 'element-plus';

const router = useRouter();
const authStore = useAuthStore();

const userInfo = ref(null);
const loading = ref(false);

const breadcrumbRoutes = [
  { path: '/dashboard', meta: { title: '首页', icon: 'Monitor' } },
  { path: '/profile', meta: { title: '个人中心' } },
];

onMounted(() => {
  userInfo.value = authStore.userInfo;
});

const handleNavigate = (path: string) => {
  router.push(path);
};

const handleLogout = () => {
  authStore.logout();
  router.push('/login');
};
</script>

<template>
  <div class="profile-page">
    <Breadcrumb :routes="breadcrumbRoutes" />

    <div class="profile__container">
      <BaseCard class="profile__card" shadow="never">
        <template #header>
          <div class="profile__header">
            <User class="profile__header-icon" />
            <div>
              <h2 class="profile__title">个人中心</h2>
              <p class="profile__subtitle">查看和管理您的个人信息</p>
            </div>
          </div>
        </template>

        <div class="profile__content" v-if="userInfo">
          <div class="profile__info">
            <div class="profile__avatar-section">
              <img
                :src="
                  userInfo.avatar ||
                  `https://ui-avatars.com/api/?name=${encodeURIComponent(
                    userInfo.nickname || 'User'
                  )}&background=1D4ED8&color=fff`
                "
                :alt="userInfo.nickname"
                class="profile__avatar"
              />
              <div class="profile__name-section">
                <h3 class="profile__nickname">{{ userInfo.nickname || userInfo.username }}</h3>
                <p class="profile__username">{{ userInfo.username }}</p>
              </div>
            </div>

            <div class="profile__details">
              <div class="profile__detail-row">
                <span class="profile__detail-label">邮箱</span>
                <span class="profile__detail-value">{{ userInfo.email || '未设置' }}</span>
              </div>
              <div class="profile__detail-row">
                <span class="profile__detail-label">状态</span>
                <span class="profile__detail-value">
                  <span :class="['profile__status', userInfo.status === 1 ? 'enabled' : 'disabled']">
                    {{ userInfo.status === 1 ? '启用' : '停用' }}
                  </span>
                </span>
              </div>
              <div class="profile__detail-row">
                <span class="profile__detail-label">创建时间</span>
                <span class="profile__detail-value">{{ userInfo.createdAt }}</span>
              </div>
              <div class="profile__detail-row">
                <span class="profile__detail-label">最后更新</span>
                <span class="profile__detail-value">{{ userInfo.updatedAt }}</span>
              </div>
            </div>
          </div>

          <div class="profile__actions">
            <ElButton
              variant="primary"
              size="default"
              :icon="Lock"
              class="profile__action-btn"
              @click="handleNavigate('/profile/password')"
            >
              修改密码
            </ElButton>
            <ElButton
              variant="default"
              size="default"
              :icon="Setting"
              class="profile__action-btn"
              @click="handleNavigate('/settings')"
            >
              设置
            </ElButton>
            <ElButton
              variant="danger"
              size="default"
              class="profile__action-btn"
              @click="handleLogout"
            >
              退出登录
            </ElButton>
          </div>
        </div>
      </BaseCard>
    </div>
  </div>
</template>

<style scoped lang="scss">
.profile-page {
  padding: 24px;
  min-height: calc(100vh - 60px);
  background: var(--color-bg-page);
}

.profile__container {
  max-width: 720px;
  margin: 0 auto;
  width: 100%;
}

.profile__card {
  width: 100%;
}

.profile__header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.profile__header-icon {
  font-size: 20px;
  color: var(--color-primary);
  flex-shrink: 0;
  margin-top: 2px;
}

.profile__title {
  margin: 0 0 4px;
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-primary);
  line-height: 1.3;
}

.profile__subtitle {
  margin: 0;
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: 1.5;
}

.profile__content {
  padding-top: 8px;
}

.profile__info {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.profile__avatar-section {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: var(--color-bg);
  border-radius: 12px;
  border: 1px solid var(--color-border-light);
}

.profile__avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid var(--color-border-light);
}

.profile__name-section {
  flex: 1;
}

.profile__nickname {
  margin: 0 0 4px;
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.profile__username {
  margin: 0;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.profile__details {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.profile__detail-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: var(--color-bg);
  border-radius: 8px;
  border: 1px solid var(--color-border-light);
}

.profile__detail-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
}

.profile__detail-value {
  font-size: 13px;
  color: var(--color-text-primary);
}

.profile__status {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 500;
}

.profile__status.enabled {
  background: var(--color-success-bg);
  color: var(--color-success);
}

.profile__status.disabled {
  background: var(--color-error-bg);
  color: var(--color-error);
}

.profile__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid var(--color-border-light);
}

.profile__action-btn {
  min-width: 120px;
}

/* Responsive */
@media (max-width: 768px) {
  .profile-page {
    padding: 16px;
  }

  .profile__card {
    :deep(.base-card__header) {
      padding: 0 16px;
    }

    :deep(.base-card__body) {
      padding: 16px;
    }
  }

  .profile__header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .profile__avatar-section {
    flex-direction: column;
    align-items: flex-start;
    text-align: left;
  }

  .profile__actions {
    flex-direction: column;
  }

  .profile__action-btn {
    width: 100%;
  }
}

@media (max-width: 480px) {
  .profile-page {
    padding: 0;
  }

  .profile__container {
    max-width: 100%;
  }

  .profile__card {
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
[data-theme='dark'] .profile-page {
  background: var(--color-bg-page);
}

[data-theme='dark'] .profile__card {
  background: var(--color-bg);
  border-color: var(--color-border-light);
}

[data-theme='dark'] .profile__avatar-section,
[data-theme='dark'] .profile__detail-row {
  background: var(--color-bg);
  border-color: var(--color-border-light);
}
</style>