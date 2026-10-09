import { describe, it, expect } from 'vitest';
import fs from 'fs';
import path from 'path';

const profileViewPath = path.resolve(__dirname, '../profile/IndexView.vue');
const profileViewContent = fs.readFileSync(profileViewPath, 'utf-8');

describe('ProfileViewSpacingSpec', () => {
  it('headerIconTitleGap — ProfileView.vue .profile__header gap 为 24px', () => {
    // 解析样式部分，查找 .profile__header 的 gap 值
    const styleMatch = profileViewContent.match(/<style[^>]*>([\s\S]*?)<\/style>/);
    expect(styleMatch).not.toBeNull();

    const styleContent = styleMatch![1];
    // 查找 .profile__header { ... gap: 24px ... }
    const headerGapMatch = styleContent.match(/\.profile__header\s*\{[\s\S]*?gap\s*:\s*24px/);
    expect(headerGapMatch).not.toBeNull();
  });

  it('actionBtnIconTextGap — ProfileView.vue .profile__action-btn :deep(.base-button) gap 为 16px', () => {
    // 解析样式部分
    const styleMatch = profileViewContent.match(/<style[^>]*>([\s\S]*?)<\/style>/);
    expect(styleMatch).not.toBeNull();

    const styleContent = styleMatch![1];
    // 查找 .profile__action-btn :deep(.base-button) { ... gap: 16px ... }
    const actionBtnGapMatch = styleContent.match(
      /\.profile__action-btn\s*\{[\s\S]*?:deep\(\(?\.base-button\)?\)\s*\{[\s\S]*?gap\s*:\s*16px/
    );
    expect(actionBtnGapMatch).not.toBeNull();
  });
});
