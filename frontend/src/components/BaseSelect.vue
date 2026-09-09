<script setup lang="ts">
  import { computed, ref, onMounted, onUnmounted, nextTick } from 'vue';

  interface Option {
    label: string;
    value: string | number;
    disabled?: boolean;
  }

  interface Props {
    modelValue: string | number | null | (string | number)[];
    options: Option[];
    placeholder?: string;
    label?: string;
    error?: string;
    disabled?: boolean;
    clearable?: boolean;
    filterable?: boolean;
    multiple?: boolean;
  }

  const props = withDefaults(defineProps<Props>(), {
    placeholder: '请选择',
    disabled: false,
    clearable: false,
    filterable: false,
    multiple: false,
  });

  const emit = defineEmits<{
    'update:modelValue': [value: string | number | null | (string | number)[]];
    change: [value: string | number | null | (string | number)[]];
    blur: [event: FocusEvent];
    focus: [event: FocusEvent];
    clear: [];
  }>();

  const isOpen = ref(false);
  const searchQuery = ref('');
  const inputRef = ref<HTMLInputElement>();
  const dropdownRef = ref<HTMLDivElement>();

  const filteredOptions = computed(() => {
    if (!props.filterable || !searchQuery.value) return props.options;
    const query = searchQuery.value.toLowerCase();
    return props.options.filter((opt) => opt.label.toLowerCase().includes(query));
  });

  const selectedOptions = computed(() => {
    if (props.multiple && Array.isArray(props.modelValue)) {
      return props.options.filter((opt) => (props.modelValue as (string | number)[]).includes(opt.value));
    }
    if (!props.multiple && props.modelValue !== null) {
      return props.options.find((opt) => opt.value === props.modelValue);
    }
    return props.multiple ? [] : null;
  });

  const displayText = computed(() => {
    if (props.multiple && Array.isArray(selectedOptions.value)) {
      return selectedOptions.value.map((opt) => opt.label).join(', ');
    }
    if (selectedOptions.value && !Array.isArray(selectedOptions.value)) {
      return selectedOptions.value.label;
    }
    return '';
  });

  const wrapperClasses = computed(() => [
    'base-select',
    { 'base-select--open': isOpen.value },
    { 'base-select--disabled': props.disabled },
    { 'base-select--error': !!props.error },
    { 'base-select--multiple': props.multiple },
  ]);

  const handleClick = () => {
    if (!props.disabled) {
      isOpen.value = !isOpen.value;
      if (isOpen.value) {
        searchQuery.value = '';
        nextTick(() => inputRef.value?.focus());
      }
    }
  };

  const handleSelect = (option: Option) => {
    if (option.disabled) return;

    if (props.multiple) {
      const current = (Array.isArray(props.modelValue) ? [...props.modelValue] : []) as (string | number)[];
      const index = current.indexOf(option.value);
      if (index > -1) {
        current.splice(index, 1);
      } else {
        current.push(option.value);
      }
      emit('update:modelValue', current);
      emit('change', current);
    } else {
      emit('update:modelValue', option.value);
      emit('change', option.value);
      isOpen.value = false;
    }
  };

  const handleClear = (event: Event) => {
    event.stopPropagation();
    const clearedValue = props.multiple ? [] : null;
    emit('update:modelValue', clearedValue);
    emit('change', clearedValue);
    emit('clear');
    isOpen.value = false;
  };

  const handleKeydown = (event: KeyboardEvent) => {
    if (event.key === 'Escape') {
      isOpen.value = false;
    } else if (event.key === 'Enter' && isOpen.value) {
      const firstEnabled = filteredOptions.value.find((opt) => !opt.disabled);
      if (firstEnabled) handleSelect(firstEnabled);
    } else if (event.key === 'ArrowDown') {
      event.preventDefault();
      if (!isOpen.value) isOpen.value = true;
    } else if (event.key === 'ArrowUp') {
      event.preventDefault();
    }
  };

  const handleClickOutside = (event: MouseEvent) => {
    if (dropdownRef.value && !dropdownRef.value.contains(event.target as Node)) {
      isOpen.value = false;
    }
  };

  onMounted(() => {
    document.addEventListener('click', handleClickOutside);
  });

  onUnmounted(() => {
    document.removeEventListener('click', handleClickOutside);
  });
</script>

