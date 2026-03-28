const API_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

export const orderService = {
  create: async (orderData) => {
    const userId = localStorage.getItem('guestId'); // Use guestId for now based on cart logic
    const reqOptions = {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(orderData)
    };
    if (userId) {
      reqOptions.headers['X-User-Id'] = userId;
    }
    console.log('[orderService] Sending payload:', JSON.stringify(orderData, null, 2));
    const res = await fetch(`${API_URL}/api/v1/orders`, reqOptions);
    if (!res.ok) {
      const errBody = await res.text();
      console.error('[orderService] Error response:', res.status, errBody);
      throw new Error('Failed to create order');
    }
    return res.json();
  },
  getHistory: async () => [],
}