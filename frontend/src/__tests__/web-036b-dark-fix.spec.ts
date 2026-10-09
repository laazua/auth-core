import { readFileSync, readdirSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

import { describe, expect, it } from 'vitest';

/**
 * web/036b 暗色模式局部白底缺陷修复——反模式源层清查（sprint-084 AC1/AC2/AC3①②）
 * RED 形态：修复前 3 处缺陷在场，断言必红；产物层断言由 smoke web-036b 用例承载。
 */
const srcRoot = join(dirname(fileURLToPath(import.meta.url)), '..');

const collectVueFiles = (dir: string): string[] => {
  const out: string[] = [];
  for (const entry of readdirSync(dir, { withFileTypes: true })) {
    if (entry.name === '__tests__' || entry.name === 'node_modules') continue;
    const full = join(dir, entry.name);
    if (entry.isDirectory()) out.push(...collectVueFiles(full));
    else if (entry.name.endsWith('.vue')) out.push(full);
  }
  return out;
};

const read = (rel: string): string => readFileSync(join(srcRoot, rel), 'utf-8');

const allVueSources = collectVueFiles(srcRoot).map((f) => ({
  path: f.slice(srcRoot.length + 1),
  content: readFileSync(f, 'utf-8'),
}));

describe('web-036b BaseTable 加载遮罩死选择器（AC1 源层）', () => {
  it("BaseTable.vue 不再含 [data-theme='dark'] & 同元素复合反模式", () => {
    expect(read('components/BaseTable.vue')).not.toContain("[data-theme='dark'] &");
  });

  it('BaseTable.vue 不再硬编码 rgba(255, 255, 255 浅底遮罩', () => {
    expect(read('components/BaseTable.vue')).not.toContain('rgba(255, 255, 255');
  });
});

describe('web-036b settings 非法 variant prop（AC2）', () => {
  const settings = read('views/settings/IndexView.vue');

  it('settings 页面零 ElButton 引用（import 与模板一并移除）', () => {
    expect(settings).not.toContain('ElButton');
  });

  it('前往修改按钮为 BaseButton variant="primary"，退出登录按钮为 variant="danger"', () => {
    expect(settings).toContain('<BaseButton variant="primary"');
    expect(settings).toContain('<BaseButton variant="danger"');
  });

  it('全仓零 <ElButton variant= 非法 prop', () => {
    const offenders = allVueSources.filter((f) => /<ElButton[^>]*variant=/.test(f.content));
    expect(offenders.map((f) => f.path)).toEqual([]);
  });
});

describe('web-036b 反模式全仓清查（AC3①②）', () => {
  it("全仓 .vue 零 [data-theme='dark'] & 同元素死模式", () => {
    const offenders = allVueSources.filter((f) => f.content.includes("[data-theme='dark'] &"));
    expect(offenders.map((f) => f.path)).toEqual([]);
  });

  it('全仓非测试 .vue 零硬编码 rgba(255, 255, 255 浅底', () => {
    const offenders = allVueSources.filter((f) => f.content.includes('rgba(255, 255, 255'));
    expect(offenders.map((f) => f.path)).toEqual([]);
  });
});
