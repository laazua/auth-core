import { describe, it, expect, beforeAll } from 'vitest';
import { readFileSync } from 'fs';
import { join } from 'path';

const variablesPath = join(__dirname, '../../src/styles/variables.scss');

describe('SidebarStyleSpec', () => {
  let variablesContent: string;

  beforeAll(() => {
    variablesContent = readFileSync(variablesPath, 'utf-8');
  });

  it('sidebarBackgroundDifferentiation - CSS variable --color-bg-sidebar exists in light mode', () => {
    expect(variablesContent).toContain('--color-bg-sidebar:');
  });

  it('sidebarBackgroundDifferentiation - CSS variable --color-bg-sidebar exists in dark mode', () => {
    expect(variablesContent).toContain('[data-theme="dark"]');
    const darkSection = variablesContent.split('[data-theme="dark"]')[1];
    expect(darkSection).toContain('--color-bg-sidebar:');
  });
});

describe('HeaderStyleSpec', () => {
  let variablesContent: string;

  beforeAll(() => {
    variablesContent = readFileSync(variablesPath, 'utf-8');
  });

  it('headerGlassBackground - CSS variable --color-bg-header exists in light mode', () => {
    expect(variablesContent).toContain('--color-bg-header:');
  });

  it('headerGlassBackground - CSS variable --color-bg-header exists in dark mode', () => {
    const darkSection = variablesContent.split('[data-theme="dark"]')[1];
    expect(darkSection).toContain('--color-bg-header:');
  });
});

describe('LayoutStyleSpec', () => {
  let variablesContent: string;

  beforeAll(() => {
    variablesContent = readFileSync(variablesPath, 'utf-8');
  });

  it('contentCardBackground - CSS variable --color-bg-content exists in light mode', () => {
    expect(variablesContent).toContain('--color-bg-content:');
  });

  it('contentCardBackground - CSS variable --color-bg-content exists in dark mode', () => {
    const darkSection = variablesContent.split('[data-theme="dark"]')[1];
    expect(darkSection).toContain('--color-bg-content:');
  });
});

describe('LoginStyleSpec', () => {
  let loginViewContent: string;

  beforeAll(() => {
    const loginViewPath = join(__dirname, '../views/LoginView.vue');
    loginViewContent = readFileSync(loginViewPath, 'utf-8');
  });

  it('loginPageVisualSeparation - login page uses gradient background', () => {
    expect(loginViewContent).toContain('var(--gradient-bg)');
  });

  it('loginPageVisualSeparation - login page has distinct visual separation from system pages', () => {
    expect(loginViewContent).toContain('background-image: var(--gradient-bg)');
  });

  it('loginPageVisualSeparation - login page has background-attachment: fixed', () => {
    expect(loginViewContent).toContain('background-attachment: fixed');
  });
});