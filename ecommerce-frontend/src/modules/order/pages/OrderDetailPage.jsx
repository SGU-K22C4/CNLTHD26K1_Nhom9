import { useEffect, useState, useRef, useCallback } from 'react'
import { useParams, useNavigate, useSearchParams } from 'react-router-dom'
import {
  CheckCircle2,
  Package,
  Truck,
  MapPin,
  Phone,
  User,
  CreditCard,
  Printer,
  ArrowLeft,
  ShoppingBag,
  Clock,
  Hash,
  Calendar,
  Receipt,
  XCircle,
  Timer,
} from 'lucide-react'
import { orderService } from '../services/orderService'
import { formatCurrency, formatDateTime } from '../../../shared/utils/format'
import PaymentResultBanner from '../components/PaymentResultBanner'

/** Grace period in minutes — must match backend CANCEL_GRACE_PERIOD_MINUTES */
const CANCEL_GRACE_MINUTES = 15

/* ─── Status map ─── */
const STATUS_MAP = {
  PENDING: { label: 'Chờ xác nhận', color: '#F59E0B', bg: '#FEF3C7', icon: Clock },
  CONFIRMED: { label: 'Đã xác nhận', color: '#10B981', bg: '#D1FAE5', icon: CheckCircle2 },
  PROCESSING: { label: 'Đang xử lý', color: '#3B82F6', bg: '#DBEAFE', icon: Package },
  SHIPPED: { label: 'Đang giao hàng', color: '#8B5CF6', bg: '#EDE9FE', icon: Truck },
  DELIVERED: { label: 'Đã giao hàng', color: '#059669', bg: '#D1FAE5', icon: CheckCircle2 },
  CANCELLED: { label: 'Đã hủy', color: '#EF4444', bg: '#FEE2E2', icon: null },
  RETURNED: { label: 'Đã trả hàng', color: '#6B7280', bg: '#F3F4F6', icon: null },
}

const PAYMENT_STATUS_MAP = {
  PENDING: { label: 'Chưa thanh toán', color: '#F59E0B', bg: '#FEF3C7' },
  PAID: { label: 'Đã thanh toán', color: '#10B981', bg: '#D1FAE5' },
  FAILED: { label: 'Thanh toán thất bại', color: '#EF4444', bg: '#FEE2E2' },
  REFUNDED: { label: 'Đã hoàn tiền', color: '#6B7280', bg: '#F3F4F6' },
}

const PAYMENT_METHOD_MAP = {
  COD: 'Thanh toán khi nhận hàng (COD)',
  VNPAY: 'VNPay',
  BANK_TRANSFER: 'Chuyển khoản ngân hàng',
  MOMO: 'Ví MoMo',
}

