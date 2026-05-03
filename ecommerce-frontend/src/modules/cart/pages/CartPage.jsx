import { useNavigate } from 'react-router-dom'
import { X, Minus, Plus } from 'lucide-react'
import { useCartContext } from '../hooks/useCartContext'
import { formatCurrency } from '../../../shared/utils/format'

const TAX_RATE = 0.08

/* ── Shared Order Summary block (used on mobile + desktop right column) ── */
const OrderSummaryFooter = ({ totalItems, subtotal, tax, total, onNext }) => (
  <div className="flex flex-col gap-4 pt-6 border-t border-[#dfdfdf]">
    <div className="flex items-center justify-between text-[#404040] text-sm lg:text-base">
      <span>Subtotal ({totalItems})</span>
      <span>{formatCurrency(subtotal)}</span>
    </div>
    <div className="flex items-center justify-between text-[#404040] text-sm lg:text-base">
      <span>Tax</span>
      <span>{formatCurrency(tax)}</span>
    </div>
    <div className="flex items-center justify-between text-[#404040] text-sm lg:text-base">
      <span>Shipping</span>
      <span>Free</span>
    </div>
    <div className="flex items-center justify-between text-[#202020] font-bold text-sm lg:text-lg pt-2 border-t border-[#dfdfdf]">
      <span>Order Totals:</span>
      <span>{formatCurrency(total)}</span>
    </div>
    <p className="text-[11px] text-[#202020] font-semibold leading-relaxed mt-1">
      The Total Amount You Pay Includes All Applicable Customs Duties &amp; Taxes. We Guarantee No Additional Charges On Delivery
    </p>
    <button
      onClick={onNext}
      className="w-full lg:w-auto lg:self-end bg-[#5A6D57] hover:bg-[#748C70] text-white text-sm font-[Montserrat] tracking-wide py-4 lg:px-14 lg:py-3 transition-colors mt-2"
    >
      Next
    </button>
  </div>
)

