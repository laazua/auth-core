import { describe, it, expect } from 'vitest';
import { mount } from '@vue/test-utils';
import BaseButton from '@/components/BaseButton.vue';
import BaseInput from '@/components/BaseInput.vue';
import BaseCard from '@/components/BaseCard.vue';

describe('BaseButton', () => {
  it('renders correctly with default props', () => {
    const wrapper = mount(BaseButton, {
      slots: { default: 'Click me' },
    });

    expect(wrapper.find('button').exists()).toBe(true);
    expect(wrapper.text()).toBe('Click me');
    expect(wrapper.classes()).toContain('base-button');
  });

  it('applies variant classes', () => {
    const wrapper = mount(BaseButton, {
      props: { variant: 'primary' },
      slots: { default: 'Primary' },
    });

    expect(wrapper.classes()).toContain('base-button--primary');
  });

  it('applies size classes', () => {
    const wrapper = mount(BaseButton, {
      props: { size: 'large' },
      slots: { default: 'Large' },
    });

    expect(wrapper.classes()).toContain('base-button--large');
  });

  it('shows loading state', () => {
    const wrapper = mount(BaseButton, {
      props: { loading: true },
      slots: { default: 'Loading' },
    });

    expect(wrapper.find('.base-button__loading').exists()).toBe(true);
    expect(wrapper.find('button').attributes('disabled')).toBeDefined();
  });

  it('emits click event', async () => {
    const wrapper = mount(BaseButton, {
      slots: { default: 'Click' },
    });

    await wrapper.find('button').trigger('click');

    expect(wrapper.emitted('click')).toBeTruthy();
  });

  it('does not emit click when disabled', async () => {
    const wrapper = mount(BaseButton, {
      props: { disabled: true },
      slots: { default: 'Disabled' },
    });

    await wrapper.find('button').trigger('click');

    expect(wrapper.emitted('click')).toBeFalsy();
  });
});

describe('BaseInput', () => {
  it('renders input element', () => {
    const wrapper = mount(BaseInput, {
      props: { modelValue: 'test' },
    });

    expect(wrapper.find('input').exists()).toBe(true);
    expect(wrapper.find('input').element.value).toBe('test');
  });

  it('shows label when provided', () => {
    const wrapper = mount(BaseInput, {
      props: { label: 'Username', modelValue: '' },
    });

    expect(wrapper.find('label').text()).toBe('Username');
  });

  it('shows error message when provided', () => {
    const wrapper = mount(BaseInput, {
      props: { modelValue: '', error: 'This field is required' },
    });

    expect(wrapper.find('.base-input__error').text()).toBe('This field is required');
  });

  it('applies disabled state', () => {
    const wrapper = mount(BaseInput, {
      props: { modelValue: '', disabled: true },
    });

    expect(wrapper.find('input').attributes('disabled')).toBeDefined();
  });

  it('emits update:modelValue on input', async () => {
    const wrapper = mount(BaseInput, {
      props: { modelValue: '' },
    });

    await wrapper.find('input').setValue('new value');

    expect(wrapper.emitted('update:modelValue')?.[0]).toEqual(['new value']);
  });

  it('shows clear button when clearable and has value', async () => {
    const wrapper = mount(BaseInput, {
      props: { modelValue: 'test', clearable: true },
    });

    expect(wrapper.find('.base-input__clear').exists()).toBe(true);

    await wrapper.find('.base-input__clear').trigger('click');

    expect(wrapper.emitted('update:modelValue')?.[0]).toEqual(['']);
  });
});

describe('BaseCard', () => {
  it('renders card with header and content', () => {
    const wrapper = mount(BaseCard, {
      props: { title: 'Card Title' },
      slots: { default: 'Card content' },
    });

    expect(wrapper.find('.base-card').exists()).toBe(true);
    const header = wrapper.find('.base-card__header');
    expect(header.exists()).toBe(true);
    expect(header.find('.base-card__title').text()).toBe('Card Title');
    expect(wrapper.find('.base-card__body').text()).toBe('Card content');
  });

  it('renders without header when title not provided', () => {
    const wrapper = mount(BaseCard, {
      slots: { default: 'No header' },
    });

    expect(wrapper.find('.base-card__header').exists()).toBe(false);
    expect(wrapper.find('.base-card__body').text()).toBe('No header');
  });

  it('applies bordered class', () => {
    const wrapper = mount(BaseCard, {
      props: { bordered: true },
      slots: { default: 'Bordered' },
    });

    expect(wrapper.classes()).toContain('base-card--bordered');
  });

  it('applies hoverable class', () => {
    const wrapper = mount(BaseCard, {
      props: { hoverable: true },
      slots: { default: 'Hoverable' },
    });

    expect(wrapper.classes()).toContain('base-card--hoverable');
  });
});
