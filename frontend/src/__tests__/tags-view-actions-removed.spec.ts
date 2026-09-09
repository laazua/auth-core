import { describe, it, expect } from 'vitest';
import fs from 'node:fs';
import path from 'node:path';

const tagsViewPath = path.resolve(__dirname, '../../src/components/Layout/TagsView.vue');
const tagsViewSource = fs.readFileSync(tagsViewPath, 'utf-8');

const templateStart = tagsViewSource.indexOf('<template>');
const templateEnd = tagsViewSource.lastIndexOf('</template>');
const template = templateStart !== -1 && templateEnd !== -1 && templateEnd > templateStart
  ? tagsViewSource.slice(templateStart + 10, templateEnd)
  : '';
const scriptMatch = tagsViewSource.match(/<script setup lang="ts">([\s\S]*?)<\/script>/);

describe('TagsViewActionsRemovedSpec', () => {
  it('actionsAreaRemoved — 模板中不存在 .tags-view__actions 与 .tags-view__refresh', () => {
    expect(template).not.toBe('');

    expect(template).not.toContain('class="tags-view__actions"');
    expect(template).not.toContain('class="tags-view__refresh"');
  });

  it('actionCodeRemoved — 脚本中移除 refreshTag 方法，保留 RefreshRight 导入供右键菜单使用', () => {
    expect(scriptMatch).not.toBeNull();
    const script = scriptMatch![1];

    // refreshTag 方法已移除（操作区域按钮专用，右键菜单通过 handleContextCommand 调用）
    expect(script).not.toContain('refreshTag');

    // RefreshRight 图标导入保留（右键菜单仍需）
    expect(script).toContain('RefreshRight');
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