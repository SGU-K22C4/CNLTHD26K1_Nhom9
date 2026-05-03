import api from '@/shared/utils/api'
import { generateUUID } from '@/shared/utils/uuid'

function getGuestUserId() {
  let id = localStorage.getItem('guestUserId')
  if (!id) {
    id = generateUUID()
    localStorage.setItem('guestUserId', id)
  }
  return id
}

function decodeJwtPayload(token) {
  try {
    const payload = token.split('.')[1]
    const normalized = payload.replace(/-/g, '+').replace(/_/g, '/')
    return JSON.parse(atob(normalized))
  } catch {
    return null
  }
}

export function getCurrentUserId() {
  const token = localStorage.getItem('accessToken')
  if (token) {
    const payload = decodeJwtPayload(token)
    return payload?.userId || payload?.sub || payload?.email || getGuestUserId()
  }
  return getGuestUserId()
}

function userHeaders() {
  return { 'X-User-Id': getCurrentUserId() }
}

function mapReview(review) {
  const star = Number(review?.star ?? review?.rating ?? 0)
  const content = review?.content ?? review?.comment ?? ''
  const images = Array.isArray(review?.images)
    ? review.images
    : Array.isArray(review?.imageUrls)
      ? review.imageUrls
      : []

  return {
    id: review?.id ?? review?.reviewId,
    reviewId: review?.reviewId,
    userId: review?.userId,
    orderId: review?.orderId,
    productId: review?.productId,
    rating: star,
    star,
    title: review?.title || `Đánh giá ${star} sao`,
    comment: content,
    content,
    imageUrls: images,
    images,
    createdAt: review?.createdAt,
    updatedAt: review?.updatedAt,
  }
}

function mapError(error, fallbackMessage) {
  return error?.message || error?.data?.message || fallbackMessage
}

export const reviewService = {
  getByProduct: async (productId, params = { page: 0, size: 50, star: undefined }) => {
    try {
      const data = await api.get(`/api/v1/reviews/product/${encodeURIComponent(String(productId))}`, {
        params: {
          page: Number(params?.page ?? 0),
          size: Number(params?.size ?? 50),
          ...(params?.star ? { star: Number(params.star) } : {}),
        },
        headers: userHeaders(),
      })

      const content = Array.isArray(data?.content) ? data.content.map(mapReview) : []
      return {
        ...data,
        content,
      }
    } catch (error) {
      throw new Error(mapError(error, 'Không thể tải đánh giá sản phẩm'))
    }
  },

  getStats: async (productId) => {
    try {
      const data = await api.get(`/api/v1/reviews/product/${encodeURIComponent(String(productId))}/stats`, {
        headers: userHeaders(),
      })

      return {
        averageRating: Number(data?.averageRating) || 0,
        totalReviews: Number(data?.totalReviews) || 0,
        starDistribution: data?.starDistribution || { 1: 0, 2: 0, 3: 0, 4: 0, 5: 0 },
      }
    } catch (error) {
      throw new Error(mapError(error, 'Không thể tải thống kê đánh giá'))
    }
  },

  getMine: async () => {
    try {
      const data = await api.get('/api/v1/reviews/mine', {
        headers: userHeaders(),
      })
      return Array.isArray(data) ? data.map(mapReview) : []
    } catch (error) {
      throw new Error(mapError(error, 'Không thể tải đánh giá của bạn'))
    }
  },

  create: async ({ orderId, productId, rating, comment, imageUrls = [] }) => {
    try {
      const payload = {
        orderId: String(orderId),
        productId: String(productId),
        star: Number(rating),
        title: `Đánh giá ${Number(rating)} sao`,
        content: String(comment || '').trim(),
        images: Array.isArray(imageUrls) ? imageUrls : [],
      }

      const data = await api.post('/api/v1/reviews', payload, {
        headers: userHeaders(),
      })

      return mapReview(data)
    } catch (error) {
      throw new Error(mapError(error, 'Không thể gửi đánh giá'))
    }
  },
}
