import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, it, expect } from 'vitest';

const mainTsPath = resolve(__dirname, '../../src/main.ts');
const sidebarVuePath = resolve(__dirname, '../../src/components/Sidebar.vue');
const useMenuTsPath = resolve(__dirname, '../../src/composables/useMenu.ts');

const mainTsContent = readFileSync(mainTsPath, 'utf-8');
const sidebarVueContent = readFileSync(sidebarVuePath, 'utf-8');
const useMenuTsContent = readFileSync(useMenuTsPath, 'utf-8');

describe('SidebarIconSpec', () => {
  it('mainRegistersAllElementPlusIcons — main.ts 批量导入并注册 @element-plus/icons-vue 所有导出为全局组件', () => {
    expect(mainTsContent).toContain("import * as ElementPlusIconsVue from '@element-plus/icons-vue'");
    expect(mainTsContent).toContain('Object.entries(ElementPlusIconsVue)');
    expect(mainTsContent).toContain('app.component(');
  });

  it('sidebarMenuIconsRenderViaGlobalRegistry — Sidebar.vue 模板与 useMenu.ts 配置保持字符串引用，依赖全局注册生效', () => {
    expect(sidebarVueContent).toContain('<component :is="item.icon" class="sidebar__icon" />');
    expect(useMenuTsContent).toContain("icon: 'Monitor'");
    expect(useMenuTsContent).toContain("icon: 'Setting'");
    expect(useMenuTsContent).toContain("icon: 'User'");
    expect(useMenuTsContent).toContain("icon: 'UserFilled'");
    expect(useMenuTsContent).toContain("icon: 'Lock'");
    expect(useMenuTsContent).toContain("icon: 'Grid'");
  });

  it('iconSizeAndThemeColorCompliance — Sidebar.vue scoped 样式中 .sidebar__icon 显式尺寸 16px、颜色主题变量化、激活态变色', () => {
    expect(sidebarVueContent).toMatch(/\.sidebar__icon\s*\{[^}]*font-size:\s*16px/);
    expect(sidebarVueContent).toMatch(/\.sidebar__icon\s*\{[^}]*color:\s*var\(--color-text-placeholder\)/);
    // .is-active 内嵌套 .sidebar__icon { color: var(--color-primary); }
    expect(sidebarVueContent).toMatch(/&\.is-active\s*\{[^}]*\.sidebar__icon\s*\{[^}]*color:\s*var\(--color-primary\)/);
  });
});