<script setup lang="ts">
  import { computed, onMounted, onUnmounted, ref } from 'vue';
  import { useRoute } from 'vue-router';
  import { useAppStore } from '@/stores/app';
  import { useTheme } from '@/composables/useTheme';
  import Sidebar from '@/components/Sidebar.vue';
  import Header from '@/components/Header.vue';
  import Breadcrumb from '@/components/Breadcrumb.vue';

  const route = useRoute();
  const appStore = useAppStore();
  const { initTheme } = useTheme();

  const isMobile = ref(false);

  const contentStyle = computed(() => ({
    marginLeft: appStore.sidebarCollapsed ? '64px' : '260px',
    minHeight: `calc(100vh - 60px)`,
    transition: 'margin-left 0.15s ease-out',
  }));

  const handleResize = () => {
    isMobile.value = window.innerWidth < 768;
    appStore.setDevice(isMobile.value ? 'mobile' : 'desktop');
    if (isMobile.value) {
      appStore.setSidebarCollapsed(true);
    }
  };

  onMounted(() => {
    handleResize();
    window.addEventListener('resize', handleResize);
    initTheme();
  });

  onUnmounted(() => {
    window.removeEventListener('resize', handleResize);
  });
</script>

<template>
  <div class="layout">
    <Sidebar :collapsed="appStore.sidebarCollapsed" @toggle="appStore.toggleSidebar" />
    <div class="layout__main" :style="contentStyle">
      <Header :sidebar-collapsed="appStore.sidebarCollapsed" @toggle="appStore.toggleSidebar" />
      <main class="layout__content">
        <Breadcrumb :routes="route.matched" />
        <div class="layout__page">
          <router-view v-slot="{ Component }">
            <transition name="fade" mode="out-in">
              <component :is="Component" />
            </transition>
          </router-view>
        </div>
      </main>
    </div>
  </div>
</template>

<style scoped lang="scss">
  .layout {
    display: flex;
    min-height: 100vh;
    background: var(--color-bg-page);
    background-image: var(--gradient-bg);
    background-attachment: fixed;
  }

  .layout__main {
    flex: 1;
    display: flex;
    flex-direction: column;
    min-width: 0;
    overflow: hidden;
  }

  .layout__content {
    flex: 1;
    overflow: auto;
    padding: 24px 28px;
    position: relative;
    z-index: 1;
  }

  .layout__page {
    min-height: 100%;
    background: var(--color-bg-content);
    border-radius: var(--color-border-radius-card);
    box-shadow: var(--color-shadow-light);
    border: 1px solid var(--color-border-light);
    padding: 24px;
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
