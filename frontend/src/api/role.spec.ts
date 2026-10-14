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
import { roleApi } from './role';

describe('roleApi.assignPermissions', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('assignPermissions 使用 PUT 方法', async () => {
    (http.put as Mock).mockResolvedValue({ code: 0, message: 'ok', data: null });

    await roleApi.assignPermissions(1, { permissionIds: [2] });

    expect(http.put).toHaveBeenCalledWith('/roles/1/permissions', { permissionIds: [2] });
    expect(http.post).not.toHaveBeenCalled();
  });
});
