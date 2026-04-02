const API_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

const getHeaders = () => {
  const headers = { 'Content-Type': 'application/json' };
  const userId = localStorage.getItem('guestId');
  if (userId) headers['X-User-Id'] = userId;
  return headers;
};

export const orderService = {
  create: async (orderData) => {
    const res = await fetch(`${API_URL}/api/v1/orders`, {
      method: 'POST',
      headers: getHeaders(),
      body: JSON.stringify(orderData),
    });
    if (!res.ok) {
      const errBody = await res.text();
      console.error('[orderService] Error response:', res.status, errBody);
      throw new Error('Failed to create order');
    }
    return res.json();
  },

  getById: async (orderId) => {
    const res = await fetch(`${API_URL}/api/v1/orders/detail/${orderId}`, {
      headers: getHeaders(),
    });
    if (!res.ok) throw new Error('Failed to fetch order');
    return res.json();
  },

  getByOrderNumber: async (orderNumber) => {
    const res = await fetch(`${API_URL}/api/v1/orders/by-number/${orderNumber}`, {
      headers: getHeaders(),
    });
    if (!res.ok) throw new Error('Failed to fetch order');
    return res.json();
  },

  getHistory: async () => [],
};