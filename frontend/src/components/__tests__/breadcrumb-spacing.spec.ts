import { describe, it, expect } from 'vitest';
import fs from 'fs';
import path from 'path';

const breadcrumbPath = path.resolve(__dirname, '../Breadcrumb.vue');
const breadcrumbContent = fs.readFileSync(breadcrumbPath, 'utf-8');

describe('BreadcrumbSpacingSpec', () => {
  it('iconTextGap — Breadcrumb.vue .breadcrumb__icon margin-right 为 12px', () => {
    // 解析样式部分，查找 .breadcrumb__icon 的 margin-right 值
    const styleMatch = breadcrumbContent.match(/<style[^>]*>([\s\S]*?)<\/style>/);
    expect(styleMatch).not.toBeNull();

    const styleContent = styleMatch![1];
    // 查找 .breadcrumb__icon { ... margin-right: 12px ... }
    const iconMarginMatch = styleContent.match(/\.breadcrumb__icon\s*\{[\s\S]*?margin-right\s*:\s*12px/);
    expect(iconMarginMatch).not.toBeNull();
  });
});