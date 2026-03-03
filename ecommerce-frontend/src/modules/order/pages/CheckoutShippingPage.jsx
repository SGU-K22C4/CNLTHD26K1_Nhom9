import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { X, Minus, Plus, ChevronLeft } from 'lucide-react'
import { useCartContext } from '../../cart/context/CartContext'

// ─── helper: custom radio button ─────────────────────────────────────────────
function RadioCircle({ value, checked, onChange }) {
  return (
    <button
      type="button"
      onClick={() => onChange(value)}
      aria-pressed={checked}
      className={`shrink-0 w-5 h-5 rounded-full border-2 flex items-center justify-center transition-colors ${
        checked ? 'border-[#5A6D57]' : 'border-[#CBCBCB]'
      } bg-white`}
    >
      {checked && <span className="w-2 h-2 rounded-full bg-[#5A6D57] block" />}
    </button>
  )
}

// ─── shared: Logo block ───────────────────────────────────────────────────────
function Logo() {
  return (
    <div className="flex flex-col items-start gap-1">
      <div className="flex items-end gap-[2px]">
        <span className="font-['League_Spartan',sans-serif] font-bold text-[28px] lg:text-[32px] text-[#404040] tracking-[3px] leading-none">
          modimal
        </span>
        <span className="w-[9px] h-[9px] rounded-full bg-[#748C70] mb-[4px] shrink-0 block" />
      </div>
      <p className="font-['League_Spartan',sans-serif] text-[10px] text-[#404040] tracking-[1px] leading-none">
        women clothing
      </p>
    </div>
  )
}

// ─── shared: Breadcrumbs ──────────────────────────────────────────────────────
function Breadcrumbs({ navigate }) {
  return (
    <nav className="flex items-center gap-2 text-sm text-[#9a9a9a]">
      <button onClick={() => navigate('/cart')} className="hover:text-[#5A6D57] transition-colors">Cart</button>
      <span>/</span>
      <button onClick={() => navigate('/checkout')} className="hover:text-[#5A6D57] transition-colors">Info</button>
      <span>/</span>
      <span className="text-[#202020] font-bold">Shipping</span>
      <span>/</span>
      <span>Payment</span>
    </nav>
  )
}

// ─── shared: delivery options form ────────────────────────────────────────────
function DeliveryOptions({ selectedExpressDate, setSelectedExpressDate, selectedGuaranteed, setSelectedGuaranteed }) {
  return (
    <>
      {/* Contact row */}
      <div className="flex items-center justify-between py-3 border-b border-[#DFDFDF]">
        <span className="text-base text-[#606060]">Contact</span>
        <button className="text-sm text-[#9a9a9a] hover:text-[#202020] transition-colors">Change</button>
      </div>

      {/* Ship To row */}
      <div className="flex items-center justify-between py-3 border-b border-[#DFDFDF] mb-6">
        <span className="text-base text-[#606060]">Ship To</span>
        <button className="text-sm text-[#9a9a9a] hover:text-[#202020] transition-colors">Change</button>
      </div>

      {/* Delivery Options heading */}
      <h2 className="font-bold text-xl text-[#202020] mb-3">Delivery Options</h2>
      <div className="border-t border-[#DFDFDF] mb-5" />

      {/* Express Courier */}
      <div className="flex items-center justify-between mb-1">
        <span className="font-bold text-sm text-[#606060]">Express Courier (Air)</span>
        <span className="font-bold text-sm text-[#202020]">Free</span>
      </div>
      <p className="text-sm text-[#606060] mb-4">3 To 4 Business Days</p>

      {/* Expected Date */}
      <p className="font-bold text-sm text-[#606060] mb-3">Expected Date:</p>
      <div className="grid grid-cols-2 gap-x-4 gap-y-3 mb-6">
        {['Monday, August 14', 'Wednesday, August 16', 'Tuesday, August 22', 'Friday, August 25'].map((date) => (
          <label key={date} className="flex items-center gap-2 cursor-pointer">
            <RadioCircle value={date} checked={selectedExpressDate === date} onChange={setSelectedExpressDate} />
            <span className="text-sm text-[#202020] leading-snug">{date}</span>
          </label>
        ))}
      </div>

      <div className="border-t border-[#DFDFDF] mb-5" />

      {/* Guaranteed By */}
      <p className="font-bold text-sm text-[#606060] mb-1">Guaranteed By:</p>
      <p className="text-sm text-[#606060] mb-4">UPS Next Day Air Saver</p>
      <div className="flex flex-col gap-3">
        {[
          { label: 'Wednesday, August 11th By 8 PM', price: '$24.00' },
          { label: 'Wednesday, August 11th  By Noon', price: '$24.00' },
        ].map((opt) => (
          <label key={opt.label} className="flex items-center justify-between gap-3 cursor-pointer">
            <div className="flex items-center gap-2">
              <RadioCircle value={opt.label} checked={selectedGuaranteed === opt.label} onChange={setSelectedGuaranteed} />
              <span className="text-sm text-[#202020]">{opt.label}</span>
            </div>
            <span className="font-bold text-sm text-[#202020] shrink-0">{opt.price}</span>
          </label>
        ))}
      </div>
    </>
  )
}

// ─── page component ───────────────────────────────────────────────────────────
const TAX_RATE = 0.08

