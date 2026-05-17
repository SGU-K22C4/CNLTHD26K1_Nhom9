import axios from 'axios';
import { API_CONFIG } from '@/config/api.config';

const AUTH_CLEARED_EVENT = 'auth:cleared';

function clearAuthSession() {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
  localStorage.removeItem('userInfo');
  window.dispatchEvent(new Event(AUTH_CLEARED_EVENT));
}

const api = axios.create({
  baseURL: API_CONFIG.BASE_URL,
  timeout: API_CONFIG.TIMEOUT,
  headers: {
    'Content-Type': 'application/json',
  },
});

let isRefreshing = false;
let failedQueue = [];

const processQueue = (error, token = null) => {
  failedQueue.forEach((promise) => {
    if (error) {
      promise.reject(error);
    } else {
      promise.resolve(token);
    }
  });
  failedQueue = [];
};

// Request interceptor - thêm token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor - xử lý errors & refresh token
api.interceptors.response.use(
  (response) => {
    // Trả về data thay vì toàn bộ response
    return response.data;
  },
  async (error) => {
    const originalRequest = error.config || {};

    // Token hết hạn, thử refresh
    if (error.response?.status === 401 && !originalRequest._retry) {
      if (originalRequest.url?.includes('/auth/login') || originalRequest.url?.includes('/auth/refresh')) {
        return Promise.reject(error);
      }

      const refreshToken = localStorage.getItem('refreshToken');
      if (!refreshToken) {
        return Promise.reject(error);
      }

      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then((token) => {
            originalRequest.headers = originalRequest.headers || {};
            originalRequest.headers.Authorization = `Bearer ${token}`;
            return api(originalRequest);
          })
          .catch((queueError) => Promise.reject(queueError));
      }

      originalRequest._retry = true;
      isRefreshing = true;
      
      try {
        const response = await axios.post(
          `${API_CONFIG.BASE_URL}/api/v1/auth/refresh`,
          { refreshToken }
        );
        
        const refreshPayload = response.data?.data && typeof response.data.data === 'object'
          ? response.data.data
          : response.data;
        const accessToken = refreshPayload?.accessToken || refreshPayload?.token;
        const nextRefreshToken = refreshPayload?.refreshToken;

        if (!accessToken) {
          throw new Error('Refresh response missing access token');
        }

        localStorage.setItem('accessToken', accessToken);
        if (nextRefreshToken) {
          localStorage.setItem('refreshToken', nextRefreshToken);
        }
        processQueue(null, accessToken);
        
        // Retry request với token mới
        originalRequest.headers = originalRequest.headers || {};
        originalRequest.headers.Authorization = `Bearer ${accessToken}`;
        return api(originalRequest);
      } catch (refreshError) {
        processQueue(refreshError, null);
        // Refresh failed - logout user
        localStorage.clear();
        window.location.href = '/login';
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    // Trả về error với message rõ ràng
    const errorMessage = error.response?.data?.message || error.message || 'Something went wrong';
    return Promise.reject({
      message: errorMessage,
      status: error.response?.status,
      data: error.response?.data,
    });
  }
);

export default api;