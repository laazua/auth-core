import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
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

describe('SidebarIconSizeSpec', () => {
  it('menuItemIconExplicitSize16px — .sidebar__el-menu :deep(.el-menu-item) .sidebar__icon 含 width: 16px; height: 16px;', () => {
    const ruleBlock = extractRuleBlock(sidebarVueContent, ':deep(.el-menu-item)');
    expect(ruleBlock).not.toBeNull();
    expect(ruleBlock).toContain('width: 16px');
    expect(ruleBlock).toContain('height: 16px');
  });

  it('subMenuTitleIconExplicitSize16px — .sidebar__el-menu :deep(.el-sub-menu) .sidebar__icon 含 width: 16px; height: 16px;', () => {
    const ruleBlock = extractRuleBlock(sidebarVueContent, ':deep(.el-sub-menu)');
    expect(ruleBlock).not.toBeNull();
    expect(ruleBlock).toContain('width: 16px');
    expect(ruleBlock).toContain('height: 16px');
  });

  it('fallbackIconExplicitSize16px — 顶层 .sidebar__icon 含 width: 16px; height: 16px;', () => {
    const ruleBlock = extractRuleBlock(sidebarVueContent, '\n.sidebar__icon');
    expect(ruleBlock).not.toBeNull();
    expect(ruleBlock).toContain('width: 16px');
    expect(ruleBlock).toContain('height: 16px');
  });
});