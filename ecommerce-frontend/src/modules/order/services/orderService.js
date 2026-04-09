const API_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

function getGuestUserId() {
  let id = localStorage.getItem('guestUserId');
  if (!id) {
    id = crypto.randomUUID();
    localStorage.setItem('guestUserId', id);
  }
  return id;
}

function decodeJwtPayload(token) {
  try {
    const payload = token.split('.')[1];
    const normalized = payload.replace(/-/g, '+').replace(/_/g, '/');
    return JSON.parse(atob(normalized));
  } catch {
    return null;
  }
}

function getCurrentUserId() {
  const token = localStorage.getItem('accessToken');
  if (token) {
    const payload = decodeJwtPayload(token);
    return payload?.userId || payload?.sub || payload?.email || getGuestUserId();
  }
  return getGuestUserId();
}

const getHeaders = () => {
  const headers = { 'Content-Type': 'application/json' };
  const userId = getCurrentUserId();
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
      let errBody = '';
      let message = 'Failed to create order';
      try {
        const data = await res.json();
        errBody = JSON.stringify(data);
        message = data?.error || data?.message || message;
      } catch {
        errBody = await res.text();
      }
      console.error('[orderService] Error response:', res.status, errBody);
      throw new Error(message);
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

  getHistory: async (params = { page: 0, size: 50 }) => {
    const page = Number(params?.page ?? 0);
    const size = Number(params?.size ?? 50);
    const res = await fetch(`${API_URL}/api/v1/orders?page=${page}&size=${size}`, {
      headers: getHeaders(),
    });
    if (!res.ok) throw new Error('Failed to fetch order history');
    return res.json();
  },
};