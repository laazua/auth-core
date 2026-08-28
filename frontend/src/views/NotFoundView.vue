<script setup lang="ts">
  import { useRouter } from 'vue-router';
  import BaseButton from '@/components/BaseButton.vue';

  const router = useRouter();

  const goBack = () => {
    router.back();
  };

  const goHome = () => {
    router.push('/dashboard');
  };
</script>

<template>
  <div class="not-found">
    <div class="not-found__content">
      <div class="not-found__code">404</div>
      <h1 class="not-found__title">页面未找到</h1>
      <p class="not-found__message">抱歉，您访问的页面不存在或已被移除</p>
      <div class="not-found__actions">
        <BaseButton variant="default" @click="goBack">
          <svg
            class="not-found__icon"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <line x1="19" y1="12" x2="5" y2="12" />
            <polyline points="12 19 5 12 12 5" />
          </svg>
          返回上一页
        </BaseButton>
        <BaseButton variant="primary" @click="goHome">
          <svg
            class="not-found__icon"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
            <polyline points="9 22 9 12 15 12 15 22" />
          </svg>
          回到首页
        </BaseButton>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
  .not-found {
    min-height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
    background: var(--color-bg-page);
    padding: 24px;
  }

  .not-found__content {
    text-align: center;
    max-width: 400px;
  }

  .not-found__code {
    font-size: 96px;
    font-weight: 700;
    color: var(--color-primary);
    line-height: 1;
    margin-bottom: 16px;
    opacity: 0.3;
  }

  .not-found__title {
    margin: 0 0 8px;
    font-size: 24px;
    font-weight: 700;
    color: var(--color-text-primary);
  }

  .not-found__message {
    margin: 0 0 32px;
    font-size: 14px;
    color: var(--color-text-secondary);
    line-height: 1.5;
  }

  .not-found__actions {
    display: flex;
    gap: 12px;
    justify-content: center;
    flex-wrap: wrap;
  }

  .not-found__icon {
    width: 16px;
    height: 16px;
    margin-right: 6px;
  }
</style>
