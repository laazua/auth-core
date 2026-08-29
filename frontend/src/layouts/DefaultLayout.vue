<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAppStore } from '@/stores/app';
import Sidebar from '@/components/Sidebar.vue';
import Header from '@/components/Header.vue';
import Breadcrumb from '@/components/Breadcrumb.vue';
import TagsView from '@/components/Layout/TagsView.vue';

const route = useRoute();
const router = useRouter();
const appStore = useAppStore();

const isMobile = ref(false);

const contentStyle = computed(() => ({
  marginLeft: appStore.sidebarCollapsed ? '64px' : '260px',
  minHeight: `calc(100vh - 100px)`,
  transition: 'margin-left 0.15s ease-out',
}));

const handleResize = () => {
  isMobile.value = window.innerWidth < 768;
  appStore.setDevice(isMobile.value ? 'mobile' : 'desktop');
  if (isMobile.value) {
    appStore.setSidebarCollapsed(true);
    appStore.setSidebarOpened(false);
  }
};

const handleRouteChange = () => {
  const fullPath = route.fullPath;
  appStore.setActiveTag(fullPath);
};

onMounted(() => {
  handleResize();
  window.addEventListener('resize', handleResize);
  appStore.initTheme();
  appStore.restoreTags();

  if (typeof router.afterEach === 'function') {
    router.afterEach(handleRouteChange);
  }
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
});
</script>

<template>
  <div class="layout">
    <Sidebar
      :collapsed="appStore.sidebarCollapsed"
      :opened="appStore.sidebarOpened"
      @toggle="appStore.toggleSidebar"
      @open-change="appStore.setSidebarOpened"
    />
    <div class="layout__main" :style="contentStyle">
      <Header
        :sidebar-collapsed="appStore.sidebarCollapsed"
        :sidebar-opened="appStore.sidebarOpened"
        @toggle="appStore.toggleSidebar"
        @open-change="appStore.setSidebarOpened"
      />
      <main class="layout__content">
        <TagsView />
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
  padding: 0 24px 24px;
  min-width: 0;
}

.layout__page {
  min-height: 100%;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.15s ease-out;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

@media (max-width: 767.98px) {
  .layout__main {
    margin-left: 0;

    &:style="contentStyle" {
      margin-left: 0 !important;
    }
  }
}
</style>