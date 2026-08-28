<script setup lang="ts">
  import type { ButtonProps } from 'element-plus';
  import { computed } from 'vue';

  interface Props extends Omit<ButtonProps, 'type'> {
    variant?: 'primary' | 'success' | 'warning' | 'danger' | 'info' | 'default';
    loading?: boolean;
  }

  const props = withDefaults(defineProps<Props>(), {
    variant: 'primary',
    size: 'default',
    loading: false,
    disabled: false,
  });

  const emit = defineEmits<{
    click: [event: MouseEvent];
  }>();

  const baseClass = computed(() => [
    'base-button',
    `base-button--${props.variant}`,
    `base-button--${props.size}`,
    { 'base-button--loading': props.loading, 'base-button--disabled': props.disabled },
  ]);

  const handleClick = (event: MouseEvent) => {
    if (!props.loading && !props.disabled) {
      emit('click', event);
    }
  };
</script>

<template>
  <button :class="baseClass" :disabled="disabled || loading" type="button" @click="handleClick">
    <span v-if="loading" class="base-button__loading">
      <svg
        class="base-button__spinner"
        viewBox="0 0 24 24"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
      >
        <circle
          cx="12"
          cy="12"
          r="10"
          stroke="currentColor"
          stroke-width="3"
          stroke-linecap="round"
          stroke-dasharray="31.4 31.4"
        >
          <animateTransform
            attributeName="transform"
            type="rotate"
            from="0 12 12"
            to="360 12 12"
            dur="1s"
            repeatCount="indefinite"
          />
        </circle>
      </svg>
    </span>
    <slot />
  </button>
</template>

<style scoped lang="scss">
  .base-button {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    font-weight: 500;
    border-radius: 4px;
    transition: all 0.15s ease-out;
    border: 1px solid transparent;
    white-space: nowrap;
    user-select: none;

    &:disabled,
    &.base-button--disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }

    &.base-button--loading {
      color: transparent !important;
    }

    &__loading {
      position: absolute;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    &__spinner {
      width: 16px;
      height: 16px;
      animation: spin 1s linear infinite;
    }

    @keyframes spin {
      to {
        transform: rotate(360deg);
      }
    }

    // Variants
    &--primary {
      background: var(--color-primary);
      color: var(--color-text-inverse);
      border-color: var(--color-primary);

      &:hover:not(:disabled) {
        background: var(--color-primary-light);
        border-color: var(--color-primary-light);
      }

      &:active:not(:disabled) {
        background: var(--color-primary-dark);
        border-color: var(--color-primary-dark);
      }
    }

    &--success {
      background: var(--color-success);
      color: var(--color-text-inverse);
      border-color: var(--color-success);

      &:hover:not(:disabled) {
        background: var(--color-success-light);
        border-color: var(--color-success-light);
      }
    }

    &--warning {
      background: var(--color-warning);
      color: var(--color-text-inverse);
      border-color: var(--color-warning);

      &:hover:not(:disabled) {
        background: var(--color-warning-light);
        border-color: var(--color-warning-light);
      }
    }

    &--danger {
      background: var(--color-error);
      color: var(--color-text-inverse);
      border-color: var(--color-error);

      &:hover:not(:disabled) {
        background: var(--color-error-light);
        border-color: var(--color-error-light);
      }
    }

    &--info {
      background: var(--color-info);
      color: var(--color-text-inverse);
      border-color: var(--color-info);

      &:hover:not(:disabled) {
        background: var(--color-info-light);
        border-color: var(--color-info-light);
      }
    }

    &--default {
      background: var(--color-bg);
      color: var(--color-text-primary);
      border-color: var(--color-border);

      &:hover:not(:disabled) {
        background: var(--color-bg-hover);
        border-color: var(--color-border-dark);
      }
    }

    // Sizes
    &--small {
      padding: 4px 12px;
      font-size: 12px;
      height: 28px;
    }

    &--default {
      padding: 8px 16px;
      font-size: 14px;
      height: 36px;
    }

    &--large {
      padding: 12px 24px;
      font-size: 16px;
      height: 44px;
    }
  }
</style>
