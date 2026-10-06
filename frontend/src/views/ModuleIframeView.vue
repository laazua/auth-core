<script setup lang="ts">
import { ref, computed } from 'vue';
import { useRoute } from 'vue-router';

const route = useRoute();
const code = computed(() => String(route.params.code ?? ''));
const src = computed(() => `/api/v1/gateway/${code.value}/`);
const loading = ref(true);
</script>

<template>
  <div class="module-iframe">
    <div v-if="loading" class="module-iframe__loading">模块加载中…</div>
    <iframe
      class="module-iframe__frame"
      :class="{ 'module-iframe__frame--hidden': loading }"
      :src="src"
      :title="`模块 ${code}`"
      @load="loading = false"
    />
  </div>
</template>

<style scoped>
.module-iframe {
  position: relative;
  width: 100%;
  height: calc(100vh - 140px);
}

.module-iframe__loading {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--el-text-color-secondary, #909399);
  font-size: 14px;
}

.module-iframe__frame {
  width: 100%;
  height: 100%;
  border: 0;
}

.module-iframe__frame--hidden {
  visibility: hidden;
}
</style>
