import { describe, it, expect } from 'vitest';
import fs from 'fs';
import path from 'path';
import { globSync } from 'glob';

const viewsDir = path.resolve(__dirname, '../../views');

describe('MainAreaIconSizeAuditSpec', () => {
  const viewFiles = globSync('**/*.vue', { cwd: viewsDir, absolute: true })
    .filter(f => !f.includes('LoginView.vue') && !f.includes('__tests__'));

  it('allViewsIconSizeControlled — 所有视图（除 LoginView）无直接使用 <Component :is="Icon" /> 渲染 EP 图标且无显式尺寸包裹', () => {
    const violations: string[] = [];

    for (const file of viewFiles) {
      const content = fs.readFileSync(file, 'utf-8');
      const relativePath = path.relative(viewsDir, file);

      // 检查：直接使用 <component :is="..." /> 渲染 EP 图标且无显式尺寸类
      // 排除：Breadcrumb.vue、Header.vue、Sidebar.vue、TagsView.vue 等组件内部使用
      // 只检查 views/ 下的页面级组件
      const componentIsIconPattern = /<component\s+:is\s*=\s*["']?(?:User|Lock|Setting|Monitor|Moon|Sunny|SwitchButton|Key|Cpu|Bell|FullScreen|Fold|Expand|More|Close|CloseBold|RefreshRight|Search)["']?/g;
      const matches = content.match(componentIsIconPattern);

      if (matches) {
        // 检查是否有显式尺寸包裹（如 class 包含 icon-size、width/height、font-size 配合尺寸）
        for (const match of matches) {
          const idx = content.indexOf(match);
          const contextBefore = content.substring(Math.max(0, idx - 200), idx);
          const contextAfter = content.substring(idx, idx + 200);
          const context = contextBefore + contextAfter;

          // 判断是否有显式尺寸控制
          const hasExplicitSize = context.includes('width: 16px') ||
            context.includes('height: 16px') ||
            context.includes('width="16"') ||
            context.includes('height="16"') ||
            context.includes('font-size: 16px') ||
            context.includes('.icon-size') ||
            context.includes('.sidebar__icon') ||
            context.includes('.header__dropdown-icon') ||
            context.includes('.tags-view__icon') ||
            context.includes('.tags-view__context-icon') ||
            context.includes('.tags-view__dropdown-icon') ||
            context.includes('.breadcrumb__icon') ||
            context.includes('.profile__header-icon');

          if (!hasExplicitSize) {
            violations.push(`${relativePath}: ${match.trim()}`);
          }
        }
      }

      // 检查：ElButton :icon prop 且无显式尺寸包裹
      const elButtonIconPattern = /<ElButton[^>]*:\s*icon\s*=\s*["']?(?:User|Lock|Setting|Monitor|Moon|Sunny|SwitchButton|Key|Cpu|Bell|FullScreen|Fold|Expand|More|Close|CloseBold|RefreshRight|Search)["']?/g;
      const elButtonMatches = content.match(elButtonIconPattern);

      if (elButtonMatches) {
        for (const match of elButtonMatches) {
          // 如果使用的是 BaseButton + slot 方式则跳过
          const idx = content.indexOf(match);
          const contextBefore = content.substring(Math.max(0, idx - 300), idx);
          const contextAfter = content.substring(idx, idx + 100);
          const context = contextBefore + contextAfter;

          // 检查是否在 BaseButton 中且使用 slot
          const isBaseButtonWithSlot = content.includes('BaseButton') &&
            (context.includes('<template #icon>') || context.includes('v-slot:icon'));

          if (!isBaseButtonWithSlot) {
            violations.push(`${relativePath}: ${match.trim()}`);
          }
        }
      }
    }

    if (violations.length > 0) {
      console.log('图标尺寸违规列表:', violations);
    }
    expect(violations).toEqual([]);
  });
});