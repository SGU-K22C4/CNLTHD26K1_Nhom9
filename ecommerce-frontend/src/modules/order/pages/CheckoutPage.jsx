import { useNavigate, Link } from 'react-router-dom'
import { X, Minus, Plus, User, ChevronLeft, MapPin } from 'lucide-react'
import { useCartContext } from '../../cart/context/CartContext'
import { formatCurrency } from '../../../shared/utils/format'
import AddressFields from '../../../shared/components/ui/AddressFields'
import BannerHeader from '../../../shared/components/layout/headers/BannerHeader'
import NavBar from '../../../shared/components/layout/headers/NavBar'
import { useCheckoutForm } from '../hooks/useCheckoutForm'
import { useLoyaltyPoints } from '../hooks/useLoyaltyPoints'
import { useCheckoutSubmit } from '../hooks/useCheckoutSubmit'

const TAX_RATE = 0.08

export default function CheckoutPage() {
  const { items, removeItem, updateQuantity, subtotal, totalItems, clearCart = () => {} } = useCartContext()
  const navigate = useNavigate()

  const tax = subtotal * TAX_RATE
  const total = subtotal + tax

  /* ── Custom Hooks ── */
  const {
    user, form, errors, validateForm, set,
    handleCityChange, handleWardChange,
    getInputClass, ErrorMsg,
    useRegisteredAddress, setUseRegisteredAddress,
    provinces, wards,
  } = useCheckoutForm()

  const {
    walletPoints, pointInput, setPointInput,
    appliedPoints, loyaltyDiscount,
    loyaltyMessage, loyaltyError, setLoyaltyError, setLoyaltyMessage,
    applyingPoints, handleApplyPoints, handleClearPoints, handleUseMaxPoints,
  } = useLoyaltyPoints()

  const { isSubmitting, handleSubmit } = useCheckoutSubmit()

  const handleContinue = () => {
    handleSubmit({
      form, items, appliedPoints, pointInput,
      validateForm, setLoyaltyError, clearCart, navigate,
    })
  }

  /* ── Shared: cart item row ── */
  const CartItemRow = ({ item }) => (
    <div className="relative flex items-start gap-4 py-5 border-b border-[#dfdfdf]">
      <div className="relative shrink-0">
        <img src={item.image} alt={item.name} className="w-[120px] h-[140px] object-cover" />
        <span className="absolute top-2 left-2 w-6 h-6 flex items-center justify-center bg-white text-[#202020] text-xs font-bold border border-[#dfdfdf]">
          {item.quantity}
        </span>
      </div>
      <div className="flex-1 flex flex-col gap-1 pr-6 pt-1">
        <p className="text-sm font-bold text-[#202020]">{item.name}</p>
        {item.size  && <p className="text-sm text-[#404040]">Size: {item.size}</p>}
        {item.color && <p className="text-sm text-[#404040]">Color: {item.color}</p>}
        <div className="flex items-center justify-between mt-3">
          <p className="text-sm font-bold text-[#202020]">{formatCurrency(item.price * item.quantity)}</p>
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
      <button onClick={() => removeItem(item.id)} aria-label={`Remove ${item.name}`}
        className="absolute top-5 right-0 text-[#202020] hover:opacity-50 transition-opacity">
        <X size={18} strokeWidth={1.5} />
      </button>
    </div>
  )

  /* ── Shared: order totals block ── */
  const OrderTotals = () => (
    <div className="flex flex-col gap-3 pt-5 border-t border-[#dfdfdf]">
      <div className="flex justify-between text-sm text-[#404040]">
        <span>Subtotal ({totalItems})</span><span>{formatCurrency(subtotal)}</span>
      </div>
      <div className="flex justify-between text-sm text-[#404040]">
        <span>Tax</span><span>{formatCurrency(tax)}</span>
      </div>
      <div className="flex justify-between text-sm text-[#404040]">
        <span>Shipping</span><span>Free</span>
      </div>
      {loyaltyDiscount > 0 && (
        <div className="flex justify-between text-sm text-[#10B981]">
          <span>Điểm tích lũy ({appliedPoints} điểm)</span>
          <span>-{formatCurrency(loyaltyDiscount)}</span>
        </div>
      )}
      <div className="flex justify-between text-sm font-bold text-[#202020] pt-3 border-t border-[#dfdfdf]">
        <span>Order Totals:</span><span>{formatCurrency(Math.max(total - loyaltyDiscount, 0))}</span>
      </div>
      <p className="text-[11px] text-[#202020] font-semibold leading-relaxed mt-1">
        The Total Amount You Pay Includes All Applicable Customs Duties &amp; Taxes. We Guarantee No Additional Charges On Delivery
      </p>
    </div>
  )

  /* ── Shared: loyalty points section ── */
  const LoyaltySection = ({ containerClass = "flex flex-col sm:flex-row gap-2" }) => (
    <div className="border border-[#dfdfdf] p-3">
      <p className="text-xs text-[#666] mb-2">Số dư hiện tại: <span className="font-semibold text-[#202020]">{walletPoints} điểm</span></p>
      <div className={containerClass}>
        <input
          type="number" min="1" value={pointInput}
          onChange={(e) => { setPointInput(e.target.value); setLoyaltyError(''); setLoyaltyMessage('') }}
          placeholder="Nhập số điểm muốn dùng"
          className={`${getInputClass('usedPoints')} flex-1`}
        />
        <button type="button" onClick={handleUseMaxPoints}
          className="h-[46px] px-4 border border-[#dfdfdf] text-xs font-medium text-[#202020] hover:border-[#5A6D57]">
          Dùng tối đa
        </button>
        <button type="button" onClick={() => handleApplyPoints(subtotal)} disabled={applyingPoints}
          className="h-[46px] px-4 bg-[#5A6D57] text-white text-xs font-medium disabled:opacity-70">
          {applyingPoints ? 'Đang áp dụng...' : 'Áp dụng'}
        </button>
        {(appliedPoints > 0 || pointInput) && (
          <button type="button" onClick={handleClearPoints}
            className="h-[46px] px-4 border border-[#dfdfdf] text-xs font-medium text-[#404040]">
            Bỏ
          </button>
        )}
      </div>
      {loyaltyMessage && <p className="text-xs text-[#0f766e] mt-2">{loyaltyMessage}</p>}
      {loyaltyError && <p className="text-xs text-red-500 mt-2">{loyaltyError}</p>}
    </div>
  )

  /* ── Shared: payment method selector ── */
  const PaymentMethodSelector = ({ radioName = "paymentMethod" }) => (
    <div className="flex flex-col gap-2">
      {[
        { value: 'COD', label: 'Thanh toán khi nhận hàng (COD)' },
        { value: 'VNPAY', label: 'VNPay' },
      ].map((opt) => (
        <label key={opt.value} className="flex items-center gap-3 p-3 border border-[#dfdfdf] cursor-pointer hover:border-[#5A6D57] transition-colors">
          <input type="radio" name={radioName} value={opt.value}
            checked={form.paymentMethod === opt.value}
            onChange={set('paymentMethod')}
            className="w-4 h-4 accent-[#5A6D57]" />
          <span className="text-sm text-[#202020]">{opt.label}</span>
        </label>
      ))}
    </div>
  )

  /* ── Shared: checkout form fields (mobile) ── */
  const CheckoutForm = () => (
    <>
      {/* Contact */}
      <div className="flex items-center justify-between mb-3 mt-6">
        <h2 className="text-base font-semibold text-[#202020]">Liên hệ</h2>
        {user ? (
          <p className="text-sm text-[#5A6D57] font-medium">👋 Xin chào, {user.firstName} {user.lastName}</p>
        ) : (
          <p className="text-sm text-[#404040]">
            Đã có tài khoản?{' '}
            <Link to="/login" className="underline font-medium hover:text-[#5A6D57] transition-colors">Đăng nhập</Link>
          </p>
        )}
      </div>
      <div className="relative mb-2">
        <User size={16} className={`absolute left-3 top-1/2 -translate-y-1/2 ${errors.email ? 'text-red-500' : 'text-[#9a9a9a]'}`} />
        <input type="email" placeholder="Email" value={form.email} onChange={set('email')}
          className={`${getInputClass('email')} pl-9`} />
      </div>
      <ErrorMsg field="email" />
      <label className="flex items-center gap-2 text-sm text-[#404040] mb-6 cursor-pointer select-none">
        <input type="checkbox" checked={form.emailOffers} onChange={set('emailOffers')}
          className="w-4 h-4 border-[#dfdfdf] accent-[#5A6D57]" />
        Gửi email khuyến mãi cho tôi
      </label>

      {/* Shipping Address */}
      <div className="flex items-center justify-between mb-3">
        <h2 className="text-base font-semibold text-[#202020]">Địa chỉ giao hàng</h2>
        {user && (
          <button type="button" onClick={() => setUseRegisteredAddress(!useRegisteredAddress)}
            className="text-xs text-[#5A6D57] underline hover:text-[#748C70] transition-colors flex items-center gap-1">
            <MapPin size={12} />
            {useRegisteredAddress ? 'Nhập địa chỉ khác' : 'Dùng thông tin đã đăng ký'}
          </button>
        )}
      </div>
      <div className="flex flex-col gap-3">
        <div>
          <input type="text" placeholder="Họ và tên" value={form.firstName} onChange={set('firstName')} className={getInputClass('firstName')} />
          <ErrorMsg field="firstName" />
        </div>
        <div>
          <input type="text" placeholder="Số điện thoại" value={form.phone} onChange={set('phone')} className={getInputClass('phone')} />
          <ErrorMsg field="phone" />
        </div>
        <AddressFields
          streetInputProps={{ value: form.street, onChange: set('street') }}
          streetError={errors.street}
          citySelectProps={{ value: form.cityCode, onChange: handleCityChange }}
          cityError={errors.city}
          wardSelectProps={{ value: form.wardCode, onChange: handleWardChange, disabled: !form.cityCode }}
          wardError={errors.ward}
          provinces={provinces} wards={wards}
          selectClassName={`${getInputClass('city')} appearance-none pr-10`}
          selectErrorClassName="" selectDefaultClassName=""
          errorTextClassName="text-red-500 text-xs mt-1 block"
          wrapperClassName="flex flex-col gap-3"
        />
      </div>

      {/* Payment Method */}
      <h2 className="text-base font-semibold text-[#202020] mt-6 mb-3">Phương thức thanh toán</h2>

      <h2 className="text-base font-semibold text-[#202020] mt-6 mb-3">Sử dụng điểm tích lũy</h2>
      <LoyaltySection />

      <PaymentMethodSelector />

      {/* Note */}
      <h2 className="text-base font-semibold text-[#202020] mt-6 mb-3">Ghi chú đơn hàng</h2>
      <textarea
        placeholder="Ghi chú cho đơn hàng (không bắt buộc)"
        value={form.note} onChange={set('note')} rows={3}
        className={`${getInputClass('note')} resize-none`}
      />

      <label className="flex items-center gap-2 text-sm text-[#404040] mt-4 cursor-pointer select-none">
        <input type="checkbox" checked={form.saveInfo} onChange={set('saveInfo')}
          className="w-4 h-4 border-[#dfdfdf] accent-[#5A6D57]" />
        Lưu thông tin cho lần sau
      </label>
    </>
  )

  return (
    <div className="min-h-screen bg-white font-[Montserrat]">

      {/* ── Site Header ── */}
      <BannerHeader />
      <NavBar />

      {/* ═══════════════════════════════════════════
          MOBILE LAYOUT (hidden on lg+)
      ═══════════════════════════════════════════ */}
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
          onClick={handleContinue}
          disabled={isSubmitting}
          className="w-full bg-[#5A6D57] hover:bg-[#748C70] text-white text-sm font-medium py-4 mt-6 transition-colors disabled:opacity-70"
        >
          {isSubmitting ? 'Đang xử lý...' : 'Tiếp tục & Hoàn tất'}
        </button>

        {/* Return To Cart */}
        <button
          onClick={() => navigate('/cart')}
          className="flex items-center justify-center gap-1 text-sm text-[#404040] hover:text-[#5A6D57] transition-colors mt-4"
        >
          <ChevronLeft size={16} />
          Quay lại Giỏ hàng
        </button>
      </div>

      {/* ═══════════════════════════════════════════
          DESKTOP LAYOUT (hidden below lg)
      ═══════════════════════════════════════════ */}
      <div className="hidden lg:flex flex-row min-h-screen">

        {/* LEFT — Form (55%) */}
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
            <h2 className="text-lg font-semibold text-[#202020]">Liên hệ</h2>
            {user ? (
              <p className="text-sm text-[#5A6D57] font-medium">👋 Xin chào, {user.firstName} {user.lastName}</p>
            ) : (
              <p className="text-sm text-[#404040]">
                Đã có tài khoản?{' '}
                <Link to="/login" className="underline font-medium hover:text-[#5A6D57] transition-colors">Đăng nhập</Link>
              </p>
            )}
          </div>
          <div className="relative mb-2">
            <User size={16} className={`absolute left-3 top-1/2 -translate-y-1/2 ${errors.email ? 'text-red-500' : 'text-[#9a9a9a]'}`} />
            <input type="email" placeholder="Email" value={form.email} onChange={set('email')}
              className={`${getInputClass('email')} pl-9`} />
          </div>
          <ErrorMsg field="email" />
          <label className="flex items-center gap-2 text-sm text-[#404040] mb-8 cursor-pointer select-none">
            <input type="checkbox" checked={form.emailOffers} onChange={set('emailOffers')}
              className="w-4 h-4 accent-[#5A6D57]" />
            Gửi email khuyến mãi cho tôi
          </label>

          {/* Shipping Address */}
          <div className="flex items-center justify-between mb-4">
            <h2 className="text-lg font-semibold text-[#202020]">Địa chỉ giao hàng</h2>
            {user && (
              <button type="button" onClick={() => setUseRegisteredAddress(!useRegisteredAddress)}
                className="text-xs text-[#5A6D57] underline hover:text-[#748C70] transition-colors flex items-center gap-1">
                <MapPin size={14} />
                {useRegisteredAddress ? 'Nhập địa chỉ khác' : 'Dùng thông tin đã đăng ký'}
              </button>
            )}
          </div>
          <div className="flex flex-col gap-4">
            <div>
              <input type="text" placeholder="Họ và tên" value={form.firstName} onChange={set('firstName')} className={getInputClass('firstName')} />
              <ErrorMsg field="firstName" />
            </div>
            <div>
              <input type="text" placeholder="Số điện thoại" value={form.phone} onChange={set('phone')} className={getInputClass('phone')} />
              <ErrorMsg field="phone" />
            </div>
            <AddressFields
              streetInputProps={{ value: form.street, onChange: set('street') }}
              streetError={errors.street}
              citySelectProps={{ value: form.cityCode, onChange: handleCityChange }}
              cityError={errors.city}
              wardSelectProps={{ value: form.wardCode, onChange: handleWardChange, disabled: !form.cityCode }}
              wardError={errors.ward}
              provinces={provinces} wards={wards}
              selectClassName={`${getInputClass('city')} appearance-none pr-10`}
              selectErrorClassName="" selectDefaultClassName=""
              errorTextClassName="text-red-500 text-xs mt-1 block"
              wrapperClassName="flex flex-col gap-4"
            />
          </div>

          <h2 className="text-lg font-semibold text-[#202020] mt-8 mb-4">Sử dụng điểm tích lũy</h2>
          <LoyaltySection containerClass="flex items-center gap-2" />

          {/* Payment Method */}
          <h2 className="text-lg font-semibold text-[#202020] mt-8 mb-4">Phương thức thanh toán</h2>
          <PaymentMethodSelector radioName="paymentMethodDesktop" />

          {/* Note */}
          <h2 className="text-lg font-semibold text-[#202020] mt-8 mb-4">Ghi chú đơn hàng</h2>
          <textarea
            placeholder="Ghi chú cho đơn hàng (không bắt buộc)"
            value={form.note} onChange={set('note')} rows={3}
            className={`${getInputClass('note')} resize-none`}
          />

          <label className="flex items-center gap-2 text-sm text-[#404040] mt-4 cursor-pointer select-none">
            <input type="checkbox" checked={form.saveInfo} onChange={set('saveInfo')}
              className="w-4 h-4 accent-[#5A6D57]" />
            Lưu thông tin cho lần sau
          </label>

          {/* Bottom actions */}
          <div className="flex items-center justify-between mt-auto pt-12">
            <button onClick={() => navigate('/cart')}
              className="flex items-center gap-1 text-sm text-[#404040] hover:text-[#5A6D57] transition-colors">
              <ChevronLeft size={16} />
              Return To Cart
            </button>
            <button onClick={handleContinue}
              disabled={isSubmitting}
              className="bg-[#5A6D57] hover:bg-[#748C70] text-white text-sm font-medium px-10 py-3 transition-colors disabled:opacity-70">
              {isSubmitting ? 'Đang xử lý...' : 'Tiếp tục & Hoàn tất'}
            </button>
          </div>
        </div>

        {/* RIGHT — Order Summary (45%) */}
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
                    <span className="text-sm font-bold text-[#202020]">{formatCurrency(item.price * item.quantity)}</span>
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
