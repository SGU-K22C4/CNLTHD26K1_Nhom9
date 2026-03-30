import api from '@/shared/utils/api'

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
  if (token) return {}
  return { 'X-User-Id': getGuestUserId() }
}

function unwrapData(response, fallback) {
  if (response == null) return fallback
  if (Object.prototype.hasOwnProperty.call(response, 'data')) return response.data ?? fallback
  return response
}

export const wishlistService = {
  getWishlist: async (params = { page: 0, size: 20 }) => {
    const res = await api.get('/api/v1/wishlists', { params, headers: userHeaders() })
    return unwrapData(res, { content: [], totalElements: 0, totalPages: 0, number: 0 })
  },

  getWishlistIds: async () => {
    const res = await api.get('/api/v1/wishlists/ids', { headers: userHeaders() })
    return unwrapData(res, [])
  },

  addToWishlist: async (productId) => {
    const res = await api.post(`/api/v1/wishlists/${productId}`, {}, { headers: userHeaders() })
    return unwrapData(res, null)
  },

  removeFromWishlist: async (productId) => {
    const res = await api.delete(`/api/v1/wishlists/${productId}`, { headers: userHeaders() })
    return unwrapData(res, null)
  },

  checkWishlisted: async (productId) => {
    const res = await api.get(`/api/v1/wishlists/${productId}/check`, { headers: userHeaders() })
    const data = unwrapData(res, { wishlisted: false })
    return data.wishlisted
  }
}
