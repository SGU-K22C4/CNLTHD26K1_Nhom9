import { useState } from 'react';
import { Lock } from 'lucide-react';
import { userService } from '../services/userService';

export default function ChangePasswordForm() {
  const [pwdForm, setPwdForm] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  });
  const [pwdError, setPwdError] = useState('');
  const [pwdSuccess, setPwdSuccess] = useState('');

  const handlePwdChange = (e) => {
    setPwdForm({ ...pwdForm, [e.target.name]: e.target.value });
  };

  const handlePwdSubmit = async (e) => {
    e.preventDefault();
    setPwdError('');
    setPwdSuccess('');

    if (!pwdForm.currentPassword || !pwdForm.newPassword || !pwdForm.confirmPassword) {
      setPwdError('Vui lòng điền đủ thông tin mật khẩu.');
      return;
    }
    if (pwdForm.newPassword !== pwdForm.confirmPassword) {
      setPwdError('Mật khẩu mới không khớp.');
      return;
    }
    if (pwdForm.newPassword.length < 8) {
      setPwdError('Mật khẩu mới phải có ít nhất 8 ký tự.');
      return;
    }

    try {
      await userService.changePassword({
        currentPassword: pwdForm.currentPassword,
        newPassword: pwdForm.newPassword,
      });
      setPwdSuccess('Đổi mật khẩu thành công.');
      setPwdForm({ currentPassword: '', newPassword: '', confirmPassword: '' });
    } catch (err) {
      setPwdError(err?.message || 'Đổi mật khẩu thất bại. Sai mật khẩu hiện tại?');
    }
  };

  return (
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
  );
}
