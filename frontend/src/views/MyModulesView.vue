<script setup lang="ts">
  import { ref, onMounted } from 'vue';
  import { useRouter } from 'vue-router';
  import { moduleApi, type ModuleAccessible } from '@/api/module';

  const router = useRouter();
  const modules = ref<ModuleAccessible[]>([]);
  const loading = ref(true);
  const loadError = ref(false);

  onMounted(async () => {
    loading.value = true;
    loadError.value = false;
    try {
      const res = await moduleApi.getAccessibles();
      modules.value = res.data ?? [];
    } catch (error) {
      console.error('[MyModules] 加载可访问模块失败:', error);
      loadError.value = true;
    } finally {
      loading.value = false;
    }
  });

  const enterModule = (code: string) => {
    void router.push(`/workspace/module/${code}`);
  };
</script>

<template>
  <div class="my-modules">
    <div class="my-modules__header">
      <h2 class="my-modules__title">我的模块</h2>
      <p class="my-modules__subtitle">点击卡片进入对应模块工作区</p>
    </div>

    <div v-if="loading" class="my-modules__loading">加载中…</div>

    <div v-else-if="loadError" class="my-modules__error">加载失败，请稍后重试</div>

    <div v-else-if="modules.length === 0" class="my-modules__empty">暂无可访问模块</div>

    <div v-else class="my-modules__grid">
      <div
        v-for="m in modules"
        :key="m.id"
        class="module-card"
        role="button"
        tabindex="0"
        @click="enterModule(m.code)"
        @keyup.enter="enterModule(m.code)"
      >
        <div class="module-card__name">{{ m.name }}</div>
        <div class="module-card__code">{{ m.code }}</div>
        <div class="module-card__desc">{{ m.description }}</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
  .my-modules {
    padding: 24px;
  }

  .my-modules__header {
    margin-bottom: 24px;
  }

  .my-modules__title {
    margin: 0 0 8px;
    font-size: 20px;
    font-weight: 600;
  }

  .my-modules__subtitle {
    margin: 0;
    color: var(--el-text-color-secondary, #909399);
    font-size: 14px;
  }

  .my-modules__loading,
  .my-modules__error,
  .my-modules__empty {
    padding: 60px 0;
    text-align: center;
    color: var(--el-text-color-secondary, #909399);
    font-size: 14px;
  }

  .my-modules__grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
    gap: 16px;
  }

  .module-card {
    padding: 20px;
    border: 1px solid var(--el-border-color, #dcdfe6);
    border-radius: 8px;
    background: var(--el-bg-color, #fff);
    cursor: pointer;
    transition:
      box-shadow 0.2s,
      border-color 0.2s;
  }

  .module-card:hover {
    border-color: var(--el-color-primary, #409eff);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  }

  .module-card:focus-visible {
    outline: 2px solid var(--el-color-primary, #409eff);
    outline-offset: 2px;
  }

  .module-card__name {
    font-size: 16px;
    font-weight: 600;
    margin-bottom: 4px;
  }

  .module-card__code {
    font-size: 12px;
    color: var(--el-text-color-secondary, #909399);
    font-family: monospace;
    margin-bottom: 8px;
  }

  .module-card__desc {
    font-size: 13px;
    color: var(--el-text-color-regular, #606266);
    line-height: 1.5;
  }
</style>
