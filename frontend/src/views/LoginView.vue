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
    const loginResponse = await authApi.login(loginForm);
    if (loginResponse.code === 0 && loginResponse.data) {
      const { token } = loginResponse.data;
      authStore.setToken(token);
      const meResponse = await authApi.me();
      if (meResponse.code === 0 && meResponse.data) {
        const { user, roles, permissions } = meResponse.data;
        authStore.login(token, user, roles.map(r => r.code), permissions);
        ElMessage.success('登录成功');

        const redirect = (route.query.redirect as string) || '/dashboard';
        await router.push(redirect);
      } else {
        throw new Error(meResponse.message || '获取用户信息失败');
      }
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
      <div class="login__brand">
        <div class="login__brand-content">
          <div class="login__brand-logo">
            <svg viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg">
              <rect width="32" height="32" rx="8" fill="var(--color-primary)" />
              <path d="M8 16L14 22L24 10" stroke="white" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" />
            </svg>
          </div>
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
  background: var(--color-bg-page);
  background-image: var(--gradient-bg);
  background-attachment: fixed;
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
      radial-gradient(circle at 15% 85%, rgba(29, 78, 216, 0.04) 0%, transparent 50%),
      radial-gradient(circle at 85% 15%, rgba(109, 40, 217, 0.03) 0%, transparent 50%);
    pointer-events: none;
  }
}

.login-container {
  display: flex;
  width: 100%;
  max-width: 1120px;
  height: calc(100vh - 48px);
  max-height: 680px;
  border-radius: 16px;
  overflow: hidden;
  position: relative;
  z-index: 1;
  box-shadow: var(--color-shadow-heavy);
  border: 1px solid var(--color-border-light);
}

.login__brand {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg);
  padding: 60px 48px;
  position: relative;
  min-width: 0;
  border-right: 1px solid var(--color-border-light);
}

.login__brand-content {
  position: relative;
  z-index: 1;
  max-width: 420px;
  color: var(--color-text-primary);
  width: 100%;
}

.login__brand-logo {
  width: 64px;
  height: 64px;
  margin-bottom: 24px;
  filter: drop-shadow(0 4px 16px var(--color-primary-glow));

  svg {
    width: 100%;
    height: 100%;
  }
}

.login__brand-title {
  margin: 0 0 8px;
  font-size: 32px;
  font-weight: var(--font-weight-bold);
  line-height: 1.2;
  letter-spacing: -1px;
  color: var(--color-text-primary);
}

.login__brand-slogan {
  margin: 0 0 16px;
  font-size: 18px;
  font-weight: var(--font-weight-normal);
  color: var(--color-text-regular);
}

.login__brand-desc {
  margin: 0 0 40px;
  font-size: 14px;
  line-height: 1.7;
  color: var(--color-text-secondary);
}

.login__features {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-bottom: 40px;
}

.login__feature {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 14px;
  background: var(--color-bg-hover);
  border-radius: 10px;
  border: 1px solid var(--color-border-light);
  transition: all var(--transition-duration-base) var(--transition-timing);

  &:hover {
    background: var(--color-bg-active);
    border-color: var(--color-border);
  }
}

.login__feature-icon {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-primary-bg);
  border-radius: 8px;
  color: var(--color-primary);

  svg {
    width: 18px;
    height: 18px;
  }
}

.login__feature-text {
  flex: 1;
  min-width: 0;
}

.login__feature-title {
  margin: 0 0 4px;
  font-size: 14px;
  font-weight: var(--font-weight-semibold);
  line-height: 1.3;
}

.login__feature-desc {
  margin: 0;
  font-size: 12px;
  color: var(--color-text-secondary);
  line-height: 1.4;
}

.login__brand-footer {
  padding-top: 24px;
  border-top: 1px solid var(--color-border-light);

  p {
    margin: 0;
    font-size: 12px;
    color: var(--color-text-placeholder);
  }
}

.login__form-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 40px;
  min-width: 0;
  overflow-y: auto;
  background: var(--color-bg-page);
}

.login__card {
  width: 100%;
  max-width: 400px;
  background: var(--color-bg);
  border-radius: 16px;
  box-shadow: var(--color-shadow-base);
  border: 1px solid var(--color-border-light);
  padding: 40px 32px;
  position: relative;
  overflow: hidden;
}

.login__header {
  text-align: center;
  margin-bottom: 32px;
}

.login__title {
  margin: 0 0 8px;
  font-size: 24px;
  font-weight: var(--font-weight-bold);
  color: var(--color-text-primary);
  letter-spacing: -0.5px;
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
  background: var(--color-error-bg);
  border: 1px solid var(--color-border);
  border-radius: 10px;
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
  font-weight: var(--font-weight-medium);

  &:hover {
    text-decoration: underline;
  }
}

.login__submit {
  width: 100%;
  border-radius: 10px;
  padding: 12px;
  font-size: var(--font-size-base);
  font-weight: var(--font-weight-semibold);
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
    padding: 48px 24px;
    text-align: center;
    border-right: none;
    border-bottom: 1px solid var(--color-border-light);
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

  .login__brand-logo {
    width: 56px;
    height: 56px;
    margin-bottom: 20px;
  }

  .login__features {
    gap: 10px;
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
    box-shadow: var(--color-shadow-light);
    border: 1px solid var(--color-border-light);
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
</style>