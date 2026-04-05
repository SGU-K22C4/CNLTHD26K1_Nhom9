import { axiosClient } from '../../../config/axiosClient';

export const authService = {
  login: async (email, password) => {
    const response = await axiosClient.post('/api/v1/auth/login', {
      email,
      password
    });
    return response;
  },
  register: async (userData) => {
    const response = await axiosClient.post('/api/v1/auth/register', userData);
    return response;
  },
}