export default function CheckoutShippingPage() {
  const { items, removeItem, updateQuantity, subtotal, totalItems } = useCartContext()
  const navigate = useNavigate()

  const tax = subtotal * TAX_RATE
  const total = subtotal + tax

  const [selectedExpressDate, setSelectedExpressDate] = useState('')
  const [selectedGuaranteed, setSelectedGuaranteed] = useState('')

  const sharedOptions = { selectedExpressDate, setSelectedExpressDate, selectedGuaranteed, setSelectedGuaranteed }

  return (
    <div className="min-h-screen bg-white font-[Montserrat]">

      {/* ══════════════════════════════
          MOBILE (hidden on lg+)
      ══════════════════════════════ */}
      <div className="lg:hidden flex flex-col min-h-screen px-4 pt-6 pb-10">
        <div className="mb-5"><Logo /></div>
        <div className="mb-6"><Breadcrumbs navigate={navigate} /></div>

        {/* Delivery options + contact/shipTo rows */}
        <DeliveryOptions {...sharedOptions} />

        {/* CTA */}
        <button
          onClick={() => navigate('/checkout/payment')}
          className="w-full bg-[#5A6D57] hover:bg-[#748C70] text-white text-sm font-medium py-4 mt-8 transition-colors"
        >
          Continue To Payment
        </button>

        {/* Return link */}
        <button
          onClick={() => navigate('/checkout')}
          className="flex items-center justify-center gap-1 text-sm text-[#404040] hover:text-[#5A6D57] transition-colors mt-4"
        >
          <ChevronLeft size={16} />
          Return To Information
        </button>
      </div>

      {/* ══════════════════════════════
          DESKTOP (hidden below lg)
      ══════════════════════════════ */}
      <div className="hidden lg:flex flex-row min-h-screen">

        {/* LEFT — form 55% */}
        <div className="w-[55%] bg-white pt-12 pb-24 pl-24 pr-16 flex flex-col">
          <div className="mb-10"><Logo /></div>
          <div className="mb-8"><Breadcrumbs navigate={navigate} /></div>

          <DeliveryOptions {...sharedOptions} />

          {/* Bottom actions */}
          <div className="flex items-center justify-between mt-auto pt-12">
            <button
              onClick={() => navigate('/checkout')}
              className="flex items-center gap-1 text-sm text-[#404040] hover:text-[#5A6D57] transition-colors"
            >
              <ChevronLeft size={16} />
              Return To Information
            </button>
            <button
              onClick={() => navigate('/checkout/payment')}
              className="bg-[#5A6D57] hover:bg-[#748C70] text-white text-sm font-medium px-10 py-3 transition-colors"
            >
              Continue To Payment
            </button>
          </div>
        </div>

        {/* RIGHT — order summary 45% */}
        <div className="w-[45%] bg-[#F5F6F3] pt-12 pb-24 pr-24 pl-16 border-l border-gray-200">
          <h2 className="text-xl font-bold text-[#202020] text-center mb-8">Your Cart</h2>

          <div className="flex flex-col">
            {items.map((item) => (
              <div key={item.id} className="flex gap-4 py-6 border-b border-[#dfdfdf] last:border-b-0">
                <div className="relative shrink-0">
                  <img src={item.image} alt={item.name} className="w-[100px] h-[120px] object-cover" />
                  <span className="absolute top-1 left-1 w-6 h-6 flex items-center justify-center bg-white text-[#202020] text-xs font-bold border border-[#dfdfdf]">
                    {item.quantity}
                  </span>
                </div>
                <div className="flex-1 min-w-0 pt-1">
                  <div className="flex items-start justify-between">
                    <p className="text-sm font-bold text-[#202020] capitalize leading-tight">{item.name}</p>
                    <button onClick={() => removeItem(item.id)} aria-label={`Remove ${item.name}`}
                      className="text-[#202020] hover:opacity-50 transition-opacity shrink-0 ml-4">
                      <X size={16} strokeWidth={1.5} />
                    </button>
                  </div>
                  <p className="text-sm text-[#404040] mt-1">Size: {item.size}</p>
                  <p className="text-sm text-[#404040]">Color: {item.color}</p>
                  <div className="flex items-center justify-between mt-3">
                    <span className="text-sm font-bold text-[#202020]">$ {(item.price * item.quantity).toFixed(0)}</span>
                    <div className="flex items-center bg-[#D1D9CF] w-[80px] px-2 py-[4px]">
                      <button onClick={() => updateQuantity(item.id, item.quantity - 1)} aria-label="Decrease"
                        className="text-[#404E3E] hover:opacity-60 transition-opacity">
                        <Minus size={12} strokeWidth={2} />
                      </button>
                      <span className="flex-1 text-[#404E3E] text-sm select-none text-center">{item.quantity}</span>
                      <button onClick={() => updateQuantity(item.id, item.quantity + 1)} aria-label="Increase"
                        className="text-[#404E3E] hover:opacity-60 transition-opacity">
                        <Plus size={12} strokeWidth={2} />
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>

          {/* Totals */}
          <div className="border-t border-[#dfdfdf] mt-6 pt-6 flex flex-col gap-3">
            <div className="flex justify-between text-sm text-[#404040]">
              <span>Subtotal ({totalItems})</span><span>${subtotal.toFixed(2)}</span>
            </div>
            <div className="flex justify-between text-sm text-[#404040]">
              <span>Tax</span><span>${tax.toFixed(2)}</span>
            </div>
            <div className="flex justify-between text-sm text-[#404040]">
              <span>Shipping</span><span>Free</span>
            </div>
            <div className="flex justify-between text-sm font-bold text-[#202020] pt-3 border-t border-[#dfdfdf]">
              <span>Order Totals:</span><span>${total.toFixed(2)}</span>
            </div>
            <p className="text-[11px] text-[#202020] font-semibold leading-relaxed mt-1">
              The Total Amount You Pay Includes All Applicable Customs Duties &amp; Taxes. We Guarantee No Additional Charges On Delivery
            </p>
          </div>
        </div>

      </div>
    </div>
  )
}