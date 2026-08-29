<script setup lang="ts">
  import { reactive, ref } from 'vue';
  import { useRouter, useRoute } from 'vue-router';
  import { ElMessage } from 'element-plus';
  import { useAuthStore } from '@/stores/auth';
  import { authApi } from '@/api';
  import BaseButton from '@/components/BaseButton.vue';
  import BaseInput from '@/components/BaseInput.vue';
  import { User, Lock, Monitor, Setting, Key, Cpu } from '@element-plus/icons-vue';

  const router = useRouter();
  const route = useRoute();
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
  const errorMessage = ref('');

  const features = [
    { icon: Monitor, title: '仪表盘概览', desc: '实时展示系统核心指标与运行状态' },
    { icon: Setting, title: '权限管理', desc: '细粒度 RBAC 权限控制体系' },
    { icon: Key, title: '认证授权', desc: 'JWT 无状态认证与动态路由' },
    { icon: Cpu, title: '模块化架构', desc: '插件式模块接入与解耦设计' },
  ];

  const handleLogin = async () => {
    if (!formRef.value) return;

    errorMessage.value = '';

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

        const redirect = (route.query.redirect as string) || '/dashboard';
        router.push(redirect);
      }
    } catch (error: unknown) {
      const message = (error as Error).message || '登录失败';
      errorMessage.value = message;
      ElMessage.error(message);
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
      <!-- Left Brand Section -->
      <div class="login__brand">
        <div class="login__brand-content">
          <svg
            class="login__brand-logo"
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
          <h1 class="login__brand-title">Auth Core</h1>
          <p class="login__brand-slogan">通用权限管理系统</p>
          <p class="login__brand-desc">
            基于 RBAC 的企业级权限管理解决方案，提供用户、角色、权限、模块全生命周期管理
          </p>

          <div class="login__features">
            <div v-for="feature in features" :key="feature.title" class="login__feature">
              <div class="login__feature-icon">
                <component :is="feature.icon" />
              </div>
              <div class="login__feature-text">
                <h4 class="login__feature-title">{{ feature.title }}</h4>
                <p class="login__feature-desc">{{ feature.desc }}</p>
              </div>
            </div>
          </div>

          <div class="login__brand-footer">
            <p>© 2024 Auth Core. All rights reserved.</p>
          </div>
        </div>
      </div>

      <!-- Right Form Section -->
      <div class="login__form-wrapper">
        <div class="login__card">
          <div class="login__header">
            <h2 class="login__title">欢迎登录</h2>
            <p class="login__subtitle">请输入您的账号信息</p>
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

            <el-form-item v-if="errorMessage" class="login__error">
              <div class="login__error-message">
                <svg
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                >
                  <circle cx="12" cy="12" r="10" />
                  <line x1="12" y1="8" x2="12" y2="12" />
                  <line x1="12" y1="16" x2="12.01" y2="16" />
                </svg>
                <span>{{ errorMessage }}</span>
              </div>
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
            <p>默认账号: admin / admin123456</p>
          </div>
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
    position: relative;
    overflow: hidden;

    &::before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background-image:
        radial-gradient(circle at 20% 80%, rgba(64, 158, 255, 0.08) 0%, transparent 50%),
        radial-gradient(circle at 80% 20%, rgba(103, 194, 58, 0.08) 0%, transparent 50%);
      pointer-events: none;
    }
  }

  .login-container {
    display: flex;
    width: 100%;
    max-width: 1120px;
    height: calc(100vh - 48px);
    max-height: 680px;
    background: var(--color-bg);
    border-radius: 16px;
    box-shadow: var(--color-shadow-heavy);
    border: 1px solid var(--color-border-light);
    overflow: hidden;
  }

  /* Brand Section (Left) */
  .login__brand {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(145deg, var(--color-primary) 0%, var(--color-primary-dark) 100%);
    padding: 60px 48px;
    position: relative;
    min-width: 0;

    &::before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background-image: url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cg fill='none' fill-rule='evenodd'%3E%3Cg fill='%23ffffff' fill-opacity='0.03'%3E%3Cpath d='M36 34v-4h-2v4h-4v2h4v4h2v-4h4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4h-4zM6 34v-4H4v4H0v2h4v4h2v-4h4zm0-30V0h-2v4h-4v2h4v4h2V6h4V4H6z'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E");
      opacity: 0.5;
    }

    &::after {
      content: '';
      position: absolute;
      bottom: 0;
      left: 0;
      right: 0;
      height: 200px;
      background: linear-gradient(180deg, transparent 0%, rgba(64, 158, 255, 0.15) 100%);
    }
  }

  .login__brand-content {
    position: relative;
    z-index: 1;
    max-width: 420px;
    color: white;
    width: 100%;
  }

  .login__brand-logo {
    width: 64px;
    height: 64px;
    margin-bottom: 24px;
    filter: drop-shadow(0 8px 24px rgba(0, 0, 0, 0.2));
  }

  .login__brand-title {
    margin: 0 0 8px;
    font-size: 32px;
    font-weight: 700;
    line-height: 1.2;
    letter-spacing: -0.5px;
  }

  .login__brand-slogan {
    margin: 0 0 16px;
    font-size: 18px;
    font-weight: 400;
    opacity: 0.9;
    color: rgba(255, 255, 255, 0.95);
  }

  .login__brand-desc {
    margin: 0 0 40px;
    font-size: 14px;
    line-height: 1.7;
    opacity: 0.8;
    color: rgba(255, 255, 255, 0.85);
  }

  .login__features {
    display: flex;
    flex-direction: column;
    gap: 16px;
    margin-bottom: 40px;
  }

  .login__feature {
    display: flex;
    align-items: flex-start;
    gap: 12px;
    padding: 12px;
    background: rgba(255, 255, 255, 0.05);
    border-radius: 10px;
    border: 1px solid rgba(255, 255, 255, 0.08);
    transition: all 0.2s ease;

    &:hover {
      background: rgba(255, 255, 255, 0.1);
      border-color: rgba(255, 255, 255, 0.15);
      transform: translateX(4px);
    }
  }

  .login__feature-icon {
    width: 36px;
    height: 36px;
    flex-shrink: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    background: rgba(255, 255, 255, 0.1);
    border-radius: 8px;

    svg {
      width: 20px;
      height: 20px;
      color: white;
    }
  }

  .login__feature-text {
    flex: 1;
    min-width: 0;
  }

  .login__feature-title {
    margin: 0 0 4px;
    font-size: 14px;
    font-weight: 600;
    line-height: 1.3;
  }

  .login__feature-desc {
    margin: 0;
    font-size: 12px;
    opacity: 0.7;
    line-height: 1.4;
  }

  .login__brand-footer {
    padding-top: 24px;
    border-top: 1px solid rgba(255, 255, 255, 0.1);

    p {
      margin: 0;
      font-size: 12px;
      opacity: 0.6;
    }
  }

  /* Form Section (Right) */
  .login__form-wrapper {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 48px 40px;
    min-width: 0;
    overflow-y: auto;
  }

  .login__card {
    width: 100%;
    max-width: 400px;
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

  .login__error {
    margin-bottom: 16px !important;

    :deep(.el-form-item__content) {
      padding: 0;
    }
  }

  .login__error-message {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 12px 16px;
    background: rgba(245, 108, 108, 0.1);
    border: 1px solid rgba(245, 108, 108, 0.2);
    border-radius: 8px;
    color: var(--color-error);
    font-size: 13px;

    svg {
      width: 16px;
      height: 16px;
      flex-shrink: 0;
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

      :deep(.el-checkbox__input) {
        :deep(.el-checkbox__inner) {
          border-color: var(--color-border);
          width: 16px;
          height: 16px;

          &:hover {
            border-color: var(--color-primary);
          }
        }

        :deep(.el-checkbox__inner--checked) {
          background: var(--color-primary);
          border-color: var(--color-primary);
        }
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

  /* Responsive */
  @media (max-width: 768px) {
    .login-page {
      padding: 16px;
      align-items: flex-start;
      min-height: auto;
    }

    .login-container {
      flex-direction: column;
      height: auto;
      max-height: none;
      border-radius: 12px;
    }

    .login__brand {
      padding: 40px 24px;
      text-align: center;
      min-height: 300px;

      &::after {
        display: none;
      }
    }

    .login__brand-content {
      max-width: 100%;
    }

    .login__brand-title {
      font-size: 28px;
    }

    .login__brand-slogan {
      font-size: 16px;
    }

    .login__features {
      gap: 12px;
    }

    .login__feature {
      padding: 10px;
    }

    .login__feature-icon {
      width: 32px;
      height: 32px;
    }

    .login__form-wrapper {
      padding: 32px 20px;
    }

    .login__card {
      padding: 32px 24px;
      box-shadow: none;
      border: none;
    }
  }

  @media (max-width: 480px) {
    .login-page {
      padding: 0;
    }

    .login-container {
      border-radius: 0;
      min-height: 100vh;
    }

    .login__brand {
      padding: 32px 20px;
      min-height: 280px;
    }

    .login__brand-title {
      font-size: 24px;
    }

    .login__brand-desc {
      font-size: 13px;
    }

    .login__features {
      display: none;
    }

    .login__form-wrapper {
      padding: 24px 16px;
    }

    .login__card {
      padding: 24px 20px;
    }
  }

  /* Dark mode adjustments */
  [data-theme='dark'] .login-page {
    background: linear-gradient(135deg, var(--color-bg-page) 0%, var(--color-bg-hover) 100%);
  }

  [data-theme='dark'] .login__card {
    background: var(--color-bg);
    border-color: var(--color-border-light);
  }

  [data-theme='dark'] .login__brand {
    background: linear-gradient(145deg, var(--color-primary) 0%, var(--color-primary-dark) 100%);
  }
</style>
