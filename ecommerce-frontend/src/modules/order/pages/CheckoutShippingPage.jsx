import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { X, Minus, Plus, ChevronLeft } from 'lucide-react'
import { useCartContext } from '../../cart/context/CartContext'

const TAX_RATE = 0.08

export default function CheckoutShippingPage() {
  const { items, removeItem, updateQuantity, subtotal, totalItems } = useCartContext()
  const navigate = useNavigate()

  const tax = subtotal * TAX_RATE
  const total = subtotal + tax

  const [selectedExpressDate, setSelectedExpressDate] = useState('')
  const [selectedGuaranteed, setSelectedGuaranteed] = useState('')

  const RadioCircle = ({ name, value, checked, onChange }) => (
    <button
      type="button"
      onClick={() => onChange(value)}
      className={`shrink-0 w-4 h-4 rounded-full border ${
        checked ? 'border-[#5A6D57] bg-[#5A6D57]' : 'border-[#A2B39F] bg-white'
      } flex items-center justify-center`}
      aria-pressed={checked}
    >
      {checked && <span className="w-[6px] h-[6px] rounded-full bg-white block" />}
    </button>
  )

  return (
    <div className="min-h-screen flex flex-col lg:flex-row font-[Montserrat]">

      {/* â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
          LEFT COLUMN â€” Shipping Form (55 %)
         â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â• */}
      <div className="w-full lg:w-[55%] bg-white pt-6 pb-12 px-4 lg:pt-8 lg:pb-24 lg:pl-24 lg:pr-16 flex flex-col">

        {/* ── Logo ── */}
        <div className="mb-10 flex flex-col items-start gap-1">
          <div className="flex items-end gap-[2px]">
            <span className="font-['League_Spartan',sans-serif] font-bold text-[32px] text-[#404040] tracking-[3.2px] leading-none">
              modimal
            </span>
            <span className="w-[10px] h-[10px] rounded-full bg-[#748C70] mb-[4px] shrink-0 block" />
          </div>
          <p className="font-['League_Spartan',sans-serif] font-normal text-[10px] text-[#404040] tracking-[1px] leading-none">
            women clothing
          </p>
        </div>

        {/* â”€â”€ Breadcrumbs â”€â”€ */}
        <nav className="flex items-center text-[18px] mb-8">
          <button onClick={() => navigate('/cart')} className="text-[#606060] font-normal leading-[1.8]">Cart</button>
          <span className="text-[#606060] px-2 leading-[1.8]">/</span>
          <button onClick={() => navigate('/checkout')} className="text-[#606060] font-normal leading-[1.8]">Info</button>
          <span className="text-[#606060] px-2 leading-[1.8]">/</span>
          <span className="text-[#202020] font-bold leading-[1.8]">Shipping</span>
          <span className="text-[#606060] px-2 leading-[1.8]">/</span>
          <span className="text-[#606060] font-normal leading-[1.8]">Payment</span>
        </nav>

        {/* â”€â”€ Contact row â”€â”€ */}
        <div className="flex items-center justify-between py-3 border-b border-[#DFDFDF]">
          <span className="text-[18px] text-[#606060] capitalize leading-[1.8]">contact</span>
          <button
            onClick={() => navigate('/checkout')}
            className="text-sm text-[#606060] leading-6 hover:text-[#202020] cursor-pointer"
          >
            Change
          </button>
        </div>

        {/* â”€â”€ Ship To row â”€â”€ */}
        <div className="flex items-center justify-between py-3 border-b border-[#DFDFDF] mb-8">
          <span className="text-[18px] text-[#606060] capitalize leading-[1.8]">Ship to</span>
          <button
            onClick={() => navigate('/checkout')}
            className="text-sm text-[#606060] leading-6 hover:text-[#202020] cursor-pointer"
          >
            Change
          </button>
        </div>

        {/* â”€â”€ Delivery Options heading â”€â”€ */}
        <h2 className="font-bold text-2xl text-[#202020] leading-[1.4] mb-4">Delivery Options</h2>
        <div className="border-t border-[#DFDFDF] mb-6" />

        {/* â”€â”€ Express Courier section â”€â”€ */}
        <div className="flex items-center justify-between mb-1">
          <span className="font-bold text-[16px] text-[#606060] leading-[1.4]">Express Courier (Air)</span>
          <span className="font-bold text-[16px] text-[#202020] leading-[1.4]">Free</span>
        </div>
        <p className="font-semibold text-[14px] text-[#606060] leading-[1.8] mb-5">3 to 4 Business Days</p>

        {/* â”€â”€ Expected Date (label left, 2Ã—2 grid right) â”€â”€ */}
        <div className="flex items-start gap-6 mb-6">
          <span className="font-bold text-[16px] text-[#606060] leading-[1.4] shrink-0 pt-[2px]">
            Expected Date:
          </span>
          <div className="grid grid-cols-1 gap-y-3 lg:grid-cols-2 lg:gap-x-8">
            {[
              'Monday, August 14',
              'Wednesday, August 16',
              'Tuesday, August 22',
              'Friday, August 25',
            ].map((date) => (
              <label key={date} className="flex items-center gap-2 cursor-pointer">
                <RadioCircle
                  name="expressDate"
                  value={date}
                  checked={selectedExpressDate === date}
                  onChange={setSelectedExpressDate}
                />
                <span className="text-[16px] text-[#202020] leading-[1.8] capitalize">{date}</span>
              </label>
            ))}
          </div>
        </div>

        {/* â”€â”€ Divider â”€â”€ */}
        <div className="border-t border-[#DFDFDF] mb-6" />

        {/* â”€â”€ Guaranteed By section (label left, options right) â”€â”€ */}
        <div className="flex flex-col lg:flex-row items-start gap-4 lg:gap-6 mb-10">
          <div className="shrink-0">
            <p className="font-bold text-[16px] text-[#606060] leading-[1.4]">Guaranteed by:</p>
            <p className="font-semibold text-[14px] text-[#606060] leading-[1.8] mt-1">UPS Next Day Air Saver</p>
          </div>
          <div className="flex flex-col gap-4">
            {[
              { label: 'Wednesday, August 11th By 8 PM', price: '$24.00' },
              { label: 'Wednesday, August 11th  By Noon', price: '$24.00' },
            ].map((opt) => (
              <label key={opt.label} className="flex items-center justify-between gap-4 cursor-pointer">
                <div className="flex items-center gap-2">
                  <RadioCircle
                    name="guaranteed"
                    value={opt.label}
                    checked={selectedGuaranteed === opt.label}
                    onChange={setSelectedGuaranteed}
                  />
                  <span className="text-[16px] text-[#0C0C0C] leading-[1.8] capitalize whitespace-pre">{opt.label}</span>
                </div>
                <span className="font-bold text-[14px] text-[#202020] leading-[1.8] shrink-0">{opt.price}</span>
              </label>
            ))}
          </div>
        </div>

        {/* â”€â”€ Bottom Actions â”€â”€ */}
        <div className="flex items-center justify-between mt-auto">
          <button
            onClick={() => navigate('/checkout')}
            className="flex items-center gap-1 text-[14px] text-[#5A6D57] leading-6 hover:opacity-70 transition-opacity capitalize"
          >
            <ChevronLeft size={16} />
            return to information
          </button>
          <button onClick={() => navigate('/checkout/payment')} className="bg-[#5A6D57] hover:bg-[#4A5D23] text-white text-[16px] leading-[1.8] px-4 h-12 w-[202px] transition-colors capitalize">
            Continue To Payment
          </button>
        </div>
      </div>

      {/* â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
          RIGHT COLUMN â€” Order Summary (45 %)
         â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â• */}
      <div className="w-full lg:w-[45%] bg-[#F5F6F3] pt-6 pb-12 px-4 lg:pt-12 lg:pb-24 lg:pr-24 lg:pl-16 border-t border-gray-200 lg:border-t-0 lg:border-l">

        {/* â”€â”€ Title â”€â”€ */}
        <h2 className="text-xl font-bold text-[#202020] text-center mb-8">Your Cart</h2>

        {/* â”€â”€ Product List â”€â”€ */}
        <div className="flex flex-col">
          {items.map((item) => (
            <div key={item.id} className="flex gap-4 py-6 border-b border-[#dfdfdf] last:border-b-0">
              <div className="w-10 h-10 border border-[#dfdfdf] rounded flex items-center justify-center text-sm text-[#404040] shrink-0 bg-white">
                {item.quantity}
              </div>
              <div className="flex-1 min-w-0">
                <div className="flex items-start justify-between">
                  <p className="text-sm font-bold text-[#202020] capitalize leading-tight">{item.name}</p>
                  <button
                    onClick={() => removeItem(item.id)}
                    aria-label={`Remove ${item.name}`}
                    className="text-[#202020] hover:opacity-50 transition-opacity shrink-0 ml-4"
                  >
                    <X size={16} strokeWidth={1.5} />
                  </button>
                </div>
                <p className="text-sm text-[#404040] mt-1">Size: {item.size}</p>
                <p className="text-sm text-[#404040]">Color: {item.color}</p>
                <div className="flex items-center justify-between mt-3">
                  <div className="flex items-center justify-between bg-[#D1D9CF] w-[80px] px-2 py-[4px] rounded-sm">
                    <button onClick={() => updateQuantity(item.id, item.quantity - 1)} aria-label="Decrease" className="text-[#404E3E] hover:opacity-60 transition-opacity leading-none">
                      <Minus size={12} strokeWidth={2} />
                    </button>
                    <span className="text-[#404E3E] text-sm leading-none select-none min-w-[14px] text-center">{item.quantity}</span>
                    <button onClick={() => updateQuantity(item.id, item.quantity + 1)} aria-label="Increase" className="text-[#404E3E] hover:opacity-60 transition-opacity leading-none">
                      <Plus size={12} strokeWidth={2} />
                    </button>
                  </div>
                  <span className="text-base font-semibold text-[#202020]">$ {(item.price * item.quantity).toFixed(0)}</span>
                </div>
              </div>
            </div>
          ))}
        </div>

        {/* â”€â”€ Totals â”€â”€ */}
        <div className="border-t border-[#dfdfdf] mt-6 pt-6 flex flex-col gap-3">
          <div className="flex items-center justify-between text-sm text-[#404040]">
            <span>Subtotal ({totalItems})</span>
            <span>${subtotal.toFixed(2)}</span>
          </div>
          <div className="flex items-center justify-between text-sm text-[#404040]">
            <span>Tax</span><span>${tax.toFixed(2)}</span>
          </div>
          <div className="flex items-center justify-between text-sm text-[#404040]">
            <span>Shipping</span><span>Free</span>
          </div>
          <div className="flex items-center justify-between text-base font-bold text-[#202020] pt-3 border-t border-[#dfdfdf]">
            <span>Total Orders:</span><span>${total.toFixed(2)}</span>
          </div>
          <p className="text-[11px] text-[#404040] font-medium leading-relaxed mt-3">
            The total amount you pay includes all applicable customs duties &amp; taxes.
            We guarantee no additional charges on delivery
          </p>
        </div>
      </div>
    </div>
  )
}
