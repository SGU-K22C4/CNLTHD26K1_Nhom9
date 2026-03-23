import { useNavigate } from 'react-router-dom'
import { CheckCircle2 } from 'lucide-react'

export default function PaymentSuccessPage() {
  const navigate = useNavigate()

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
        Payment Successful
      </h2>

      {/* Thank you message */}
      <p className="font-normal text-[20px] text-[#404040] leading-[1.8] max-w-[720px] mb-2 capitalize">
        Thank You For Choosing Modimal, Your Order Will Be Generated Based On Your Delivery Request.
      </p>
      <p className="font-normal text-[20px] text-[#404040] leading-[1.8] mb-10 capitalize">
        The Receipt Has Been Sent To Your Email
      </p>

      {/* Contact info */}
      <div className="flex flex-col items-center gap-1">
        <p className="font-normal text-[16px] text-[#404040] leading-[1.8] capitalize">
          Please Contact Us For Any Query
        </p>
        <p className="font-normal text-[16px] text-[#404040] leading-[1.8]">
          +1(929)460-3208
        </p>
        <p className="font-normal text-[16px] text-[#404040] leading-[1.8] uppercase">
          OR
        </p>
        <p className="font-normal text-[16px] text-[#404040] leading-[1.8] capitalize">
          Hello @ Modimal.Com
        </p>
      </div>

      {/* Back to home button */}
      <button
        onClick={() => navigate('/')}
        className="mt-12 bg-[#5A6D57] hover:bg-[#4A5D23] text-white text-[14px] font-normal leading-6 px-8 py-3 capitalize transition-colors"
      >
        Continue Shopping
      </button>
    </div>
  )
}
