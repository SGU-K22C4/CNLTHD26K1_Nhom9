import Modal from '../../../shared/components/ui/Modal';

/**
 * VerifyEmailModal
 *
 * Shown right after the user submits the registration form.
 * Props:
 *  - isOpen   {boolean}
 *  - email    {string}   the address the verification was sent to
 *  - onClose  {function}
 *  - onResend {function} called when "Click Here" is pressed
 */
export default function VerifyEmailModal({ isOpen, email, onClose, onResend }) {
  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      className="rounded-none sm:rounded-none"
    >
      {/* Inner content — generous padding matching Figma spacing */}
      <div className="flex flex-col items-center gap-6 px-8 py-12 sm:px-14 sm:py-14 text-center">
        {/* Title */}
        <h2 className="text-xl sm:text-2xl font-semibold text-gray-900 leading-snug">
          Verify Your Email Address
        </h2>

        {/* Body copy */}
        <p className="text-sm sm:text-[15px] text-gray-700 leading-relaxed max-w-sm">
          We&apos;ve Sent An Email To{' '}
          <span className="font-medium">{email}</span> To Verify Your
          Email Address And Activate Your Account. The Link In The Email
          Will Expire In 24 Hours.
        </p>

        {/* Resend / change email */}
        <p className="text-sm sm:text-[15px] text-gray-700 leading-relaxed max-w-sm">
          <button
            type="button"
            onClick={onResend}
            className="text-[#5A6D57] hover:underline font-medium focus:outline-none"
          >
            Click Here
          </button>{' '}
          If You Did Not Receive An Email Or Would Like To Change The
          Email Address You Registered With
        </p>
      </div>
    </Modal>
  );
}