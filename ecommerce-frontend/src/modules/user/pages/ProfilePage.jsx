import { useEffect, useState, useContext } from 'react'
import { Link } from 'react-router-dom'
import { userService } from '../services/userService'
import AuthContext from '../../auth/context/AuthContext'
import { User, Shield } from 'lucide-react'
import ProfileInfoForm from '../components/ProfileInfoForm'
import AddressManager from '../components/AddressManager'
import ChangePasswordForm from '../components/ChangePasswordForm'

export default function ProfilePage() {
  const { user } = useContext(AuthContext)
  const [loading, setLoading] = useState(true)
  const [profile, setProfile] = useState(null)

  useEffect(() => {
    let mounted = true
    const loadProfile = async () => {
      setLoading(true)
      try {
        const profileData = await userService.getProfile()
        if (mounted) setProfile(profileData)
      } catch (error) {
        console.error('Lỗi khi tải profile', error)
      } finally {
        if (mounted) setLoading(false)
      }
    }
    loadProfile()
    return () => { mounted = false }
  }, [])

  const handleUpdateProfile = async (updateData) => {
    const updatedProfile = await userService.updateProfile({
      ...updateData,
      avatarUrl: profile?.avatarUrl,
    })
    setProfile(updatedProfile)

    // Update global context so header updates
    if (user) {
      const currentUserInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
      const newUserInfo = { ...currentUserInfo, firstName: updatedProfile.firstName, lastName: updatedProfile.lastName }
      localStorage.setItem('userInfo', JSON.stringify(newUserInfo))
      setTimeout(() => window.location.reload(), 1000)
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
            <ProfileInfoForm profile={profile} onUpdateProfile={handleUpdateProfile} />
            <AddressManager />
            <ChangePasswordForm />
          </div>
        </div>
      </div>
    </div>
  )
}