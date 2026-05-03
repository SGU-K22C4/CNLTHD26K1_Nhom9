import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { orderService } from '../services/orderService'
import { reviewService } from '../../review/services/reviewService'
import ReviewComposerModal from '../../review/components/ReviewComposerModal'
import { formatCurrency } from '../../../shared/utils/format'

function isDeliveredOrder(status) {
  return String(status || '').toUpperCase() === 'DELIVERED'
}

function formatDate(value) {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '-'
  return new Intl.DateTimeFormat('vi-VN', {
    hour: '2-digit',
    minute: '2-digit',
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(date)
}

function getDemoOrders() {
  return [
    {
      id: 900001,
      orderNumber: 'DEMO-900001',
      status: 'DELIVERED',
      createdAt: new Date(Date.now() - 1000 * 60 * 60 * 24 * 4).toISOString(),
      total: 890000,
      items: [
        {
          productId: 1,
          productName: 'Áo khoác demo',
          imageUrl: '/assets/images/placeholder.png',
          color: 'Beige',
          size: 'M',
          quantity: 1,
          totalPrice: 890000,
        },
      ],
    },
  ]
}

export default function OrderHistoryPage() {
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [reviewWarning, setReviewWarning] = useState('')
  const [orders, setOrders] = useState([])

  const [reviewTarget, setReviewTarget] = useState(null)
  const [submitError, setSubmitError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [mineReviews, setMineReviews] = useState([])

  const loadData = async () => {
    setLoading(true)
    setError('')
    setReviewWarning('')

    const [orderResult, reviewResult] = await Promise.allSettled([
      orderService.getHistory({ page: 0, size: 50 }),
      reviewService.getMine(),
    ])

    if (orderResult.status === 'fulfilled') {
      const orderPage = orderResult.value
      setOrders(Array.isArray(orderPage?.content) ? orderPage.content : [])
    } else {
      console.error('[OrderHistoryPage] Failed to load order history:', orderResult.reason)
      setError('Chưa kết nối được backend đơn hàng, đang dùng dữ liệu demo để test giao diện.')
      setOrders(getDemoOrders())
    }

    if (reviewResult.status === 'fulfilled') {
      const myReviews = reviewResult.value
      setMineReviews(Array.isArray(myReviews) ? myReviews : [])
    } else {
      console.warn('[OrderHistoryPage] Review service unavailable, skipping review status')
      setMineReviews([])
    }

    setLoading(false)
  }

  useEffect(() => {
    loadData()
  }, [])

  const reviewedKeys = useMemo(() => {
    const keys = new Set()
    mineReviews.forEach((review) => {
      const productId = String(review.productId || '').trim()
      if (productId) {
        keys.add(`product-${productId}`)
      }

      const orderId = String(review.orderId || '').trim()
      if (orderId && productId) {
        keys.add(`order-${orderId}-product-${productId}`)
      }
    })
    return keys
  }, [mineReviews])

  const handleSubmitReview = async (payload) => {
    setSubmitError('')
    setSubmitting(true)

    try {
      await reviewService.create(payload)
      setReviewTarget(null)
      await loadData()
    } catch (err) {
      setSubmitError(err?.message || 'Không thể gửi đánh giá. Vui lòng thử lại.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="min-h-screen bg-[#FAFAFA] py-8">
      <div className="max-w-screen-xl mx-auto px-4 sm:px-6">
        <div className="flex items-start justify-between gap-4 flex-wrap mb-6">
          <div>
            <h1 className="text-[24px] font-semibold text-[#202020]">Lịch sử đơn hàng</h1>
            <p className="text-[13px] text-[#666] mt-1">Chỉ đơn đã giao thành công mới mở tính năng đánh giá sản phẩm.</p>
          </div>
          <Link to="/products" className="text-[12px] uppercase tracking-[0.08em] border border-[#D9D9D9] px-4 h-10 inline-flex items-center hover:border-[#202020] transition-colors">
            Mua thêm
          </Link>
        </div>

        <div className="mb-5 p-4 border border-[#E8E8E8] bg-white">
          <p className="text-[12px] text-[#444]">
            Quy tắc đánh giá: mỗi khách hàng chỉ gửi 1 đánh giá cho mỗi sản phẩm đã mua.
          </p>
        </div>

        {error && <p className="text-[13px] text-red-500 mb-4">{error}</p>}
        {reviewWarning && <p className="text-[13px] text-amber-600 mb-4">{reviewWarning}</p>}

        {loading ? (
          <p className="text-[13px] text-[#666]">Đang tải đơn hàng...</p>
        ) : orders.length === 0 ? (
          <div className="border border-dashed border-[#D9D9D9] bg-white p-8 text-center">
            <p className="text-[14px] text-[#444] mb-3">Bạn chưa có đơn hàng nào.</p>
            <Link to="/products" className="text-[12px] uppercase tracking-[0.08em] text-[#5A6D57] underline">
              Khám phá sản phẩm ngay
            </Link>
          </div>
        ) : (
          <div className="space-y-5">
            {orders.map((order) => {
              const delivered = isDeliveredOrder(order.status)

              return (
                <section key={order.id} className="border border-[#E8E8E8] bg-white">
                  <header className="px-5 py-4 border-b border-[#EFEFEF] flex flex-wrap gap-4 items-center justify-between">
                    <div>
                      <p className="text-[13px] font-semibold text-[#202020]">Đơn #{order.orderNumber || order.id}</p>
                      <p className="text-[12px] text-[#888] mt-1">{formatDate(order.createdAt)}</p>
                    </div>
                    <div className="text-right">
                      <p className="text-[12px] text-[#666]">Trạng thái: <span className="font-medium text-[#202020]">{order.status || '-'}</span></p>
                      <p className="text-[12px] text-[#666] mt-1">Tổng: <span className="font-semibold text-[#202020]">{formatCurrency(Number(order.total) || 0)}</span></p>
                      <Link
                        to={`/orders/${order.id}`}
                        className="inline-flex mt-2 h-8 items-center border border-[#D9D9D9] px-3 text-[11px] uppercase tracking-[0.08em] text-[#404040] hover:border-[#202020] transition-colors"
                      >
                        Xem chi tiết
                      </Link>
                    </div>
                  </header>

                  <div className="divide-y divide-[#F2F2F2]">
                    {(order.items || []).map((item, index) => {
                      const productIdKey = String(item.productId || '').trim()
                      const orderIdKey = String(order.id || '').trim()
                      const reviewed = reviewedKeys.has(`product-${productIdKey}`)
                        || reviewedKeys.has(`order-${orderIdKey}-product-${productIdKey}`)

                      return (
                        <article key={`${order.id}-${item.productId}-${index}`} className="p-5 flex flex-col sm:flex-row gap-4 sm:items-center justify-between">
                          <div className="flex gap-3 min-w-0">
                            <img
                              src={item.imageUrl || '/assets/images/placeholder.png'}
                              alt={item.productName}
                              className="w-16 h-20 object-cover border border-[#E8E8E8] shrink-0"
                            />
                            <div className="min-w-0">
                              <h3 className="text-[14px] font-medium text-[#202020] truncate">{item.productName}</h3>
                              <p className="text-[12px] text-[#666] mt-1">Màu: {item.color || '-'} | Size: {item.size || '-'}</p>
                              <p className="text-[12px] text-[#666] mt-1">SL: {item.quantity} | {formatCurrency(Number(item.totalPrice) || 0)}</p>
                            </div>
                          </div>

                          <button
                            type="button"
                            disabled={!delivered || reviewed}
                            onClick={() => {
                              setSubmitError('')
                              setReviewTarget({
                                orderId: String(order.id),
                                productId: String(item.productId),
                                productName: item.productName,
                              })
                            }}
                            className={`h-10 px-4 text-[11px] uppercase tracking-[0.08em] border transition-colors ${(!delivered || reviewed) ? 'border-[#E0E0E0] text-[#A0A0A0] cursor-not-allowed' : 'border-[#5A6D57] text-[#5A6D57] hover:bg-[#5A6D57] hover:text-white'}`}
                          >
                            {!delivered ? 'Chưa thể đánh giá' : reviewed ? 'Đã đánh giá' : 'Đánh giá sản phẩm'}
                          </button>
                        </article>
                      )
                    })}
                  </div>
                </section>
              )
            })}
          </div>
        )}
      </div>

      {reviewTarget && (
        <ReviewComposerModal
          targetItem={reviewTarget}
          submitting={submitting}
          errorMessage={submitError}
          onClose={() => setReviewTarget(null)}
          onSubmit={handleSubmitReview}
        />
      )}
    </div>
  )
}