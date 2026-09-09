import { describe, it, expect } from 'vitest';
import fs from 'fs';
import path from 'path';

const profileViewPath = path.resolve(__dirname, '../profile/IndexView.vue');
const profileViewContent = fs.readFileSync(profileViewPath, 'utf-8');

describe('ProfileViewIconSizeSpec', () => {
  it('elButtonIconSizeFixed — ProfileView.vue 不使用 ElButton :icon prop，改用 BaseButton + slot 或显式尺寸包裹', () => {
    // 断言：不再使用 <ElButton :icon="Lock" /> 或 <ElButton :icon="Setting" />
    expect(profileViewContent).not.toMatch(/<ElButton[^>]*:\s*icon\s*=/);
    expect(profileViewContent).not.toMatch(/<ElButton[^>]*\sicon\s*=/);

    // 断言：使用 BaseButton 且图标通过 slot 显式包裹（width/height 16px）
    // 或者使用显式尺寸容器包裹图标
    const hasBaseButtonWithIconSlot = profileViewContent.includes('BaseButton') &&
      profileViewContent.includes('<template #icon>') &&
      (profileViewContent.includes('width: 16px') || profileViewContent.includes('width="16"') ||
       profileViewContent.includes('.profile__action-icon') || profileViewContent.includes('icon-size'));

    expect(hasBaseButtonWithIconSlot).toBe(true);
  });

  it('elButtonIconSizeFixed — 两个按钮（修改密码、设置）的图标均被显式尺寸包裹', () => {
    // 统计 BaseButton 中包含图标 slot 的数量
    const iconSlotMatches = profileViewContent.match(/<template #icon>/g) || [];
    expect(iconSlotMatches.length).toBeGreaterThanOrEqual(2);
  });

  it('elButtonIconSizeFixed — 移除 ElButton 导入，改用 BaseButton', () => {
    // 断言：不再从 element-plus 导入 ElButton
    expect(profileViewContent).not.toMatch(/import.*ElButton.*from.*element-plus/);

    // 断言：导入 BaseButton
    expect(profileViewContent).toMatch(/import BaseButton from ['"@\/components\/BaseButton.vue['"]/);
  });
});