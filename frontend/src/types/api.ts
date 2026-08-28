export interface Result<T = unknown> {
  code: number;
  message: string;
  data: T;
  timestamp: number;
}

export interface PageResult<T = unknown> {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages: number;
}

export enum ErrorCode {
  SUCCESS = 0,
  PARAM_ERROR = 1001,
  UNAUTHORIZED = 1401,
  TOKEN_EXPIRED = 1402,
  TOKEN_INVALID = 1403,
  FORBIDDEN = 1404,
  NOT_FOUND = 1405,
  SERVER_ERROR = 1500,
}

export interface RequestConfig {
  headers?: Record<string, string>;
  params?: Record<string, unknown>;
  data?: unknown;
  timeout?: number;
}
