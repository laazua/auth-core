<script setup lang="ts">
  import { onMounted } from 'vue';
  import { useAppStore } from '@/stores/app';

  const appStore = useAppStore();

  onMounted(() => {
    appStore.initTheme();
  });
</script>

<template>
  <div id="app">
    <router-view v-slot="{ Component }">
      <transition name="fade" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>
  </div>
</template>

<style>
  #app {
    width: 100%;
    min-height: 100vh;
  }

  .fade-enter-active,
  .fade-leave-active {
    transition: opacity 0.15s ease-out;
  }

  .fade-enter-from,
  .fade-leave-to {
    opacity: 0;
  }
</style>
