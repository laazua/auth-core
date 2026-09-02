import { describe, it, expect, beforeAll } from 'vitest';
import { readFileSync } from 'fs';
import { join } from 'path';

const defaultLayoutPath = join(__dirname, '../DefaultLayout.vue');

describe('DefaultLayout.vue', () => {
  let defaultLayoutContent: string;

  beforeAll(() => {
    defaultLayoutContent = readFileSync(defaultLayoutPath, 'utf-8');
  });

  describe('AC1: hasGradientBackground', () => {
    it('.layout class includes background-image: var(--gradient-bg)', () => {
      expect(defaultLayoutContent).toContain('background-image: var(--gradient-bg)');
    });

    it('.layout class includes background-attachment: fixed', () => {
      expect(defaultLayoutContent).toContain('background-attachment: fixed');
    });

    it('.layout class includes background: var(--color-bg-page)', () => {
      expect(defaultLayoutContent).toContain('background: var(--color-bg-page)');
    });
  });
});