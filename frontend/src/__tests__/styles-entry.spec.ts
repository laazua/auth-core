import { describe, it, expect, beforeAll } from 'vitest';
import { readFileSync, readdirSync, statSync } from 'fs';
import { join } from 'path';

const stylesDir = join(__dirname, '../../src/styles');
const globalCssPath = join(stylesDir, 'global.css');
const variablesPath = join(stylesDir, 'variables.scss');
const srcDir = join(__dirname, '../../src');

const readAllSrcTexts = (dir: string): string[] => {
  const texts: string[] = [];
  const walk = (current: string): void => {
    for (const entry of readdirSync(current)) {
      const full = join(current, entry);
      if (statSync(full).isDirectory()) {
        walk(full);
      } else if (
        /\.(ts|vue|scss|css)$/.test(entry) &&
        // 本测试文件的断言字符串含 'variables.css'，非模块引用，排除自身
        entry !== 'styles-entry.spec.ts'
      ) {
        texts.push(readFileSync(full, 'utf-8'));
      }
    }
  };
  walk(dir);
  return texts;
};

describe('StylesEntrySpec', () => {
  let globalCss: string;

  beforeAll(() => {
    globalCss = readFileSync(globalCssPath, 'utf-8');
  });

  it('globalEntryImportsVariablesBeforeReset - global.css 顶部 @import 区在 reset.css 之前接入 variables.scss (AC1)', () => {
    expect(globalCss).toContain("@import './variables.scss';");
    const variablesIdx = globalCss.indexOf('./variables.scss');
    const resetIdx = globalCss.indexOf('./reset.css');
    expect(variablesIdx).toBeGreaterThanOrEqual(0);
    expect(resetIdx).toBeGreaterThanOrEqual(0);
    expect(variablesIdx).toBeLessThan(resetIdx);
  });

  it('noDanglingReferenceToMissingVariablesCss - src 内不存在对缺失文件 variables.css 的引用 (AC2)', () => {
    const allTexts = readAllSrcTexts(srcDir);
    for (const [index, text] of allTexts.entries()) {
      expect(text, `src 文件 #${index} 引用了不存在的 variables.css`).not.toContain(
        'variables.css'
      );
    }
  });

  it('themeVariablesClosedLoopReachable - variables.scss 经入口链可达且亮/暗两套变量定义完整 (AC3)', () => {
    // 入口 @import 链可达性：global.css 直接 @import 的相对目标须包含 variables.scss
    const importMatches = [...globalCss.matchAll(/@import\s+['"]([^'"]+)['"]/g)].map(
      (match) => match[1]
    );
    expect(importMatches).toContain('./variables.scss');

    const variablesCss = readFileSync(variablesPath, 'utf-8');
    // 亮色 :root 段定义 --gradient-bg（linear-gradient）与 --color-primary
    const lightSection = variablesCss.split('[data-theme="dark"]')[0];
    expect(lightSection).toContain(':root');
    expect(lightSection).toContain('--gradient-bg: linear-gradient');
    expect(lightSection).toContain('--color-primary:');
    // 暗色 [data-theme="dark"] 段定义 --gradient-bg
    const darkSection = variablesCss.split('[data-theme="dark"]')[1];
    expect(darkSection).toContain('--gradient-bg:');
  });
});
