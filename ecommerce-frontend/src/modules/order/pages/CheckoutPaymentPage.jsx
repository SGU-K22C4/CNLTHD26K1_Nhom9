import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ChevronLeft, User, Mail, Flag, Home, MapPin, Building2, Phone, Info } from 'lucide-react'
import { useCartContext } from '../../cart/context/CartContext'

const TAX_RATE = 0.08

export default function CheckoutPaymentPage() {
  const { subtotal, totalItems } = useCartContext()
  const navigate = useNavigate()

  const tax = subtotal * TAX_RATE
  const total = subtotal + tax

  const [billingOption, setBillingOption] = useState('default')
  const [cardNumber, setCardNumber] = useState('')
  const [expiryMonth, setExpiryMonth] = useState('')
  const [expiryYear, setExpiryYear] = useState('')
  const [securityCode, setSecurityCode] = useState('')

  const billingFields = [
    { icon: <User size={16} />, placeholder: 'Name' },
    { icon: <Mail size={16} />, placeholder: 'Email' },
    { icon: <Flag size={16} />, placeholder: 'Country' },
    { icon: <Home size={16} />, placeholder: 'Address Line1' },
    { icon: <Home size={16} />, placeholder: 'Address Line2' },
    { icon: <Building2 size={16} />, placeholder: 'City / Suburb' },
    { icon: <MapPin size={16} />, placeholder: 'Zip / Postcode' },
    { icon: <Phone size={16} />, placeholder: 'Phone' },
  ]

  return (
    <div className="min-h-screen bg-white font-[Montserrat]">
      <div className="max-w-[1280px] mx-auto pt-6 pb-12 px-4 lg:pt-8 lg:pb-24 lg:px-24 flex flex-col">

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

        {/* ── Breadcrumbs ── */}
        <nav className="flex items-center text-[18px] mb-10">
          <button onClick={() => navigate('/cart')} className="text-[#404040] font-normal leading-[1.8]">Cart</button>
          <span className="text-[#404040] px-2 leading-[1.8]">/</span>
          <button onClick={() => navigate('/checkout')} className="text-[#404040] font-normal leading-[1.8]">Info</button>
          <span className="text-[#404040] px-2 leading-[1.8]">/</span>
          <button onClick={() => navigate('/checkout/shipping')} className="text-[#404040] font-normal leading-[1.8]">Shipping</button>
          <span className="text-[#404040] px-2 leading-[1.8]">/</span>
          <span className="text-[#202020] font-bold leading-[1.8]">Payment</span>
        </nav>

        {/* ── Two-column content ── */}
        <div className="flex flex-col lg:flex-row gap-8 lg:gap-16 items-start">

          {/* LEFT — Billing Address */}
          <div className="flex-1">
            <h2 className="font-bold text-lg text-[#202020] leading-[1.4] mb-4">Billing Address</h2>
            <div className="border-t border-[#dfdfdf] mb-6" />

            {/* Billing option checkboxes */}
            <div className="flex flex-col gap-4 mb-6">
              <label className="flex items-center gap-2 cursor-pointer">
                <button
                  type="button"
                  onClick={() => setBillingOption('default')}
                  className={`shrink-0 w-4 h-4 border border-[#A2B39F] ${
                    billingOption === 'default' ? 'bg-[#5A6D57]' : 'bg-white'
                  }`}
                />
                <span className="text-[16px] text-[#404040] font-normal leading-[1.8] capitalize">
                  Default (Same As Billing Address)
                </span>
              </label>
              <label className="flex items-center gap-2 cursor-pointer">
                <button
                  type="button"
                  onClick={() => setBillingOption('alternative')}
                  className={`shrink-0 w-4 h-4 border border-[#A2B39F] ${
                    billingOption === 'alternative' ? 'bg-[#5A6D57]' : 'bg-white'
                  }`}
                />
                <span className="text-[16px] text-[#404040] font-normal leading-[1.8] capitalize">
                  Add An Alternative Delivery Address
                </span>
              </label>
            </div>

            {/* Billing form fields */}
            <div className="flex flex-col gap-2">
              {billingFields.map(({ icon, placeholder }) => (
                <div key={placeholder} className="flex items-center gap-3 h-10 border border-[#dfdfdf] px-4">
                  <span className="text-[#404040] shrink-0">{icon}</span>
                  <input
                    type="text"
                    placeholder={placeholder}
                    className="flex-1 text-sm text-[#404040] placeholder:text-[#404040] bg-transparent outline-none capitalize"
                  />
                </div>
              ))}
            </div>
          </div>

          {/* RIGHT — Payment */}
          <div className="flex-1 flex flex-col">
            <h2 className="font-bold text-lg text-[#202020] leading-[1.4] mb-4">Payment</h2>
            <div className="border-t border-[#dfdfdf] mb-6" />

            <p className="text-[16px] text-[#404040] font-normal leading-[1.8] capitalize mb-5">
              Please Choose Your Payment Method
            </p>

            {/* Payment method logos */}
            <div className="flex items-center border border-[#dfdfdf] mb-8 w-fit">
              <div className="flex items-center justify-center h-10 w-[100px] border-r border-[#dfdfdf] px-4">
                <span className="text-[11px] font-bold text-[#016fd0] leading-tight tracking-wide">AMERICAN<br />EXPRESS</span>
              </div>
              <div className="flex items-center justify-center h-10 w-[100px] border-r border-[#dfdfdf] px-4">
                <span className="text-[22px] font-bold italic text-[#1a1f71] leading-none tracking-tight">VISA</span>
              </div>
              <div className="flex items-center justify-center h-10 w-[100px] border-r border-[#dfdfdf] px-4">
                <div className="flex items-center">
                  <span className="w-6 h-6 rounded-full bg-[#eb001b] block -mr-2" />
                  <span className="w-6 h-6 rounded-full bg-[#f79e1b] block opacity-90" />
                </div>
              </div>
              <div className="flex items-center justify-center h-10 w-[100px] px-4">
                <span className="text-[14px] font-bold text-[#003087] leading-none">Pay<span className="text-[#009cde]">Pal</span></span>
              </div>
            </div>

            {/* Card Number */}
            <div className="flex flex-col lg:flex-row lg:items-center gap-2 lg:gap-8 mb-4">
              <label className="text-sm text-[#404040] font-normal leading-[1.8] capitalize shrink-0 lg:w-[130px]">
                Card Number*
              </label>
              <input
                type="text"
                value={cardNumber}
                onChange={(e) => setCardNumber(e.target.value)}
                className="flex-1 h-10 border border-[#dfdfdf] px-4 text-sm text-[#404040] placeholder:text-[#404040] outline-none"
              />
            </div>

            {/* Expiry Date */}
            <div className="flex flex-col lg:flex-row lg:items-center gap-2 lg:gap-8 mb-4">
              <label className="text-sm text-[#404040] font-normal leading-[1.8] capitalize shrink-0 lg:w-[130px]">
                Expiry Date*
              </label>
              <div className="flex gap-4 flex-1">
                <input
                  type="text"
                  placeholder="Month"
                  value={expiryMonth}
                  onChange={(e) => setExpiryMonth(e.target.value)}
                  className="flex-1 h-10 border border-[#dfdfdf] px-4 text-sm text-[#404040] placeholder:text-[#404040] outline-none capitalize"
                />
                <input
                  type="text"
                  placeholder="Year"
                  value={expiryYear}
                  onChange={(e) => setExpiryYear(e.target.value)}
                  className="flex-1 h-10 border border-[#dfdfdf] px-4 text-sm text-[#404040] placeholder:text-[#404040] outline-none capitalize"
                />
              </div>
            </div>

            {/* Security Code */}
            <div className="flex flex-col lg:flex-row lg:items-center gap-2 lg:gap-8 mb-8">
              <label className="text-sm text-[#404040] font-normal leading-[1.8] capitalize shrink-0 lg:w-[130px]">
                Security Code*
              </label>
              <div className="flex items-center gap-4">
                <input
                  type="text"
                  value={securityCode}
                  onChange={(e) => setSecurityCode(e.target.value)}
                  className="w-[130px] h-10 border border-[#dfdfdf] px-4 text-sm text-[#404040] outline-none"
                />
                <div className="flex items-center gap-1 cursor-pointer">
                  <Info size={18} className="text-[#404040] shrink-0" />
                  <span className="text-xs text-[#748C70] underline leading-[1.8] capitalize">
                    What Is This?
                  </span>
                </div>
              </div>
            </div>

            {/* Pay And Place Order button */}
            <button
              onClick={() => navigate('/checkout/success')}
              className="w-full bg-[#4A5D23] hover:bg-[#3a4d13] text-white text-[16px] font-normal leading-[1.8] h-12 capitalize transition-colors mb-4">
              Pay And Place Order
            </button>

            {/* Disclaimer */}
            <p className="text-xs text-[#404040] leading-relaxed capitalize">
              By Clicking On &apos;Pay And Place Order&apos;, You Agree (I) To Make Your Purchase From Global-E As
              Merchant Of Record For This Transaction, Subject To Global-E&apos;s{' '}
              <span className="text-[#748C70] underline cursor-pointer">Term Of Sale</span>
              ; (II) That Your Information Will Be Handled By Global-E In Accordance With The Global-E{' '}
              <span className="text-[#748C70] underline cursor-pointer">Privacy Policy</span>
              ; And (III) That Global-E Will Share Your Information (Excluding The Payment Details) With Modimal.
            </p>

            {/* Back link */}
            <div className="mt-auto pt-10">
              <button
                onClick={() => navigate('/checkout/shipping')}
                className="flex items-center gap-1 text-[14px] text-[#404040] leading-6 hover:opacity-70 transition-opacity capitalize"
              >
                <ChevronLeft size={16} />
                return to shipping
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
