import { useState, useEffect, useContext } from 'react'
import AuthContext from '../../auth/context/AuthContext'
import { userService } from '../../user/services/userService'
import { useProvinces } from '../../../shared/hooks/useProvinces'

const INITIAL_FORM = {
  email: '',
  emailOffers: false,
  firstName: '',
  lastName: '',
  street: '',
  ward: '',
  wardCode: '',
  city: '',
  cityCode: '',
  phone: '',
  saveInfo: false,
  paymentMethod: 'COD',
  note: '',
}

/**
 * Custom hook for checkout form state, validation, and address helpers.
 * Extracts form logic from CheckoutPage for better separation of concerns.
 */
export function useCheckoutForm() {
  const { user } = useContext(AuthContext)

  const [form, setForm] = useState(INITIAL_FORM)
  const [errors, setErrors] = useState({})
  const [useRegisteredAddress, setUseRegisteredAddress] = useState(true)
  const [registeredAddress, setRegisteredAddress] = useState(null)
  const { provinces } = useProvinces(2)

  const wards = form.cityCode
    ? (provinces.find((province) => String(province.code) === String(form.cityCode))?.wards || [])
    : []

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
    Promise.all([userService.getProfile(), userService.getAddresses()])
      .then(([profile, addresses]) => {
        if (!profile) return
        const defaultAddress = (addresses || []).find((address) => address.isDefault) || (addresses || [])[0] || null
        setRegisteredAddress(defaultAddress)
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

  /* ── Auto-fill registered address ── */
  useEffect(() => {
    if (!user || !useRegisteredAddress || !registeredAddress || provinces.length === 0) return

    const matchedCity = provinces.find((province) => province.name === registeredAddress.city)
    const matchedWard = matchedCity?.wards?.find((ward) => ward.name === registeredAddress.ward)

    setForm((prev) => ({
      ...prev,
      street: registeredAddress.street || '',
      city: matchedCity?.name || registeredAddress.city || '',
      cityCode: matchedCity?.code ? String(matchedCity.code) : '',
      ward: matchedWard?.name || registeredAddress.ward || '',
      wardCode: matchedWard?.code ? String(matchedWard.code) : '',
    }))
  }, [user, useRegisteredAddress, registeredAddress, provinces])

  /* ── Handlers ── */
  const handleCityChange = (e) => {
    const cityCode = e.target.value
    const matchedCity = provinces.find((province) => String(province.code) === cityCode)
    setForm(prev => ({ ...prev, city: matchedCity?.name || '', cityCode, ward: '', wardCode: '' }))
    setErrors(prev => ({ ...prev, city: null, ward: null }))
  }

  const handleWardChange = (e) => {
    const wardCode = e.target.value
    const matchedWard = wards.find((ward) => String(ward.code) === wardCode)
    setForm(prev => ({ ...prev, ward: matchedWard?.name || '', wardCode }))
    setErrors(prev => ({ ...prev, ward: null }))
  }

  const set = (field) => (e) => {
    const value = e.target.type === 'checkbox' ? e.target.checked : e.target.value
    setForm((prev) => ({ ...prev, [field]: value }))
    if (errors[field]) setErrors(prev => ({ ...prev, [field]: null }))
  }

  /* ── Validation ── */
  const validateForm = () => {
    const newErrors = {}
    if (!form.email.trim()) newErrors.email = 'Vui lòng nhập email'
    else if (!/^\S+@\S+\.\S+$/.test(form.email)) newErrors.email = 'Email không hợp lệ'

    if (!form.firstName.trim()) newErrors.firstName = 'Vui lòng nhập họ'
    if (!form.lastName.trim()) newErrors.lastName = 'Vui lòng nhập tên'

    if (!form.phone.trim()) newErrors.phone = 'Vui lòng nhập số điện thoại'
    else if (!/(84|0[3|5|7|8|9])+([0-9]{8})\b/.test(form.phone)) newErrors.phone = 'Số điện thoại không hợp lệ'

    if (!form.street.trim()) newErrors.street = 'Vui lòng nhập địa chỉ'
    if (!form.cityCode) newErrors.city = 'Vui lòng chọn Tỉnh / Thành phố'
    if (!form.wardCode) newErrors.ward = 'Vui lòng chọn Phường / Xã'

    setErrors(newErrors)
    return Object.keys(newErrors).length === 0
  }

  /* ── UI Helpers ── */
  const getInputClass = (field) =>
    `w-full border p-3 text-sm text-[#202020] placeholder-[#9a9a9a] outline-none transition-colors bg-white ${
      errors[field] ? 'border-red-500 focus:border-red-500' : 'border-[#dfdfdf] focus:border-[#5A6D57]'
    }`

  const ErrorMsg = ({ field }) => errors[field] ? <span className="text-red-500 text-xs mt-1 block">{errors[field]}</span> : null

  return {
    user,
    form,
    setForm,
    errors,
    setErrors,
    validateForm,
    set,
    handleCityChange,
    handleWardChange,
    getInputClass,
    ErrorMsg,
    useRegisteredAddress,
    setUseRegisteredAddress,
    registeredAddress,
    provinces,
    wards,
  }
}
