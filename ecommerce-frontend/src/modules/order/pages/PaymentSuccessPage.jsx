import { useNavigate, useSearchParams } from 'react-router-dom'
import { CheckCircle2 } from 'lucide-react'

export default function PaymentSuccessPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const orderId = searchParams.get('orderId')

  return (
    <div className="min-h-[70vh] flex flex-col items-center justify-center font-[Montserrat] py-24 px-8 text-center">

      {/* Check circle icon */}
      <CheckCircle2
        size={64}
        strokeWidth={1.5}
        className="text-[#00966D] mb-6"
      />

      {/* Heading */}
      <h2 className="font-['Montserrat',sans-serif] font-bold text-[40px] text-[#00966D] leading-[1.4] mb-6">
        Đặt hàng thành công!
      </h2>

      {/* Thank you message */}
      <p className="font-normal text-[20px] text-[#404040] leading-[1.8] max-w-[720px] mb-2">
        Cảm ơn bạn đã mua sắm tại Modimal. Đơn hàng của bạn đang được xử lý và sẽ sớm được giao.
      </p>
      <p className="font-normal text-[20px] text-[#404040] leading-[1.8] mb-10">
        Biên lai chi tiết đã được gửi tới email của bạn.
      </p>

      {/* Contact info */}
      <div className="flex flex-col items-center gap-1">
        <p className="font-normal text-[16px] text-[#404040] leading-[1.8]">
          Vui lòng liên hệ nếu bạn có bất kỳ thắc mắc nào:
        </p>
        <p className="font-normal text-[16px] text-[#404040] leading-[1.8]">
          +84 (0) 123 456 789
        </p>
        <p className="font-normal text-[16px] text-[#404040] leading-[1.8] uppercase">
          HOẶC
        </p>
        <p className="font-normal text-[16px] text-[#404040] leading-[1.8]">
          support@modimal.com
        </p>
      </div>

      {/* Action buttons */}
      <div className="flex flex-row items-center gap-4 mt-12">
        <button
          onClick={() => navigate('/')}
          className="bg-[#5A6D57] hover:bg-[#4A5D23] text-white text-[14px] font-normal leading-6 px-8 py-3 transition-colors"
        >
          Khám phá thêm sản phẩm
        </button>
        {orderId && (
          <button
            onClick={() => navigate(`/orders/${orderId}`)}
            className="bg-white border border-[#5A6D57] text-[#5A6D57] hover:bg-gray-50 text-[14px] font-normal leading-6 px-8 py-3 transition-colors"
          >
            Xem chi tiết đơn hàng
          </button>
        )}
      </div>
    </div>
  )
}