<template>
  <div :class="wrapperClasses" @click="handleClick">
    <label v-if="label" class="base-select__label">{{ label }}</label>
    <div
      class="base-select__trigger"
      :class="{ 'base-select__trigger--has-value': displayText }"
      @keydown="handleKeydown"
    >
      <span v-if="!displayText" class="base-select__placeholder">{{ placeholder }}</span>
      <span v-else class="base-select__value">{{ displayText }}</span>
      <span
        v-if="clearable && modelValue !== null && modelValue !== '' && !disabled"
        class="base-select__clear"
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
      <svg
        class="base-select__arrow"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
      >
        <polyline points="6 9 12 15 18 9" />
      </svg>
      <input
        v-if="filterable && isOpen"
        ref="inputRef"
        v-model="searchQuery"
        type="text"
        :placeholder="placeholder"
        class="base-select__search"
        @click.stop
        @input="() => {}"
      />
    </div>
    <div v-if="isOpen" ref="dropdownRef" class="base-select__dropdown">
      <div v-if="filterable" class="base-select__search-wrapper">
        <input
          ref="inputRef"
          v-model="searchQuery"
          type="text"
          :placeholder="placeholder"
          class="base-select__search-input"
          @click.stop
        />
      </div>
      <div class="base-select__options" role="listbox">
        <div
          v-for="option in filteredOptions"
          :key="option.value"
          :class="[
            'base-select__option',
            {
              'base-select__option--selected': props.multiple
                ? (Array.isArray(modelValue) ? modelValue : [])?.includes(option.value)
                : modelValue === option.value,
              'base-select__option--disabled': option.disabled,
            },
          ]"
          role="option"
          :aria-selected="
            props.multiple
              ? (Array.isArray(modelValue) ? modelValue : [])?.includes(option.value)
              : modelValue === option.value
          "
          @click="handleSelect(option)"
        >
          {{ option.label }}
        </div>
        <div v-if="filteredOptions.length === 0" class="base-select__empty">
          {{ filterable ? '无匹配选项' : '暂无数据' }}
        </div>
      </div>
    </div>
    <p v-if="error" class="base-select__error">{{ error }}</p>
  </div>
</template>

<style scoped lang="scss">
  .base-select {
    display: flex;
    flex-direction: column;
    gap: 4px;
    width: 100%;
    position: relative;

    &__label {
      font-size: 13px;
      font-weight: 500;
      color: var(--color-text-primary);
    }

    &__trigger {
      display: flex;
      align-items: center;
      justify-content: space-between;
      height: 36px;
      padding: 0 12px;
      background: var(--color-bg);
      border: 1px solid var(--color-border);
      border-radius: 4px;
      cursor: pointer;
      transition: all 0.15s ease-out;
      position: relative;

      &:hover:not(.base-select--disabled):not(.base-select--error) {
        border-color: var(--color-border-dark);
      }

      &.base-select--focused {
        border-color: var(--color-primary);
        box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
      }
    }

    &--disabled .base-select__trigger {
      background: var(--color-bg-active);
      cursor: not-allowed;
      color: var(--color-text-disabled);
    }

    &--error .base-select__trigger {
      border-color: var(--color-error);

      &:focus-within {
        box-shadow: 0 0 0 2px rgba(245, 108, 108, 0.2);
      }
    }

    &__placeholder {
      color: var(--color-text-placeholder);
    }

    &__value {
      color: var(--color-text-primary);
      flex: 1;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    &__clear {
      display: flex;
      align-items: center;
      justify-content: center;
      margin-left: 8px;
      padding: 2px;
      color: var(--color-text-placeholder);
      cursor: pointer;
      border-radius: 2px;
      transition: all 0.15s ease-out;

      &:hover {
        color: var(--color-text-secondary);
        background: var(--color-bg-hover);
      }

      svg {
        width: 14px;
        height: 14px;
      }
    }

    &__arrow {
      margin-left: 8px;
      width: 16px;
      height: 16px;
      color: var(--color-text-secondary);
      transition: transform 0.15s ease-out;

      .base-select--open & {
        transform: rotate(180deg);
      }
    }

    &__search {
      flex: 1;
      margin-left: 8px;
      padding: 4px 8px;
      font-size: 13px;
      border: none;
      outline: none;
      background: transparent;
      min-width: 100px;
    }

    &__dropdown {
      position: absolute;
      top: calc(100% + 4px);
      left: 0;
      right: 0;
      z-index: 100;
      background: var(--color-bg);
      border: 1px solid var(--color-border);
      border-radius: 4px;
      box-shadow: var(--color-shadow-base);
      overflow: hidden;
      max-height: 280px;
    }

    &__search-wrapper {
      padding: 8px;
      border-bottom: 1px solid var(--color-border-light);
    }

    &__search-input {
      width: 100%;
      padding: 8px 12px;
      font-size: 13px;
      border: 1px solid var(--color-border);
      border-radius: 4px;
      outline: none;
      transition: all 0.15s ease-out;

      &:focus {
        border-color: var(--color-primary);
        box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
      }
    }

    &__options {
      max-height: 240px;
      overflow-y: auto;
    }

    &__option {
      padding: 8px 12px;
      font-size: 13px;
      color: var(--color-text-primary);
      cursor: pointer;
      transition: background 0.1s ease-out;

      &:hover:not(.base-select__option--disabled) {
        background: var(--color-bg-hover);
      }

      &--selected {
        color: var(--color-primary);
        background: rgba(64, 158, 255, 0.08);
        font-weight: 500;
      }

      &--disabled {
        color: var(--color-text-disabled);
        cursor: not-allowed;
      }
    }

    &__empty {
      padding: 16px;
      text-align: center;
      font-size: 13px;
      color: var(--color-text-placeholder);
    }

    &__error {
      margin: 0;
      font-size: 12px;
      color: var(--color-error);
    }
  }
</style>
