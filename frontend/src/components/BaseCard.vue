<script setup lang="ts">
  import { computed } from 'vue';

  interface Props {
    title?: string;
    subtitle?: string;
    bordered?: boolean;
    hoverable?: boolean;
    shadow?: 'always' | 'hover' | 'never';
    padding?: string | number;
    headerClass?: string;
    bodyClass?: string;
    footerClass?: string;
  }

  const props = withDefaults(defineProps<Props>(), {
    bordered: true,
    hoverable: false,
    shadow: 'hover',
    padding: '16px',
  });

  const cardClasses = computed(() => [
    'base-card',
    { 'base-card--bordered': props.bordered },
    { 'base-card--hoverable': props.hoverable },
    { 'base-card--shadow-always': props.shadow === 'always' },
    { 'base-card--shadow-hover': props.shadow === 'hover' },
    { 'base-card--shadow-never': props.shadow === 'never' },
  ]);

  const headerClasses = computed(() => ['base-card__header', props.headerClass]);

  const bodyClasses = computed(() => ['base-card__body', props.bodyClass]);

  const footerClasses = computed(() => ['base-card__footer', props.footerClass]);
</script>

<template>
  <div
    :class="cardClasses"
    :style="{ '--card-padding': padding + (typeof padding === 'number' ? 'px' : '') }"
  >
    <div v-if="title || $slots.header" :class="headerClasses">
      <slot name="header">
        <div class="base-card__header-content">
          <h3 v-if="title" class="base-card__title">{{ title }}</h3>
          <p v-if="subtitle" class="base-card__subtitle">{{ subtitle }}</p>
        </div>
      </slot>
    </div>
    <div :class="bodyClasses">
      <slot />
    </div>
    <div v-if="$slots.footer" :class="footerClasses">
      <slot name="footer" />
    </div>
  </div>
</template>

<style scoped lang="scss">
  .base-card {
    background: var(--color-bg);
    border-radius: 8px;
    overflow: hidden;

    &--bordered {
      border: 1px solid var(--color-border-light);
    }

    &--hoverable {
      transition: all 0.15s ease-out;

      &:hover {
        box-shadow: var(--color-shadow-hover);
        transform: translateY(-2px);
      }
    }

    &--shadow-always {
      box-shadow: var(--color-shadow-base);
    }

    &--shadow-hover {
      transition: box-shadow 0.15s ease-out;

      &:hover {
        box-shadow: var(--color-shadow-hover);
      }
    }

    &--shadow-never {
      box-shadow: none;
    }

    &__header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 0 var(--card-padding);
      height: 56px;
      border-bottom: 1px solid var(--color-border-light);
      background: var(--color-bg-page);

      .base-card--bordered & {
        border-bottom: 1px solid var(--color-border);
      }
    }

    &__header-content {
      display: flex;
      flex-direction: column;
      gap: 2px;
    }

    &__title {
      margin: 0;
      font-size: 16px;
      font-weight: 600;
      color: var(--color-text-primary);
      line-height: 1.3;
    }

    &__subtitle {
      margin: 0;
      font-size: 13px;
      color: var(--color-text-secondary);
      line-height: 1.4;
    }

    &__body {
      padding: var(--card-padding);
    }

    &__footer {
      display: flex;
      align-items: center;
      justify-content: flex-end;
      gap: 12px;
      padding: 12px var(--card-padding);
      border-top: 1px solid var(--color-border-light);
      background: var(--color-bg-page);
    }
  }
</style>
