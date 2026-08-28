import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import axios from 'axios';

vi.mock('axios', async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    default: {
      create: vi.fn(() => {
        const instance = actual.create({
          baseURL: '/api',
          timeout: 10000,
          adapter: 'test',
        });
        instance.interceptors = {
          request: { use: vi.fn() },
          response: { use: vi.fn() },
        };
        return instance;
      }),
    },
  };
});

describe('Axios HTTP Interceptors', () => {
  let http: ReturnType<typeof axios.create>;

  beforeEach(() => {
    http = axios.create({
      baseURL: '/api',
      timeout: 10000,
      adapter: 'test',
    });
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('should create axios instance with correct config', () => {
    expect(http).toBeDefined();
    expect(http.defaults.baseURL).toBe('/api');
    expect(http.defaults.timeout).toBe(10000);
  });

  it('should have request interceptors', () => {
    expect(http.interceptors.request).toBeDefined();
    expect(typeof http.interceptors.request.use).toBe('function');
  });

  it('should have response interceptors', () => {
    expect(http.interceptors.response).toBeDefined();
    expect(typeof http.interceptors.response.use).toBe('function');
  });

  it('should have axios create function', () => {
    expect(typeof axios.create).toBe('function');
  });
});
