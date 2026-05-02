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
    const originalRequest = error.config;

    // Token hết hạn, thử refresh
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      
      try {
        const refreshToken = localStorage.getItem('refreshToken');
        if (!refreshToken) {
          clearAuthSession();
          return Promise.reject(error);
        }

        const response = await axios.post(
          `${API_CONFIG.BASE_URL}/api/v1/auth/refresh`,
          { refreshToken }
        );
        
        const { accessToken } = response.data;
        localStorage.setItem('accessToken', accessToken);
        
        // Retry request với token mới
        originalRequest.headers.Authorization = `Bearer ${accessToken}`;
        return api(originalRequest);
      } catch (refreshError) {
        // Refresh failed - clear local auth state and let route guards handle navigation.
        clearAuthSession();
        return Promise.reject(refreshError);
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