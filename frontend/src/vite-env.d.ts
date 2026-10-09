/// <reference types="vite/client" />
/// <reference types="vitest/globals" />
/// <reference types="@vue/test-utils" />

declare global {
  namespace vi {
    const fn: <T extends (...args: any[]) => any>(
      implementation?: T
    ) => ReturnType<T> & {
      mockImplementation: (fn: (...args: any[]) => any) => void;
      mockReturnValue: <T>(value: T) => void;
      mockResolvedValue: <T>(value: T) => void;
      mockRejectedValue: <T>(value: T) => void;
      mockClear: () => void;
      mockReset: () => void;
      mockRestore: () => void;
    };
    const mock: <T extends object>(obj: T, method?: string) => void;
    const unmock: (moduleName: string) => void;
    const mockModule: <T>(moduleName: string, factory: () => T) => void;
    const hoisted: <T>(factory: () => T) => T;
    const spyOn: <T extends object>(object: T, method: string) => void;
    const clearAllMocks: () => void;
    const resetAllMocks: () => void;
    const restoreAllMocks: () => void;
    const useFakeTimers: () => void;
    const useRealTimers: () => void;
    const advanceTimersByTime: (ms: number) => void;
    const runOnlyPendingTimers: () => void;
    const waitFor: <T>(
      callback: () => Promise<T> | T,
      options?: { timeout?: number; interval?: number }
    ) => Promise<T>;
  }
}

declare module '*.vue' {
  import type { DefineComponent } from 'vue';
  const component: DefineComponent<Record<string, unknown>, Record<string, unknown>, unknown>;
  export default component;
}

declare module '*.scss' {
  const content: Record<string, string>;
  export default content;
}

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string;
  readonly VITE_BASE_URL: string;
  readonly VITE_PORT: string;
  readonly VITE_APP_TITLE: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
