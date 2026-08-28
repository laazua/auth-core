<script setup lang="ts">
  import { reactive, ref } from 'vue';
  import { useRouter } from 'vue-router';
  import { ElMessage } from 'element-plus';
  import { useAuthStore } from '@/stores/auth';
  import { authApi } from '@/api';
  import BaseButton from '@/components/BaseButton.vue';
  import BaseInput from '@/components/BaseInput.vue';

  const router = useRouter();
  const authStore = useAuthStore();

  const loginForm = reactive({
    username: '',
    password: '',
    rememberMe: false,
  });

  const rules = {
    username: [
      { required: true, message: '请输入用户名', trigger: 'blur' },
      { min: 3, max: 20, message: '用户名长度为 3-20 位', trigger: 'blur' },
    ],
    password: [
      { required: true, message: '请输入密码', trigger: 'blur' },
      { min: 6, max: 30, message: '密码长度为 6-30 位', trigger: 'blur' },
    ],
  };

  const loading = ref(false);
  const formRef = ref();

  const handleLogin = async () => {
    if (!formRef.value) return;

    try {
      await formRef.value.validate();
    } catch {
      return;
    }

    loading.value = true;

    try {
      const response = await authApi.login(loginForm);
      if (response.code === 0 && response.data) {
        const { token, userInfo } = response.data;
        authStore.login(token, userInfo, [], []);
        ElMessage.success('登录成功');

        const redirect = (router.currentRoute.value.query.redirect as string) || '/dashboard';
        router.push(redirect);
      }
    } catch (error: unknown) {
      ElMessage.error((error as Error).message || '登录失败');
    } finally {
      loading.value = false;
    }
  };

  const handleKeyUp = (event: KeyboardEvent) => {
    if (event.key === 'Enter') {
      handleLogin();
    }
  };
</script>

<template>
  <div class="login-page" @keyup="handleKeyUp">
    <div class="login-container">
      <div class="login__card">
        <div class="login__header">
          <svg
            class="login__logo"
            viewBox="0 0 32 32"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
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
          <h1 class="login__title">Auth Core</h1>
          <p class="login__subtitle">权限管理系统</p>
        </div>
        <el-form
          ref="formRef"
          :model="loginForm"
          :rules="rules"
          class="login__form"
          label-position="top"
          :label-width="0"
        >
          <el-form-item prop="username">
            <BaseInput
              v-model="loginForm.username"
              placeholder="请输入用户名"
              :prefix-icon="User"
              autocomplete="username"
              @keyup.enter="handleLogin"
            />
          </el-form-item>
          <el-form-item prop="password">
            <BaseInput
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              :prefix-icon="Lock"
              :show-password="true"
              autocomplete="current-password"
              @keyup.enter="handleLogin"
            />
          </el-form-item>
          <el-form-item>
            <div class="login__remember">
              <el-checkbox v-model="loginForm.rememberMe">记住我</el-checkbox>
              <a href="#" class="login__forgot">忘记密码？</a>
            </div>
          </el-form-item>
          <el-form-item>
            <BaseButton
              variant="primary"
              size="large"
              :loading="loading"
              class="login__submit"
              @click="handleLogin"
            >
              登 录
            </BaseButton>
          </el-form-item>
        </el-form>
        <div class="login__footer">
          <p>默认账号: admin / 123456</p>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
  .login-page {
    min-height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, var(--color-bg-page) 0%, var(--color-bg-hover) 100%);
    padding: 24px;
  }

  .login-container {
    width: 100%;
    max-width: 400px;
  }

  .login__card {
    background: var(--color-bg);
    border-radius: 12px;
    box-shadow: var(--color-shadow-base);
    border: 1px solid var(--color-border-light);
    padding: 40px 32px;
  }

  .login__header {
    text-align: center;
    margin-bottom: 32px;
  }

  .login__logo {
    width: 56px;
    height: 56px;
    margin: 0 auto 16px;
  }

  .login__title {
    margin: 0 0 8px;
    font-size: 24px;
    font-weight: 700;
    color: var(--color-text-primary);
  }

  .login__subtitle {
    margin: 0;
    font-size: 14px;
    color: var(--color-text-secondary);
  }

  .login__form {
    margin-bottom: 24px;

    :deep(.el-form-item) {
      margin-bottom: 20px;
    }

    :deep(.el-form-item__error) {
      font-size: 12px;
      margin-top: 6px;
    }
  }

  .login__remember {
    display: flex;
    align-items: center;
    justify-content: space-between;

    :deep(.el-checkbox) {
      :deep(.el-checkbox__label) {
        font-size: 13px;
        color: var(--color-text-regular);
      }
    }
  }

  .login__forgot {
    font-size: 13px;
    color: var(--color-primary);

    &:hover {
      text-decoration: underline;
    }
  }

  .login__submit {
    width: 100%;
  }

  .login__footer {
    text-align: center;
    padding-top: 16px;
    border-top: 1px solid var(--color-border-light);

    p {
      margin: 0;
      font-size: 12px;
      color: var(--color-text-placeholder);
    }
  }

  @media (max-width: 480px) {
    .login__card {
      padding: 24px 20px;
    }
  }
</style>
