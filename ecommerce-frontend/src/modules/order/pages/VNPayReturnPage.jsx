import { useEffect, useRef, useState } from 'react'
import { useSearchParams, useNavigate } from 'react-router-dom'
import { useCartContext } from '@/modules/cart/context/CartContext'
import { paymentService } from '../services/paymentService'
import PaymentResultBanner from '../components/PaymentResultBanner'

export default function VNPayReturnPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const { clearCart } = useCartContext()
  const [loading, setLoading] = useState(true)
  const [result, setResult] = useState(null)
  const hasVerifiedRef = useRef(false)

  useEffect(() => {
    if (hasVerifiedRef.current) {
      return
    }
    hasVerifiedRef.current = true

    const verify = async () => {
      try {
        // Forward ALL query params from VNPay to our backend for verification
        const queryString = searchParams.toString()
        const data = await paymentService.verifyVnpayPayment(queryString)
        setResult(data)

        // If payment successful, redirect to order detail page
        if (data.success) {
          // Clear cart after successful VNPay payment
          try {
            await clearCart()
          } catch (e) {
            console.warn('Failed to clear cart:', e)
          }

          // Use orderId if available, else orderNumber
          const id = data.orderId || data.orderNumber
          if (id) {
            navigate(`/orders/${id}?from=payment`, { replace: true })
            return
          }
        }
      } catch (err) {
        console.error('VNPay verification failed:', err)
        setResult({ success: false, message: 'Không thể xác thực thanh toán. Vui lòng liên hệ hỗ trợ.' })
      } finally {
        setLoading(false)
      }
    }
    verify()
  }, [searchParams, navigate])

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '60vh' }}>
        <div style={{ textAlign: 'center' }}>
          <div style={{
            width: 48, height: 48, border: '4px solid #e5e5e5', borderTop: '4px solid #5A6D57',
            borderRadius: '50%', animation: 'spin 1s linear infinite', margin: '0 auto 16px'
          }} />
          <p style={{ color: '#666', fontSize: 16 }}>Đang xác thực thanh toán VNPay...</p>
          <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
        </div>
      </div>
    )
  }

  if (result?.success) {
    // This is a fallback — normally we redirect above
    return (
      <div className="min-h-[60vh] flex items-center justify-center px-4">
        <div className="w-full max-w-[500px]">
          <PaymentResultBanner
            variant="success"
            title="Thanh toán thành công!"
            message="Cảm ơn bạn đã mua hàng. Đơn hàng của bạn đã được xác nhận và đang được xử lý."
            orderNumber={result.orderNumber}
            transactionNo={result.transactionNo}
            showActions
            onContinueShopping={() => navigate('/')}
            onViewOrder={() => navigate(`/orders/${result.orderNumber}`)}
          />
        </div>
      </div>
    )
  }

  // Payment failed
  return (
    <div className="min-h-[60vh] flex items-center justify-center px-4">
      <div className="w-full max-w-[500px]">
        <PaymentResultBanner
          variant="failed"
          title="Thanh toán thất bại"
          message={result?.message || 'Đã xảy ra lỗi trong quá trình thanh toán.'}
          showActions
          onRetry={() => navigate('/checkout')}
          onContinueShopping={() => navigate('/')}
        />
      </div>
    </div>
  )
}
