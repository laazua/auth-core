import axios, {
  AxiosInstance,
  AxiosRequestConfig,
  AxiosResponse,
  AxiosError,
  InternalAxiosRequestConfig,
} from 'axios';
import { ElMessage } from 'element-plus';
import router from '@/router';
import { useAuthStore } from '@/stores/auth';
import type { Result, ErrorCode } from '@/types/api';

const REQUEST_TIMEOUT = 10000;

const createHttp = (): AxiosInstance => {
  const http = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
    timeout: REQUEST_TIMEOUT,
    headers: {
      'Content-Type': 'application/json',
    },
  });

  http.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
      const authStore = useAuthStore();
      const token = authStore.token;

      if (token && config.headers) {
        config.headers.Authorization = `Bearer ${token}`;
      }

      return config;
    },
    (error: AxiosError) => {
      return Promise.reject(error);
    }
  );

  http.interceptors.response.use(
    (response: AxiosResponse<Result>) => {
      const { data } = response;

      if (data.code !== ErrorCode.SUCCESS) {
        const message = data.message || '请求失败';
        ElMessage.error(message);

        if (
          data.code === ErrorCode.UNAUTHORIZED ||
          data.code === ErrorCode.TOKEN_EXPIRED ||
          data.code === ErrorCode.TOKEN_INVALID
        ) {
          handleUnauthorized();
        }

        return Promise.reject(new Error(message));
      }

      return response;
    },
    (error: AxiosError<Result>) => {
      const { response } = error;

      if (response) {
        const { status, data } = response;

        switch (status) {
          case 401:
            handleUnauthorized();
            break;
          case 403:
            ElMessage.error(data?.message || '权限不足');
            break;
          case 404:
            ElMessage.error('请求资源不存在');
            break;
          case 500:
            ElMessage.error(data?.message || '服务器内部错误');
            break;
          default:
            ElMessage.error(data?.message || `请求失败 (${status})`);
        }
      } else if (error.code === 'ECONNABORTED') {
        ElMessage.error('请求超时，请稍后重试');
      } else if (error.message === 'Network Error') {
        ElMessage.error('网络错误，请检查网络连接');
      } else {
        ElMessage.error(error.message || '未知错误');
      }

      return Promise.reject(error);
    }
  );

  return http;
};

const handleUnauthorized = () => {
  const authStore = useAuthStore();
  authStore.logout();
  router.push({ name: 'Login', query: { redirect: router.currentRoute.value.fullPath } });
};

export const http = createHttp();

export const request = <T = unknown>(config: AxiosRequestConfig): Promise<Result<T>> => {
  return http.request(config).then((res) => res.data);
};

export const get = <T = unknown>(url: string, params?: unknown): Promise<Result<T>> => {
  return http.get(url, { params }).then((res) => res.data);
};

export const post = <T = unknown>(url: string, data?: unknown): Promise<Result<T>> => {
  return http.post(url, data).then((res) => res.data);
};

export const put = <T = unknown>(url: string, data?: unknown): Promise<Result<T>> => {
  return http.put(url, data).then((res) => res.data);
};

export const del = <T = unknown>(url: string): Promise<Result<T>> => {
  return http.delete(url).then((res) => res.data);
};
