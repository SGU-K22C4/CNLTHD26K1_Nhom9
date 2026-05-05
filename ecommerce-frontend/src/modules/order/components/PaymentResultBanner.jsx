import { CheckCircle2, CreditCard, Clock, ShoppingBag, RotateCcw, Home } from 'lucide-react'

const VARIANTS = {
  success: {
    gradient: 'from-[#D1FAE5] to-[#E8F5E1]',
    border: 'border-[#86EFAC]',
    iconBg: 'bg-[#10B981]',
    Icon: CheckCircle2,
    titleColor: 'text-[#065F46]',
    messageColor: 'text-[#047857]',
  },
  failed: {
    gradient: 'from-[#FEE2E2] to-[#FEF2F2]',
    border: 'border-[#FCA5A5]',
    iconBg: 'bg-[#EF4444]',
    Icon: CreditCard,
    titleColor: 'text-[#991B1B]',
    messageColor: 'text-[#B91C1C]',
  },
  processing: {
    gradient: 'from-[#FEF3C7] to-[#FFFBEB]',
    border: 'border-[#FCD34D]',
    iconBg: 'bg-[#F59E0B]',
    Icon: Clock,
    titleColor: 'text-[#92400E]',
    messageColor: 'text-[#B45309]',
  },
}

/**
 * Shared payment result banner used across OrderDetailPage and VNPayReturnPage.
 *
 * @param {'success'|'failed'|'processing'} variant - Banner variant
 * @param {string} title - Banner heading text
 * @param {string} message - Banner body text
 * @param {string} [orderNumber] - Order number to display (optional)
 * @param {string} [transactionNo] - VNPay transaction number (optional)
 * @param {Function} [onContinueShopping] - Navigate to home/products
 * @param {Function} [onViewOrder] - Navigate to order detail
 * @param {Function} [onRetry] - Navigate back to checkout to retry
 * @param {boolean} [showActions=false] - Whether to show action buttons below the banner
 * @param {string} [className] - Additional CSS classes
 */
export default function PaymentResultBanner({
  variant = 'success',
  title,
  message,
  orderNumber,
  transactionNo,
  onContinueShopping,
  onViewOrder,
  onRetry,
  showActions = false,
  className = '',
}) {
  const config = VARIANTS[variant] || VARIANTS.success
  const { gradient, border, iconBg, Icon, titleColor, messageColor } = config

  return (
    <div className={className}>
      {/* Banner */}
      <div className={`animate-scale-in bg-gradient-to-r ${gradient} ${border} border rounded-xl p-6 flex items-center gap-4`}>
        <div className={`w-14 h-14 rounded-full ${iconBg} flex items-center justify-center shrink-0`}>
          <Icon size={28} className="text-white" />
        </div>
        <div>
          <h2 className={`text-lg font-bold ${titleColor}`}>{title}</h2>
          <p className={`text-sm ${messageColor} mt-1`}>{message}</p>
          {orderNumber && (
            <p className={`text-sm ${messageColor} mt-1`}>
              Mã đơn hàng: <strong>{orderNumber}</strong>
            </p>
          )}
          {transactionNo && (
            <p className={`text-sm ${messageColor} mt-1`}>
              Mã giao dịch VNPay: <strong>{transactionNo}</strong>
            </p>
          )}
        </div>
      </div>

      {/* Optional action buttons */}
      {showActions && (
        <div className="flex items-center justify-center gap-3 mt-6">
          {variant === 'success' && (
            <>
              {onContinueShopping && (
                <button onClick={onContinueShopping}
                  className="flex items-center gap-2 px-6 py-3 bg-[#5A6D57] hover:bg-[#4A5D47] text-white text-sm font-semibold transition-colors rounded-lg">
                  <ShoppingBag size={16} />
                  Tiếp tục mua sắm
                </button>
              )}
              {onViewOrder && (
                <button onClick={onViewOrder}
                  className="flex items-center gap-2 px-6 py-3 bg-white border border-[#5A6D57] text-[#5A6D57] text-sm font-semibold transition-colors rounded-lg hover:bg-[#F5F6F3]">
                  Xem chi tiết đơn hàng
                </button>
              )}
            </>
          )}
          {variant === 'failed' && (
            <>
              {onRetry && (
                <button onClick={onRetry}
                  className="flex items-center gap-2 px-6 py-3 bg-[#5A6D57] hover:bg-[#4A5D47] text-white text-sm font-semibold transition-colors rounded-lg">
                  <RotateCcw size={16} />
                  Thử lại
                </button>
              )}
              {onContinueShopping && (
                <button onClick={onContinueShopping}
                  className="flex items-center gap-2 px-6 py-3 bg-white border border-[#dfdfdf] text-[#202020] text-sm font-semibold transition-colors rounded-lg hover:bg-[#F5F6F3]">
                  <Home size={16} />
                  Về trang chủ
                </button>
              )}
            </>
          )}
        </div>
      )}
    </div>
  )
}
