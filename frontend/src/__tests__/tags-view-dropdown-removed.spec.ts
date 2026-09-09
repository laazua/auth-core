import { describe, it, expect } from 'vitest';
import fs from 'node:fs';
import path from 'node:path';

const tagsViewPath = path.resolve(__dirname, '../../src/components/Layout/TagsView.vue');
const tagsViewSource = fs.readFileSync(tagsViewPath, 'utf-8');

// 处理 Vue SFC 嵌套 template：找到第一个 <template> 和最后一个 </template> 之间的内容
const templateStart = tagsViewSource.indexOf('<template>');
const templateEnd = tagsViewSource.lastIndexOf('</template>');
const template = templateStart !== -1 && templateEnd !== -1 && templateEnd > templateStart
  ? tagsViewSource.slice(templateStart + 10, templateEnd)
  : '';
const scriptMatch = tagsViewSource.match(/<script setup lang="ts">([\s\S]*?)<\/script>/);

describe('TagsViewDropdownRemovedSpec', () => {
  it('dropdownRemoved — 模板中不存在 el-dropdown/el-dropdown-menu/el-dropdown-item', () => {
    expect(template).not.toBe('');

    expect(template).not.toContain('<el-dropdown');
    expect(template).not.toContain('<el-dropdown-menu');
    expect(template).not.toContain('<el-dropdown-item');
  });

  it('interactionCodeRemoved — 脚本中移除 showMore（下拉菜单专用），保留 closeOtherTags/closeAllTags（右键菜单仍需）', () => {
    expect(scriptMatch).not.toBeNull();
    const script = scriptMatch![1];

    // showMore 状态已移除（仅下拉菜单使用）
    expect(script).not.toContain('showMore');

    // closeOtherTags 和 closeAllTags 方法保留（右键菜单通过 handleContextCommand 调用）
    expect(script).toContain('closeOtherTags');
    expect(script).toContain('closeAllTags');
  });

  it('refreshButtonRetained — 保留刷新按钮及功能', () => {
    expect(template).not.toBe('');

    // 存在刷新按钮
    expect(template).toContain('class="tags-view__refresh"');
    // 绑定点击事件
    expect(template).toContain('@click="refreshTag(activeTab)"');

    expect(scriptMatch).not.toBeNull();
    const script = scriptMatch![1];

    // refreshTag 方法保留
    expect(script).toContain('refreshTag');
  });

  it('contextMenuIntact — 右键上下文菜单完整保留四项功能', () => {
    expect(template).not.toBe('');

    // 上下文菜单容器存在
    expect(template).toContain('class="tags-view__context-menu"');
    // 四个菜单项：刷新、关闭当前、关闭其他、关闭所有
    expect(template).toContain("handleContextCommand('refresh')");
    expect(template).toContain("handleContextCommand('close')");
    expect(template).toContain("handleContextCommand('closeOther')");
    expect(template).toContain("handleContextCommand('closeAll')");

    expect(scriptMatch).not.toBeNull();
    const script = scriptMatch![1];

    // 相关状态保留
    expect(script).toContain('contextMenuVisible');
    expect(script).toContain('contextMenuTarget');
    expect(script).toContain('contextMenuStyle');
    // handleContextCommand 保留四个 case
    expect(script).toContain("case 'refresh':");
    expect(script).toContain("case 'close':");
    expect(script).toContain("case 'closeOther':");
    expect(script).toContain("case 'closeAll':");
  });
});