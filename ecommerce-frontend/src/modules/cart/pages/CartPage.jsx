import { useNavigate } from 'react-router-dom'
import { X, Minus, Plus } from 'lucide-react'
import { useCartContext } from '../context/CartContext'

const TAX_RATE = 0.08

export default function CartPage() {
  const { items, removeItem, updateQuantity, subtotal, totalItems } = useCartContext()
  const navigate = useNavigate()

  const tax   = subtotal * TAX_RATE
  const total = subtotal + tax

  return (
    <div className="min-h-screen bg-white font-[Montserrat]">
      <div className="max-w-[1440px] mx-auto px-4 py-6 lg:px-[80px] lg:py-12">

        {/* ── Top action bar ── */}
        <div className="flex items-center mb-10">
          <button
            onClick={() => navigate(-1)}
            className="text-sm text-[#5A6D57] hover:opacity-70 transition-opacity mr-6 shrink-0"
          >
            Back
          </button>
          <h1 className="flex-1 text-[32px] font-semibold leading-[1.4] text-[#202020]">
            Your Cart
          </h1>
          <button
            onClick={() => navigate('/')}
            className="text-sm text-[#404040] hover:opacity-70 transition-opacity shrink-0"
          >
            Continue Shopping
          </button>
        </div>

        {/* ── Two-column container (50/50 split) ── */}
        <div className="flex flex-col lg:flex-row">

          {/* ===== LEFT BLOCK (w-1/2): Header + Product rows ===== */}
          <div className="w-full lg:w-1/2 lg:pr-6">
            {/* Header */}
            <div className="pb-4 border-b border-[#dfdfdf]">
              <span className="text-lg text-[#202020]">Order Summary</span>
            </div>

            {/* Product rows */}
            {items.map((item) => (
              <div key={item.id} className="flex justify-between items-center py-8 border-b border-[#dfdfdf]">
                <div className="flex items-start gap-4 min-w-0">
                  <img
                    src={item.image}
                    alt={item.name}
                    className="w-[80px] h-[100px] lg:w-[142px] lg:h-[163px] object-cover shrink-0"
                  />
                  <div className="pt-1">
                    <p className="text-base font-bold leading-[1.4] text-[#202020] capitalize mb-1">
                      {item.name}
                    </p>
                    <p className="text-base text-[#404040] leading-[1.8]">Size: {item.size}</p>
                    <p className="text-base text-[#404040] leading-[1.8]">Color: {item.color}</p>
                  </div>
                </div>
                <button
                  onClick={() => removeItem(item.id)}
                  aria-label={`Remove ${item.name}`}
                  className="text-[#202020] hover:opacity-50 transition-opacity self-start pt-1 shrink-0 ml-4"
                >
                  <X size={18} strokeWidth={1.5} />
                </button>
              </div>
            ))}
          </div>

          {/* ===== RIGHT BLOCK (w-1/2): Header + Price/Qty/Total rows + Summary ===== */}
          <div className="w-full lg:w-1/2 lg:pl-6 mt-8 lg:mt-0 pt-8 lg:pt-0 border-t border-[#dfdfdf] lg:border-t-0">
            {/* Header */}
            <div className="grid grid-cols-3 gap-4 pb-4 border-b border-[#dfdfdf] text-[#404040]">
              <span className="text-lg">Price</span>
              <span className="text-lg text-center">Quantity</span>
              <span className="text-lg text-right">Total</span>
            </div>

            {/* Data rows — same height as left side product rows */}
            {items.map((item) => (
              <div key={item.id} className="grid grid-cols-3 gap-4 py-8 border-b border-[#dfdfdf] items-center">
                {/* Price */}
                <span className="text-lg text-[#202020]">
                  ${item.price.toFixed(2)}
                </span>

                {/* Qty stepper */}
                <div className="flex items-center justify-center">
                  <div className="flex items-center justify-between bg-[#D1D9CF] w-[88px] px-2 py-[5px]">
                    <button
                      onClick={() => updateQuantity(item.id, item.quantity - 1)}
                      aria-label="Decrease"
                      className="text-[#404E3E] hover:opacity-60 transition-opacity leading-none"
                    >
                      <Minus size={13} strokeWidth={2} />
                    </button>
                    <span className="text-[#404E3E] text-lg leading-none select-none min-w-[16px] text-center">
                      {item.quantity}
                    </span>
                    <button
                      onClick={() => updateQuantity(item.id, item.quantity + 1)}
                      aria-label="Increase"
                      className="text-[#404E3E] hover:opacity-60 transition-opacity leading-none"
                    >
                      <Plus size={13} strokeWidth={2} />
                    </button>
                  </div>
                </div>

                {/* Row total */}
                <span className="text-lg text-[#202020] text-right">
                  ${(item.price * item.quantity).toFixed(2)}
                </span>
              </div>
            ))}

            {/* ── Order Summary (sits under Price/Qty/Total) ── */}
            <div className="border-t border-[#dfdfdf] pt-8 flex flex-col gap-4">
              <div className="flex items-center justify-between text-[#404040]">
                <span>Subtotal ({totalItems})</span>
                <span>${subtotal.toFixed(2)}</span>
              </div>

              <div className="flex items-center justify-between text-[#404040]">
                <span>Tax</span>
                <span>${tax.toFixed(2)}</span>
              </div>

              <div className="flex items-center justify-between text-[#404040]">
                <span>Shipping</span>
                <span>Free</span>
              </div>

              <div className="flex items-center justify-between text-[#202020] font-bold text-lg pt-2">
                <span>Total Orders:</span>
                <span>${total.toFixed(2)}</span>
              </div>

              <p className="text-[12px] text-[#202020] font-medium leading-relaxed mt-2">
                The total amount you pay includes all applicable customs duties &amp; taxes. We guarantee no additional charges on delivery
              </p>

              <div className="flex justify-end mt-4">
                <button
                  onClick={() => navigate('/checkout')}
                  className="bg-[#4A5D23] text-white px-14 py-3 font-medium hover:bg-opacity-90 transition-colors"
                >
                  Next
                </button>
              </div>
            </div>
          </div>

        </div>

      </div>
    </div>
  )
}
