import api from '@/shared/utils/api'
import { getCurrentUserId } from '../../review/services/reviewService'

function userHeaders() {
  return { 'X-User-Id': getCurrentUserId() }
}

function mapError(error, fallbackMessage) {
  return error?.message || error?.data?.message || fallbackMessage
}

export const loyaltyService = {
  getWallet: async () => {
    try {
      return await api.get('/api/v1/promotions/loyalty/wallet', {
        headers: userHeaders(),
      })
    } catch (error) {
      throw new Error(mapError(error, 'Không thể tải ví điểm'))
    }
  },

  getTransactions: async (size = 10) => {
    try {
      const data = await api.get('/api/v1/promotions/loyalty/transactions', {
        params: { size: Number(size) || 10 },
        headers: userHeaders(),
      })
      return Array.isArray(data) ? data : []
    } catch (error) {
      throw new Error(mapError(error, 'Không thể tải lịch sử điểm'))
    }
  },

  previewRedeem: async ({ orderAmount, requestedPoints }) => {
    try {
      return await api.post('/api/v1/promotions/loyalty/redeem/preview', {
        userId: getCurrentUserId(),
        orderAmount: Number(orderAmount),
        requestedPoints: Number(requestedPoints),
      }, {
        headers: userHeaders(),
      })
    } catch (error) {
      throw new Error(mapError(error, 'Không thể áp dụng điểm lúc này'))
    }
  },
}