export default function CartPage() {
  const { items, removeItem, updateQuantity, subtotal, totalItems } = useCartContext()
  const navigate = useNavigate()

  const tax   = subtotal * TAX_RATE
  const total = subtotal + tax

  const handleNext = () => navigate('/checkout')

  return (
    <div className="min-h-screen bg-white font-[Montserrat]">
      <div className="max-w-[1440px] mx-auto px-4 py-6 lg:px-[80px] lg:py-12">

        {/* ── Top action bar ── */}
        <div className="flex items-center mb-6 lg:mb-10">
          <button
            onClick={() => navigate(-1)}
            className="text-sm text-[#404040] hover:opacity-70 transition-opacity shrink-0 mr-4 lg:mr-6"
          >
            Back
          </button>
          <h1 className="flex-1 text-center text-2xl lg:text-[32px] font-bold leading-[1.4] text-[#202020]">
            Your Cart
          </h1>
          {/* Continue Shopping — desktop only */}
          <button
            onClick={() => navigate('/')}
            className="hidden lg:block text-sm text-[#404040] hover:opacity-70 transition-opacity shrink-0 ml-6"
          >
            Continue Shopping
          </button>
          {/* Spacer on mobile to keep title centred */}
          <span className="lg:hidden w-[40px] shrink-0" />
        </div>

        {/* ════════════════════════════════════════
            MOBILE LAYOUT  (hidden on lg+)
        ════════════════════════════════════════ */}
        <div className="lg:hidden">
          {/* Section label */}
          <p className="text-sm text-[#202020] mb-4">Order Summary</p>

          {/* Item rows */}
          {items.map((item) => (
            <div key={item.id} className="relative flex items-start gap-4 py-5 border-b border-[#dfdfdf]">

              {/* Image with quantity badge */}
              <div className="relative shrink-0">
                <img
                  src={item.image}
                  alt={item.name}
                  className="w-[120px] h-[140px] object-cover"
                />
                <span className="absolute top-2 left-2 w-6 h-6 flex items-center justify-center bg-white text-[#202020] text-xs font-bold border border-[#dfdfdf]">
                  {item.quantity}
                </span>
              </div>

              {/* Info block */}
              <div className="flex-1 flex flex-col gap-1 pr-6 pt-1">
                <p className="text-sm font-bold text-[#202020]">{item.name}</p>
                {item.size  && <p className="text-sm text-[#404040]">Size: {item.size}</p>}
                {item.color && <p className="text-sm text-[#404040]">Color: {item.color}</p>}

                {/* Price + stepper on same row */}
                <div className="flex items-center justify-between mt-3">
                  <p className="text-sm font-bold text-[#202020]">
                    {formatCurrency(item.price * item.quantity)}
                  </p>
                  <div className="flex items-center border border-[#dfdfdf] bg-[#D1D9CF]">
                    <button
                      onClick={() => updateQuantity(item.id, item.quantity - 1)}
                      aria-label="Decrease"
                      className="flex h-8 w-8 items-center justify-center text-[#404040] hover:bg-black/5 transition-colors"
                    >
                      <Minus size={12} strokeWidth={2} />
                    </button>
                    <span className="flex h-8 w-8 items-center justify-center select-none text-sm text-[#202020]">
                      {item.quantity}
                    </span>
                    <button
                      onClick={() => updateQuantity(item.id, item.quantity + 1)}
                      aria-label="Increase"
                      className="flex h-8 w-8 items-center justify-center text-[#404040] hover:bg-black/5 transition-colors"
                    >
                      <Plus size={12} strokeWidth={2} />
                    </button>
                  </div>
                </div>
              </div>

              {/* X — absolute top-right */}
              <button
                onClick={() => removeItem(item.id)}
                aria-label={`Remove ${item.name}`}
                className="absolute top-5 right-0 text-[#202020] hover:opacity-50 transition-opacity"
              >
                <X size={18} strokeWidth={1.5} />
              </button>
            </div>
          ))}

          {/* Mobile order summary footer */}
          <div className="mt-6">
            <OrderSummaryFooter 
              totalItems={totalItems}
              subtotal={subtotal}
              tax={tax}
              total={total}
              onNext={handleNext}
            />
          </div>
        </div>

        {/* ════════════════════════════════════════
            DESKTOP LAYOUT  (hidden below lg)
        ════════════════════════════════════════ */}
        <div className="hidden lg:flex flex-row">

          {/* LEFT: Order Summary header + product rows */}
          <div className="w-1/2 pr-6">
            <div className="pb-4 border-b border-[#dfdfdf]">
              <span className="text-lg text-[#202020]">Order Summary</span>
            </div>
            {items.map((item) => (
              <div key={item.id} className="flex justify-between items-center py-8 border-b border-[#dfdfdf]">
                <div className="flex items-start gap-4 min-w-0">
                  <img
                    src={item.image}
                    alt={item.name}
                    className="w-[142px] h-[163px] object-cover shrink-0"
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

          {/* RIGHT: Price / Qty / Total columns + summary footer */}
          <div className="w-1/2 pl-6">
            <div className="grid grid-cols-3 gap-4 pb-4 border-b border-[#dfdfdf] text-[#404040]">
              <span className="text-lg">Price</span>
              <span className="text-lg text-center">Quantity</span>
              <span className="text-lg text-right">Total</span>
            </div>
            {items.map((item) => (
              <div key={item.id} className="grid grid-cols-3 gap-4 py-8 border-b border-[#dfdfdf] items-center">
                <span className="text-lg text-[#202020]">{formatCurrency(item.price)}</span>
                <div className="flex items-center justify-center">
                  <div className="flex items-center justify-between bg-[#D1D9CF] w-[88px] px-2 py-[5px]">
                    <button
                      onClick={() => updateQuantity(item.id, item.quantity - 1)}
                      aria-label="Decrease"
                      className="text-[#404E3E] hover:opacity-60 transition-opacity"
                    >
                      <Minus size={13} strokeWidth={2} />
                    </button>
                    <span className="text-[#404E3E] text-lg select-none min-w-[16px] text-center">
                      {item.quantity}
                    </span>
                    <button
                      onClick={() => updateQuantity(item.id, item.quantity + 1)}
                      aria-label="Increase"
                      className="text-[#404E3E] hover:opacity-60 transition-opacity"
                    >
                      <Plus size={13} strokeWidth={2} />
                    </button>
                  </div>
                </div>
                <span className="text-lg text-[#202020] text-right">
                  {formatCurrency(item.price * item.quantity)}
                </span>
              </div>
            ))}
            <div className="mt-8">
              <OrderSummaryFooter 
                totalItems={totalItems}
                subtotal={subtotal}
                tax={tax}
                total={total}
                onNext={handleNext}
              />
            </div>
          </div>

        </div>

      </div>
    </div>
  )
}
