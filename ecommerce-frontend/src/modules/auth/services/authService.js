import axios from 'axios';
import { API_CONFIG } from '../../../config/api.config';

export const authService = {
  login: async (email, password) => {
    const response = await axios.post(`${API_CONFIG.BASE_URL}/api/v1/auth/login`, {
      email,
      password
    });
    return response.data;
  },
  register: async (userData) => {
    const response = await axios.post(`${API_CONFIG.BASE_URL}/api/v1/auth/register`, userData);
    return response.data;
  },
}