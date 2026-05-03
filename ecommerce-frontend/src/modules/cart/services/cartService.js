import api from '@/shared/utils/api'
import { buildUserHeaders } from '@/shared/utils/userHeaders'

/**
 * Cart Service — gọi API Gateway → cart-service (Redis)
 *
 * Luôn gửi X-User-Id để backend cart-service định danh user ổn định
 * cho cả guest và logged-in flow.
 */

function unwrapData(response, fallback) {
  if (response == null) return fallback
  if (Object.prototype.hasOwnProperty.call(response, 'data')) return response.data ?? fallback
  return response
}

export const cartService = {
  /**
   * Lấy toàn bộ giỏ hàng
   * GET /api/v1/cart → [{ variantSizeId, quantity }]
   */
  getCart: async () => {
    const res = await api.get('/api/v1/cart', { headers: buildUserHeaders() })
    return unwrapData(res, [])
  },

  /**
   * Thêm sản phẩm vào giỏ hàng (cộng dồn nếu đã có)
   * POST /api/v1/cart/items
   */
  addItem: async (variantSizeId, quantity = 1) => {
    const res = await api.post('/api/v1/cart/items', { variantSizeId, quantity }, { headers: buildUserHeaders() })
    return unwrapData(res, [])
  },

  /**
   * Cập nhật số lượng (đặt lại giá trị mới)
   * PATCH /api/v1/cart/items/:variantSizeId
   */
  updateQuantity: async (variantSizeId, quantity) => {
    const res = await api.patch(`/api/v1/cart/items/${variantSizeId}`, { quantity }, { headers: buildUserHeaders() })
    return unwrapData(res, [])
  },

  /**
   * Xóa 1 sản phẩm khỏi giỏ
   * DELETE /api/v1/cart/items/:variantSizeId
   */
  removeItem: async (variantSizeId) => {
    const res = await api.delete(`/api/v1/cart/items/${variantSizeId}`, { headers: buildUserHeaders() })
    return unwrapData(res, [])
  },

  /**
   * Xóa toàn bộ giỏ hàng
   * DELETE /api/v1/cart
   */
  clearCart: async () => {
    await api.delete('/api/v1/cart', { headers: buildUserHeaders() })
  },

  /**
   * Đếm tổng số lượng sản phẩm
   * GET /api/v1/cart/count → number
   */
  getCount: async () => {
    const res = await api.get('/api/v1/cart/count', { headers: buildUserHeaders() })
    return unwrapData(res, 0)
  },
}