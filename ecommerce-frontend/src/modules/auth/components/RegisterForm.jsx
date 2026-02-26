import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link } from 'react-router-dom';
import { Eye, EyeOff } from 'lucide-react';
import Input from '../../../shared/components/ui/Input';
import VerifyEmailModal from './VerifyEmailModal';

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
    formState: { errors, isSubmitting },
  } = useForm();

  const onSubmit = async (data) => {
    try {
      // TODO: wire up authService.register(data)
      console.log('Register payload:', data);
      // Show verify-email modal with the submitted email
      setVerifyEmail(data.email);
    } catch (err) {
      console.error(err);
    }
  };

  return (
    <>
      <VerifyEmailModal
        isOpen={!!verifyEmail}
        email={verifyEmail}
        onClose={() => setVerifyEmail('')}
        onResend={() => console.log('Resend email to', verifyEmail)}
      />

    <div className="w-full max-w-[460px] mx-auto flex flex-col gap-6 px-4 py-10 sm:px-0">
      {/* Title */}
      <h2 className="text-[26px] font-semibold text-center text-gray-900 tracking-wide">
        Create Account
      </h2>

      {/* Form */}
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-4">
        {/* First Name */}
        <Input
          type="text"
          placeholder="First Name"
          error={errors.firstName?.message}
          {...register('firstName', { required: 'First name is required' })}
        />

        {/* Last Name */}
        <Input
          type="text"
          placeholder="Last Name"
          error={errors.lastName?.message}
          {...register('lastName', { required: 'Last name is required' })}
        />

        {/* Email */}
        <Input
          type="email"
          placeholder="Email"
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
            minLength: { value: 8, message: 'Password must be at least 8 characters' },
          })}
        />

        {/* Submit */}
        <button
          type="submit"
          disabled={isSubmitting}
          className="w-full py-3 bg-[#5A6D57] hover:bg-[#4a5c48] active:bg-[#3d4e3b] text-white text-sm font-semibold tracking-widest uppercase rounded transition-colors disabled:opacity-60 disabled:cursor-not-allowed mt-2"
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