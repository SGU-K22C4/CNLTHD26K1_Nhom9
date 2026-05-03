import api from '@/shared/utils/api'
import { buildUserHeaders } from '@/shared/utils/userHeaders'

function unwrapData(response, fallback) {
  if (response == null) return fallback
  if (Object.prototype.hasOwnProperty.call(response, 'data')) return response.data ?? fallback
  return response
}

export const wishlistService = {
  getWishlist: async (params = { page: 0, size: 20 }) => {
    const res = await api.get('/api/v1/wishlists', { params, headers: buildUserHeaders() })
    return unwrapData(res, { content: [], totalElements: 0, totalPages: 0, number: 0 })
  },

  getWishlistIds: async () => {
    const res = await api.get('/api/v1/wishlists/ids', { headers: buildUserHeaders() })
    return unwrapData(res, [])
  },

  addToWishlist: async (productId) => {
    const res = await api.post(`/api/v1/wishlists/${productId}`, {}, { headers: buildUserHeaders() })
    return unwrapData(res, null)
  },

  removeFromWishlist: async (productId) => {
    const res = await api.delete(`/api/v1/wishlists/${productId}`, { headers: buildUserHeaders() })
    return unwrapData(res, null)
  },

  checkWishlisted: async (productId) => {
    const res = await api.get(`/api/v1/wishlists/${productId}/check`, { headers: buildUserHeaders() })
    const data = unwrapData(res, { wishlisted: false })
    return data.wishlisted
  }
}
