import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import StarRating from './StarRating'
import { reviewService } from '../services/reviewService'

const PAGE_SIZE = 6

function formatDate(value) {
  if (!value) return 'Vừa xong'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return 'Vừa xong'
  return new Intl.DateTimeFormat('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
  }).format(date)
}

function maskUser(userId = '') {
  const text = String(userId)
  if (text.length <= 4) return `${text}***`
  return `${text.slice(0, 2)}***${text.slice(-2)}`
}

export default function ProductReviewSection({ productId }) {
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [reviews, setReviews] = useState([])
  const [stats, setStats] = useState({ averageRating: 0, totalReviews: 0, starDistribution: {} })
  const [selectedStar, setSelectedStar] = useState(0)
  const [currentPage, setCurrentPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)

  const loadReviews = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const [reviewPage, reviewStats] = await Promise.all([
        reviewService.getByProduct(productId, {
          page: currentPage,
          size: PAGE_SIZE,
          star: selectedStar || undefined,
        }),
        reviewService.getStats(productId),
      ])

      setReviews(Array.isArray(reviewPage?.content) ? reviewPage.content : [])
      setTotalPages(Math.max(Number(reviewPage?.totalPages) || 0, 0))
      setTotalElements(Math.max(Number(reviewPage?.totalElements) || 0, 0))
      setStats({
        averageRating: Number(reviewStats?.averageRating) || 0,
        totalReviews: Number(reviewStats?.totalReviews) || 0,
        starDistribution: reviewStats?.starDistribution || {},
      })
    } catch (err) {
      setError(err?.message || 'Không thể tải đánh giá')
    } finally {
      setLoading(false)
    }
  }, [productId, currentPage, selectedStar])

  useEffect(() => {
    setCurrentPage(0)
  }, [selectedStar, productId])

  useEffect(() => {
    let mounted = true

    const load = async () => {
      if (!mounted) return
      await loadReviews()
    }

    load()

    return () => {
      mounted = false
    }
  }, [loadReviews])

  const distribution = useMemo(() => {
    return [5, 4, 3, 2, 1].map((star) => {
      const count = Number(stats.starDistribution?.[star]) || 0
      const total = Number(stats.totalReviews) || 1
      return {
        star,
        count,
        percent: Math.round((count / total) * 100),
      }
    })
  }, [stats])

  const filteredReviews = useMemo(() => {
    if (!selectedStar) return reviews
    return reviews.filter((review) => Number(review.rating ?? review.star) === selectedStar)
  }, [reviews, selectedStar])

  if (loading) {
    return (
      <section className="mt-14 border-t border-[#E8E8E8] pt-10">
        <h2 className="text-[18px] font-semibold text-[#202020] mb-3">Đánh giá sản phẩm</h2>
        <p className="text-[13px] text-[#888]">Đang tải đánh giá...</p>
      </section>
    )
  }

  return (
    <section className="mt-14 border-t border-[#E8E8E8] pt-10">
      <div className="flex items-start justify-between gap-4 flex-wrap">
        <div>
          <h2 className="text-[18px] font-semibold text-[#202020] mb-1">Đánh giá sản phẩm</h2>
          <p className="text-[13px] text-[#666]">
            Chỉ khách đã mua và nhận hàng thành công mới được đánh giá.
          </p>
        </div>
        <Link
          to="/orders"
          className="text-[13px] font-medium uppercase tracking-[0.08em] text-[#5A6D57] border border-[#5A6D57] px-4 py-2 hover:bg-[#5A6D57] hover:text-white transition-colors"
        >
          Viết đánh giá
        </Link>
      </div>

      {error && (
        <p className="text-[13px] text-red-500 mt-4">{error}</p>
      )}

      {!error && (
        <>
          <div className="mt-6 grid grid-cols-1 lg:grid-cols-[280px_1fr] gap-8">
            <aside className="border border-[#E8E8E8] p-5 h-fit">
              <p className="text-[34px] leading-none font-semibold text-[#202020] mb-2">
                {stats.averageRating.toFixed(1)}
              </p>
              <StarRating value={Math.round(stats.averageRating)} size={20} />
              <p className="text-[12px] text-[#888] mt-2">
                {stats.totalReviews} đánh giá đã xác thực
              </p>

              <div className="mt-5 space-y-2.5">
                {distribution.map((item) => (
                  <button
                    key={item.star}
                    type="button"
                    onClick={() => setSelectedStar((prev) => (prev === item.star ? 0 : item.star))}
                    className={`w-full flex items-center gap-2 text-[12px] ${selectedStar === item.star ? 'text-[#202020]' : 'text-[#666]'}`}
                  >
                    <span className="w-9 text-left">{item.star} sao</span>
                    <span className="flex-1 h-2 bg-[#EFEFEF]">
                      <span
                        className="h-full block bg-[#5A6D57]"
                        style={{ width: `${item.percent}%` }}
                      />
                    </span>
                    <span className="w-8 text-right">{item.count}</span>
                  </button>
                ))}
              </div>
            </aside>

            <div>
              <div className="flex items-center gap-2 mb-4 flex-wrap">
                <button
                  type="button"
                  onClick={() => setSelectedStar(0)}
                  className={`px-3 py-1.5 text-[12px] border transition-colors ${selectedStar === 0 ? 'bg-[#202020] text-white border-[#202020]' : 'border-[#D9D9D9] text-[#666] hover:border-[#202020]'}`}
                >
                  Tất cả
                </button>
                {[5, 4, 3, 2, 1].map((star) => (
                  <button
                    key={star}
                    type="button"
                    onClick={() => setSelectedStar(star)}
                    className={`px-3 py-1.5 text-[12px] border transition-colors ${selectedStar === star ? 'bg-[#202020] text-white border-[#202020]' : 'border-[#D9D9D9] text-[#666] hover:border-[#202020]'}`}
                  >
                    {star} sao
                  </button>
                ))}
              </div>

              {filteredReviews.length === 0 ? (
                <div className="border border-dashed border-[#D9D9D9] p-6 text-center">
                  <p className="text-[13px] text-[#666]">Chưa có đánh giá phù hợp với bộ lọc.</p>
                </div>
              ) : (
                <div className="space-y-4">
                  {filteredReviews.map((review) => (
                    <article key={review.id} className="border border-[#E8E8E8] p-5">
                      <div className="flex items-center justify-between gap-3 flex-wrap mb-2">
                        <div>
                          <p className="text-[13px] font-semibold text-[#202020]">{maskUser(review.userId)}</p>
                          <p className="text-[11px] text-[#888]">{formatDate(review.createdAt)}</p>
                        </div>
                        <StarRating value={Number(review.rating ?? review.star) || 0} size={16} />
                      </div>

                      <p className="text-[13px] leading-relaxed text-[#444] whitespace-pre-wrap">
                        {review.comment || review.content || 'Khách hàng chưa để lại nhận xét chi tiết.'}
                      </p>

                      {(Array.isArray(review.imageUrls) ? review.imageUrls : review.images || []).length > 0 && (
                        <div className="mt-3 grid grid-cols-3 sm:grid-cols-4 gap-2">
                          {(Array.isArray(review.imageUrls) ? review.imageUrls : review.images || []).slice(0, 4).map((imageUrl, index) => (
                            <img
                              key={`${review.id}-${index}`}
                              src={imageUrl}
                              alt={`Review image ${index + 1}`}
                              className="w-full h-20 object-cover border border-[#E8E8E8]"
                            />
                          ))}
                        </div>
                      )}
                    </article>
                  ))}
                </div>
              )}

              {!loading && totalPages > 1 && (
                <div className="mt-6 flex items-center justify-between gap-3 border-t border-[#EFEFEF] pt-4">
                  <button
                    type="button"
                    onClick={() => setCurrentPage((prev) => Math.max(0, prev - 1))}
                    disabled={currentPage === 0}
                    className={`h-9 px-3 text-[12px] border transition-colors ${currentPage === 0 ? 'border-[#E0E0E0] text-[#A0A0A0] cursor-not-allowed' : 'border-[#D9D9D9] text-[#444] hover:border-[#202020]'}`}
                  >
                    Trang trước
                  </button>

                  <p className="text-[12px] text-[#666] text-center">
                    Trang {currentPage + 1}/{totalPages} • {totalElements} đánh giá
                  </p>

                  <button
                    type="button"
                    onClick={() => setCurrentPage((prev) => Math.min(totalPages - 1, prev + 1))}
                    disabled={currentPage >= totalPages - 1}
                    className={`h-9 px-3 text-[12px] border transition-colors ${currentPage >= totalPages - 1 ? 'border-[#E0E0E0] text-[#A0A0A0] cursor-not-allowed' : 'border-[#D9D9D9] text-[#444] hover:border-[#202020]'}`}
                  >
                    Trang sau
                  </button>
                </div>
              )}
            </div>
          </div>
        </>
      )}
    </section>
  )
}
