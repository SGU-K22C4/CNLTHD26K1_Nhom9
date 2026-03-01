import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { X, Minus, Plus, Search, Phone, User, ChevronLeft } from 'lucide-react'
import { useCartContext } from '../../cart/context/CartContext'

const TAX_RATE = 0.08

export default function CheckoutPage() {
  const { items, removeItem, updateQuantity, subtotal, totalItems } = useCartContext()
  const navigate = useNavigate()

  const tax = subtotal * TAX_RATE
  const total = subtotal + tax

  /* ── Form state ── */
  const [form, setForm] = useState({
    email: '',
    emailOffers: false,
    country: '',
    firstName: '',
    lastName: '',
    company: '',
    address: '',
    apartment: '',
    postalCode: '',
    city: '',
    phone: '',
    saveInfo: false,
  })

  const set = (field) => (e) => {
    const value = e.target.type === 'checkbox' ? e.target.checked : e.target.value
    setForm((prev) => ({ ...prev, [field]: value }))
  }

  const inputBase =
    'w-full border border-[#dfdfdf] rounded-md p-3 text-sm text-[#202020] placeholder-[#9a9a9a] outline-none focus:border-[#5A6D57] transition-colors'

  return (
    <div className="min-h-screen flex flex-col lg:flex-row font-[Montserrat]">

      {/* ════════════════════════════════════════════════════════════════════
          LEFT COLUMN — Form (55 %)
         ════════════════════════════════════════════════════════════════════ */}
      <div className="w-full lg:w-[55%] bg-white pt-6 pb-12 px-4 lg:pt-12 lg:pb-24 lg:pl-24 lg:pr-16 flex flex-col">

        {/* ── Logo ── */}
        <div className="mb-8 flex flex-col items-start gap-1">
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

        {/* ── Breadcrumbs ── */}
        <nav className="flex items-center gap-2 text-sm mb-10">
          <button onClick={() => navigate('/cart')} className="text-[#9a9a9a] hover:text-[#5A6D57] transition-colors">
            Cart
          </button>
          <span className="text-[#9a9a9a]">/</span>
          <span className="text-[#202020] font-semibold">Info</span>
          <span className="text-[#9a9a9a]">/</span>
          <span className="text-[#9a9a9a]">Shipping</span>
          <span className="text-[#9a9a9a]">/</span>
          <span className="text-[#9a9a9a]">Payment</span>
        </nav>

        {/* ── Contact Section ── */}
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-semibold text-[#202020]">Contact</h2>
          <p className="text-sm text-[#404040]">
            Have An Account?{' '}
            <button className="underline font-medium hover:text-[#5A6D57] transition-colors">
              Log In
            </button>
          </p>
        </div>

        <div className="relative mb-2">
          <User size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-[#9a9a9a]" />
          <input
            type="email"
            placeholder="Email"
            value={form.email}
            onChange={set('email')}
            className={`${inputBase} pl-9`}
          />
        </div>

        <label className="flex items-center gap-2 text-sm text-[#404040] mb-8 cursor-pointer select-none">
          <input
            type="checkbox"
            checked={form.emailOffers}
            onChange={set('emailOffers')}
            className="w-4 h-4 border-[#dfdfdf] rounded accent-[#5A6D57]"
          />
          Email Me With News And Offers
        </label>

        {/* ── Shipping Address Section ── */}
        <h2 className="text-lg font-semibold text-[#202020] mb-4">Shipping Address</h2>

        <div className="flex flex-col gap-4">
          {/* Country / Region */}
          <div className="relative">
            <select
              value={form.country}
              onChange={set('country')}
              className={`${inputBase} appearance-none pr-10`}
            >
              <option value="" disabled>Country/Region</option>
              <option value="US">United States</option>
              <option value="CA">Canada</option>
              <option value="GB">United Kingdom</option>
              <option value="AU">Australia</option>
              <option value="VN">Vietnam</option>
            </select>
            <X
              size={14}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-[#9a9a9a] pointer-events-none"
            />
          </div>

          {/* First / Last Name */}
          <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
            <input
              type="text"
              placeholder="First Name"
              value={form.firstName}
              onChange={set('firstName')}
              className={inputBase}
            />
            <input
              type="text"
              placeholder="Last Name"
              value={form.lastName}
              onChange={set('lastName')}
              className={inputBase}
            />
          </div>

          {/* Company */}
          <input
            type="text"
            placeholder="Company(Optional)"
            value={form.company}
            onChange={set('company')}
            className={inputBase}
          />

          {/* Address */}
          <div className="relative">
            <input
              type="text"
              placeholder="Address"
              value={form.address}
              onChange={set('address')}
              className={`${inputBase} pr-10`}
            />
            <Search
              size={16}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-[#9a9a9a]"
            />
          </div>

          {/* Apartment */}
          <input
            type="text"
            placeholder="Apartment,Suite,Etc.(Optional)"
            value={form.apartment}
            onChange={set('apartment')}
            className={inputBase}
          />

          {/* Postal Code / City */}
          <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
            <input
              type="text"
              placeholder="Postal Code"
              value={form.postalCode}
              onChange={set('postalCode')}
              className={inputBase}
            />
            <input
              type="text"
              placeholder="City"
              value={form.city}
              onChange={set('city')}
              className={inputBase}
            />
          </div>

          {/* Phone */}
          <div className="relative">
            <input
              type="tel"
              placeholder="Phone"
              value={form.phone}
              onChange={set('phone')}
              className={`${inputBase} pr-10`}
            />
            <Phone
              size={16}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-[#9a9a9a]"
            />
          </div>
        </div>

        {/* Save info checkbox */}
        <label className="flex items-center gap-2 text-sm text-[#404040] mt-4 cursor-pointer select-none">
          <input
            type="checkbox"
            checked={form.saveInfo}
            onChange={set('saveInfo')}
            className="w-4 h-4 border-[#dfdfdf] rounded accent-[#5A6D57]"
          />
          Save This Information For Next Time
        </label>

        {/* ── Bottom Actions ── */}
        <div className="flex items-center justify-between mt-auto pt-12">
          <button
            onClick={() => navigate('/cart')}
            className="flex items-center gap-1 text-sm text-[#404040] hover:text-[#5A6D57] transition-colors"
          >
            <ChevronLeft size={16} />
            Return To Cart
          </button>
          <button
            onClick={() => navigate('/checkout/shipping')}
            className="bg-[#4A5D23] hover:bg-[#5A6D57] text-white text-sm font-medium px-8 py-3 rounded-md transition-colors"
          >
            Continue To Shipping
          </button>
        </div>
      </div>

      {/* ════════════════════════════════════════════════════════════════════
          RIGHT COLUMN — Order Summary (45 %)
         ════════════════════════════════════════════════════════════════════ */}
      <div className="w-full lg:w-[45%] bg-[#F5F6F3] pt-6 pb-12 px-4 lg:pt-12 lg:pb-24 lg:pr-24 lg:pl-16 border-t border-gray-200 lg:border-t-0 lg:border-l">

        {/* ── Title ── */}
        <h2 className="text-xl font-bold text-[#202020] text-center mb-8">Your Cart</h2>

        {/* ── Product List ── */}
        <div className="flex flex-col">
          {items.map((item) => (
            <div
              key={item.id}
              className="flex gap-4 py-6 border-b border-[#dfdfdf] last:border-b-0"
            >
              {/* Quantity badge */}
              <div className="w-10 h-10 border border-[#dfdfdf] rounded flex items-center justify-center text-sm text-[#404040] shrink-0 bg-white">
                {item.quantity}
              </div>

              {/* Product details */}
              <div className="flex-1 min-w-0">
                <div className="flex items-start justify-between">
                  <p className="text-sm font-bold text-[#202020] capitalize leading-tight">
                    {item.name}
                  </p>
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

                {/* Qty stepper + Price row */}
                <div className="flex items-center justify-between mt-3">
                  <div className="flex items-center justify-between bg-[#D1D9CF] w-[80px] px-2 py-[4px] rounded-sm">
                    <button
                      onClick={() => updateQuantity(item.id, item.quantity - 1)}
                      aria-label="Decrease"
                      className="text-[#404E3E] hover:opacity-60 transition-opacity leading-none"
                    >
                      <Minus size={12} strokeWidth={2} />
                    </button>
                    <span className="text-[#404E3E] text-sm leading-none select-none min-w-[14px] text-center">
                      {item.quantity}
                    </span>
                    <button
                      onClick={() => updateQuantity(item.id, item.quantity + 1)}
                      aria-label="Increase"
                      className="text-[#404E3E] hover:opacity-60 transition-opacity leading-none"
                    >
                      <Plus size={12} strokeWidth={2} />
                    </button>
                  </div>

                  <span className="text-base font-semibold text-[#202020]">
                    $ {(item.price * item.quantity).toFixed(0)}
                  </span>
                </div>
              </div>
            </div>
          ))}
        </div>

        {/* ── Totals ── */}
        <div className="border-t border-[#dfdfdf] mt-6 pt-6 flex flex-col gap-3">
          <div className="flex items-center justify-between text-sm text-[#404040]">
            <span>Subtotal ({totalItems})</span>
            <span>${subtotal.toFixed(2)}</span>
          </div>
          <div className="flex items-center justify-between text-sm text-[#404040]">
            <span>Tax</span>
            <span>${tax.toFixed(2)}</span>
          </div>
          <div className="flex items-center justify-between text-sm text-[#404040]">
            <span>Shipping</span>
            <span>Free</span>
          </div>

          <div className="flex items-center justify-between text-base font-bold text-[#202020] pt-3 border-t border-[#dfdfdf]">
            <span>Total Orders:</span>
            <span>${total.toFixed(2)}</span>
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