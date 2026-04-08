import { useEffect, useState } from 'react'
import { useSearchParams, useNavigate } from 'react-router-dom'
import { paymentService } from '../services/paymentService'

export default function VNPayReturnPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const [loading, setLoading] = useState(true)
  const [result, setResult] = useState(null)

  useEffect(() => {
    const verify = async () => {
      try {
        // Forward ALL query params from VNPay to our backend for verification
        const queryString = searchParams.toString()
        const data = await paymentService.verifyVnpayPayment(queryString)
        setResult(data)

        // If payment successful, redirect to order detail page
        if (data.success) {
          // Clear cart after successful VNPay payment
          const guestId = localStorage.getItem('guestId')
          if (guestId) {
            const API_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
            try {
              await fetch(`${API_URL}/api/v1/cart`, {
                method: 'DELETE',
                headers: { 'X-User-Id': guestId },
              })
            } catch (e) {
              console.warn('Failed to clear cart:', e)
            }
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
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '60vh' }}>
        <div style={{ textAlign: 'center', maxWidth: 500, padding: 40 }}>
          <div style={{ fontSize: 64, marginBottom: 16 }}>✅</div>
          <h1 style={{ fontSize: 24, fontWeight: 700, color: '#202020', marginBottom: 8 }}>Thanh toán thành công!</h1>
          <p style={{ color: '#666', marginBottom: 8 }}>Mã đơn hàng: <strong>{result.orderNumber}</strong></p>
          {result.transactionNo && (
            <p style={{ color: '#666', marginBottom: 24 }}>Mã giao dịch VNPay: <strong>{result.transactionNo}</strong></p>
          )}
          <p style={{ color: '#888', fontSize: 14, marginBottom: 24 }}>
            Cảm ơn bạn đã mua hàng. Đơn hàng của bạn đã được xác nhận và đang được xử lý.
          </p>
          <div style={{ display: 'flex', gap: 12, justifyContent: 'center' }}>
            <button
              onClick={() => navigate('/')}
              style={{
                padding: '12px 32px', backgroundColor: '#5A6D57', color: '#fff',
                border: 'none', cursor: 'pointer', fontSize: 14, fontWeight: 600
              }}
            >
              Tiếp tục mua sắm
            </button>
            <button
              onClick={() => navigate(`/orders/${result.orderNumber}`)}
              style={{
                padding: '12px 32px', backgroundColor: '#fff', color: '#5A6D57',
                border: '1px solid #5A6D57', cursor: 'pointer', fontSize: 14, fontWeight: 600
              }}
            >
              Xem chi tiết đơn hàng
            </button>
          </div>
        </div>
      </div>
    )
  }

  // Payment failed
  return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '60vh' }}>
      <div style={{ textAlign: 'center', maxWidth: 500, padding: 40 }}>
        <div style={{ fontSize: 64, marginBottom: 16 }}>❌</div>
        <h1 style={{ fontSize: 24, fontWeight: 700, color: '#202020', marginBottom: 8 }}>Thanh toán thất bại</h1>
        <p style={{ color: '#666', marginBottom: 24 }}>{result?.message || 'Đã xảy ra lỗi trong quá trình thanh toán.'}</p>
        <div style={{ display: 'flex', gap: 12, justifyContent: 'center' }}>
          <button
            onClick={() => navigate('/checkout')}
            style={{
              padding: '12px 32px', backgroundColor: '#5A6D57', color: '#fff',
              border: 'none', cursor: 'pointer', fontSize: 14, fontWeight: 600
            }}
          >
            Thử lại
          </button>
          <button
            onClick={() => navigate('/')}
            style={{
              padding: '12px 32px', backgroundColor: '#fff', color: '#202020',
              border: '1px solid #dfdfdf', cursor: 'pointer', fontSize: 14, fontWeight: 600
            }}
          >
            Về trang chủ
          </button>
        </div>
      </div>
    </div>
  )
}
