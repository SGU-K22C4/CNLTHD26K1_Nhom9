import { useNavigate } from 'react-router-dom'
import { ChevronLeft } from 'lucide-react'

export default function PaymentFailedPage() {
  const navigate = useNavigate()

  return (
    <div className="min-h-[70vh] flex flex-col items-center justify-center font-[Montserrat] py-24 px-8 text-center">

      {/* Error icon — red circle with ! */}
      <div className="w-16 h-16 rounded-full bg-[#C30000] flex items-center justify-center mb-6">
        <span className="text-white font-bold text-[28px] leading-none select-none">!</span>
      </div>

      {/* Heading */}
      <h2 className="font-['Montserrat',sans-serif] font-bold text-[40px] text-[#C30000] leading-[1.4] mb-6">
        Sorry, Payment Failed
      </h2>

      {/* Body text */}
      <div className="flex flex-col gap-0 mb-10 max-w-[760px]">
        <p className="font-normal text-[20px] text-[#404040] leading-[1.8] capitalize">
          Unfortunately. Your Order Cannot Be Completed.
        </p>
        <p className="font-normal text-[20px] text-[#404040] leading-[1.8] capitalize">
          Please Ensure That The Billing Address You Provided Is The Same One Where Your Debit/Credit Card Is Registered.
        </p>
        <p className="font-normal text-[20px] text-[#404040] leading-[1.8] capitalize">
          Alternatively, Please Try A Different Payment Method.
        </p>
      </div>

      {/* Pay Now button */}
      <button
        onClick={() => navigate('/checkout/payment')}
        className="bg-[#5A6D57] hover:bg-[#4A5D23] text-white text-[16px] font-normal leading-[1.8] h-12 w-[280px] capitalize transition-colors mb-5"
      >
        Pay Now
      </button>

      {/* Back to My Orders link */}
      <button
        onClick={() => navigate('/')}
        className="flex items-center gap-1 text-[14px] text-[#404040] font-normal leading-6 hover:opacity-70 transition-opacity capitalize"
      >
        <ChevronLeft size={16} strokeWidth={2} />
        Back To My Orders
      </button>
    </div>
  )
}
