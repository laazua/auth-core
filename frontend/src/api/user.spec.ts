import { describe, it, expect, vi, beforeEach } from 'vitest';
import type { Mock } from 'vitest';

vi.mock('./http', () => ({
  http: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

import { http } from './http';
import { userApi } from './user';

describe('userApi.assignRoles', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('assignRoles 使用 PUT 方法', async () => {
    (http.put as Mock).mockResolvedValue({ code: 0, message: 'ok', data: null });

    await userApi.assignRoles(1, { roleIds: [2] });

    expect(http.put).toHaveBeenCalledWith('/users/1/roles', { roleIds: [2] });
    expect(http.post).not.toHaveBeenCalled();
  });
});
