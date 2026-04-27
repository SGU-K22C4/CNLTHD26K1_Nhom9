import { useEffect, useState, useContext } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { userService } from '../services/userService'
import AuthContext from '../../auth/context/AuthContext'
import { User, Mail, Phone, Lock, Edit2, Check, X, Shield, MapPin, Plus, Trash2 } from 'lucide-react'
import AddressModal from '../components/AddressModal'

export default function ProfilePage() {
  const { user, login } = useContext(AuthContext)
  const navigate = useNavigate()

  const [loading, setLoading] = useState(true)
  const [profile, setProfile] = useState(null)
  
  // Edit Profile mode state
  const [isEditing, setIsEditing] = useState(false)
  const [editForm, setEditForm] = useState({
    firstName: '',
    lastName: '',
    phoneNumber: '',
  })
  const [editError, setEditError] = useState('')
  const [editSuccess, setEditSuccess] = useState('')

  // Address state
  const [addresses, setAddresses] = useState([])
  const [isAddressModalOpen, setIsAddressModalOpen] = useState(false)
  const [selectedAddress, setSelectedAddress] = useState(null)

  // Change Password state
  const [pwdForm, setPwdForm] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  })
  const [pwdError, setPwdError] = useState('')
  const [pwdSuccess, setPwdSuccess] = useState('')

  useEffect(() => {
    let mounted = true
    const loadProfile = async () => {
      setLoading(true)
      try {
        const [profileData, addressesData] = await Promise.all([
          userService.getProfile(),
          userService.getAddresses()
        ])
        
        if (mounted) {
          setProfile(profileData)
          setAddresses(addressesData || [])
          setEditForm({
            firstName: profileData.firstName || '',
            lastName: profileData.lastName || '',
            phoneNumber: profileData.phoneNumber || '',
          })
        }
      } catch (err) {
        if (mounted) setEditError('Không thể tải thông tin hồ sơ.')
      } finally {
        if (mounted) setLoading(false)
      }
    }
    loadProfile()

    return () => { mounted = false }
  }, [])

  const handleEditChange = (e) => {
    setEditForm({ ...editForm, [e.target.name]: e.target.value })
  }

  const handleEditSubmit = async (e) => {
    e.preventDefault()
    setEditError('')
    setEditSuccess('')
    if (!editForm.firstName.trim() || !editForm.lastName.trim()) {
      setEditError('Họ và Tên không được để trống.')
      return
    }

    try {
      const updatedProfile = await userService.updateProfile({
        fullName: `${editForm.lastName} ${editForm.firstName}`.trim(),
        phone: editForm.phoneNumber,
      })

      setProfile(updatedProfile)
      setIsEditing(false)
      setEditSuccess('Cập nhật hồ sơ thành công.')
      
      // Update global user context so nav bar updates
      if (user) {
        // Note: we can't fully update the jwt or local storage easily if the format changes,
        // but we can try to re-trigger a login context refresh. For now, updating local state is fine,
        // user may need to re-login to ensure token has new name if it's encoded in JWT.
        const currentUserInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
        const newUserInfo = { ...currentUserInfo, firstName: updatedProfile.firstName, lastName: updatedProfile.lastName }
        localStorage.setItem('userInfo', JSON.stringify(newUserInfo))
        
        // Reload page to refresh AuthContext properly (safest way without complex state management)
        setTimeout(() => window.location.reload(), 1000)
      }

    } catch (err) {
      setEditError(err?.message || 'Cập nhật thất bại.')
    }
  }

  const handlePwdChange = (e) => {
    setPwdForm({ ...pwdForm, [e.target.name]: e.target.value })
  }

  const handlePwdSubmit = async (e) => {
    e.preventDefault()
    setPwdError('')
    setPwdSuccess('')

    if (!pwdForm.currentPassword || !pwdForm.newPassword || !pwdForm.confirmPassword) {
      setPwdError('Vui lòng điền đủ thông tin mật khẩu.')
      return
    }
    if (pwdForm.newPassword !== pwdForm.confirmPassword) {
      setPwdError('Mật khẩu mới không khớp.')
      return
    }
    if (pwdForm.newPassword.length < 8) {
      setPwdError('Mật khẩu mới phải có ít nhất 8 ký tự.')
      return
    }

    try {
      await userService.changePassword({
        currentPassword: pwdForm.currentPassword,
        newPassword: pwdForm.newPassword,
      })
      setPwdSuccess('Đổi mật khẩu thành công.')
      setPwdForm({ currentPassword: '', newPassword: '', confirmPassword: '' })
    } catch (err) {
      setPwdError(err?.message || 'Đổi mật khẩu thất bại. Sai mật khẩu hiện tại?')
    }
  }

  const loadAddresses = async () => {
    try {
      const data = await userService.getAddresses()
      setAddresses(data || [])
    } catch(err) {
      console.error(err)
    }
  }

  const handleSaveAddress = async (addressData, id) => {
    if (id) {
      await userService.updateAddress(id, addressData)
    } else {
      await userService.addAddress(addressData)
    }
    await loadAddresses()
  }

  const handleDeleteAddress = async (id) => {
    if (window.confirm('Bạn có chắc muốn xóa địa chỉ này?')) {
      try {
        await userService.deleteAddress(id)
        await loadAddresses()
      } catch (err) {
        alert('Tạm thời không thể xóa địa chỉ lúc này.')
      }
    }
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-[#FAFAFA] flex items-center justify-center">
        <p className="text-[13px] text-[#666]">Đang tải hồ sơ...</p>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-[#FAFAFA] py-8 font-[Montserrat]">
      <div className="max-w-screen-lg mx-auto px-4 sm:px-6">
        
        <div className="flex items-start justify-between gap-4 flex-wrap mb-6">
          <div>
            <h1 className="text-[24px] font-semibold text-[#202020]">Thông tin tài khoản</h1>
            <p className="text-[13px] text-[#666] mt-1">Quản lý hồ sơ cá nhân và bảo mật tài khoản của bạn.</p>
          </div>
          <Link to="/orders" className="text-[12px] uppercase tracking-[0.08em] border border-[#D9D9D9] px-4 h-10 inline-flex items-center hover:border-[#202020] transition-colors">
            Lịch sử đơn hàng
          </Link>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {/* LEO TRÁI - Avatar / Summary */}
          <div className="md:col-span-1">
            <div className="bg-white border border-[#E8E8E8] p-6 flex flex-col items-center text-center">
              <div className="w-24 h-24 bg-[#EFEFEF] rounded-full flex items-center justify-center mb-4 overflow-hidden border border-[#D9D9D9]">
                {profile?.avatarUrl ? (
                  <img src={profile.avatarUrl} alt="Avatar" className="w-full h-full object-cover" />
                ) : (
                  <User size={40} className="text-[#999]" />
                )}
              </div>
              <h2 className="text-[18px] font-semibold text-[#202020]">
                {profile?.lastName} {profile?.firstName}
              </h2>
              <p className="text-[13px] text-[#666] mt-1 break-all">{profile?.email}</p>
              
              <div className="mt-4 pt-4 border-t border-[#F0F0F0] w-full flex justify-center">
                 <span className="inline-flex items-center gap-1 text-[11px] font-medium px-2 py-1 bg-[#F3F8F2] text-[#0f766e] rounded-sm">
                    <Shield size={12} />
                    {profile?.role === 'ADMIN' ? 'Quản trị viên' : 'Thành viên'}
                 </span>
              </div>
            </div>
          </div>

          {/* LÊN PHẢI - CHI TIẾT */}
          <div className="md:col-span-2 space-y-6">
            
            {/* THÔNG TIN CÁ NHÂN */}
            <div className="bg-white border border-[#E8E8E8]">
              <div className="px-6 py-4 border-b border-[#E8E8E8] flex justify-between items-center">
                <h3 className="text-[15px] font-semibold text-[#202020]">Hồ sơ cá nhân</h3>
                {!isEditing && (
                  <button 
                    onClick={() => setIsEditing(true)}
                    className="text-[12px] text-[#5A6D57] hover:text-[#748C70] flex items-center gap-1 font-medium transition-colors"
                  >
                    <Edit2 size={14} /> Chỉnh sửa
                  </button>
                )}
              </div>

              <div className="p-6">
                {editSuccess && <p className="text-[13px] text-[#0f766e] mb-4 bg-[#F3F8F2] p-3 border border-[#0f766e]/20">{editSuccess}</p>}
                {editError && <p className="text-[13px] text-red-600 mb-4 bg-red-50 p-3 border border-red-200">{editError}</p>}

                {!isEditing ? (
                  <div className="space-y-4">
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                      <div>
                        <p className="text-[12px] text-[#888] mb-1">Họ</p>
                        <p className="text-[14px] text-[#202020] font-medium">{profile?.lastName || '-'}</p>
                      </div>
                      <div>
                        <p className="text-[12px] text-[#888] mb-1">Tên</p>
                        <p className="text-[14px] text-[#202020] font-medium">{profile?.firstName || '-'}</p>
                      </div>
                    </div>
                    <div>
                      <p className="text-[12px] text-[#888] mb-1 flex items-center gap-1"><Mail size={12} /> Email (Không thể thay đổi)</p>
                      <p className="text-[14px] text-[#202020] font-medium">{profile?.email || '-'}</p>
                    </div>
                    <div>
                      <p className="text-[12px] text-[#888] mb-1 flex items-center gap-1"><Phone size={12} /> Số điện thoại</p>
                      <p className="text-[14px] text-[#202020] font-medium">{profile?.phoneNumber || 'Chưa cập nhật'}</p>
                    </div>
                  </div>
                ) : (
                  <form onSubmit={handleEditSubmit} className="space-y-4">
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                      <div>
                        <label className="text-[12px] text-[#666] mb-1 block">Họ *</label>
                        <input 
                          type="text" 
                          name="lastName"
                          value={editForm.lastName} 
                          onChange={handleEditChange}
                          className="w-full border border-[#D9D9D9] px-3 py-2 text-[14px] focus:outline-none focus:border-[#5A6D57]"
                        />
                      </div>
                      <div>
                        <label className="text-[12px] text-[#666] mb-1 block">Tên *</label>
                        <input 
                          type="text" 
                          name="firstName"
                          value={editForm.firstName} 
                          onChange={handleEditChange}
                          className="w-full border border-[#D9D9D9] px-3 py-2 text-[14px] focus:outline-none focus:border-[#5A6D57]"
                        />
                      </div>
                    </div>
                    <div>
                      <label className="text-[12px] text-[#666] mb-1 block">Email</label>
                      <input 
                        type="email" 
                        value={profile?.email || ''} 
                        disabled
                        className="w-full border border-[#EFEFEF] bg-[#FAFAFA] px-3 py-2 text-[14px] text-[#888]"
                      />
                    </div>
                    <div>
                      <label className="text-[12px] text-[#666] mb-1 block">Số điện thoại</label>
                      <input 
                        type="text" 
                        name="phoneNumber"
                        value={editForm.phoneNumber} 
                        onChange={handleEditChange}
                        className="w-full border border-[#D9D9D9] px-3 py-2 text-[14px] focus:outline-none focus:border-[#5A6D57]"
                      />
                    </div>

                    <div className="pt-4 flex items-center gap-3">
                      <button 
                        type="submit"
                        className="h-9 px-5 bg-[#5A6D57] text-white text-[13px] font-medium flex items-center gap-1 hover:bg-[#748C70] transition-colors"
                      >
                        <Check size={14} /> Lưu thay đổi
                      </button>
                      <button 
                        type="button"
                        onClick={() => {
                          setIsEditing(false)
                          setEditError('')
                          setEditForm({
                            firstName: profile.firstName || '',
                            lastName: profile.lastName || '',
                            phoneNumber: profile.phoneNumber || '',
                          })
                        }}
                        className="h-9 px-5 border border-[#D9D9D9] text-[#404040] text-[13px] font-medium flex items-center gap-1 hover:bg-[#F5F5F5] transition-colors"
                      >
                        <X size={14} /> Hủy
                      </button>
                    </div>
                  </form>
                )}
              </div>
            </div>

            {/* SỔ ĐỊA CHỈ */}
            <div className="bg-white border border-[#E8E8E8]">
              <div className="px-6 py-4 border-b border-[#E8E8E8] flex justify-between items-center">
                <div className="flex items-center gap-2">
                  <MapPin size={16} className="text-[#202020]" />
                  <h3 className="text-[15px] font-semibold text-[#202020]">Sổ địa chỉ</h3>
                </div>
                <button 
                  onClick={() => {
                    setSelectedAddress(null)
                    setIsAddressModalOpen(true)
                  }}
                  className="text-[12px] text-[#5A6D57] hover:text-[#748C70] flex items-center gap-1 font-medium transition-colors"
                >
                  <Plus size={14} /> Thêm địa chỉ mới
                </button>
              </div>

              <div className="p-6">
                {addresses.length === 0 ? (
                  <div className="text-center py-6 text-gray-500 text-[13px]">
                    Bạn chưa có địa chỉ giao hàng nào.
                  </div>
                ) : (
                  <div className="space-y-4">
                    {addresses.map((addr) => (
                      <div key={addr.id} className="border border-[#EFEFEF] rounded p-4 flex justify-between items-start">
                        <div>
                          <div className="flex items-center gap-2 mb-1">
                            <span className="text-[14px] font-semibold text-[#202020]">{addr.fullName}</span>
                            {addr.isDefault && (
                              <span className="text-[10px] bg-[#5A6D57] text-white px-2 py-0.5 tracking-wide rounded-sm uppercase">
                                Mặc định
                              </span>
                            )}
                          </div>
                          <p className="text-[13px] text-[#666] mb-1">SĐT: {addr.phoneNumber}</p>
                          <p className="text-[13px] text-[#666] leading-relaxed">
                            {addr.street}<br/>
                            {addr.ward}, {addr.city}
                          </p>
                        </div>
                        <div className="flex flex-col gap-2 items-end">
                          <button 
                            onClick={() => {
                              setSelectedAddress(addr)
                              setIsAddressModalOpen(true)
                            }}
                            className="text-[12px] font-medium text-[#5A6D57] hover:underline"
                          >
                            Chỉnh sửa
                          </button>
                          {!addr.isDefault && (
                            <button 
                              onClick={() => handleDeleteAddress(addr.id)}
                              className="text-[12px] font-medium text-red-500 hover:underline flex items-center gap-1"
                            >
                              Xóa
                            </button>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>

            {/* ĐỔI MẬT KHẨU */}
            <div className="bg-white border border-[#E8E8E8]">
              <div className="px-6 py-4 border-b border-[#E8E8E8] flex items-center gap-2">
                <Lock size={16} className="text-[#202020]" />
                <h3 className="text-[15px] font-semibold text-[#202020]">Đổi mật khẩu</h3>
              </div>
              <div className="p-6">
                {pwdSuccess && <p className="text-[13px] text-[#0f766e] mb-4 bg-[#F3F8F2] p-3 border border-[#0f766e]/20">{pwdSuccess}</p>}
                {pwdError && <p className="text-[13px] text-red-600 mb-4 bg-red-50 p-3 border border-red-200">{pwdError}</p>}

                <form onSubmit={handlePwdSubmit} className="space-y-4 max-w-md">
                   <div>
                      <label className="text-[12px] text-[#666] mb-1 block">Mật khẩu hiện tại</label>
                      <input 
                        type="password" 
                        name="currentPassword"
                        value={pwdForm.currentPassword}
                        onChange={handlePwdChange}
                        className="w-full border border-[#D9D9D9] px-3 py-2 text-[14px] focus:outline-none focus:border-[#5A6D57]"
                        placeholder="••••••••"
                      />
                    </div>
                    <div>
                      <label className="text-[12px] text-[#666] mb-1 block">Mật khẩu mới</label>
                      <input 
                        type="password" 
                        name="newPassword"
                        value={pwdForm.newPassword}
                        onChange={handlePwdChange}
                        className="w-full border border-[#D9D9D9] px-3 py-2 text-[14px] focus:outline-none focus:border-[#5A6D57]"
                        placeholder="Tối thiểu 8 ký tự"
                      />
                    </div>
                    <div>
                      <label className="text-[12px] text-[#666] mb-1 block">Xác nhận mật khẩu mới</label>
                      <input 
                        type="password" 
                        name="confirmPassword"
                        value={pwdForm.confirmPassword}
                        onChange={handlePwdChange}
                        className="w-full border border-[#D9D9D9] px-3 py-2 text-[14px] focus:outline-none focus:border-[#5A6D57]"
                        placeholder="Nhập lại mật khẩu mới"
                      />
                    </div>
                    
                    <div className="pt-2">
                      <button 
                        type="submit"
                        className="h-9 px-5 border border-[#202020] text-[#202020] text-[13px] font-medium hover:bg-[#202020] hover:text-white transition-colors"
                      >
                        Cập nhật mật khẩu
                      </button>
                    </div>
                </form>
              </div>
            </div>

          </div>
        </div>
      </div>

      <AddressModal
        isOpen={isAddressModalOpen}
        onClose={() => setIsAddressModalOpen(false)}
        initialData={selectedAddress}
        onSave={handleSaveAddress}
      />
    </div>
  )
}