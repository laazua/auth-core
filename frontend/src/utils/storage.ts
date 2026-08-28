const isBrowser = typeof window !== 'undefined';

const safeGet = (storage: Storage, key: string): unknown => {
  if (!isBrowser) return null;
  try {
    const value = storage.getItem(key);
    return value ? JSON.parse(value) : null;
  } catch {
    return null;
  }
};

const safeSet = (storage: Storage, key: string, value: unknown): void => {
  if (!isBrowser) return;
  try {
    storage.setItem(key, JSON.stringify(value));
  } catch {
    // ignore
  }
};

const safeRemove = (storage: Storage, key: string): void => {
  if (!isBrowser) return;
  try {
    storage.removeItem(key);
  } catch {
    // ignore
  }
};

const safeClear = (storage: Storage): void => {
  if (!isBrowser) return;
  try {
    storage.clear();
  } catch {
    // ignore
  }
};

const createStorageWrapper = (storage: Storage) => ({
  get: (key: string) => safeGet(storage, key),
  set: (key: string, value: unknown) => safeSet(storage, key, value),
  remove: (key: string) => safeRemove(storage, key),
  clear: () => safeClear(storage),
});

export const storage = isBrowser
  ? createStorageWrapper(localStorage)
  : {
      get: () => null,
      set: () => {},
      remove: () => {},
      clear: () => {},
    };

export const sessionStorage = isBrowser
  ? createStorageWrapper(window.sessionStorage)
  : {
      get: () => null,
      set: () => {},
      remove: () => {},
      clear: () => {},
    };

export const cookieStorage = {
  get: (key: string): string | null => {
    if (!isBrowser) return null;
    const value = `; ${document.cookie}`.split(`; ${key}=`).pop()?.split(';').shift();
    return value ? decodeURIComponent(value) : null;
  },
  set: (key: string, value: string, days = 7): void => {
    if (!isBrowser) return;
    const expires = new Date(Date.now() + days * 864e5).toUTCString();
    document.cookie = `${key}=${encodeURIComponent(value)}; expires=${expires}; path=/; SameSite=Lax`;
  },
  remove: (key: string): void => {
    if (!isBrowser) return;
    document.cookie = `${key}=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/`;
  },
};
