import axios from 'axios';
import { API_CONFIG } from './api.config';

export const axiosClient = axios.create({
  baseURL: API_CONFIG.BASE_URL,
  timeout: API_CONFIG.TIMEOUT,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Interceptor nạp Token vào mỗi Request
axiosClient.interceptors.request.use(
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

// Interceptor bắt lỗi Server trả về (Đặc biệt là 401 Unauthorized)
axiosClient.interceptors.response.use(
  (response) => {
    return response.data; // Chỉ lấy cục JS object trả về
  },
  async (error) => {
    // Nếu token hết hạn hoặc sai, đá văng khỏi phiên đăng nhập
    if (error.response && error.response.status === 401) {
      console.error("Token expired or unauthorized!");
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
      localStorage.removeItem('userInfo');
      // Ở đây có thể dùng window.location.href = '/login' để đá về login
      // window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);
