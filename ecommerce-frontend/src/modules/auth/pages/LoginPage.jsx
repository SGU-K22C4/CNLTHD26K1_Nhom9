import Layout from '../../../shared/components/layout/Layout';
import LoginForm from '../components/LoginForm';

export default function LoginPage() {
  return (
    <Layout>
      {/* Two-column layout — same structure as RegisterPage */}
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
          <LoginForm />
        </div>

      </div>
    </Layout>
  );
}