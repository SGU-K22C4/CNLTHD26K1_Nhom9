import clsx from 'clsx';
import { forwardRef } from 'react';

const Input = forwardRef(({
  label,
  error,
  helperText,
  type = 'text',
  className,
  ...props
}, ref) => {
  return (
    <div className="w-full">
      {label && (
        <label className="block text-sm font-medium text-gray-700 mb-1.5">
          {label}
        </label>
      )}
      
      <input
        ref={ref}
        type={type}
        className={clsx(
          'input',
          error && 'input-error',
          className
        )}
        {...props}
      />
      
      {error && (
        <p className="mt-1.5 text-sm text-red-600">{error}</p>
      )}
      
      {helperText && !error && (
        <p className="mt-1.5 text-sm text-gray-500">{helperText}</p>
      )}
    </div>
  );
});

Input.displayName = 'Input';

export default Input;