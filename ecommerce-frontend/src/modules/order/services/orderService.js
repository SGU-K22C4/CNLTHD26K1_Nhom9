import api from '@/shared/utils/api'
import { buildUserHeaders } from '@/shared/utils/userHeaders'

export const orderService = {
  /**
   * Create a new order.
   * @param {Object} orderData - The order payload
   * @returns {Promise<Object>} The created order
   */
  create: async (orderData) => {
    return api.post('/api/v1/orders', orderData, {
      headers: buildUserHeaders(),
    })
  },

  /**
   * Get order detail by ID.
   * @param {number|string} orderId
   * @returns {Promise<Object>}
   */
  getById: async (orderId) => {
    return api.get(`/api/v1/orders/detail/${orderId}`, {
      headers: buildUserHeaders(),
    })
  },

  /**
   * Get order detail by order number (used after payment success).
   * @param {string} orderNumber
   * @returns {Promise<Object>}
   */
  getByOrderNumber: async (orderNumber) => {
    return api.get(`/api/v1/orders/by-number/${orderNumber}`, {
      headers: buildUserHeaders(),
    })
  },

  /**
   * Get paginated order history for the current user.
   * @param {Object} params - { page, size }
   * @returns {Promise<Object>}
   */
  getHistory: async (params = { page: 0, size: 50 }) => {
    const page = Number(params?.page ?? 0)
    const size = Number(params?.size ?? 50)
    return api.get(`/api/v1/orders?page=${page}&size=${size}`, {
      headers: buildUserHeaders(),
    })
  },

  /**
   * Cancel a PENDING order (within 15-minute grace period).
   * After 15 minutes, the order is auto-confirmed and can no longer be cancelled.
   * @param {number|string} orderId
   * @returns {Promise<Object>} The updated order
   */
  cancel: async (orderId) => {
    return api.patch(`/api/v1/orders/${orderId}/cancel`, {}, {
      headers: buildUserHeaders(),
    })
  },
}