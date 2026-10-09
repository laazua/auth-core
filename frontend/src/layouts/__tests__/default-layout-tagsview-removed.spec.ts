import { describe, it, expect } from 'vitest';
import fs from 'node:fs';
import path from 'node:path';

const defaultLayoutPath = path.resolve(__dirname, '../DefaultLayout.vue');
const defaultLayoutSource = fs.readFileSync(defaultLayoutPath, 'utf-8');

const templateStart = defaultLayoutSource.indexOf('<template>');
const templateEnd = defaultLayoutSource.lastIndexOf('</template>');
const template =
  templateStart !== -1 && templateEnd !== -1 && templateEnd > templateStart
    ? defaultLayoutSource.slice(templateStart + 10, templateEnd)
    : '';
const scriptMatch = defaultLayoutSource.match(/<script setup lang="ts">([\s\S]*?)<\/script>/);

describe('DefaultLayoutTagsViewRemovedSpec', () => {
  it('tagsViewImportRemoved — DefaultLayout.vue 不再导入和使用 TagsView', () => {
    expect(scriptMatch).not.toBeNull();
    const script = scriptMatch![1];

    // 不导入 TagsView
    expect(script).not.toContain("import TagsView from '@/components/Layout/TagsView.vue'");

    expect(template).not.toBe('');

    // 模板中不使用 TagsView 组件
    expect(template).not.toContain('<TagsView />');
    expect(template).not.toContain('<TagsView>');
  });

  it('contentStyleMinHeightAdjusted — contentStyle minHeight 调整为 calc(100vh - 60px)', () => {
    expect(scriptMatch).not.toBeNull();
    const script = scriptMatch![1];

    // contentStyle 计算属性中 minHeight 为 calc(100vh - 60px)
    expect(script).toContain('calc(100vh - 60px)');
    // 不再包含旧的 calc(100vh - 100px)
    expect(script).not.toContain('calc(100vh - 100px)');
  });

  it('breadcrumbRetained — 保留 Breadcrumb 组件导入与使用', () => {
    expect(scriptMatch).not.toBeNull();
    const script = scriptMatch![1];

    // 保留 Breadcrumb 导入
    expect(script).toContain("import Breadcrumb from '@/components/Breadcrumb.vue'");

    expect(template).not.toBe('');

    // 模板中保留 Breadcrumb 使用
    expect(template).toContain('<Breadcrumb :routes="route.matched" />');
  });
});
