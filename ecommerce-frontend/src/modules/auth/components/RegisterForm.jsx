import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link } from 'react-router-dom';
import { Eye, EyeOff } from 'lucide-react';
import Input from '../../../shared/components/ui/Input';
import VerifyEmailModal from './VerifyEmailModal';

import SocialLoginGroup from './SocialLoginGroup';

/* ── Component ────────────────────────────────────────────── */
export default function RegisterForm({ onSuccess }) {
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [verifyEmail, setVerifyEmail] = useState('');

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors, isSubmitting },
  } = useForm();

  const password = watch('password', '');

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
            pattern: {
              value: /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d@$!%*#?&]{8,}$/,
              message: 'Password must contain at least one letter and one number'
            }
          })}
        />

        {/* Confirm Password */}
        <Input
          type={showConfirmPassword ? 'text' : 'password'}
          placeholder="Confirm Password"
          error={errors.confirmPassword?.message}
          suffix={
            <button
              type="button"
              tabIndex={-1}
              onClick={() => setShowConfirmPassword((v) => !v)}
              className="text-gray-400 hover:text-gray-600 transition-colors"
              aria-label={showConfirmPassword ? 'Hide confirm password' : 'Show confirm password'}
            >
              {showConfirmPassword ? <EyeOff size={18} /> : <Eye size={18} />}
            </button>
          }
          {...register('confirmPassword', {
            required: 'Please confirm your password',
            validate: value => value === password || 'Passwords do not match'
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
      <SocialLoginGroup actionText="Sign up" />

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