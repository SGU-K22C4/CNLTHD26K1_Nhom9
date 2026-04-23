import { useState, useEffect, useContext } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { X, Minus, Plus, User, ChevronLeft, ChevronDown, MapPin } from 'lucide-react'
import { useCartContext } from '../../cart/context/CartContext'
import { formatCurrency } from '../../../shared/utils/format'
import { orderService } from '../services/orderService'
import { paymentService } from '../services/paymentService'
import { loyaltyService } from '../services/loyaltyService'
import { userService } from '../../user/services/userService'
import AuthContext from '../../auth/context/AuthContext'
import BannerHeader from '../../../shared/components/layout/headers/BannerHeader'
import NavBar from '../../../shared/components/layout/headers/NavBar'

const TAX_RATE = 0.08

export default function CheckoutPage() {
  const { items, removeItem, updateQuantity, subtotal, totalItems, clearCart = () => {} } = useCartContext()
  const { user } = useContext(AuthContext)
  const navigate = useNavigate()

  const tax = subtotal * TAX_RATE
  const total = subtotal + tax

  /* ―― Form state ―― */
  const [form, setForm] = useState({
    email: '',
    emailOffers: false,
    firstName: '',
    lastName: '',
    company: '',
    address: '',
    apartment: '',
    ward: '',
    wardId: '',
    district: '',
    districtId: '',
    city: '',
    cityId: '',
    phone: '',
    saveInfo: false,
    paymentMethod: 'COD',
    note: '',
  })

  const [errors, setErrors] = useState({})
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [walletPoints, setWalletPoints] = useState(0)
  const [pointInput, setPointInput] = useState('')
  const [appliedPoints, setAppliedPoints] = useState(0)
  const [loyaltyDiscount, setLoyaltyDiscount] = useState(0)
  const [loyaltyMessage, setLoyaltyMessage] = useState('')
  const [loyaltyError, setLoyaltyError] = useState('')
  const [applyingPoints, setApplyingPoints] = useState(false)
  const [useRegisteredAddress, setUseRegisteredAddress] = useState(true)

  /* ── Address Dropdowns State ── */
  const [provinces, setProvinces] = useState([])
  const [districts, setDistricts] = useState([])
  const [wards, setWards] = useState([])

  /* ── Auto-fill from user profile ── */
  useEffect(() => {
    if (!user) return
    // Fill from AuthContext immediately
    setForm(prev => ({
      ...prev,
      email: prev.email || user.email || '',
      firstName: prev.firstName || user.firstName || '',
      lastName: prev.lastName || user.lastName || '',
    }))
    // Then fetch full profile for phone
    userService.getProfile()
      .then(profile => {
        if (!profile) return
        setForm(prev => ({
          ...prev,
          email: prev.email || profile.email || '',
          firstName: prev.firstName || profile.firstName || '',
          lastName: prev.lastName || profile.lastName || '',
          phone: prev.phone || profile.phoneNumber || '',
        }))
      })
      .catch(err => console.warn('Could not load user profile for auto-fill:', err))
  }, [user])

  useEffect(() => {
    fetch('https://provinces.open-api.vn/api/p/')
      .then(res => res.json())
      .then(data => {
        setProvinces(data)
      })
      .catch(err => console.error('Failed to load provinces:', err))
  }, [])

  useEffect(() => {
    let mounted = true
    loyaltyService.getWallet()
      .then((wallet) => {
        if (!mounted) return
        setWalletPoints(Number(wallet?.currentPoints) || 0)
      })
      .catch((err) => {
        if (!mounted) return
        setWalletPoints(0)
        setLoyaltyError(err?.message || 'Không tải được số dư điểm')
      })

    return () => {
      mounted = false
    }
  }, [])

  const handleCityChange = (e) => {
    const [id, name] = e.target.value.split('|')
    setForm(prev => ({ ...prev, city: name, cityId: id, district: '', districtId: '', ward: '', wardId: '' }))
    setErrors(prev => ({ ...prev, city: null, district: null, ward: null }))
    setDistricts([])
    setWards([])
    if (id) {
      fetch(`https://provinces.open-api.vn/api/p/${id}?depth=2`)
        .then(res => res.json())
        .then(data => {
          setDistricts(data.districts || [])
        })
    }
  }

  const handleDistrictChange = (e) => {
    const [id, name] = e.target.value.split('|')
    setForm(prev => ({ ...prev, district: name, districtId: id, ward: '', wardId: '' }))
    setErrors(prev => ({ ...prev, district: null, ward: null }))
    setWards([])
    if (id) {
      fetch(`https://provinces.open-api.vn/api/d/${id}?depth=2`)
        .then(res => res.json())
        .then(data => {
          setWards(data.wards || [])
        })
    }
  }

  const handleWardChange = (e) => {
    const [id, name] = e.target.value.split('|')
    setForm(prev => ({ ...prev, ward: name, wardId: id }))
    setErrors(prev => ({ ...prev, ward: null }))
  }

  const set = (field) => (e) => {
    const value = e.target.type === 'checkbox' ? e.target.checked : e.target.value
    setForm((prev) => ({ ...prev, [field]: value }))
    if (errors[field]) setErrors(prev => ({ ...prev, [field]: null }))
  }

  const validateForm = () => {
    const newErrors = {}
    if (!form.email.trim()) newErrors.email = 'Vui lòng nhập email'
    else if (!/^\S+@\S+\.\S+$/.test(form.email)) newErrors.email = 'Email không hợp lệ'

    if (!form.firstName.trim()) newErrors.firstName = 'Vui lòng nhập họ'
    if (!form.lastName.trim()) newErrors.lastName = 'Vui lòng nhập tên'
    
    if (!form.phone.trim()) newErrors.phone = 'Vui lòng nhập số điện thoại'
    else if (!/(84|0[3|5|7|8|9])+([0-9]{8})\b/.test(form.phone)) newErrors.phone = 'Số điện thoại không hợp lệ'
    
    if (!form.address.trim()) newErrors.address = 'Vui lòng nhập địa chỉ'
    if (!form.cityId) newErrors.city = 'Vui lòng chọn Tỉnh / Thành phố'
    if (!form.districtId) newErrors.district = 'Vui lòng chọn Quận / Huyện'
    if (!form.wardId) newErrors.ward = 'Vui lòng chọn Phường / Xã'

    setErrors(newErrors)
    return Object.keys(newErrors).length === 0
  }

  const handleContinue = async () => {
    if (validateForm()) {
      if (pointInput && Number(pointInput) > 0 && appliedPoints <= 0) {
        setLoyaltyError('Hãy nhấn "Áp dụng" điểm trước khi đặt hàng')
        return
      }

      try {
        setIsSubmitting(true)
        const payload = buildOrderPayload()
        const savedOrder = await orderService.create(payload)
        
        // If VNPAY, redirect to VNPay payment page
        if (form.paymentMethod === 'VNPAY') {
          const { paymentUrl } = await paymentService.createVnpayPayment(savedOrder.id)
          window.location.href = paymentUrl
          return // Don't clear cart or navigate — VNPay will redirect back
        }

        // COD or other methods: clear cart and go to order detail page
        await clearCart()
        navigate(`/orders/${savedOrder.id}?from=payment`, { replace: true })
      } catch (err) {
        console.error('Submit order failed:', err)
        alert(err?.message || 'Tạo đơn hàng thất bại. Vui lòng thử lại.')
      } finally {
        setIsSubmitting(false)
      }
    }
  }

  const handleApplyPoints = async () => {
    const requested = Number(pointInput)
    if (!Number.isInteger(requested) || requested <= 0) {
      setLoyaltyError('Số điểm không hợp lệ')
      return
    }

    setApplyingPoints(true)
    setLoyaltyError('')
    setLoyaltyMessage('')

    try {
      const preview = await loyaltyService.previewRedeem({
        orderAmount: subtotal,
        requestedPoints: requested,
      })

      if (!preview?.valid) {
        setAppliedPoints(0)
        setLoyaltyDiscount(0)
        setLoyaltyError(preview?.message || 'Không thể áp dụng điểm')
        return
      }

      setAppliedPoints(Number(preview?.appliedPoints) || 0)
      setLoyaltyDiscount(Number(preview?.discountAmount) || 0)
      setWalletPoints(Number(preview?.currentPoints) || walletPoints)
      setLoyaltyMessage(preview?.message || 'Áp dụng điểm thành công')
      setLoyaltyError('')
    } catch (err) {
      setAppliedPoints(0)
      setLoyaltyDiscount(0)
      setLoyaltyError(err?.message || 'Không thể áp dụng điểm')
    } finally {
      setApplyingPoints(false)
    }
  }

  const handleClearPoints = () => {
    setPointInput('')
    setAppliedPoints(0)
    setLoyaltyDiscount(0)
    setLoyaltyMessage('')
    setLoyaltyError('')
  }

  const handleUseMaxPoints = () => {
    if (walletPoints <= 0) {
      setLoyaltyError('Bạn chưa có điểm để sử dụng')
      return
    }
    setPointInput(String(walletPoints))
    setLoyaltyError('')
  }

  const getInputClass = (field) =>
    `w-full border p-3 text-sm text-[#202020] placeholder-[#9a9a9a] outline-none transition-colors bg-white ${
      errors[field] ? 'border-red-500 focus:border-red-500' : 'border-[#dfdfdf] focus:border-[#5A6D57]'
    }`

  const ErrorMsg = ({ field }) => errors[field] ? <span className="text-red-500 text-xs mt-1 block">{errors[field]}</span> : null

  /* ── Build payload matching DB Order entity ── */
  const buildOrderPayload = () => ({
    recipientName: `${form.firstName} ${form.lastName}`.trim(),
    recipientPhone: form.phone,
    shippingAddress: [form.address, form.apartment, form.ward, form.district, form.city]
      .filter(Boolean).join(', '),
    paymentMethod: form.paymentMethod,
    note: form.note || null,
    email: form.email || null,
    usedPoints: appliedPoints,
    items: items.map(item => ({
      productId: item.productId || item.id,
      productName: item.name,
      productSlug: item.slug || item.productSlug || '',
      imageUrl: item.imageUrl || item.image || '',
      color: item.color,
      size: item.size,
      quantity: item.quantity,
      unitPrice: item.price
    }))
  })

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

  /* â”€â”€ Shared: checkout form fields â”€â”€ */
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
          <button
            type="button"
            onClick={() => setUseRegisteredAddress(!useRegisteredAddress)}
            className="text-xs text-[#5A6D57] underline hover:text-[#748C70] transition-colors flex items-center gap-1"
          >
            <MapPin size={12} />
            {useRegisteredAddress ? 'Nhập địa chỉ khác' : 'Dùng thông tin đã đăng ký'}
          </button>
        )}
      </div>
      <div className="flex flex-col gap-3">
        <div>
          <input type="text" placeholder="Họ" value={form.firstName} onChange={set('firstName')} className={getInputClass('firstName')} />
          <ErrorMsg field="firstName" />
        </div>
        <div>
          <input type="text" placeholder="Tên" value={form.lastName} onChange={set('lastName')} className={getInputClass('lastName')} />
          <ErrorMsg field="lastName" />
        </div>
        <div>
          <input type="text" placeholder="Số điện thoại" value={form.phone} onChange={set('phone')}
            className={getInputClass('phone')} />
          <ErrorMsg field="phone" />
        </div>
        <div>
          <input type="text" placeholder="Địa chỉ (số nhà, tên đường)" value={form.address} onChange={set('address')} className={getInputClass('address')} />
          <ErrorMsg field="address" />
        </div>
        <input type="text" placeholder="Căn hộ, Tầng, Tòa nhà (không bắt buộc)" value={form.apartment} onChange={set('apartment')} className={getInputClass('apartment')} />
        
        <div>
          <div className="relative">
            <select value={form.cityId ? `${form.cityId}|${form.city}` : ""} onChange={handleCityChange}
              className={`${getInputClass('city')} appearance-none pr-10`}>
              <option value="" disabled>Tỉnh / Thành phố</option>
              {provinces.map(p => <option key={p.code} value={`${p.code}|${p.name}`}>{p.name}</option>)}
            </select>
            <ChevronDown size={16} className="absolute right-3 top-1/2 -translate-y-1/2 text-[#9a9a9a] pointer-events-none" />
          </div>
          <ErrorMsg field="city" />
        </div>
        
        <div>
          <div className="relative">
            <select value={form.districtId ? `${form.districtId}|${form.district}` : ""} onChange={handleDistrictChange}
              disabled={!form.cityId}
              className={`${getInputClass('district')} appearance-none pr-10 disabled:bg-gray-100 disabled:cursor-not-allowed`}>
              <option value="" disabled>Quận / Huyện</option>
              {districts.map(d => <option key={d.code} value={`${d.code}|${d.name}`}>{d.name}</option>)}
            </select>
            <ChevronDown size={16} className="absolute right-3 top-1/2 -translate-y-1/2 text-[#9a9a9a] pointer-events-none" />
          </div>
          <ErrorMsg field="district" />
        </div>
        
        <div>
          <div className="relative">
            <select value={form.wardId ? `${form.wardId}|${form.ward}` : ""} onChange={handleWardChange}
              disabled={!form.districtId}
              className={`${getInputClass('ward')} appearance-none pr-10 disabled:bg-gray-100 disabled:cursor-not-allowed`}>
              <option value="" disabled>Phường / Xã</option>
              {wards.map(w => <option key={w.code} value={`${w.code}|${w.name}`}>{w.name}</option>)}
            </select>
            <ChevronDown size={16} className="absolute right-3 top-1/2 -translate-y-1/2 text-[#9a9a9a] pointer-events-none" />
          </div>
          <ErrorMsg field="ward" />
        </div>

        <input type="text" placeholder="Công ty (không bắt buộc)" value={form.company} onChange={set('company')} className={getInputClass('company')} />
      </div>

      {/* Payment Method */}
      <h2 className="text-base font-semibold text-[#202020] mt-6 mb-3">Phương thức thanh toán</h2>

      <h2 className="text-base font-semibold text-[#202020] mt-6 mb-3">Sử dụng điểm tích lũy</h2>
      <div className="border border-[#dfdfdf] p-3">
        <p className="text-xs text-[#666] mb-2">Số dư hiện tại: <span className="font-semibold text-[#202020]">{walletPoints} điểm</span></p>
        <div className="flex flex-col sm:flex-row gap-2">
          <input
            type="number"
            min="1"
            value={pointInput}
            onChange={(e) => {
              setPointInput(e.target.value)
              setLoyaltyError('')
              setLoyaltyMessage('')
            }}
            placeholder="Nhập số điểm muốn dùng"
            className={`${getInputClass('usedPoints')} flex-1`}
          />
          <button
            type="button"
            onClick={handleUseMaxPoints}
            className="h-[46px] px-4 border border-[#dfdfdf] text-xs font-medium text-[#202020] hover:border-[#5A6D57]"
          >
            Dùng tối đa
          </button>
          <button
            type="button"
            onClick={handleApplyPoints}
            disabled={applyingPoints}
            className="h-[46px] px-4 bg-[#5A6D57] text-white text-xs font-medium disabled:opacity-70"
          >
            {applyingPoints ? 'Đang áp dụng...' : 'Áp dụng'}
          </button>
          {(appliedPoints > 0 || pointInput) && (
            <button
              type="button"
              onClick={handleClearPoints}
              className="h-[46px] px-4 border border-[#dfdfdf] text-xs font-medium text-[#404040]"
            >
              Bỏ
            </button>
          )}
        </div>
        {loyaltyMessage && <p className="text-xs text-[#0f766e] mt-2">{loyaltyMessage}</p>}
        {loyaltyError && <p className="text-xs text-red-500 mt-2">{loyaltyError}</p>}
      </div>

      <div className="flex flex-col gap-2">
        {[
          { value: 'COD', label: 'Thanh toán khi nhận hàng (COD)' },
          { value: 'VNPAY', label: 'VNPay' },
        ].map((opt) => (
          <label key={opt.value} className="flex items-center gap-3 p-3 border border-[#dfdfdf] cursor-pointer hover:border-[#5A6D57] transition-colors">
            <input type="radio" name="paymentMethod" value={opt.value}
              checked={form.paymentMethod === opt.value}
              onChange={set('paymentMethod')}
              className="w-4 h-4 accent-[#5A6D57]" />
            <span className="text-sm text-[#202020]">{opt.label}</span>
          </label>
        ))}
      </div>

      {/* Note */}
      <h2 className="text-base font-semibold text-[#202020] mt-6 mb-3">Ghi chú đơn hàng</h2>
      <textarea
        placeholder="Ghi chú cho đơn hàng (không bắt buộc)"
        value={form.note}
        onChange={set('note')}
        rows={3}
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

        {/* Return To Card */}
        <button
          onClick={() => navigate('/cart')}
          className="flex items-center justify-center gap-1 text-sm text-[#404040] hover:text-[#5A6D57] transition-colors mt-4"
        >
          <ChevronLeft size={16} />
          Quay lại Giỏ hàng
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
              <button
                type="button"
                onClick={() => setUseRegisteredAddress(!useRegisteredAddress)}
                className="text-xs text-[#5A6D57] underline hover:text-[#748C70] transition-colors flex items-center gap-1"
              >
                <MapPin size={14} />
                {useRegisteredAddress ? 'Nhập địa chỉ khác' : 'Dùng thông tin đã đăng ký'}
              </button>
            )}
          </div>
          <div className="flex flex-col gap-4">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <input type="text" placeholder="Họ" value={form.firstName} onChange={set('firstName')} className={getInputClass('firstName')} />
                <ErrorMsg field="firstName" />
              </div>
              <div>
                <input type="text" placeholder="Tên" value={form.lastName} onChange={set('lastName')} className={getInputClass('lastName')} />
                <ErrorMsg field="lastName" />
              </div>
            </div>
            <div>
              <input type="text" placeholder="Số điện thoại" value={form.phone} onChange={set('phone')} className={getInputClass('phone')} />
              <ErrorMsg field="phone" />
            </div>
            <div>
              <input type="text" placeholder="Địa chỉ (số nhà, tên đường)" value={form.address} onChange={set('address')} className={getInputClass('address')} />
              <ErrorMsg field="address" />
            </div>
            <input type="text" placeholder="Căn hộ, Tầng, Tòa nhà (không bắt buộc)" value={form.apartment} onChange={set('apartment')} className={getInputClass('apartment')} />
            
            <div>
              <div className="relative">
                <select value={form.cityId ? `${form.cityId}|${form.city}` : ""} onChange={handleCityChange}
                  className={`${getInputClass('city')} appearance-none pr-10`}>
                  <option value="" disabled>Tỉnh / Thành phố</option>
                  {provinces.map(p => <option key={p.code} value={`${p.code}|${p.name}`}>{p.name}</option>)}
                </select>
                <ChevronDown size={16} className="absolute right-3 top-1/2 -translate-y-1/2 text-[#9a9a9a] pointer-events-none" />
              </div>
              <ErrorMsg field="city" />
            </div>
            
            <div className="grid grid-cols-2 gap-4">
              <div>
                <div className="relative">
                  <select value={form.districtId ? `${form.districtId}|${form.district}` : ""} onChange={handleDistrictChange}
                    disabled={!form.cityId}
                    className={`${getInputClass('district')} appearance-none pr-10 disabled:bg-gray-100 disabled:cursor-not-allowed`}>
                    <option value="" disabled>Quận / Huyện</option>
                    {districts.map(d => <option key={d.code} value={`${d.code}|${d.name}`}>{d.name}</option>)}
                  </select>
                  <ChevronDown size={16} className="absolute right-3 top-1/2 -translate-y-1/2 text-[#9a9a9a] pointer-events-none" />
                </div>
                <ErrorMsg field="district" />
              </div>
              
              <div>
                <div className="relative">
                  <select value={form.wardId ? `${form.wardId}|${form.ward}` : ""} onChange={handleWardChange}
                    disabled={!form.districtId}
                    className={`${getInputClass('ward')} appearance-none pr-10 disabled:bg-gray-100 disabled:cursor-not-allowed`}>
                    <option value="" disabled>Phường / Xã</option>
                    {wards.map(w => <option key={w.code} value={`${w.code}|${w.name}`}>{w.name}</option>)}
                  </select>
                  <ChevronDown size={16} className="absolute right-3 top-1/2 -translate-y-1/2 text-[#9a9a9a] pointer-events-none" />
                </div>
                <ErrorMsg field="ward" />
              </div>
            </div>

            <input type="text" placeholder="Công ty (không bắt buộc)" value={form.company} onChange={set('company')} className={getInputClass('company')} />
          </div>

          <h2 className="text-lg font-semibold text-[#202020] mt-8 mb-4">Sử dụng điểm tích lũy</h2>
          <div className="border border-[#dfdfdf] p-3">
            <p className="text-xs text-[#666] mb-2">Số dư hiện tại: <span className="font-semibold text-[#202020]">{walletPoints} điểm</span></p>
            <div className="flex items-center gap-2">
              <input
                type="number"
                min="1"
                value={pointInput}
                onChange={(e) => {
                  setPointInput(e.target.value)
                  setLoyaltyError('')
                  setLoyaltyMessage('')
                }}
                placeholder="Nhập số điểm muốn dùng"
                className={`${getInputClass('usedPoints')} flex-1`}
              />
              <button
                type="button"
                onClick={handleUseMaxPoints}
                className="h-[46px] px-4 border border-[#dfdfdf] text-xs font-medium text-[#202020] hover:border-[#5A6D57]"
              >
                Dùng tối đa
              </button>
              <button
                type="button"
                onClick={handleApplyPoints}
                disabled={applyingPoints}
                className="h-[46px] px-4 bg-[#5A6D57] text-white text-xs font-medium disabled:opacity-70"
              >
                {applyingPoints ? 'Đang áp dụng...' : 'Áp dụng'}
              </button>
              {(appliedPoints > 0 || pointInput) && (
                <button
                  type="button"
                  onClick={handleClearPoints}
                  className="h-[46px] px-4 border border-[#dfdfdf] text-xs font-medium text-[#404040]"
                >
                  Bỏ
                </button>
              )}
            </div>
            {loyaltyMessage && <p className="text-xs text-[#0f766e] mt-2">{loyaltyMessage}</p>}
            {loyaltyError && <p className="text-xs text-red-500 mt-2">{loyaltyError}</p>}
          </div>

          {/* Payment Method */}
          <h2 className="text-lg font-semibold text-[#202020] mt-8 mb-4">Phương thức thanh toán</h2>
          <div className="flex flex-col gap-2">
            {[
              { value: 'COD', label: 'Thanh toán khi nhận hàng (COD)' },
              { value: 'VNPAY', label: 'VNPay' },
            ].map((opt) => (
              <label key={opt.value} className="flex items-center gap-3 p-3 border border-[#dfdfdf] cursor-pointer hover:border-[#5A6D57] transition-colors">
                <input type="radio" name="paymentMethodDesktop" value={opt.value}
                  checked={form.paymentMethod === opt.value}
                  onChange={set('paymentMethod')}
                  className="w-4 h-4 accent-[#5A6D57]" />
                <span className="text-sm text-[#202020]">{opt.label}</span>
              </label>
            ))}
          </div>

          {/* Note */}
          <h2 className="text-lg font-semibold text-[#202020] mt-8 mb-4">Ghi chú đơn hàng</h2>
          <textarea
            placeholder="Ghi chú cho đơn hàng (không bắt buộc)"
            value={form.note}
            onChange={set('note')}
            rows={3}
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
