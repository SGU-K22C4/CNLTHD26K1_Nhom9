const API_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

export const paymentService = {
  /**
   * Get VNPay redirect URL for a given order.
   * @param {number} orderId - The order ID from the backend
   * @returns {Promise<{paymentUrl: string}>}
   */
  createVnpayPayment: async (orderId) => {
    const res = await fetch(`${API_URL}/api/v1/payments/vnpay/create-payment?orderId=${orderId}`);
    if (!res.ok) throw new Error('Failed to create VNPay payment');
    return res.json();
  },

  /**
   * Verify VNPay payment result by forwarding the return query params to backend.
   * @param {string} queryString - The full query string from VNPay redirect (e.g. "vnp_TxnRef=1&vnp_ResponseCode=00&...")
   * @returns {Promise<{success: boolean, message: string, orderNumber?: string}>}
   */
  verifyVnpayPayment: async (queryString) => {
    const res = await fetch(`${API_URL}/api/v1/payments/vnpay/payment-return?${queryString}`);
    return res.json();
  },
};
