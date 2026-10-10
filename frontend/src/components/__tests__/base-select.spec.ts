import { describe, it, expect, afterEach, vi } from 'vitest';
import { mount, flushPromises, type VueWrapper } from '@vue/test-utils';
import BaseSelect from '@/components/BaseSelect.vue';

// 用例对应 web/041 AC1/AC2：click-outside 判定边界——组件根内点击传播到达 document 不得关闭下拉，
// 根外点击必须关闭。背景：真实浏览器中根 div handleClick 置 isOpen=true 后 Vue 在 listener 边界
// checkpoint 完成渲染，同一 click 传播到 document 时 handleClickOutside 误判 target(=trigger) 在
// dropdown 外而立即关闭（四管理页下拉打不开根因）。jsdom 完整传播不复现该时序，
// 故采用「打开并渲染后重放 document 监听器」的同构形态构造 RED。
describe('BaseSelect - click-outside 判定边界', () => {
  let wrapper: VueWrapper | null = null;

  const options = [
    { label: '启用', value: 1 },
    { label: '停用', value: 0 },
  ];

  const spyOnDocAdd = () => vi.spyOn(document, 'addEventListener');

  const getOutsideHandler = (spy: ReturnType<typeof spyOnDocAdd>) => {
    const calls = spy.mock.calls.filter((call) => call[0] === 'click');
    const listener = calls[calls.length - 1]?.[1];
    if (typeof listener !== 'function') {
      throw new Error('未捕获到组件注册的 document click 监听器');
    }
    return listener as (event: MouseEvent) => void;
  };

  const openSelect = async () => {
    const root = wrapper!.element as HTMLElement;
    const trigger = root.querySelector('.base-select__trigger') as HTMLElement;
    trigger.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }));
    await flushPromises();
    return trigger;
  };

  afterEach(() => {
    wrapper?.unmount();
    wrapper = null;
    vi.restoreAllMocks();
  });

  // Given 下拉已打开且 dropdown 渲染就位（等价于真实浏览器 listener 边界 checkpoint 后的状态）
  // When 同一点击事件传播的后半段到达 document，target=组件根内 trigger（dropdown 之外）
  // Then 不得判为 outside：下拉保持展开
  it('组件根内点击传播到达document后下拉保持展开', async () => {
    const spy = spyOnDocAdd();
    wrapper = mount(BaseSelect, {
      props: { modelValue: null, options },
    });
    const outsideHandler = getOutsideHandler(spy);
    const trigger = await openSelect();

    expect(wrapper.classes()).toContain('base-select--open');
    expect(wrapper.find('.base-select__dropdown').exists()).toBe(true);

    outsideHandler({ target: trigger } as unknown as MouseEvent);
    await flushPromises();

    expect(wrapper.classes()).toContain('base-select--open');
    expect(wrapper.find('.base-select__dropdown').exists()).toBe(true);
  });

  // Given 下拉已打开
  // When 点击事件传播到达 document，target=组件根外元素
  // Then 判为 outside：下拉关闭
  it('组件根外点击到达document后下拉关闭', async () => {
    const spy = spyOnDocAdd();
    wrapper = mount(BaseSelect, {
      props: { modelValue: null, options },
    });
    const outsideHandler = getOutsideHandler(spy);
    await openSelect();
    expect(wrapper.find('.base-select__dropdown').exists()).toBe(true);

    const external = document.createElement('div');
    outsideHandler({ target: external } as unknown as MouseEvent);
    await flushPromises();

    expect(wrapper.classes()).not.toContain('base-select--open');
    expect(wrapper.find('.base-select__dropdown').exists()).toBe(false);
  });
});
