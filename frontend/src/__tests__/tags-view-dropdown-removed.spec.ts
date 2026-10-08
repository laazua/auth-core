import { describe, it, expect } from 'vitest';
import fs from 'node:fs';
import path from 'node:path';

const srcRoot = path.resolve(__dirname, '../..');
const dtsPath = path.join(srcRoot, 'src/components.d.ts');
const layoutPath = path.join(srcRoot, 'src/layouts/DefaultLayout.vue');

// 源码扫描面：src/ 下全部文件（文件名与内容两级扫描，排除测试文件）
function collectSourceFiles(): string[] {
  const entries = fs.readdirSync(path.join(srcRoot, 'src'), { recursive: true }) as string[];
  return entries
    .map((entry) => path.join(srcRoot, 'src', entry))
    .filter((full) => fs.statSync(full).isFile())
    .filter((full) => {
      const rel = path.relative(srcRoot, full);
      return !rel.includes('__tests__') && !rel.endsWith('.spec.ts');
    });
}

// web/023 源码层收口：022 期「下拉移除」断言语义升级为「整体移除」删除态断言 + 保留面守卫
describe('TagsViewDropdownRemovedSpec', () => {
  it('componentsDtsNoTagsViewDeclaration — components.d.ts 无 TagsView 自动声明（TRACKED 文件防残留）', () => {
    // Arrange: 自动声明文件在库内，需随组件移除同步清零
    // Act
    const dts = fs.readFileSync(dtsPath, 'utf-8');

    // Assert: 声明文件不含 TagsView（回归面=unplugin 残留或重新生成）
    expect(dts).not.toContain('TagsView');
  });

  it('noTagsViewSfcFileAnywhere — src 目录树不存在任意 TagsView*.vue（防换路径复加）', () => {
    // Arrange: 文件名级扫描不限定既知路径
    // Act
    const hits = collectSourceFiles().filter((full) =>
      /^TagsView.*\.vue$/.test(path.basename(full))
    );

    // Assert: 命中清单为空（回归面=组件以其他路径重新加入）
    expect(hits).toEqual([]);
  });

  it('sourceNoRefreshTagSymbol — 源码零 refreshTag 符号（操作区刷新入口随组件移除）', () => {
    // Arrange: 内容级扫描 020 期操作区入口符号
    // Act
    const hits = collectSourceFiles()
      .filter((full) => /\.(vue|ts)$/.test(full))
      .filter((full) => fs.readFileSync(full, 'utf-8').includes('refreshTag'));

    // Assert: 命中清单为空（回归面=孤立函数残留在其他源文件）
    expect(hits).toEqual([]);
  });

  it('layoutContentHeightCompensated — DefaultLayout contentStyle minHeight 为 calc(100vh - 60px)（标签栏移除后高度补偿守卫）', () => {
    // Arrange: 保留面守卫——标签栏移除后内容区高度补偿不可回退（与 smoke web-023 同源断言）
    // Act
    const layout = fs.readFileSync(layoutPath, 'utf-8');

    // Assert: 布局保持 60px 顶栏高度补偿
    expect(layout).toContain('calc(100vh - 60px)');
  });
});
