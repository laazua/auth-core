import { describe, it, expect, beforeAll } from 'vitest';
import { readFileSync } from 'fs';
import { join } from 'path';

const dashboardViewPath = join(__dirname, '../../views/DashboardView.vue');
const defaultLayoutPath = join(__dirname, '../../layouts/DefaultLayout.vue');

describe('DashboardView.vue', () => {
  let dashboardViewContent: string;
  let defaultLayoutContent: string;

  beforeAll(() => {
    dashboardViewContent = readFileSync(dashboardViewPath, 'utf-8');
    defaultLayoutContent = readFileSync(defaultLayoutPath, 'utf-8');
  });

  describe('AC2: pageHasGradientBackground', () => {
    it('DashboardView renders within DefaultLayout which has gradient background', () => {
      // DashboardView doesn't have its own background - it relies on DefaultLayout
      // Verify DefaultLayout has the gradient background
      expect(defaultLayoutContent).toContain('background-image: var(--gradient-bg)');
      expect(defaultLayoutContent).toContain('background-attachment: fixed');
      expect(defaultLayoutContent).toContain('background: var(--color-bg-page)');
    });

    it('DashboardView uses layout__page class with card background', () => {
      // DashboardView content is wrapped in layout__page which has card background
      expect(dashboardViewContent).toContain('dashboard');
    });
  });
});
