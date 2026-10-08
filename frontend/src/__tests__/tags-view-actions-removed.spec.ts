import { describe, it, expect } from 'vitest';
import fs from 'node:fs';
import path from 'node:path';

const srcRoot = path.resolve(__dirname, '../..');
const tagsViewPath = path.join(srcRoot, 'src/components/Layout/TagsView.vue');

// 源码扫描面：src/ 下 .vue/.ts/.scss，排除测试文件（AC1 口径=非测试源码，含 components.d.ts）
function collectSourceFiles(): string[] {
  const entries = fs.readdirSync(path.join(srcRoot, 'src'), { recursive: true }) as string[];
  return entries
    .map((entry) => path.join(srcRoot, 'src', entry))
    .filter((full) => fs.statSync(full).isFile())
    .filter((full) => {
      const rel = path.relative(srcRoot, full);
      return /\.(vue|ts|scss)$/.test(rel) && !rel.includes('__tests__') && !rel.endsWith('.spec.ts');
    });
}

function filesContaining(keyword: string): string[] {
  return collectSourceFiles().filter((full) => fs.readFileSync(full, 'utf-8').includes(keyword));
}

// web/023 源码层收口：020 期「操作区移除」断言语义升级为「整体移除」删除态断言
describe('TagsViewActionsRemovedSpec', () => {
  it('tagsViewComponentFileRemoved — TagsView.vue 组件文件已整体移除', () => {
    // Arrange: 删除态断言目标=组件文件路径
    // Act
    const exists = fs.existsSync(tagsViewPath);

    // Assert: 组件文件不存在（回归面=重新加入组件）
    expect(exists).toBe(false);
  });

  it('sourceNoTagsViewIdentifierReference — 源码零 TagsView 标识引用（含 components.d.ts，排除测试）', () => {
    // Arrange: 扫描非测试源码中的标识引用
    // Act
    const hits = filesContaining('TagsView');

    // Assert: 命中清单为空（回归面=任意源文件重新引用组件标识）
    expect(hits).toEqual([]);
  });

  it('sourceNoTagsViewClassLeftover — 源码零 tags-view 类名残留（组件样式随移除清除）', () => {
    // Arrange: 扫描非测试源码中的样式类名残留
    // Act
    const hits = filesContaining('tags-view');

    // Assert: 命中清单为空（回归面=标签栏样式残留在其他源文件）
    expect(hits).toEqual([]);
  });
});
