import { useNavigate } from 'react-router-dom';
import Layout from '../../../shared/components/layout/Layout';
import RegisterForm from '../components/RegisterForm';

export default function RegisterPage() {
  const navigate = useNavigate();

  return (
    <Layout>
      {/* ── Two-column layout ──────────────────────────────────── */}
      <div className="flex min-h-[calc(100vh-120px)]">

        {/* Left — hero image (hidden on mobile) */}
        <div className="hidden md:block md:w-[42%] lg:w-[45%] xl:w-[48%] flex-shrink-0">
          <img
            src="/assets/images/hero-desktop.webp"
            alt="Modimal fashion"
            className="h-full w-full object-cover object-top"
          />
        </div>

        {/* Right — form panel */}
        <div className="flex flex-1 flex-col items-center justify-center bg-white px-4 py-12 sm:px-8 md:px-12">
          <RegisterForm onSuccess={() => navigate('/login')} />
        </div>

      </div>
    </Layout>
  );
}