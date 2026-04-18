import { axiosClient } from '../../../config/axiosClient';

export const authService = {
  login: async (email, password) => {
    const response = await axiosClient.post('/api/v1/auth/login', { email, password });
    return response;
  },

  register: async (userData) => {
    const response = await axiosClient.post('/api/v1/auth/register', userData);
    return response;
  },

  // Gọi Backend để revoke Refresh Token trong DB
  // Backend endpoint: POST /api/v1/auth/logout — cần header X-User-Id (Kong tiêm)
  logout: async () => {
    await axiosClient.post('/api/v1/auth/logout');
  },

  refreshToken: async (refreshToken) => {
    const response = await axiosClient.post('/api/v1/auth/refresh', { refreshToken });
    return response;
  },

  resendVerification: async (email) => {
    const response = await axiosClient.post('/api/v1/auth/resend-verification', { email });
    return response;
  },
};