<script setup lang="ts">
  import { reactive, ref } from 'vue';
  import { useRouter } from 'vue-router';
  import { ElMessage } from 'element-plus';
  import { useAuthStore } from '@/stores/auth';
  import { userApi } from '@/api/user';
  import BaseButton from '@/components/BaseButton.vue';
  import BaseInput from '@/components/BaseInput.vue';
  import BaseCard from '@/components/BaseCard.vue';
  import Breadcrumb from '@/components/Breadcrumb.vue';
  import { Lock, Key } from '@element-plus/icons-vue';

  const router = useRouter();
  const authStore = useAuthStore();

  const formRef = ref();

  const form = reactive({
    oldPassword: '',
    newPassword: '',
    confirmPassword: '',
  });

  const errors = reactive<Record<string, string>>({});
  const loading = ref(false);

  const breadcrumbRoutes = [
    { path: '/dashboard', meta: { title: '首页', icon: 'Monitor' } },
    { path: '/profile', meta: { title: '个人中心' } },
    { path: '/profile/password', meta: { title: '修改密码' } },
  ];

  const validatePasswordComplexity = (password: string): boolean => {
    if (password.length < 8) return false;
    let categories = 0;
    if (/[A-Z]/.test(password)) categories++;
    if (/[a-z]/.test(password)) categories++;
    if (/[0-9]/.test(password)) categories++;
    if (/[^A-Za-z0-9]/.test(password)) categories++;
    return categories >= 3;
  };

  const validateForm = (): boolean => {
    Object.keys(errors).forEach((key) => delete errors[key]);

    if (!form.oldPassword) {
      errors.oldPassword = '请输入旧密码';
    }

    if (!form.newPassword) {
      errors.newPassword = '请输入新密码';
    } else if (!validatePasswordComplexity(form.newPassword)) {
      errors.newPassword = '密码需包含大小写字母、数字、特殊字符至少三类且长度≥8';
    } else if (form.newPassword === form.oldPassword) {
      errors.newPassword = '新密码不能与旧密码相同';
    }

    if (!form.confirmPassword) {
      errors.confirmPassword = '请确认新密码';
    } else if (form.confirmPassword !== form.newPassword) {
      errors.confirmPassword = '两次输入的密码不一致';
    }

    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async () => {
    if (!formRef.value) return;

    try {
      await formRef.value.validate();
    } catch {
      return;
    }

    if (!validateForm()) {
      return;
    }

    loading.value = true;

    try {
      const response = await userApi.changePassword({
        oldPassword: form.oldPassword,
        newPassword: form.newPassword,
      });

      if (response.code === 0) {
        ElMessage.success('密码修改成功，请重新登录');
        authStore.logout();
        router.push('/login');
      } else {
        const message = response.message || '密码修改失败';
        ElMessage.error(message);

        if (response.code === 1005) {
          errors.oldPassword = '旧密码错误';
        } else if (response.code === 1006) {
          errors.newPassword = '新密码不能与旧密码相同';
        }
      }
    } catch (error: unknown) {
      const message = (error as Error).message || '密码修改失败';
      ElMessage.error(message);
    } finally {
      loading.value = false;
    }
  };

  const clearError = (field: string) => {
    delete errors[field];
  };
</script>

<template>
  <div class="password-page">
    <Breadcrumb :routes="breadcrumbRoutes" />

    <div class="password__container">
      <BaseCard class="password__card" shadow="never">
        <template #header>
          <div class="password__header">
            <Key class="password__header-icon" />
            <div>
              <h2 class="password__title">修改密码</h2>
              <p class="password__subtitle">请输入旧密码和新密码，新密码需满足复杂度要求</p>
            </div>
          </div>
        </template>

        <el-form ref="formRef" class="password__form" label-position="top" :label-width="0">
          <el-form-item prop="oldPassword">
            <BaseInput
              v-model="form.oldPassword"
              type="password"
              label="旧密码"
              placeholder="请输入当前密码"
              :prefix-icon="Lock"
              :show-password="true"
              :error="errors.oldPassword"
              autocomplete="current-password"
              @input="clearError('oldPassword')"
              @keyup.enter="handleSubmit"
            />
          </el-form-item>

          <el-form-item prop="newPassword">
            <BaseInput
              v-model="form.newPassword"
              type="password"
              label="新密码"
              placeholder="请输入新密码（≥8位，含大小写字母、数字、特殊字符至少三类）"
              :prefix-icon="Lock"
              :show-password="true"
              :error="errors.newPassword"
              autocomplete="new-password"
              @input="clearError('newPassword')"
              @keyup.enter="handleSubmit"
            />
          </el-form-item>

          <el-form-item prop="confirmPassword">
            <BaseInput
              v-model="form.confirmPassword"
              type="password"
              label="确认新密码"
              placeholder="请再次输入新密码"
              :prefix-icon="Lock"
              :show-password="true"
              :error="errors.confirmPassword"
              autocomplete="new-password"
              @input="clearError('confirmPassword')"
              @keyup.enter="handleSubmit"
            />
          </el-form-item>

          <el-form-item>
            <BaseButton
              variant="primary"
              size="large"
              :loading="loading"
              class="password__submit"
              @click="handleSubmit"
            >
              确认修改
            </BaseButton>
          </el-form-item>
        </el-form>
      </BaseCard>
    </div>
  </div>
</template>

<style scoped lang="scss">
  .password-page {
    padding: 24px;
    min-height: calc(100vh - 60px);
    background: var(--color-bg-page);
  }

  .password__container {
    max-width: 480px;
    margin: 0 auto;
    width: 100%;
  }

  .password__card {
    width: 100%;
  }

  .password__header {
    display: flex;
    align-items: flex-start;
    gap: 12px;
  }

  .password__header-icon {
    font-size: 20px;
    color: var(--color-primary);
    flex-shrink: 0;
    margin-top: 2px;
  }

  .password__title {
    margin: 0 0 4px;
    font-size: 18px;
    font-weight: 600;
    color: var(--color-text-primary);
    line-height: 1.3;
  }

  .password__subtitle {
    margin: 0;
    font-size: 13px;
    color: var(--color-text-secondary);
    line-height: 1.5;
  }

  .password__form {
    :deep(.el-form-item) {
      margin-bottom: 20px;
    }

    :deep(.el-form-item__error) {
      font-size: 12px;
      margin-top: 6px;
    }
  }

  .password__submit {
    width: 100%;
  }

  /* Responsive */
  @media (max-width: 768px) {
    .password-page {
      padding: 16px;
    }

    .password__card {
      :deep(.base-card__header) {
        padding: 0 16px;
      }

      :deep(.base-card__body) {
        padding: 16px;
      }
    }

    .password__header {
      flex-direction: column;
      align-items: flex-start;
      gap: 8px;
    }
  }

  @media (max-width: 480px) {
    .password-page {
      padding: 0;
    }

    .password__container {
      max-width: 100%;
    }

    .password__card {
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
  [data-theme='dark'] .password-page {
    background: var(--color-bg-page);
  }

  [data-theme='dark'] .password__card {
    background: var(--color-bg);
    border-color: var(--color-border-light);
  }
</style>
