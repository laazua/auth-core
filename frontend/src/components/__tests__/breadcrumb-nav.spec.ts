import { describe, it, expect } from 'vitest';
import fs from 'fs';
import path from 'path';

const defaultLayoutPath = path.resolve(__dirname, '../../layouts/DefaultLayout.vue');
const breadcrumbPath = path.resolve(__dirname, '../Breadcrumb.vue');
const defaultLayoutContent = fs.readFileSync(defaultLayoutPath, 'utf-8');
const breadcrumbContent = fs.readFileSync(breadcrumbPath, 'utf-8');

const extractStyle = (content: string): string => {
  const styleMatch = content.match(/<style[^>]*>([\s\S]*?)<\/style>/);
  expect(styleMatch).not.toBeNull();
  return styleMatch![1];
};

describe('web-037 面包屑导航优化', () => {
  it('layout__content 顶距 16px 与顶栏留白', () => {
    const styleContent = extractStyle(defaultLayoutContent);
    const contentRule = styleContent.match(/\.layout__content\s*\{[^{}]*\}/);
    expect(contentRule).not.toBeNull();
    expect(contentRule![0]).toMatch(/padding:\s*16px 24px 24px/);
    expect(contentRule![0]).not.toMatch(/padding:\s*0 24px 24px/);
  });

  it('breadcrumb 与内容卡片间距 24px', () => {
    const styleContent = extractStyle(breadcrumbContent);
    const breadcrumbRule = styleContent.match(/\.breadcrumb\s*\{[^{}]*/);
    expect(breadcrumbRule).not.toBeNull();
    expect(breadcrumbRule![0]).toMatch(/margin-bottom:\s*24px/);
    expect(breadcrumbRule![0]).not.toMatch(/margin-bottom:\s*16px/);
  });

  it('el-breadcrumb__inner 横排规则与图标尺寸不回归', () => {
    const styleContent = extractStyle(breadcrumbContent);

    // 横排规则：单 :deep() 形式（串联 :deep() 在产物中会残留字面量导致失效），
    // 使 .el-breadcrumb__inner 成为 flex 容器，图标与文字同行且垂直居中
    const innerRule = styleContent.match(
      /\.breadcrumb\s*:deep\(\s*\.el-breadcrumb__inner\s*\)\s*\{[^{}]*\}/
    );
    expect(innerRule).not.toBeNull();
    expect(innerRule![0]).toMatch(/display:\s*inline-flex/);
    expect(innerRule![0]).toMatch(/align-items:\s*center/);

    // 图标尺寸与右侧间距不回归（web/025 既定规格）
    const iconRule = styleContent.match(/\.breadcrumb__icon\s*\{[^{}]*\}/);
    expect(iconRule).not.toBeNull();
    expect(iconRule![0]).toMatch(/width:\s*13px/);
    expect(iconRule![0]).toMatch(/height:\s*13px/);
    expect(iconRule![0]).toMatch(/margin-right:\s*12px/);
  });
});
