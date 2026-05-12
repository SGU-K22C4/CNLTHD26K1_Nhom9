import { useState, useEffect } from 'react';
import { Mail, Phone, User, Edit2, Check, X } from 'lucide-react';

export default function ProfileInfoForm({ profile, onUpdateProfile }) {
  const [isEditing, setIsEditing] = useState(false);
  const [editForm, setEditForm] = useState({
    firstName: '',
    lastName: '',
    phoneNumber: '',
    gender: '',
  });
  const [editError, setEditError] = useState('');
  const [editSuccess, setEditSuccess] = useState('');

  useEffect(() => {
    if (profile && !isEditing) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setEditForm({
        firstName: profile.firstName || '',
        lastName: profile.lastName || '',
        phoneNumber: profile.phoneNumber || '',
        gender: profile.gender !== null && profile.gender !== undefined ? profile.gender.toString() : '',
      });
    }
  }, [profile, isEditing]);

  const handleEditChange = (e) => {
    setEditForm({ ...editForm, [e.target.name]: e.target.value });
  };

  const handleEditSubmit = async (e) => {
    e.preventDefault();
    setEditError('');
    setEditSuccess('');
    if (!editForm.firstName.trim() || !editForm.lastName.trim()) {
      setEditError('Họ và Tên không được để trống.');
      return;
    }

    try {
      await onUpdateProfile({
        firstName: editForm.firstName,
        lastName: editForm.lastName,
        phoneNumber: editForm.phoneNumber,
        gender: editForm.gender !== '' ? parseInt(editForm.gender, 10) : null,
      });
      setIsEditing(false);
      setEditSuccess('Cập nhật hồ sơ thành công.');
    } catch (err) {
      setEditError(err?.message || 'Cập nhật thất bại.');
    }
  };

  return (
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
              <p className="text-[12px] text-[#888] mb-1 flex items-center gap-1"><User size={12} /> Giới tính</p>
              <p className="text-[14px] text-[#202020] font-medium">
                {profile?.gender === 0 ? 'Nam' : profile?.gender === 1 ? 'Nữ' : 'Chưa cập nhật'}
              </p>
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
              <label className="text-[12px] text-[#666] mb-1 block">Giới tính</label>
              <div className="flex items-center gap-4 h-[38px]">
                <label className="flex items-center gap-2 cursor-pointer">
                  <input 
                    type="radio" 
                    name="gender" 
                    value="0" 
                    checked={editForm.gender === '0'} 
                    onChange={handleEditChange} 
                    className="accent-[#5A6D57]" 
                  />
                  <span className="text-[14px] text-[#202020]">Nam</span>
                </label>
                <label className="flex items-center gap-2 cursor-pointer">
                  <input 
                    type="radio" 
                    name="gender" 
                    value="1" 
                    checked={editForm.gender === '1'} 
                    onChange={handleEditChange} 
                    className="accent-[#5A6D57]" 
                  />
                  <span className="text-[14px] text-[#202020]">Nữ</span>
                </label>
              </div>
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
                  setIsEditing(false);
                  setEditError('');
                  // Reset form will be handled by useEffect when isEditing becomes false
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
  );
}
