import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link } from 'react-router-dom';
import { Eye, EyeOff } from 'lucide-react';
import Input from '../../../shared/components/ui/Input';
import WelcomeModal from './WelcomeModal';

import SocialLoginGroup from './SocialLoginGroup';

/* ── Component ────────────────────────────────────────────── */
export default function LoginForm() {
  const [showPassword, setShowPassword] = useState(false);
  const [showWelcome, setShowWelcome] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm();

  const onSubmit = async (data) => {
    try {
      // TODO: wire up authService.login(data)
      console.log('Login payload:', data);
      setShowWelcome(true);
    } catch (err) {
      console.error(err);
    }
  };

  return (
    <>
      <WelcomeModal isOpen={showWelcome} onClose={() => setShowWelcome(false)} />

    <div className="w-full max-w-[460px] mx-auto flex flex-col gap-6 px-4 py-10 sm:px-0">
      {/* Title */}
      <h2 className="text-[26px] font-semibold text-center text-gray-900 tracking-wide">
        Log In
      </h2>

      {/* Form */}
      <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-4">
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
          })}
        />

        {/* Remember me & Forgot password */}
        <div className="flex items-center justify-between mt-1">
          <label className="flex items-center gap-2 cursor-pointer">
            <input
              type="checkbox"
              {...register('rememberMe')}
              className="w-4 h-4 rounded border-gray-300 text-[#5A6D57] focus:ring-[#5A6D57]"
            />
            <span className="text-sm text-gray-600 select-none">Remember Me</span>
          </label>
          <Link
            to="/forgot-password"
            className="text-sm text-gray-600 hover:text-gray-900 hover:underline"
          >
            Forgot Your Password?
          </Link>
        </div>

        {/* Submit */}
        <button
          type="submit"
          disabled={isSubmitting}
          className="w-full py-3 bg-[#5A6D57] hover:bg-[#4a5c48] active:bg-[#3d4e3b] text-white text-sm font-semibold tracking-widest uppercase rounded transition-colors disabled:opacity-60 disabled:cursor-not-allowed mt-1"
        >
          {isSubmitting ? 'Logging in…' : 'Log In'}
        </button>
      </form>

      {/* Divider */}
      <div className="flex items-center gap-4">
        <hr className="flex-1 border-gray-200" />
        <span className="text-sm text-gray-400">Or</span>
        <hr className="flex-1 border-gray-200" />
      </div>

      {/* Social Sign In */}
      <SocialLoginGroup actionText="Sign in" />

      {/* Create account */}
      <p className="text-center text-sm text-gray-600">
        New To Modimal?{' '}
        <Link to="/register" className="font-semibold text-gray-900 hover:underline">
          Create An Account
        </Link>
      </p>
    </div>
    </>
  );
}