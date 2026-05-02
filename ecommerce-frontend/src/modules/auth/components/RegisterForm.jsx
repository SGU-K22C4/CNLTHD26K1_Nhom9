import { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { Link } from 'react-router-dom';
import { Eye, EyeOff } from 'lucide-react';
import Input from '../../../shared/components/ui/Input';
import AddressFields from '../../../shared/components/ui/AddressFields';
import VerifyEmailModal from './VerifyEmailModal';
import { useProvinces } from '../../../shared/hooks/useProvinces';
import { authService } from '../services/authService';

/* ── Social icon SVGs ─────────────────────────────────────── */
const AppleIcon = () => (
  <svg viewBox="0 0 24 24" className="w-5 h-5 fill-white" xmlns="http://www.w3.org/2000/svg">
    <path d="M18.71 19.5c-.83 1.24-1.71 2.45-3.05 2.47-1.34.03-1.77-.79-3.29-.79-1.53 0-2 .77-3.27.82-1.31.05-2.3-1.32-3.14-2.53C4.25 17 2.94 12.45 4.7 9.39c.87-1.52 2.43-2.48 4.12-2.51 1.28-.02 2.5.87 3.29.87.78 0 2.26-1.07 3.8-.91.65.03 2.47.26 3.64 1.98-.09.06-2.17 1.28-2.15 3.81.03 3.02 2.65 4.03 2.68 4.04-.03.07-.42 1.44-1.38 2.83M13 3.5c.73-.83 1.94-1.46 2.94-1.5.13 1.17-.34 2.35-1.04 3.19-.69.85-1.83 1.51-2.95 1.42-.15-1.15.41-2.35 1.05-3.11z" />
  </svg>
);

const GoogleIcon = () => (
  <svg viewBox="0 0 24 24" className="w-5 h-5" xmlns="http://www.w3.org/2000/svg">
    <path fill="#EA4335" d="M5.266 9.765A7.077 7.077 0 0 1 12 4.909c1.69 0 3.218.6 4.418 1.582L19.91 3C17.782 1.145 15.055 0 12 0 7.27 0 3.198 2.698 1.24 6.65l4.026 3.115z" />
    <path fill="#34A853" d="M16.04 18.013c-1.09.703-2.474 1.078-4.04 1.078a7.077 7.077 0 0 1-6.723-4.823l-4.04 3.067A11.965 11.965 0 0 0 12 24c2.933 0 5.735-1.043 7.834-3l-3.793-2.987z" />
    <path fill="#4A90E2" d="M19.834 21c2.195-2.048 3.62-5.096 3.62-9 0-.71-.109-1.473-.272-2.182H12v4.637h6.436c-.317 1.559-1.17 2.766-2.395 3.558L19.834 21z" />
    <path fill="#FBBC05" d="M5.277 14.268A7.12 7.12 0 0 1 4.909 12c0-.782.125-1.533.357-2.235L1.24 6.65A11.934 11.934 0 0 0 0 12c0 1.92.445 3.73 1.237 5.335l4.04-3.067z" />
  </svg>
);

const FacebookIcon = () => (
  <svg viewBox="0 0 24 24" className="w-5 h-5 fill-white" xmlns="http://www.w3.org/2000/svg">
    <path d="M24 12.073c0-6.627-5.373-12-12-12s-12 5.373-12 12c0 5.99 4.388 10.954 10.125 11.854v-8.385H7.078v-3.47h3.047V9.43c0-3.007 1.792-4.669 4.533-4.669 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.874v2.25h3.328l-.532 3.47h-2.796v8.385C19.612 23.027 24 18.062 24 12.073z" />
  </svg>
);

const SocialButton = ({ children, label }) => (
  <button
    type="button"
    aria-label={label}
    className="w-10 h-10 rounded-full flex items-center justify-center bg-[#1A1A1A] hover:bg-black transition-colors shadow-sm"
  >
    {children}
  </button>
);

/* ── Component ────────────────────────────────────────────── */
export default function RegisterForm({ onSuccess }) {
  const [showPassword, setShowPassword] = useState(false);
  const [verifyEmail, setVerifyEmail] = useState('');

  const {
    register,
    handleSubmit,
    watch,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm({
    defaultValues: {
      gender: "0",
      avatar: '/assets/images/avatarnam.png',
      isDefault: false
    }
  });

  const gender = watch('gender');
  const avatarUrl = watch('avatar');

  // Change avatar whenever gender changes
  useEffect(() => {
    setValue('avatar', gender === "0" ? '/assets/images/avatarnam.png' : '/assets/images/avatarnu.png');
  }, [gender, setValue]);

  // Province API Custom Hook with Caching (Performance)
  const { provinces } = useProvinces(2);
  const [wards, setWards] = useState([]);

  const selectedCityCode = watch('cityCode');

  useEffect(() => {
    setValue('wardCode', '');
    if (selectedCityCode) {
      const city = provinces.find(p => p.code == selectedCityCode);
      setWards(city && city.wards ? city.wards : []);
      setValue('city', city ? city.name : '');
    } else {
      setWards([]);
      setValue('city', '');
    }
  }, [selectedCityCode, provinces, setValue]);

  const onSubmit = async (data) => {
    try {
      if (data.wardCode && wards.length > 0) {
        const w = wards.find(w => w.code == data.wardCode);
        if (w) data.ward = w.name;
      }
      // Prepare payload to match backend
      const payload = {
        fullName: data.fullName,
        email: data.email,
        password: data.password,
        phone: data.phone,
        gender: parseInt(data.gender, 10),
        avatar: data.avatar,
        street: data.street,
        city: data.city,
        ward: data.ward,
        isDefault: data.isDefault
      };
      
      await authService.register(payload);
      
      // Mở modal báo cho user check email
      setVerifyEmail(data.email);
    } catch (err) {
      console.error(err);
      alert(err.response?.data?.message || 'Đăng ký thất bại, email có thể đã bị trùng!');
    }
  };

  return (
    <>
      <VerifyEmailModal
        isOpen={!!verifyEmail}
        email={verifyEmail}
        onClose={() => setVerifyEmail('')}
        onResend={async () => {
          try {
            await authService.resendVerification(verifyEmail);
            alert('Email xác thực đã được gửi lại thành công! Vui lòng kiểm tra hộp thư.');
          } catch (err) {
            const msg = err.response?.data?.message || 'Gửi lại email thất bại.';
            alert(msg);
          }
        }}
      />

      <div className="w-full max-w-[500px] mx-auto flex flex-col gap-6 px-4 py-8 sm:px-0">
        {/* Title */}
        <h2 className="text-[26px] font-semibold text-center text-gray-900 tracking-wide">
          Create Account
        </h2>

        {/* Form */}
        <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-4">
          {/* Avatar Preview */}
          <div className="flex flex-col items-center gap-2">
            <img
              src={avatarUrl}
              alt="Avatar Preview"
              className="w-20 h-20 rounded-full object-cover shadow-md bg-gray-50 border"
            />
            <span className="text-[10px] text-gray-500 text-center leading-tight">
              Avatar uniquely chosen for you based on gender
            </span>
          </div>

          {/* Full Name */}
          <Input
            type="text"
            placeholder="Full Name"
            error={errors.fullName?.message}
            {...register('fullName', { required: 'Full name is required' })}
          />

          {/* Missing Phone & Gender Container */}
          <div className="flex flex-col gap-4 sm:flex-row sm:items-start">
            <div className="flex-1">
              <Input
                type="text"
                placeholder="Phone Number"
                error={errors.phone?.message}
                {...register('phone', { 
                  required: 'Phone number is required',
                  pattern: {
                    value: /^(84|0[3|5|7|8|9])+([0-9]{8})$/,
                    message: 'Invalid Vietnamese phone number',
                  }
                })}
              />
            </div>

            <div className="flex items-center gap-4 bg-[#F9F9F9] border border-transparent rounded px-4 h-[46px]">
              <label className="flex items-center gap-2 cursor-pointer">
                <input type="radio" value="0" {...register('gender')} className="accent-[#5A6D57]" />
                <span className="text-sm text-gray-700">Male</span>
              </label>
              <label className="flex items-center gap-2 cursor-pointer">
                <input type="radio" value="1" {...register('gender')} className="accent-[#5A6D57]" />
                <span className="text-sm text-gray-700">Female</span>
              </label>
            </div>
          </div>

          {/* Email */}
          <Input
            type="email"
            placeholder="Email Address"
            error={errors.email?.message}
            {...register('email', {
              required: 'Email is required',
              pattern: {
                value: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
                message: 'Enter a valid email address',
              },
            })}
          />

          {/* Password */}
          <Input
            type={showPassword ? 'text' : 'password'}
            placeholder="Password"
            error={errors.password?.message}
            suffix={
              <button
                type="button"
                tabIndex={-1}
                onClick={() => setShowPassword((v) => !v)}
                className="text-gray-400 hover:text-gray-600 transition-colors"
                aria-label={showPassword ? 'Hide password' : 'Show password'}
              >
                {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            }
            {...register('password', {
              required: 'Password is required',
              pattern: {
                value: /^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=_!]).{8,100}$/,
                message: 'Password must have 1 digit, 1 lower, 1 upper, 1 special char.',
              },
            })}
          />

          {/* ADDRESS SECTION */}
          <div className="text-sm text-gray-700 font-semibold border-t pt-4 mt-2">Delivery Address</div>

          <AddressFields
            streetInputProps={register('street', { required: 'Street address is required' })}
            streetError={errors.street?.message}
            citySelectProps={register('cityCode', { required: 'City is required' })}
            cityError={errors.cityCode?.message}
            wardSelectProps={{
              ...register('wardCode', { required: 'Ward is required' }),
              disabled: !wards.length,
            }}
            wardError={errors.wardCode?.message}
            provinces={provinces}
            wards={wards}
          />

          <label className="flex items-center gap-2 cursor-pointer mt-1">
            <input type="checkbox" {...register('isDefault')} className="w-4 h-4 accent-[#5A6D57] rounded" />
            <span className="text-sm text-gray-600">Set as default address</span>
          </label>

          {/* Submit */}
          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full py-3 bg-[#5A6D57] hover:bg-[#4a5c48] active:bg-[#3d4e3b] text-white text-sm font-semibold tracking-widest uppercase rounded transition-colors disabled:opacity-60 disabled:cursor-not-allowed mt-4"
          >
            {isSubmitting ? 'Registering…' : 'Register Now'}
          </button>
        </form>

        {/* Already have account */}
        <p className="text-center text-sm text-gray-600">
          Already Have An Account?{' '}
          <Link to="/login" className="font-semibold text-gray-900 hover:underline">
            Log In
          </Link>
        </p>

        {/* Divider */}
        <div className="flex items-center gap-4">
          <hr className="flex-1 border-gray-200" />
          <span className="text-sm text-gray-400">Or</span>
          <hr className="flex-1 border-gray-200" />
        </div>

        {/* Social Sign Up */}
        <div className="flex items-center justify-center gap-6">
          <SocialButton label="Sign up with Apple">
            <AppleIcon />
          </SocialButton>
          <SocialButton label="Sign up with Google">
            <GoogleIcon />
          </SocialButton>
          <SocialButton label="Sign up with Facebook">
            <FacebookIcon />
          </SocialButton>
        </div>

        {/* Terms */}
        <p className="text-center text-xs text-gray-500 leading-relaxed px-2">
          By Clicking Register Now, You Agree To{' '}
          <Link to="/terms" className="underline hover:text-gray-700">
            Terms &amp; Conditions
          </Link>{' '}
          And{' '}
          <Link to="/privacy" className="underline hover:text-gray-700">
            Privacy Policy
          </Link>
        </p>
      </div>
    </>
  );
}