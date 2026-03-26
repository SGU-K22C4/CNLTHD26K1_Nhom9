import { useNavigate } from 'react-router-dom';
import Modal from '../../../shared/components/ui/Modal';

/**
 * WelcomeModal
 *
 * Shown right after successful login.
 * Props:
 *  - isOpen  {boolean}
 *  - onClose {function}
 */
export default function WelcomeModal({ isOpen, onClose }) {
  const navigate = useNavigate();

  const handleCTA = () => {
    onClose();
    navigate('/');
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose}>
      <div className="flex flex-col items-center gap-5 px-8 py-12 sm:px-14 sm:py-14 text-center">
        {/* Title */}
        <h2 className="text-2xl sm:text-[28px] font-semibold text-gray-900 leading-snug">
          Welcome To Modimal
        </h2>

        {/* Tagline — italic */}
        <p className="text-sm sm:text-base italic text-gray-700">
          Elegance In Simplicity, Earth's Harmony
        </p>

        {/* Question */}
        <p className="text-sm sm:text-[15px] font-semibold text-gray-900">
          Is It Your First Experience On Modimal?
        </p>

        {/* CTA button */}
        <button
          type="button"
          onClick={handleCTA}
          className="w-full py-3 bg-[#5A6D57] hover:bg-[#4a5c48] active:bg-[#3d4e3b] text-white text-sm font-semibold tracking-widest uppercase rounded transition-colors"
        >
          Create Your Own Style
        </button>
      </div>
    </Modal>
  );
}