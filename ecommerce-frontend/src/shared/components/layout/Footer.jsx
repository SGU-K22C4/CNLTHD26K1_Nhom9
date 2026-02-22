import { ArrowRight, CircleUserRound, Facebook, Instagram, Twitter, Youtube } from 'lucide-react'
import { Link } from 'react-router-dom'

const aboutLinks = [
  { label: 'Collection', href: '/collection' },
  { label: 'Sustainability', href: '/sustainability' },
  { label: 'Privacy Policy', href: '/privacy' },
  { label: 'Support System', href: '/support' },
  { label: 'Terms & Condition', href: '/terms' },
  { label: 'Copyright Notice', href: '/copyright' },
]

const supportLinks = [
  { label: 'Orders & Shipping', href: '/orders' },
  { label: 'Returns & Refunds', href: '/returns' },
  { label: 'FAQs', href: '/faqs' },
  { label: 'Contact Us', href: '/contact' },
]

const joinLinks = [
  { label: 'Modimal Club', href: '/club' },
  { label: 'Careers', href: '/careers' },
  { label: 'Visit Us', href: '/visit' },
]

const socialLinks = [
  { icon: Instagram, href: 'https://instagram.com', label: 'Instagram' },
  { icon: Facebook, href: 'https://facebook.com', label: 'Facebook' },
  { icon: Youtube, href: 'https://youtube.com', label: 'YouTube' },
  { icon: Twitter, href: 'https://twitter.com', label: 'Twitter' },
]

export default function Footer() {
  return (
    <footer className="bg-[#2C2C2C] text-white">
      <div className="mx-auto w-full max-w-[1440px] px-6 py-16 lg:px-12 lg:py-20">
        
        <div className="grid grid-cols-1 gap-12 lg:grid-cols-12 lg:gap-16">
          
          {/* Newsletter Section - Takes more space */}
          <section className="lg:col-span-5">
            <h3 className="mb-6 text-l font-bold leading-tight lg:text-xl">
              Join Our Club, Get 15% Off For Your Birthday
            </h3>

            <form className="mb-8">
              <div className="flex items-center border-b border-white/30 pb-2.5 transition-colors focus-within:border-white">
                <input 
                  type="email" 
                  placeholder="Enter Your Email Address" 
                  className="flex-1 bg-transparent text-[15px] text-white outline-none placeholder:text-gray-400" 
                  required
                />
                <button 
                  type="submit" 
                  aria-label="Subscribe" 
                  className="ml-3 text-white hover:scale-110 transition-transform"
                >
                  <ArrowRight size={20} />
                </button>
              </div>
            </form>

            {/* Social Links */}
            <div className="mb-12 flex items-center gap-5">
              {socialLinks.map((social) => (
                <a 
                  key={social.label}
                  href={social.href} 
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-gray-300 hover:text-white transition-colors hover:scale-110 transform"
                  aria-label={social.label}
                >
                  <social.icon size={24} />
                </a>
              ))}
            </div>

            {/* Copyright */}
            <p className="text-sm text-gray-400">
              © 2026 SGU-Clothes. All Rights Reserved.
            </p>
          </section>

          {/* About Section */}
          <section className="lg:col-span-2 text-justify-center">
            <h4 className="mb-6 text-lg font-semibold uppercase tracking-wider text-gray-300">
              About Our Shop
            </h4>
            <ul className="space-y-3">
              {aboutLinks.map((link) => (
                <li key={link.label}>
                  <Link 
                    to={link.href} 
                    className="text-[15px] text-gray-300 hover:text-white transition-colors inline-block"
                  >
                    {link.label}
                  </Link>
                </li>
              ))}
            </ul>
          </section>

          {/* Support Section */}
          <section className="lg:col-span-2 text-justify-center ">
            <h4 className="mb-6 text-lg font-semibold uppercase tracking-wider text-gray-300">
              Help & Support
            </h4>
            <ul className="space-y-3">
              {supportLinks.map((link) => (
                <li key={link.label}>
                  <Link 
                    to={link.href} 
                    className="text-[15px] text-gray-300 hover:text-white transition-colors inline-block"
                  >
                    {link.label}
                  </Link>
                </li>
              ))}
            </ul>
          </section>

          {/* Join Up Section */}
          <section className="lg:col-span-2 text-justify-center">
            <h4 className="mb-6 text-lg font-semibold uppercase tracking-wider text-gray-300">
              Join Up
            </h4>
            <ul className="space-y-3 mb-12">
              {joinLinks.map((link) => (
                <li key={link.label}>
                  <Link 
                    to={link.href} 
                    className="text-[15px] text-gray-300 hover:text-white transition-colors inline-block"
                  >
                    {link.label}
                  </Link>
                </li>
              ))}
            </ul>

            {/* User Profile Button */}
            <Link
              to="/profile"
              className="inline-flex ml-12 h-12 w-12 items-center justify-center border border-white/20 bg-brand-olive/80 text-white hover:bg-brand-olive transition-colors rounded-sm"
              aria-label="User profile"
            >
              <CircleUserRound size={22} />
            </Link>
          </section>
        </div>
      </div>
    </footer>
  )
}