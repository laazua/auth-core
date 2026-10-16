import { describe, it, expect } from 'vitest';
import { mount } from '@vue/test-utils';
import BaseInput from '@/components/BaseInput.vue';

/**
 * web/045：clearable 点 × 必须触发 change（值变更语义），
 * 使四管理页搜索框（@change 重查接线）在清空后自动刷新列表。
 */
describe('BaseInput clearable 清除交互', () => {
  it('点 × 后 emit change 恰一次且值为空串（清除即值变更，消费方须重查）', async () => {
    // Given：可清除输入框已有值
    const wrapper = mount(BaseInput, { props: { modelValue: 'abc', clearable: true } });

    // When：点击清除按钮
    await wrapper.find('.base-input__clear').trigger('click');

    // Then：change 事件恰一次且值为 ''
    expect(wrapper.emitted('change')).toEqual([['']]);
  });

  it('点 × 后保留 update:modelValue 与 clear 双 emit 契约', async () => {
    // Given：可清除输入框已有值
    const wrapper = mount(BaseInput, { props: { modelValue: 'abc', clearable: true } });

    // When：点击清除按钮
    await wrapper.find('.base-input__clear').trigger('click');

    // Then：既有两条契约零回归
    expect(wrapper.emitted('update:modelValue')).toEqual([['']]);
    expect(wrapper.emitted('clear')).toHaveLength(1);
  });

  it('点 × 后 input 保持焦点（连续输入体验）', async () => {
    // Given：挂载到文档的可清除输入框
    const wrapper = mount(BaseInput, {
      props: { modelValue: 'abc', clearable: true },
      attachTo: document.body,
    });

    // When：点击清除按钮
    await wrapper.find('.base-input__clear').trigger('click');

    // Then：焦点仍在输入框
    expect(document.activeElement).toBe(wrapper.find('input').element);
    wrapper.unmount();
  });

  it('不可清除或值为空时不渲染清除按钮（v-if 契约回归）', () => {
    // Given/When：默认不可清除、以及 clearable 但值为空
    const notClearable = mount(BaseInput, { props: { modelValue: 'abc' } });
    const emptyValue = mount(BaseInput, { props: { modelValue: '', clearable: true } });

    // Then：两种情况均无清除按钮
    expect(notClearable.find('.base-input__clear').exists()).toBe(false);
    expect(emptyValue.find('.base-input__clear').exists()).toBe(false);
  });
});
