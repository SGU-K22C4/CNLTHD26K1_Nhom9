import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { X, Minus, Plus, Phone, User, ChevronLeft, ChevronDown } from 'lucide-react'
import { useCartContext } from '../../cart/context/CartContext'

const TAX_RATE = 0.08

export default function CheckoutPage() {
  const { items, removeItem, updateQuantity, subtotal, totalItems } = useCartContext()
  const navigate = useNavigate()

  const tax = subtotal * TAX_RATE
  const total = subtotal + tax

  /* â”€â”€ Form state â”€â”€ */
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
    'w-full border border-[#dfdfdf] p-3 text-sm text-[#202020] placeholder-[#9a9a9a] outline-none focus:border-[#5A6D57] transition-colors bg-white'

  /* â”€â”€ Shared: cart item row â”€â”€ */
  const CartItemRow = ({ item }) => (
    <div className="relative flex items-start gap-4 py-5 border-b border-[#dfdfdf]">
      {/* Image + quantity badge */}
      <div className="relative shrink-0">
        <img src={item.image} alt={item.name} className="w-[120px] h-[140px] object-cover" />
        <span className="absolute top-2 left-2 w-6 h-6 flex items-center justify-center bg-white text-[#202020] text-xs font-bold border border-[#dfdfdf]">
          {item.quantity}
        </span>
      </div>
      {/* Info */}
      <div className="flex-1 flex flex-col gap-1 pr-6 pt-1">
        <p className="text-sm font-bold text-[#202020]">{item.name}</p>
        {item.size  && <p className="text-sm text-[#404040]">Size: {item.size}</p>}
        {item.color && <p className="text-sm text-[#404040]">Color: {item.color}</p>}
        {/* Price + stepper */}
        <div className="flex items-center justify-between mt-3">
          <p className="text-sm font-bold text-[#202020]">$ {(item.price * item.quantity).toFixed(0)}</p>
          <div className="flex items-center bg-[#D1D9CF]">
            <button onClick={() => updateQuantity(item.id, item.quantity - 1)} aria-label="Decrease"
              className="flex h-8 w-8 items-center justify-center text-[#404040] hover:bg-black/5 transition-colors">
              <Minus size={12} strokeWidth={2} />
            </button>
            <span className="flex h-8 w-8 items-center justify-center select-none text-sm text-[#202020]">
              {item.quantity}
            </span>
            <button onClick={() => updateQuantity(item.id, item.quantity + 1)} aria-label="Increase"
              className="flex h-8 w-8 items-center justify-center text-[#404040] hover:bg-black/5 transition-colors">
              <Plus size={12} strokeWidth={2} />
            </button>
          </div>
        </div>
      </div>
      {/* X */}
      <button onClick={() => removeItem(item.id)} aria-label={`Remove ${item.name}`}
        className="absolute top-5 right-0 text-[#202020] hover:opacity-50 transition-opacity">
        <X size={18} strokeWidth={1.5} />
      </button>
    </div>
  )

  /* â”€â”€ Shared: order totals block â”€â”€ */
  const OrderTotals = () => (
    <div className="flex flex-col gap-3 pt-5 border-t border-[#dfdfdf]">
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
  )

  /* â”€â”€ Shared: checkout form fields â”€â”€ */
  const CheckoutForm = () => (
    <>
      {/* Contact */}
      <div className="flex items-center justify-between mb-3 mt-6">
        <h2 className="text-base font-semibold text-[#202020]">Contact</h2>
        <p className="text-sm text-[#404040]">
          Have An Account?{' '}
          <button className="underline font-medium hover:text-[#5A6D57] transition-colors">Log In</button>
        </p>
      </div>
      <div className="relative mb-2">
        <User size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-[#9a9a9a]" />
        <input type="email" placeholder="Email" value={form.email} onChange={set('email')}
          className={`${inputBase} pl-9`} />
      </div>
      <label className="flex items-center gap-2 text-sm text-[#404040] mb-6 cursor-pointer select-none">
        <input type="checkbox" checked={form.emailOffers} onChange={set('emailOffers')}
          className="w-4 h-4 border-[#dfdfdf] accent-[#5A6D57]" />
        Email Me With News And Offers
      </label>

      {/* Shipping Address */}
      <h2 className="text-base font-semibold text-[#202020] mb-3">Shipping Address</h2>
      <div className="flex flex-col gap-3">
        {/* Country */}
        <div className="relative">
          <select value={form.country} onChange={set('country')}
            className={`${inputBase} appearance-none pr-10`}>
            <option value="" disabled>Country/Region</option>
            <option value="US">United States</option>
            <option value="CA">Canada</option>
            <option value="GB">United Kingdom</option>
            <option value="AU">Australia</option>
            <option value="VN">Vietnam</option>
          </select>
          <ChevronDown size={16} className="absolute right-3 top-1/2 -translate-y-1/2 text-[#9a9a9a] pointer-events-none" />
        </div>
        <input type="text" placeholder="First Name" value={form.firstName} onChange={set('firstName')} className={inputBase} />
        <input type="text" placeholder="Last Name" value={form.lastName} onChange={set('lastName')} className={inputBase} />
        <input type="text" placeholder="Company(Optional)" value={form.company} onChange={set('company')} className={inputBase} />
        <input type="text" placeholder="Address" value={form.address} onChange={set('address')} className={inputBase} />
        <input type="text" placeholder="Apartment, Suite, Etc.(Optional)" value={form.apartment} onChange={set('apartment')} className={inputBase} />
        <input type="text" placeholder="Postal Code" value={form.postalCode} onChange={set('postalCode')} className={inputBase} />
        <input type="text" placeholder="City" value={form.city} onChange={set('city')} className={inputBase} />
        <div className="relative">
          <input type="tel" placeholder="Phone" value={form.phone} onChange={set('phone')}
            className={`${inputBase} pr-10`} />
          <Phone size={16} className="absolute right-3 top-1/2 -translate-y-1/2 text-[#9a9a9a]" />
        </div>
      </div>
      <label className="flex items-center gap-2 text-sm text-[#404040] mt-4 cursor-pointer select-none">
        <input type="checkbox" checked={form.saveInfo} onChange={set('saveInfo')}
          className="w-4 h-4 border-[#dfdfdf] accent-[#5A6D57]" />
        Save This Information For Next Time
      </label>
    </>
  )

  return (
    <div className="min-h-screen bg-white font-[Montserrat]">

      {/* â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
          MOBILE LAYOUT (hidden on lg+)
      â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â• */}
      <div className="lg:hidden flex flex-col min-h-screen px-4 pt-6 pb-10">

        {/* Logo */}
        <div className="mb-4 flex flex-col items-start gap-1">
          <div className="flex items-end gap-[2px]">
            <span className="font-['League_Spartan',sans-serif] font-bold text-2xl text-[#404040] tracking-[2px] leading-none">
              modimal
            </span>
            <span className="w-[7px] h-[7px] rounded-full bg-[#748C70] mb-[3px] shrink-0 block" />
          </div>
          <p className="font-['League_Spartan',sans-serif] text-[9px] text-[#404040] tracking-[1px] leading-none">
            women clothing
          </p>
        </div>

        {/* Breadcrumb */}
        <nav className="flex items-center gap-1.5 text-xs mb-6 text-[#9a9a9a]">
          <button onClick={() => navigate('/cart')} className="hover:text-[#5A6D57] transition-colors">Cart</button>
          <span>/</span>
          <span className="text-[#202020] font-semibold">Info</span>
          <span>/</span>
          <span>Shipping</span>
          <span>/</span>
          <span>Payment</span>
        </nav>

        {/* Cart title */}
        <h1 className="text-xl font-bold text-[#202020] text-center mb-4">Your Cart</h1>

        {/* Cart items */}
        {items.map((item) => <CartItemRow key={item.id} item={item} />)}

        {/* Totals */}
        <div className="mt-4 mb-2">
          <OrderTotals />
        </div>

        {/* Form */}
        <CheckoutForm />

        {/* Continue To Shipping */}
        <button
          onClick={() => navigate('/checkout/shipping')}
          className="w-full bg-[#5A6D57] hover:bg-[#748C70] text-white text-sm font-medium py-4 mt-6 transition-colors"
        >
          Continue To Shipping
        </button>

        {/* Return To Card */}
        <button
          onClick={() => navigate('/cart')}
          className="flex items-center justify-center gap-1 text-sm text-[#404040] hover:text-[#5A6D57] transition-colors mt-4"
        >
          <ChevronLeft size={16} />
          Return To Card
        </button>
      </div>

      {/* â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
          DESKTOP LAYOUT (hidden below lg)
      â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â• */}
      <div className="hidden lg:flex flex-row min-h-screen">

        {/* LEFT â€” Form (55%) */}
        <div className="w-[55%] bg-white pt-12 pb-24 pl-24 pr-16 flex flex-col">

          {/* Logo */}
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

          {/* Breadcrumbs */}
          <nav className="flex items-center gap-2 text-sm mb-10 text-[#9a9a9a]">
            <button onClick={() => navigate('/cart')} className="hover:text-[#5A6D57] transition-colors">Cart</button>
            <span>/</span>
            <span className="text-[#202020] font-semibold">Info</span>
            <span>/</span>
            <span>Shipping</span>
            <span>/</span>
            <span>Payment</span>
          </nav>

          {/* Contact */}
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-[#202020]">Contact</h2>
            <p className="text-sm text-[#404040]">
              Have An Account?{' '}
              <button className="underline font-medium hover:text-[#5A6D57] transition-colors">Log In</button>
            </p>
          </div>
          <div className="relative mb-2">
            <User size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-[#9a9a9a]" />
            <input type="email" placeholder="Email" value={form.email} onChange={set('email')}
              className={`${inputBase} pl-9`} />
          </div>
          <label className="flex items-center gap-2 text-sm text-[#404040] mb-8 cursor-pointer select-none">
            <input type="checkbox" checked={form.emailOffers} onChange={set('emailOffers')}
              className="w-4 h-4 accent-[#5A6D57]" />
            Email Me With News And Offers
          </label>

          {/* Shipping Address */}
          <h2 className="text-lg font-semibold text-[#202020] mb-4">Shipping Address</h2>
          <div className="flex flex-col gap-4">
            <div className="relative">
              <select value={form.country} onChange={set('country')}
                className={`${inputBase} appearance-none pr-10`}>
                <option value="" disabled>Country/Region</option>
                <option value="US">United States</option>
                <option value="CA">Canada</option>
                <option value="GB">United Kingdom</option>
                <option value="AU">Australia</option>
                <option value="VN">Vietnam</option>
              </select>
              <ChevronDown size={16} className="absolute right-3 top-1/2 -translate-y-1/2 text-[#9a9a9a] pointer-events-none" />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <input type="text" placeholder="First Name" value={form.firstName} onChange={set('firstName')} className={inputBase} />
              <input type="text" placeholder="Last Name" value={form.lastName} onChange={set('lastName')} className={inputBase} />
            </div>
            <input type="text" placeholder="Company(Optional)" value={form.company} onChange={set('company')} className={inputBase} />
            <input type="text" placeholder="Address" value={form.address} onChange={set('address')} className={inputBase} />
            <input type="text" placeholder="Apartment, Suite, Etc.(Optional)" value={form.apartment} onChange={set('apartment')} className={inputBase} />
            <div className="grid grid-cols-2 gap-4">
              <input type="text" placeholder="Postal Code" value={form.postalCode} onChange={set('postalCode')} className={inputBase} />
              <input type="text" placeholder="City" value={form.city} onChange={set('city')} className={inputBase} />
            </div>
            <div className="relative">
              <input type="tel" placeholder="Phone" value={form.phone} onChange={set('phone')}
                className={`${inputBase} pr-10`} />
              <Phone size={16} className="absolute right-3 top-1/2 -translate-y-1/2 text-[#9a9a9a]" />
            </div>
          </div>
          <label className="flex items-center gap-2 text-sm text-[#404040] mt-4 cursor-pointer select-none">
            <input type="checkbox" checked={form.saveInfo} onChange={set('saveInfo')}
              className="w-4 h-4 accent-[#5A6D57]" />
            Save This Information For Next Time
          </label>

          {/* Bottom actions */}
          <div className="flex items-center justify-between mt-auto pt-12">
            <button onClick={() => navigate('/cart')}
              className="flex items-center gap-1 text-sm text-[#404040] hover:text-[#5A6D57] transition-colors">
              <ChevronLeft size={16} />
              Return To Cart
            </button>
            <button onClick={() => navigate('/checkout/shipping')}
              className="bg-[#5A6D57] hover:bg-[#748C70] text-white text-sm font-medium px-10 py-3 transition-colors">
              Continue To Shipping
            </button>
          </div>
        </div>

        {/* RIGHT â€” Order Summary (45%) */}
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
          <div className="mt-6">
            <OrderTotals />
          </div>
        </div>
      </div>

    </div>
  )
}