export default function OrderDetailPage() {
  const { orderId } = useParams()
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const [order, setOrder] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [isCancelling, setIsCancelling] = useState(false)
  const [cancelSecondsLeft, setCancelSecondsLeft] = useState(0)
  const printRef = useRef(null)

  const isFromPayment = searchParams.get('from') === 'payment'
  const isFromCod = searchParams.get('from') === 'cod'

  useEffect(() => {
    const fetchOrder = async () => {
      try {
        setLoading(true)
        let data
        // Try fetching by ID first (numeric), then by order number
        if (/^\d+$/.test(orderId)) {
          data = await orderService.getById(orderId)
        } else {
          data = await orderService.getByOrderNumber(orderId)
        }
        setOrder(data)
      } catch (err) {
        console.error('Failed to fetch order:', err)
        setError('Không tìm thấy đơn hàng. Vui lòng kiểm tra lại.')
      } finally {
        setLoading(false)
      }
    }
    if (orderId) fetchOrder()
  }, [orderId])

  // ── Smart Polling: tự động cập nhật trạng thái khi đơn hàng đang PENDING ──
  useEffect(() => {
    // Chỉ poll khi đơn hàng đang ở trạng thái PENDING
    if (!order || order.status !== 'PENDING') return

    const pollOrder = async () => {
      try {
        const data = /^\d+$/.test(orderId)
          ? await orderService.getById(orderId)
          : await orderService.getByOrderNumber(orderId)
        setOrder(data)
      } catch (err) {
        console.error('Polling failed:', err)
      }
    }

    const interval = setInterval(pollOrder, 10_000) // Mỗi 10 giây
    return () => clearInterval(interval) // Dọn dẹp khi rời trang hoặc status đổi
  }, [order?.status, orderId])

  // ── Countdown timer for cancel grace period ──
  useEffect(() => {
    if (!order?.createdAt) return
    const isCancellable = order.status === 'PENDING'
    if (!isCancellable) return

    const calcRemaining = () => {
      // Backend (Docker/UTC) trả về LocalDateTime không có timezone suffix.
      // Thêm 'Z' để JavaScript hiểu đúng đây là giờ UTC.
      const raw = order.createdAt
      const created = new Date(raw.endsWith('Z') || raw.includes('+') ? raw : raw + 'Z').getTime()
      const deadline = created + CANCEL_GRACE_MINUTES * 60 * 1000
      return Math.max(0, Math.floor((deadline - Date.now()) / 1000))
    }

    setCancelSecondsLeft(calcRemaining())
    const timer = setInterval(() => {
      const remaining = calcRemaining()
      setCancelSecondsLeft(remaining)
      if (remaining <= 0) clearInterval(timer)
    }, 1000)

    return () => clearInterval(timer)
  }, [order?.createdAt, order?.status])

  const canCancel = cancelSecondsLeft > 0 && order?.status === 'PENDING'

  const formatCountdown = useCallback((totalSeconds) => {
    const m = Math.floor(totalSeconds / 60)
    const s = totalSeconds % 60
    return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
  }, [])

  /* ─── Reusable Cancel Button ─── */
  const CancelButton = ({ size = 'sm' }) => {
    const px = size === 'lg' ? 'px-6 py-3' : 'px-4 py-2.5'
    if (canCancel) {
      return (
        <button
          onClick={handleCancelOrder}
          disabled={isCancelling}
          className={`flex items-center gap-2 ${px} border border-[#EF4444] bg-white hover:bg-[#FEE2E2] text-sm text-[#EF4444] font-medium transition-all rounded-lg hover:shadow-sm disabled:opacity-50`}
        >
          <XCircle size={16} />
          {isCancelling ? 'Đang hủy...' : 'Hủy đơn hàng'}
          <span className="inline-flex items-center gap-1 bg-[#FEE2E2] text-[#EF4444] text-xs font-mono px-1.5 py-0.5 rounded">
            <Timer size={12} />
            {formatCountdown(cancelSecondsLeft)}
          </span>
        </button>
      )
    }
    if (order?.status === 'PENDING') {
      return (
        <div className={`flex items-center gap-2 ${px} bg-[#FFF8E1] border border-[#FFE082] text-sm text-[#B8860B] font-medium rounded-lg`}>
          <Clock size={16} />
          Đã hết thời gian hủy đơn. Liên hệ Hotline để được hỗ trợ.
        </div>
      )
    }
    return null
  }

  const handlePrint = () => {
    window.print()
  }

  const handleCancelOrder = async () => {
    if (!window.confirm('Bạn có chắc chắn muốn hủy đơn hàng này không?')) return
    try {
      setIsCancelling(true)
      const updatedOrder = await orderService.cancel(order.id)
      setOrder(updatedOrder)
    } catch (err) {
      console.error('Failed to cancel order:', err)
      const msg = err?.response?.data?.message
        || err?.response?.data?.error
        || 'Có lỗi xảy ra khi hủy đơn hàng. Vui lòng thử lại.'
      alert(msg)
    } finally {
      setIsCancelling(false)
    }
  }

  /* ─── Loading State ─── */
  if (loading) {
    return (
      <div className="min-h-[70vh] flex items-center justify-center font-[Montserrat]">
        <div className="text-center">
          <div
            className="w-12 h-12 border-4 border-[#e5e5e5] border-t-[#5A6D57] rounded-full mx-auto mb-4"
            style={{ animation: 'spin 1s linear infinite' }}
          />
          <p className="text-[#666] text-base">Đang tải thông tin đơn hàng...</p>
          <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
        </div>
      </div>
    )
  }

  /* ─── Error State ─── */
  if (error || !order) {
    return (
      <div className="min-h-[70vh] flex flex-col items-center justify-center font-[Montserrat] px-4">
        <div className="w-16 h-16 rounded-full bg-[#FEE2E2] flex items-center justify-center mb-6">
          <FileText size={28} className="text-[#EF4444]" />
        </div>
        <h2 className="text-2xl font-bold text-[#202020] mb-3">Không tìm thấy đơn hàng</h2>
        <p className="text-[#666] mb-8">{error || 'Đơn hàng không tồn tại hoặc đã bị xóa.'}</p>
        <button
          onClick={() => navigate('/')}
          className="bg-[#5A6D57] hover:bg-[#4A5D47] text-white px-8 py-3 text-sm font-medium transition-colors"
        >
          Về trang chủ
        </button>
      </div>
    )
  }

  const status = STATUS_MAP[order.status] || STATUS_MAP.PENDING
  const paymentStatus = PAYMENT_STATUS_MAP[order.paymentStatus] || PAYMENT_STATUS_MAP.PENDING
  const StatusIcon = status.icon
  const isPaidOrder = order.paymentStatus === 'PAID' && order.status !== 'CANCELLED'
  const isFailedOrder = order.paymentStatus === 'FAILED' || order.status === 'CANCELLED'
  const isProcessingPaymentResult = !isPaidOrder && !isFailedOrder

  return (
    <div className="font-[Montserrat] bg-[#FAFBF9] min-h-[70vh] print:bg-white">
      {/* ─── CSS for print ─── */}
      <style>{`
        @media print {
          header, footer, nav, .no-print { display: none !important; }
          .print-container { padding: 0 !important; max-width: 100% !important; }
          .print-shadow { box-shadow: none !important; border: 1px solid #e5e5e5 !important; }
        }
        @keyframes fadeInUp {
          from { opacity: 0; transform: translateY(20px); }
          to { opacity: 1; transform: translateY(0); }
        }
        @keyframes scaleIn {
          from { opacity: 0; transform: scale(0.8); }
          to { opacity: 1; transform: scale(1); }
        }
        .animate-fade-in-up { animation: fadeInUp 0.6s ease-out; }
        .animate-scale-in { animation: scaleIn 0.5s ease-out; }
        .animate-delay-1 { animation-delay: 0.1s; animation-fill-mode: both; }
        .animate-delay-2 { animation-delay: 0.2s; animation-fill-mode: both; }
        .animate-delay-3 { animation-delay: 0.3s; animation-fill-mode: both; }
        .animate-delay-4 { animation-delay: 0.4s; animation-fill-mode: both; }
      `}</style>

      <div className="max-w-[900px] mx-auto px-4 sm:px-6 py-8 sm:py-12 print-container">

        {/* ─── Payment Result Banner (only when returning from payment) ─── */}
        {/* ─── COD Success Banner ─── */}
        {isFromCod && (
          <PaymentResultBanner
            variant="success"
            title="Đặt hàng thành công!"
            message="Cảm ơn bạn đã mua sắm tại Modimal. Đơn hàng sẽ được thanh toán khi nhận hàng (COD)."
            className="mb-8"
          />
        )}

        {/* ─── VNPay Payment Result Banners ─── */}
        {isFromPayment && isPaidOrder && (
          <PaymentResultBanner
            variant="success"
            title="Đặt hàng thành công!"
            message="Cảm ơn bạn đã mua sắm tại Modimal. Đơn hàng của bạn đang được xử lý."
            className="mb-8"
          />
        )}

        {isFromPayment && isFailedOrder && (
          <PaymentResultBanner
            variant="failed"
            title="Thanh toán chưa hoàn tất"
            message={`Đơn hàng đang ở trạng thái ${status.label.toLowerCase()} / ${paymentStatus.label.toLowerCase()}. Vui lòng kiểm tra lịch sử giao dịch hoặc liên hệ hỗ trợ.`}
            className="mb-8"
          />
        )}

        {isFromPayment && isProcessingPaymentResult && (
          <PaymentResultBanner
            variant="processing"
            title="Đang xác nhận thanh toán"
            message="Kết quả thanh toán đang được đồng bộ qua hệ thống. Vui lòng tải lại trang sau ít phút để xem trạng thái mới nhất."
            className="mb-8"
          />
        )}

        {/* ─── Header ─── */}
        <div className="animate-fade-in-up flex flex-col sm:flex-row sm:items-center justify-between mb-8 gap-4">
          <div>
            <div className="flex items-center gap-2 mb-1">
              <Receipt size={20} className="text-[#5A6D57]" />
              <h1 className="text-2xl sm:text-3xl font-bold text-[#202020]">Chi tiết đơn hàng</h1>
            </div>
            <p className="text-sm text-[#888] ml-7">
              Mã đơn: <span className="font-semibold text-[#5A6D57]">{order.orderNumber}</span>
            </p>
          </div>
          <div className="flex items-center gap-3 no-print flex-wrap">
            <button
              onClick={handlePrint}
              className="flex items-center gap-2 px-4 py-2.5 border border-[#dfdfdf] bg-white hover:bg-[#F5F6F3] text-sm text-[#404040] font-medium transition-all rounded-lg hover:shadow-sm"
            >
              <Printer size={16} />
              In hóa đơn
            </button>
            <CancelButton />
            <button
              onClick={() => navigate('/')}
              className="flex items-center gap-2 px-4 py-2.5 bg-[#5A6D57] hover:bg-[#4A5D47] text-white text-sm font-medium transition-all rounded-lg hover:shadow-md"
            >
              <ShoppingBag size={16} />
              Tiếp tục mua sắm
            </button>
          </div>
        </div>

        {/* ─── Invoice Card ─── */}
        <div ref={printRef} className="animate-fade-in-up animate-delay-1 bg-white rounded-2xl shadow-[0_4px_24px_rgba(0,0,0,0.06)] overflow-hidden print-shadow">

          {/* Invoice Top Bar */}
          <div className="bg-gradient-to-r from-[#5A6D57] to-[#748C70] px-6 sm:px-8 py-6">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              {/* Logo */}
              <div className="flex flex-col items-start gap-0.5">
                <div className="flex items-end gap-[2px]">
                  <span className="font-['League_Spartan',sans-serif] font-bold text-[28px] text-white tracking-[2.8px] leading-none">
                    modimal
                  </span>
                  <span className="w-[8px] h-[8px] rounded-full bg-[#D1FAE5] mb-[3px] shrink-0 block" />
                </div>
                <p className="font-['League_Spartan',sans-serif] text-[9px] text-white/70 tracking-[1px] leading-none">
                  women clothing
                </p>
              </div>
              {/* Status badges */}
              <div className="flex items-center gap-3 flex-wrap">
                <span
                  className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-full text-xs font-semibold"
                  style={{ backgroundColor: status.bg, color: status.color }}
                >
                  {StatusIcon && <StatusIcon size={14} />}
                  {status.label}
                </span>
                <span
                  className="inline-flex items-center gap-1.5 px-3.5 py-1.5 rounded-full text-xs font-semibold"
                  style={{ backgroundColor: paymentStatus.bg, color: paymentStatus.color }}
                >
                  <CreditCard size={14} />
                  {paymentStatus.label}
                </span>
              </div>
            </div>
          </div>

          {/* Order Meta */}
          <div className="px-6 sm:px-8 py-5 bg-[#FAFBF9] border-b border-[#eee] grid grid-cols-2 sm:grid-cols-4 gap-4">
            <div>
              <div className="flex items-center gap-1.5 text-xs text-[#888] mb-1">
                <Hash size={12} />
                Mã đơn hàng
              </div>
              <p className="text-sm font-semibold text-[#202020]">{order.orderNumber}</p>
            </div>
            <div>
              <div className="flex items-center gap-1.5 text-xs text-[#888] mb-1">
                <Calendar size={12} />
                Ngày đặt
              </div>
              <p className="text-sm font-semibold text-[#202020]">
                {order.createdAt ? formatDateTime(order.createdAt) : '—'}
              </p>
            </div>
            <div>
              <div className="flex items-center gap-1.5 text-xs text-[#888] mb-1">
                <CreditCard size={12} />
                Thanh toán
              </div>
              <p className="text-sm font-semibold text-[#202020]">
                {PAYMENT_METHOD_MAP[order.paymentMethod] || order.paymentMethod}
              </p>
            </div>
            <div>
              <div className="flex items-center gap-1.5 text-xs text-[#888] mb-1">
                <Package size={12} />
                Số sản phẩm
              </div>
              <p className="text-sm font-semibold text-[#202020]">
                {order.items?.reduce((acc, item) => acc + item.quantity, 0) || 0} sản phẩm
              </p>
            </div>
          </div>

          {/* Shipping Info */}
          <div className="px-6 sm:px-8 py-6 border-b border-[#eee]">
            <h3 className="text-sm font-bold text-[#202020] uppercase tracking-wider mb-4 flex items-center gap-2">
              <Truck size={16} className="text-[#5A6D57]" />
              Thông tin giao hàng
            </h3>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="flex items-start gap-3">
                <div className="w-9 h-9 rounded-lg bg-[#F0F3EE] flex items-center justify-center shrink-0 mt-0.5">
                  <User size={16} className="text-[#5A6D57]" />
                </div>
                <div>
                  <p className="text-xs text-[#888] mb-0.5">Người nhận</p>
                  <p className="text-sm font-semibold text-[#202020]">{order.recipientName || '—'}</p>
                </div>
              </div>
              <div className="flex items-start gap-3">
                <div className="w-9 h-9 rounded-lg bg-[#F0F3EE] flex items-center justify-center shrink-0 mt-0.5">
                  <Phone size={16} className="text-[#5A6D57]" />
                </div>
                <div>
                  <p className="text-xs text-[#888] mb-0.5">Số điện thoại</p>
                  <p className="text-sm font-semibold text-[#202020]">{order.recipientPhone || '—'}</p>
                </div>
              </div>
              <div className="flex items-start gap-3 sm:col-span-2">
                <div className="w-9 h-9 rounded-lg bg-[#F0F3EE] flex items-center justify-center shrink-0 mt-0.5">
                  <MapPin size={16} className="text-[#5A6D57]" />
                </div>
                <div>
                  <p className="text-xs text-[#888] mb-0.5">Địa chỉ</p>
                  <p className="text-sm font-semibold text-[#202020]">{order.shippingAddress || '—'}</p>
                </div>
              </div>
            </div>
            {order.note && (
              <div className="mt-4 p-3 bg-[#FFF8E1] border border-[#FFE082] rounded-lg">
                <p className="text-xs text-[#B8860B] font-medium mb-0.5">Ghi chú:</p>
                <p className="text-sm text-[#795548]">{order.note}</p>
              </div>
            )}
          </div>

          {/* Product Items */}
          <div className="px-6 sm:px-8 py-6 border-b border-[#eee]">
            <h3 className="text-sm font-bold text-[#202020] uppercase tracking-wider mb-5 flex items-center gap-2">
              <ShoppingBag size={16} className="text-[#5A6D57]" />
              Sản phẩm đã đặt
            </h3>

            {/* Table header - desktop */}
            <div className="hidden sm:grid grid-cols-[1fr_100px_120px_120px] gap-4 pb-3 border-b border-[#eee] text-xs text-[#888] uppercase tracking-wider font-medium">
              <span>Sản phẩm</span>
              <span className="text-center">Số lượng</span>
              <span className="text-right">Đơn giá</span>
              <span className="text-right">Thành tiền</span>
            </div>

            {/* Product rows */}
            {order.items?.map((item, idx) => (
              <div
                key={item.id || idx}
                className="grid grid-cols-1 sm:grid-cols-[1fr_100px_120px_120px] gap-2 sm:gap-4 py-4 border-b border-[#f5f5f5] last:border-b-0 items-center hover:bg-[#FAFBF9] transition-colors rounded-lg"
              >
                {/* Product info */}
                <div className="flex items-center gap-4">
                  {item.imageUrl ? (
                    <img
                      src={item.imageUrl}
                      alt={item.productName}
                      className="w-16 h-20 sm:w-[72px] sm:h-[88px] object-cover rounded-lg border border-[#eee] shrink-0"
                    />
                  ) : (
                    <div className="w-16 h-20 sm:w-[72px] sm:h-[88px] bg-[#F0F3EE] rounded-lg flex items-center justify-center shrink-0">
                      <Package size={24} className="text-[#aaa]" />
                    </div>
                  )}
                  <div className="min-w-0">
                    <p className="text-sm font-semibold text-[#202020] leading-snug line-clamp-2">
                      {item.productName}
                    </p>
                    <div className="flex flex-wrap items-center gap-2 mt-1.5">
                      {item.color && (
                        <span className="inline-flex items-center gap-1 text-xs text-[#666] bg-[#F5F6F3] px-2 py-0.5 rounded">
                          Màu: {item.color}
                        </span>
                      )}
                      {item.size && (
                        <span className="inline-flex items-center gap-1 text-xs text-[#666] bg-[#F5F6F3] px-2 py-0.5 rounded">
                          Size: {item.size}
                        </span>
                      )}
                    </div>
                    {/* Mobile price info */}
                    <div className="flex items-center gap-3 mt-2 sm:hidden text-xs text-[#888]">
                      <span>SL: {item.quantity}</span>
                      <span>×</span>
                      <span>{formatCurrency(item.unitPrice)}</span>
                      <span className="font-semibold text-[#202020]">= {formatCurrency(item.totalPrice)}</span>
                    </div>
                  </div>
                </div>

                {/* Quantity - desktop */}
                <div className="hidden sm:flex justify-center">
                  <span className="inline-flex items-center justify-center w-10 h-8 bg-[#F5F6F3] text-sm font-medium text-[#202020] rounded">
                    {item.quantity}
                  </span>
                </div>

                {/* Unit price - desktop */}
                <p className="hidden sm:block text-sm text-[#404040] text-right">
                  {formatCurrency(item.unitPrice)}
                </p>

                {/* Total price - desktop */}
                <p className="hidden sm:block text-sm font-bold text-[#202020] text-right">
                  {formatCurrency(item.totalPrice)}
                </p>
              </div>
            ))}
          </div>

          {/* Order Summary / Totals */}
          <div className="px-6 sm:px-8 py-6">
            <div className="flex justify-end">
              <div className="w-full sm:w-[320px] space-y-3">
                <div className="flex justify-between text-sm text-[#666]">
                  <span>Tạm tính</span>
                  <span className="font-medium text-[#202020]">{formatCurrency(order.subtotal)}</span>
                </div>
                <div className="flex justify-between text-sm text-[#666]">
                  <span>Phí vận chuyển</span>
                  <span className="font-medium text-[#202020]">
                    {order.shippingFee > 0 ? formatCurrency(order.shippingFee) : 'Miễn phí'}
                  </span>
                </div>
                {order.discount > 0 && (
                  <div className="flex justify-between text-sm text-[#10B981]">
                    <span className="flex items-center gap-1">
                      Giảm giá
                      {order.couponCode && (
                        <span className="text-[10px] bg-[#D1FAE5] text-[#065F46] px-1.5 py-0.5 rounded font-medium">
                          {order.couponCode}
                        </span>
                      )}
                    </span>
                    <span className="font-medium">-{formatCurrency(order.discount)}</span>
                  </div>
                )}
                {order.loyaltyDiscount > 0 && (
                  <div className="flex justify-between text-sm text-[#0f766e]">
                    <span className="flex items-center gap-1">
                      Điểm tích lũy
                      {order.usedPoints > 0 && (
                        <span className="text-[10px] bg-[#CCFBF1] text-[#115E59] px-1.5 py-0.5 rounded font-medium">
                          {order.usedPoints} điểm
                        </span>
                      )}
                    </span>
                    <span className="font-medium">-{formatCurrency(order.loyaltyDiscount)}</span>
                  </div>
                )}
                <div className="pt-3 border-t-2 border-[#5A6D57]">
                  <div className="flex justify-between items-baseline">
                    <span className="text-base font-bold text-[#202020]">Tổng cộng</span>
                    <span className="text-xl font-bold text-[#5A6D57]">{formatCurrency(order.total)}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

        </div>

        {/* ─── Bottom Actions ─── */}
        <div className="animate-fade-in-up animate-delay-3 flex flex-col sm:flex-row items-center justify-between gap-4 mt-8 no-print">
          <button
            onClick={() => navigate('/')}
            className="flex items-center gap-2 text-sm text-[#666] hover:text-[#5A6D57] font-medium transition-colors"
          >
            <ArrowLeft size={16} />
            Về trang chủ
          </button>
          <div className="flex items-center gap-3 flex-wrap">
            <button
              onClick={handlePrint}
              className="flex items-center gap-2 px-6 py-3 border border-[#dfdfdf] bg-white hover:bg-[#F5F6F3] text-sm text-[#404040] font-medium transition-all rounded-lg hover:shadow-sm"
            >
              <Printer size={16} />
              In hóa đơn
            </button>
            <CancelButton size="lg" />
            <button
              onClick={() => navigate('/products')}
              className="flex items-center gap-2 px-6 py-3 bg-[#5A6D57] hover:bg-[#4A5D47] text-white text-sm font-medium transition-all rounded-lg hover:shadow-md"
            >
              <ShoppingBag size={16} />
              Tiếp tục mua sắm
            </button>
          </div>
        </div>

        {/* ─── Support info ─── */}
        <div className="animate-fade-in-up animate-delay-4 mt-8 text-center no-print">
          <div className="inline-flex flex-col items-center gap-1.5 px-8 py-5 bg-white rounded-xl border border-[#eee] shadow-sm">
            <p className="text-xs text-[#888]">Bạn cần hỗ trợ? Liên hệ chúng tôi</p>
            <div className="flex items-center gap-4 text-sm">
              <span className="text-[#5A6D57] font-semibold">+84 (0) 123 456 789</span>
              <span className="text-[#ddd]">|</span>
              <span className="text-[#5A6D57] font-semibold">support@modimal.com</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}