import { readFileSync, readdirSync } from 'node:fs';
import { resolve, join } from 'node:path';
import { describe, it, expect } from 'vitest';

const sidebarVuePath = resolve(__dirname, '../../src/components/Sidebar.vue');
const sidebarVueContent = readFileSync(sidebarVuePath, 'utf-8');

// 辅助：提取完整的嵌套规则块（按大括号配平）
function extractRuleBlock(content: string, startMarker: string): string | null {
  const startIdx = content.indexOf(startMarker);
  if (startIdx === -1) return null;
  let braceCount = 0;
  let inBlock = false;
  for (let i = startIdx; i < content.length; i++) {
    const ch = content[i];
    if (ch === '{') {
      braceCount++;
      inBlock = true;
    } else if (ch === '}') {
      braceCount--;
      if (inBlock && braceCount === 0) {
        return content.slice(startIdx, i + 1);
      }
    }
  }
  return null;
}

// 辅助：统计「选择器含 :deep( 且规则体内又含 :deep(」的嵌套规则数
// （@vue/compiler-sfc 仅转换首个 :deep()，后续以字面选择器残留致规则失效——web/036c3 治理对象）
function countNestedDeepRules(content: string): number {
  const stripped = content.replace(/\/\*[\s\S]*?\*\//g, '').replace(/\/\/[^\n]*/g, '');
  let count = 0;
  const re = /([^{}]+)\{/g;
  let m: RegExpExecArray | null;
  while ((m = re.exec(stripped)) !== null) {
    if (!m[1].includes(':deep(')) continue;
    let depth = 1;
    let i = re.lastIndex;
    while (i < stripped.length && depth > 0) {
      const ch = stripped[i];
      if (ch === '{') depth++;
      else if (ch === '}') depth--;
      i++;
    }
    if (stripped.slice(re.lastIndex, i - 1).includes(':deep(')) count++;
  }
  return count;
}

describe('SidebarIconSizeSpec', () => {
  it('menuItemIconExplicitSize16px — :deep(.el-menu .el-menu-item) 规则块含 width: 16px; height: 16px;', () => {
    // web/036c3 扁平化同步：嵌套形态已并入单 :deep() 完整后代路径（原 marker=:deep(.el-menu-item)）
    const ruleBlock = extractRuleBlock(sidebarVueContent, ':deep(.el-menu .el-menu-item)');
    expect(ruleBlock).not.toBeNull();
    expect(ruleBlock).toContain('width: 16px');
    expect(ruleBlock).toContain('height: 16px');
  });

  it('subMenuTitleIconExplicitSize16px — :deep(.el-menu .el-sub-menu .el-sub-menu__title) 规则块含 width: 16px; height: 16px;', () => {
    // web/036c3 扁平化同步：三层嵌套已并入单 :deep() 完整后代路径（原 marker=:deep(.el-sub-menu)）
    const ruleBlock = extractRuleBlock(
      sidebarVueContent,
      ':deep(.el-menu .el-sub-menu .el-sub-menu__title)'
    );
    expect(ruleBlock).not.toBeNull();
    expect(ruleBlock).toContain('width: 16px');
    expect(ruleBlock).toContain('height: 16px');
  });

  it('fallbackIconExplicitSize16px — 顶层 .sidebar__icon 含 width: 16px; height: 16px;', () => {
    // web/011c 格式治理同步：vueIndentScriptAndStyle 使顶层选择器带 2 空格缩进，标记随格式口径更新（语义不变=顶层规则块）
    const ruleBlock = extractRuleBlock(sidebarVueContent, '\n  .sidebar__icon');
    expect(ruleBlock).not.toBeNull();
    expect(ruleBlock).toContain('width: 16px');
    expect(ruleBlock).toContain('height: 16px');
  });
});

describe('ScopedDeepRuleStructure（web/036c3 嵌套 :deep 编译失效治理）', () => {
  it('全仓 .vue scoped 样式无「规则体内再含 :deep(」的嵌套规则（编译器仅转首个，残留字面致死规则）', () => {
    const srcRoot = resolve(__dirname, '..');
    const files = readdirSync(srcRoot, { recursive: true }) as string[];
    const violations: string[] = [];
    for (const rel of files) {
      if (!rel.endsWith('.vue')) continue;
      const content = readFileSync(join(srcRoot, rel), 'utf-8');
      const n = countNestedDeepRules(content);
      if (n > 0) violations.push(`${rel}: ${n} 处`);
    }
    expect(violations).toEqual([]);
  });
});
