import { useEffect } from 'react';
import { createPortal } from 'react-dom';
import { X } from 'lucide-react';
import clsx from 'clsx';

/**
 * Reusable Modal overlay.
 *
 * Props:
 *  - isOpen   {boolean}   controls visibility
 *  - onClose  {function}  called when backdrop / X is clicked
 *  - children {ReactNode}
 *  - className {string}   extra classes for the inner panel
 *  - showClose {boolean}  default true
 */
export default function Modal({
  isOpen,
  onClose,
  children,
  className,
  showClose = true,
}) {
  // Lock body scroll while open
  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = 'hidden';
    } else {
      document.body.style.overflow = '';
    }
    return () => { document.body.style.overflow = ''; };
  }, [isOpen]);

  if (!isOpen) return null;

  return createPortal(
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4"
      role="dialog"
      aria-modal="true"
    >
      {/* Backdrop */}
      <div
        className="absolute inset-0 bg-black/40 backdrop-blur-sm"
        onClick={onClose}
        aria-hidden="true"
      />

      {/* Panel */}
      <div
        className={clsx(
          'relative w-full max-w-lg bg-white shadow-xl',
          className
        )}
      >
        {showClose && (
          <button
            type="button"
            onClick={onClose}
            aria-label="Close"
            className="absolute top-4 left-4 text-gray-500 hover:text-gray-800 transition-colors"
          >
            <X size={20} />
          </button>
        )}
        {children}
      </div>
    </div>,
    document.body
  );
}