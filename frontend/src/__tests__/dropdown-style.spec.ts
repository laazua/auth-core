import { describe, it, expect, beforeAll } from 'vitest';
import { readFileSync } from 'fs';
import { join } from 'path';

// web/017：顶栏用户下拉菜单 UI 突兀/尺寸异常修复 —— 源级静态断言（与 styles.spec.ts 同款约定）
const headerPath = join(__dirname, '../components/Header.vue');
const elementPlusScssPath = join(__dirname, '../styles/element-plus.scss');
const variablesPath = join(__dirname, '../styles/variables.scss');

/** 取指定选择器声明块的正文（自首个 { 至配对的 }，正文内不再嵌套花括号） */
function ruleBody(css: string, selector: string): string {
  const start = css.indexOf(`${selector}{`);
  expect(start, `样式文件中应存在选择器 ${selector}{`).toBeGreaterThan(-1);
  const bodyStart = start + selector.length + 1;
  let depth = 1;
  let i = bodyStart;
  while (depth > 0 && i < css.length) {
    if (css[i] === '{') depth += 1;
    else if (css[i] === '}') depth -= 1;
    i += 1;
  }
  return css.slice(bodyStart, i - 1);
}

describe('DropdownThemeSpec', () => {
  let headerCss: string;
  let elementPlusScss: string;
  let variablesScss: string;

  beforeAll(() => {
    const headerFile = readFileSync(headerPath, 'utf-8');
    headerCss = headerFile.slice(headerFile.indexOf('<style'));
    elementPlusScss = readFileSync(elementPlusScssPath, 'utf-8');
    variablesScss = readFileSync(variablesPath, 'utf-8');
  });

  // AC1：下拉图标显式定宽 16px，杜绝 flex 拉伸成巨图
  it('dropdownIconExplicitSize16px - .header__dropdown-icon 显式 width/height/flex-shrink', () => {
    const body = ruleBody(headerCss, '.header__dropdown-icon ');
    expect(body).toContain('width: 16px;');
    expect(body).toContain('height: 16px;');
    expect(body).toContain('flex-shrink: 0;');
  });

  // AC2：弹层覆盖位于全局层（element-plus.scss），主题变量化，无硬编码背景色
  it('popperOverridesThemeVarsInGlobalLayer - .el-dropdown__popper 全 var() 主题变量化', () => {
    const body = ruleBody(elementPlusScss, '.el-dropdown__popper.is-light ');
    expect(body).toContain('background: var(--color-bg-overlay);');
    expect(body).toContain('border-radius: var(--color-border-radius-lg);');
    expect(body).toContain('box-shadow: var(--color-shadow-base);');
    // 覆盖层内禁止硬编码十六进制颜色（背景色必须随 data-theme 切换）
    const ownRules = elementPlusScss
      .split('\n')
      .filter((line) => !line.trim().startsWith('@use'))
      .join('\n');
    expect(ownRules).not.toMatch(/#[0-9a-fA-F]{3,6}\b/);
  });

  it('popperDarkModeSwitchesByDataTheme - variables.scss 暗色段含弹层背景变量定义', () => {
    const darkSection = variablesScss.split('[data-theme="dark"]')[1];
    expect(darkSection).toContain('--color-bg-overlay: rgba(15, 23, 42, 0.94);');
  });

  // AC3：菜单尺寸稳定 + 文字/hover/disabled/divided 分隔线主题化
  it('menuMinWidthStable - 用户菜单 min-width: 200px 不再内容挤压', () => {
    const body = ruleBody(elementPlusScss, '.el-dropdown__popper .el-dropdown-menu ');
    expect(body).toContain('min-width: 200px;');
  });

  it('menuItemTextAndHoverThemed - 菜单项文字与 hover 态使用主题变量', () => {
    const body = ruleBody(elementPlusScss, '.el-dropdown__popper .el-dropdown-menu__item ');
    expect(body).toContain('color: var(--color-text-regular);');
    // hover 与 focus 共用同一声明块（选择器列表），以末选择器定位块体
    expect(elementPlusScss).toContain(
      '.el-dropdown__popper .el-dropdown-menu__item:not(.is-disabled):hover,'
    );
    const hoverBody = ruleBody(
      elementPlusScss,
      '.el-dropdown__popper .el-dropdown-menu__item:not(.is-disabled):focus '
    );
    expect(hoverBody).toContain('background: var(--color-bg-hover);');
    expect(hoverBody).toContain('color: var(--color-primary);');
    const disabledBody = ruleBody(
      elementPlusScss,
      '.el-dropdown__popper .el-dropdown-menu__item.is-disabled '
    );
    expect(disabledBody).toContain('color: var(--color-text-secondary);');
  });

  it('menuDividedThemed - divided 分隔线颜色主题变量化', () => {
    const body = ruleBody(
      elementPlusScss,
      '.el-dropdown__popper .el-dropdown-menu__item--divided '
    );
    expect(body).toContain('border-top-color: var(--color-border-light);');
  });
});
