import axios from 'axios';
import { API_CONFIG } from './api.config';

export const axiosClient = axios.create({
  baseURL: API_CONFIG.BASE_URL,
  timeout: API_CONFIG.TIMEOUT,
  headers: {
    'Content-Type': 'application/json',
  },
});

// --- Các biến hỗ trợ xử lý hàng đợi (Queue) ---
let isRefreshing = false;
let failedQueue = [];

const processQueue = (error, token = null) => {
  failedQueue.forEach(prom => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

// --- Helper an toàn cho môi trường Next.js (SSR) ---
const isBrowser = typeof window !== 'undefined';
const getStorageItem = (key) => isBrowser ? localStorage.getItem(key) : null;
const setStorageItem = (key, value) => { if (isBrowser) localStorage.setItem(key, value); };
const removeStorageItem = (key) => { if (isBrowser) localStorage.removeItem(key); };

const handleLogout = () => {
  removeStorageItem('accessToken');
  removeStorageItem('refreshToken');
  removeStorageItem('userInfo');
  if (isBrowser) {
    window.location.href = '/login';
  }
};

// --- Interceptors ---

axiosClient.interceptors.request.use(
  (config) => {
    const token = getStorageItem('accessToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

axiosClient.interceptors.response.use(
  (response) => {
    return response.data; // Chỉ lấy cục JS object trả về
  },
  async (error) => {
    const originalRequest = error.config;

    // Bỏ qua nếu lỗi mạng không có response hoặc request đã được retry rồi
    if (!error.response || originalRequest._retry) {
      return Promise.reject(error);
    }

    if (error.response.status === 401) {
      // Bỏ qua nếu lỗi 401 xuất phát từ chính API login hoặc refresh
      if (originalRequest.url.includes('/login') || originalRequest.url.includes('/refresh')) {
        return Promise.reject(error);
      }

      // NẾU ĐANG REFRESH: Đẩy các request bị 401 tiếp theo vào hàng đợi (Queue)
      if (isRefreshing) {
        return new Promise(function (resolve, reject) {
          failedQueue.push({ resolve, reject });
        })
          .then(token => {
            originalRequest.headers.Authorization = `Bearer ${token}`;
            return axiosClient(originalRequest);
          })
          .catch(err => Promise.reject(err));
      }

      // NẾU CHƯA REFRESH: Bật cờ và bắt đầu gọi API Refresh
      originalRequest._retry = true;
      isRefreshing = true;

      const refreshToken = getStorageItem('refreshToken');
      if (!refreshToken) {
        isRefreshing = false;
        handleLogout();
        return Promise.reject(new Error('No refresh token available'));
      }

      try {
        const res = await axios.post(`${API_CONFIG.BASE_URL}/api/v1/auth/refresh`, {
          refreshToken: refreshToken
        });

        const data = res.data;
        if (data && data.accessToken) {
          setStorageItem('accessToken', data.accessToken);
          if (data.refreshToken) {
            setStorageItem('refreshToken', data.refreshToken);
          }

          // Giải phóng hàng đợi: Chạy lại TẤT CẢ các request đang xếp hàng với token mới
          processQueue(null, data.accessToken);

          originalRequest.headers.Authorization = `Bearer ${data.accessToken}`;
          return axiosClient(originalRequest);
        } else {
          throw new Error('Refresh token request failed');
        }
      } catch (err) {
        console.error("Token expired or unauthorized and refresh failed:", err);
        // Hủy toàn bộ hàng đợi nếu refresh thất bại
        processQueue(err, null);
        handleLogout();
        return Promise.reject(err);
      } finally {
        isRefreshing = false; // Tắt cờ khi hoàn tất (dù thành công hay thất bại)
      }
    }

    return Promise.reject(error);
  }
);