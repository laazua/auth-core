<script setup lang="ts">
import { computed, ref, onMounted, onBeforeUnmount } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useAppStore } from '@/stores/app';
import { ElTabs, ElTabPane, ElDropdown, ElDropdownMenu, ElDropdownItem, ElTooltip } from 'element-plus';
import {
  Close,
  CloseBold,
  RefreshRight,
  More,
} from '@element-plus/icons-vue';

const router = useRouter();
const route = useRoute();
const appStore = useAppStore();

const activeTab = ref<string>(route.fullPath);
const showMore = ref(false);
const contextMenuVisible = ref(false);
const contextMenuStyle = ref<{ top: number; left: number }>({ top: 0, left: 0 });
const contextMenuTarget = ref<string>('');

const tags = computed(() => appStore.tagsViewList);
const normalTags = computed(() => tags.value.filter((t) => !t.affix));

const handleTabClick = (tab: any) => {
  const fullPath = tab.paneName;
  appStore.setActiveTag(fullPath);
  router.push(fullPath);
};

const closeTag = (fullPath: string, event?: Event) => {
  if (event) event.stopPropagation();
  appStore.removeTag(fullPath);
};

const closeOtherTags = (fullPath: string) => {
  appStore.removeOtherTags(fullPath);
};

const closeAllTags = () => {
  appStore.removeAllTags();
};

const refreshTag = (fullPath: string) => {
  router.replace('/redirect' + fullPath);
};

const handleContextCommand = (command: string) => {
  const fullPath = contextMenuTarget.value;
  switch (command) {
    case 'refresh':
      refreshTag(fullPath);
      break;
    case 'close':
      closeTag(fullPath);
      break;
    case 'closeOther':
      closeOtherTags(fullPath);
      break;
    case 'closeAll':
      closeAllTags();
      break;
  }
  contextMenuVisible.value = false;
};

const handleKeydown = (event: KeyboardEvent) => {
  if (event.ctrlKey && event.key === 'Tab') {
    event.preventDefault();
    const normalTagsList = normalTags.value;
    if (normalTagsList.length <= 1) return;

    const currentIndex = normalTagsList.findIndex((t) => t.fullPath === activeTab.value);
    const nextIndex = event.shiftKey
      ? (currentIndex - 1 + normalTagsList.length) % normalTagsList.length
      : (currentIndex + 1) % normalTagsList.length;

    router.push(normalTagsList[nextIndex].fullPath);
  }

  if (event.key === 'Escape') {
    const currentTag = tags.value.find((t) => t.fullPath === activeTab.value);
    if (currentTag && !currentTag.affix) {
      closeTag(activeTab.value);
    }
  }
};

onMounted(() => {
  appStore.restoreTags();
  activeTab.value = route.fullPath;
  appStore.setActiveTag(route.fullPath);
  document.addEventListener('keydown', handleKeydown);
  document.addEventListener('click', () => {
    contextMenuVisible.value = false;
  });
});

onBeforeUnmount(() => {
  document.removeEventListener('keydown', handleKeydown);
});

import { onBeforeRouteLeave } from 'vue-router';
onBeforeRouteLeave((to) => {
  activeTab.value = to.fullPath;
});

const scrollToActive = () => {
  const tabsContainer = document.querySelector('.tags-view__nav');
  const activeTabEl = document.querySelector('.tags-view__tab.is-active');
  if (tabsContainer && activeTabEl) {
    const containerRect = tabsContainer.getBoundingClientRect();
    const tabRect = activeTabEl.getBoundingClientRect();
    if (tabRect.left < containerRect.left || tabRect.right > containerRect.right) {
      activeTabEl.scrollIntoView({ behavior: 'smooth', inline: 'center', block: 'nearest' });
    }
  }
};
</script>

