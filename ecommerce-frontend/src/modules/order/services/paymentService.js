import api from '@/shared/utils/api'

export const paymentService = {
  /**
   * Get VNPay redirect URL for a given order.
   * @param {number} orderId - The order ID from the backend
   * @returns {Promise<{paymentUrl: string}>}
   */
  createVnpayPayment: async (orderId) => {
    return api.get(`/api/v1/payments/vnpay/create-payment?orderId=${orderId}`)
  },

  /**
   * Verify VNPay payment result by forwarding the return query params to backend.
   * @param {string} queryString - The full query string from VNPay redirect
   * @returns {Promise<{success: boolean, message: string, orderNumber?: string}>}
   */
  verifyVnpayPayment: async (queryString) => {
    return api.get(`/api/v1/payments/vnpay/payment-return?${queryString}`)
  },
}
