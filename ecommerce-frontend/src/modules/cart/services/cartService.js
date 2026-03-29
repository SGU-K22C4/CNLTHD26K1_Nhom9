import api from '@/shared/utils/api'

/**
 * Cart Service — gọi API Gateway → cart-service (Redis)
 *
 * Hiện tại dùng guest user ID (UUID) lưu trong localStorage.
 * Khi có JWT auth, API Gateway sẽ tự lấy userId từ token.
 */

function getGuestUserId() {
  let id = localStorage.getItem('guestUserId')
  if (!id) {
    id = crypto.randomUUID()
    localStorage.setItem('guestUserId', id)
  }
  return id
}

function userHeaders() {
  const token = localStorage.getItem('accessToken')
  // Nếu đã đăng nhập thì không cần gửi X-User-Id (JWT filter sẽ set)
  if (token) return {}
  return { 'X-User-Id': getGuestUserId() }
}

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
    const res = await api.get('/api/v1/cart', { headers: userHeaders() })
    return unwrapData(res, [])
  },

  /**
   * Thêm sản phẩm vào giỏ hàng (cộng dồn nếu đã có)
   * POST /api/v1/cart/items
   */
  addItem: async (variantSizeId, quantity = 1) => {
    const res = await api.post('/api/v1/cart/items', { variantSizeId, quantity }, { headers: userHeaders() })
    return unwrapData(res, [])
  },

  /**
   * Cập nhật số lượng (đặt lại giá trị mới)
   * PATCH /api/v1/cart/items/:variantSizeId
   */
  updateQuantity: async (variantSizeId, quantity) => {
    const res = await api.patch(`/api/v1/cart/items/${variantSizeId}`, { quantity }, { headers: userHeaders() })
    return unwrapData(res, [])
  },

  /**
   * Xóa 1 sản phẩm khỏi giỏ
   * DELETE /api/v1/cart/items/:variantSizeId
   */
  removeItem: async (variantSizeId) => {
    const res = await api.delete(`/api/v1/cart/items/${variantSizeId}`, { headers: userHeaders() })
    return unwrapData(res, [])
  },

  /**
   * Xóa toàn bộ giỏ hàng
   * DELETE /api/v1/cart
   */
  clearCart: async () => {
    await api.delete('/api/v1/cart', { headers: userHeaders() })
  },

  /**
   * Đếm tổng số lượng sản phẩm
   * GET /api/v1/cart/count → number
   */
  getCount: async () => {
    const res = await api.get('/api/v1/cart/count', { headers: userHeaders() })
    return unwrapData(res, 0)
  },
}