<template>
  <div class="tags-view">
    <div class="tags-view__container">
      <el-tabs
        v-model="activeTab"
        type="card"
        class="tags-view__tabs"
        @tab-click="handleTabClick"
        @click="scrollToActive"
      >
        <el-tab-pane
          v-for="tag in tags"
          :key="tag.fullPath"
          :name="tag.fullPath"
          :closable="!tag.affix"
          :lazy="true"
        >
          <template #label>
            <span class="tags-view__tab" :class="{ 'is-active': activeTab === tag.fullPath }">
              <component v-if="tag.icon" :is="tag.icon" class="tags-view__icon" />
              <span class="tags-view__title">{{ tag.title }}</span>
              <el-tooltip
                v-if="!tag.affix"
                content="关闭"
                placement="top"
                :disabled="tag.affix"
              >
                <Close
                  class="tags-view__close"
                  @click.stop="closeTag(tag.fullPath, $event)"
                />
              </el-tooltip>
            </span>
          </template>
        </el-tab-pane>
      </el-tabs>

      <div class="tags-view__actions">
        <el-dropdown trigger="click">
          <button
            class="tags-view__more"
            :class="{ 'is-active': showMore }"
            @click="showMore = !showMore"
            aria-label="更多操作"
          >
            <More />
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="closeOtherTags(activeTab)">
                <CloseBold class="tags-view__dropdown-icon" />
                <span>关闭其他标签</span>
              </el-dropdown-item>
              <el-dropdown-item @click="closeAllTags">
                <Close class="tags-view__dropdown-icon" />
                <span>关闭所有标签</span>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <button
          class="tags-view__refresh"
          @click="refreshTag(activeTab)"
          aria-label="刷新当前页面"
        >
          <RefreshRight />
        </button>
      </div>
    </div>

    <div
      v-if="contextMenuVisible"
      class="tags-view__context-menu"
      :style="contextMenuStyle"
      @click.stop
    >
      <div class="tags-view__context-item" @click="handleContextCommand('refresh')">
        <RefreshRight class="tags-view__context-icon" />
        <span>刷新</span>
      </div>
      <div class="tags-view__context-divider" />
      <div class="tags-view__context-item" @click="handleContextCommand('close')">
        <Close class="tags-view__context-icon" />
        <span>关闭当前</span>
      </div>
      <div class="tags-view__context-item" @click="handleContextCommand('closeOther')">
        <CloseBold class="tags-view__context-icon" />
        <span>关闭其他</span>
      </div>
      <div class="tags-view__context-item" @click="handleContextCommand('closeAll')">
        <Close class="tags-view__context-icon" />
        <span>关闭所有</span>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.tags-view {
  height: 40px;
  background: var(--color-bg);
  border-bottom: 1px solid var(--color-border-light);
  position: sticky;
  top: 60px;
  z-index: var(--z-index-sticky);
  overflow: hidden;
}

.tags-view__container {
  display: flex;
  align-items: center;
  height: 100%;
  padding: 0 12px;
  overflow: hidden;
}

.tags-view__tabs {
  flex: 1;
  min-width: 0;

  :deep(.el-tabs__nav) {
    display: flex;
    align-items: center;
    height: 100%;
    border-bottom: none;
    padding: 0;
    margin: 0;
    overflow-x: auto;
    scrollbar-width: none;
    -ms-overflow-style: none;

    &::-webkit-scrollbar {
      display: none;
    }

    :deep(.el-tabs__item) {
      height: 32px;
      line-height: 32px;
      padding: 0 12px;
      margin: 0 4px;
      border: none;
      border-radius: 6px;
      background: transparent;
      color: var(--color-text-regular);
      font-size: 13px;
      transition: all 0.1s ease-out;

      &:hover {
        background: var(--color-bg-hover);
        color: var(--color-primary);
      }

      &.is-active {
        background: rgba(64, 158, 255, 0.1);
        color: var(--color-primary);
        font-weight: 500;
      }

      &.is-closable {
        padding-right: 28px;
      }
    }

    :deep(.el-tabs__active-bar) {
      display: none;
    }
  }
}

.tags-view__tab {
  display: flex;
  align-items: center;
  gap: 6px;
  max-width: 160px;
  overflow: hidden;

  .tags-view__icon {
    flex-shrink: 0;
    font-size: 13px;
    color: var(--color-text-secondary);
  }

  .tags-view__title {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .tags-view__close {
    flex-shrink: 0;
    width: 14px;
    height: 14px;
    font-size: 12px;
    color: var(--color-text-placeholder);
    cursor: pointer;
    transition: color 0.1s ease-out;

    &:hover {
      color: var(--color-error);
    }
  }
}

.tags-view__actions {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-left: 8px;
  flex-shrink: 0;
}

.tags-view__more,
.tags-view__refresh {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 6px;
  color: var(--color-text-secondary);
  background: transparent;
  transition: all 0.1s ease-out;

  &:hover {
    background: var(--color-bg-hover);
    color: var(--color-primary);
  }

  svg {
    width: 16px;
    height: 16px;
  }
}

.tags-view__more.is-active {
  background: var(--color-bg-hover);
  color: var(--color-primary);
}

.tags-view__context-menu {
  position: fixed;
  min-width: 140px;
  padding: 6px 0;
  background: var(--color-bg);
  border: 1px solid var(--color-border-light);
  border-radius: 6px;
  box-shadow: var(--color-shadow-heavy);
  z-index: var(--z-index-popper);
  animation: contextMenuFadeIn 0.1s ease-out;

  @keyframes contextMenuFadeIn {
    from {
      opacity: 0;
      transform: translateY(-4px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }
}

.tags-view__context-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  font-size: 13px;
  color: var(--color-text-regular);
  cursor: pointer;
  transition: all 0.1s ease-out;

  &:hover {
    background: var(--color-bg-hover);
    color: var(--color-primary);
  }

  .tags-view__context-icon {
    flex-shrink: 0;
    width: 14px;
    height: 14px;
    font-size: 13px;
  }
}

.tags-view__context-divider {
  height: 1px;
  margin: 4px 0;
  background: var(--color-border-light);
}

.tags-view__dropdown-icon {
  margin-right: 8px;
  font-size: 13px;
  color: var(--color-text-secondary);
}
</style>