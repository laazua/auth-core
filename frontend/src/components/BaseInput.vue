<script setup lang="ts">
  import { computed, ref } from 'vue';

  interface Props {
    modelValue: string;
    type?: 'text' | 'password' | 'number' | 'email' | 'tel' | 'url';
    placeholder?: string;
    label?: string;
    error?: string;
    disabled?: boolean;
    readonly?: boolean;
    clearable?: boolean;
    showPassword?: boolean;
    maxlength?: number;
    minlength?: number;
    autocomplete?: string;
    autofocus?: boolean;
    prefixIcon?: string | object;
    suffixIcon?: string | object;
  }

  const props = withDefaults(defineProps<Props>(), {
    type: 'text',
    disabled: false,
    readonly: false,
    clearable: false,
    showPassword: false,
    autocomplete: 'off',
  });

  const emit = defineEmits<{
    'update:modelValue': [value: string];
    blur: [event: FocusEvent];
    focus: [event: FocusEvent];
    change: [value: string];
    clear: [];
  }>();

  const passwordVisible = ref(false);
  const isFocused = ref(false);
  const inputRef = ref<HTMLInputElement>();

  const inputType = computed(() => {
    if (props.type === 'password') {
      return passwordVisible.value ? 'text' : 'password';
    }
    return props.type;
  });

  const wrapperClasses = computed(() => [
    'base-input',
    { 'base-input--focused': isFocused.value },
    { 'base-input--disabled': props.disabled },
    { 'base-input--readonly': props.readonly },
    { 'base-input--error': !!props.error },
    { 'base-input--with-prefix': !!props.prefixIcon },
    { 'base-input--with-suffix': !!props.suffixIcon || props.clearable || props.showPassword },
  ]);

  const handleInput = (event: Event) => {
    const target = event.target as HTMLInputElement;
    emit('update:modelValue', target.value);
  };

  const handleBlur = (event: FocusEvent) => {
    isFocused.value = false;
    emit('blur', event);
  };

  const handleFocus = (event: FocusEvent) => {
    isFocused.value = true;
    emit('focus', event);
  };

  const handleChange = (event: Event) => {
    const target = event.target as HTMLInputElement;
    emit('change', target.value);
  };

  const handleClear = () => {
    emit('update:modelValue', '');
    emit('clear');
    inputRef.value?.focus();
  };

  const togglePassword = () => {
    passwordVisible.value = !passwordVisible.value;
    inputRef.value?.focus();
  };
</script>

<template>
  <div :class="wrapperClasses">
    <label v-if="label" class="base-input__label">{{ label }}</label>
    <div class="base-input__wrapper">
      <span v-if="prefixIcon" class="base-input__prefix">
        <component :is="prefixIcon" />
      </span>
      <input
        ref="inputRef"
        :type="inputType"
        :value="modelValue"
        :placeholder="placeholder"
        :disabled="disabled"
        :readonly="readonly"
        :maxlength="maxlength"
        :minlength="minlength"
        :autocomplete="autocomplete"
        :autofocus="autofocus"
        :aria-invalid="!!error"
        :aria-describedby="error ? 'error-message' : undefined"
        class="base-input__input"
        @input="handleInput"
        @blur="handleBlur"
        @focus="handleFocus"
        @change="handleChange"
      />
      <span
        v-if="clearable && modelValue && !disabled && !readonly"
        class="base-input__clear"
        @click="handleClear"
      >
        <svg
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <line x1="18" y1="6" x2="6" y2="18" />
          <line x1="6" y1="6" x2="18" y2="18" />
        </svg>
      </span>
      <span
        v-if="props.showPassword && props.type === 'password'"
        class="base-input__suffix"
        @click="togglePassword"
      >
        <svg
          v-if="passwordVisible"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path
            d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"
          />
          <line x1="1" y1="1" x2="23" y2="23" />
        </svg>
        <svg
          v-else
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
          <circle cx="12" cy="12" r="3" />
        </svg>
      </span>
      <span v-if="suffixIcon" class="base-input__suffix">
        <component :is="suffixIcon" />
      </span>
    </div>
    <p v-if="error" id="error-message" class="base-input__error">{{ error }}</p>
  </div>
</template>

<style scoped lang="scss">
  .base-input {
    display: flex;
    flex-direction: column;
    gap: 4px;
    width: 100%;

    &__label {
      font-size: 13px;
      font-weight: 500;
      color: var(--color-text-primary);
    }

    &__wrapper {
      position: relative;
      display: flex;
      align-items: center;
      background: var(--color-bg);
      border: 1px solid var(--color-border);
      border-radius: 4px;
      transition: all 0.15s ease-out;

      &:hover:not(.base-input--disabled):not(.base-input--error) {
        border-color: var(--color-border-dark);
      }

      &.base-input--focused {
        border-color: var(--color-primary);
        box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
      }

      &.base-input--error {
        border-color: var(--color-error);

        &:focus-within {
          box-shadow: 0 0 0 2px rgba(245, 108, 108, 0.2);
        }
      }

      &.base-input--disabled {
        background: var(--color-bg-active);
        cursor: not-allowed;

        .base-input__input {
          color: var(--color-text-disabled);
        }
      }

      &.base-input--readonly {
        background: var(--color-bg-active);
      }
    }

    &__prefix,
    &__suffix {
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 0 12px;
      color: var(--color-text-placeholder);
      pointer-events: none;

      svg {
        width: 16px;
        height: 16px;
      }
    }

    &__input {
      flex: 1;
      width: 100%;
      min-width: 0;
      padding: 8px 12px;
      font-size: 14px;
      line-height: 1.5;
      color: var(--color-text-primary);
      background: transparent;
      border: none;
      outline: none;

      &::placeholder {
        color: var(--color-text-placeholder);
      }

      &::-webkit-search-cancel-button {
        display: none;
      }
    }

    &__clear {
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 0 12px;
      color: var(--color-text-placeholder);
      cursor: pointer;
      transition: color 0.15s ease-out;

      &:hover {
        color: var(--color-text-secondary);
      }

      svg {
        width: 14px;
        height: 14px;
      }
    }

    &__suffix {
      cursor: pointer;
      pointer-events: auto;

      &:hover {
        color: var(--color-text-secondary);
      }
    }

    &__error {
      margin: 0;
      font-size: 12px;
      color: var(--color-error);
    }
  }
</style>
