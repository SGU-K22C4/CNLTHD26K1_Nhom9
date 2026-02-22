import { Heart, Menu, Search, ShoppingBag, User, X } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router-dom'

const navItems = [
  { key: 'collection', label: 'Collection', href: '/collection' },
  { key: 'new-in', label: 'New In', href: '/new-in' },
  { key: 'modiweek', label: 'Modiweek', href: '/modiweek' },
  { key: 'plus-size', label: 'Plus Size', href: '/plus-size' },
  { key: 'sustainability', label: 'Sustainability', href: '/sustainability' },
]

export default function Header() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false)

  return (
    <header className="sticky top-0 z-50 w-full bg-white shadow-sm">
      {/* Top Banner */}
      <div className="flex h-8 items-center justify-center bg-brand-olive px-4 text-center text-[13px] font-medium tracking-wide text-white">
        Enjoy Free Shipping On All Orders
      </div>

      {/* Main Header */}
      <div className="bg-white">
        <div className="mx-auto flex h-20 w-full max-w-[1440px] items-center justify-between gap-8 px-6 lg:px-12">
          
          {/* Mobile: Menu + Search (Left) */}
          <div className="flex items-center gap-3 lg:hidden">
            <button 
              className="p-2 hover:bg-gray-100 rounded-lg transition-colors" 
              aria-label="Open menu" 
              type="button"
              onClick={() => setMobileMenuOpen(true)}
            >
              <Menu size={24} className="text-gray-700" />
            </button>
            <button 
              className="p-2 hover:bg-gray-100 rounded-lg transition-colors" 
              aria-label="Search" 
              type="button"
            >
              <Search size={22} className="text-gray-700" />
            </button>
          </div>

          {/* Logo - Desktop (Left) */}
          <Link to="/" className="hidden shrink-0 lg:block" aria-label="SGU Home">
            <img 
              src="/assets/icons/logoSGU.jpg" 
              alt="SGU Clothes" 
              className="h-12 w-auto object-contain" 
            />
          </Link>

          {/* Logo - Mobile (Center) */}
          <Link
            to="/"
            className="absolute left-1/2 -translate-x-1/2 lg:hidden"
            aria-label="SGU Home"
          >
            <img 
              src="/assets/icons/logoSGU.jpg" 
              alt="SGU Clothes" 
              className="h-10 w-auto object-contain" 
            />
          </Link>

          {/* Desktop Navigation (Center) */}
          <nav className="hidden lg:flex items-center gap-8 flex-1 justify-center">
            {navItems.map((item) => (
              <Link
                key={item.key}
                to={item.href}
                className="relative text-[15px] font-medium text-gray-700 hover:text-gray-900 transition-colors py-2 group"
              >
                {item.label}
                <span className="absolute bottom-0 left-0 w-0 h-0.5 bg-gray-900 transition-all group-hover:w-full" />
              </Link>
            ))}
          </nav>

          {/* Action Icons (Right) */}
          <div className="flex items-center gap-2">
            {/* Desktop Only Icons */}
            <button 
              className="hidden lg:flex p-2 hover:bg-gray-100 rounded-lg transition-colors" 
              aria-label="Search" 
              type="button"
            >
              <Search size={22} className="text-gray-700" />
            </button>
            <button 
              className="hidden lg:flex p-2 hover:bg-gray-100 rounded-lg transition-colors" 
              aria-label="Profile" 
              type="button"
            >
              <User size={22} className="text-gray-700" />
            </button>
            
            {/* Always Visible Icons */}
            <button 
              className="p-2 hover:bg-gray-100 rounded-lg transition-colors" 
              aria-label="Favorites" 
              type="button"
            >
              <Heart size={22} className="text-gray-700" />
            </button>
            <button 
              className="relative p-2 hover:bg-gray-100 rounded-lg transition-colors" 
              aria-label="Cart" 
              type="button"
            >
              <ShoppingBag size={22} className="text-gray-700" />
              {/* Cart Badge */}
              <span className="absolute top-1 right-1 flex h-4 w-4 items-center justify-center rounded-full bg-brand-olive text-[10px] font-bold text-white">
                0
              </span>
            </button>
          </div>
        </div>
      </div>

      {/* Mobile Menu Overlay */}
      {mobileMenuOpen && (
        <div 
          className="fixed inset-0 z-50 bg-black/50 lg:hidden"
          onClick={() => setMobileMenuOpen(false)}
        >
          <div 
            className="absolute left-0 top-0 h-full w-4/5 max-w-sm bg-white shadow-xl"
            onClick={(e) => e.stopPropagation()}
          >
            {/* Mobile Menu Header */}
            <div className="flex items-center justify-between border-b border-gray-200 p-6">
              <img 
                src="/assets/icons/logoSGU.jpg" 
                alt="SGU Clothes" 
                className="h-10 w-auto object-contain" 
              />
              <button
                onClick={() => setMobileMenuOpen(false)}
                className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
                aria-label="Close menu"
              >
                <X size={24} className="text-gray-700" />
              </button>
            </div>

            {/* Mobile Menu Items */}
            <nav className="flex flex-col p-6 space-y-1">
              {navItems.map((item) => (
                <Link
                  key={item.key}
                  to={item.href}
                  onClick={() => setMobileMenuOpen(false)}
                  className="text-base font-medium text-gray-700 hover:text-gray-900 hover:bg-gray-50 px-4 py-3 rounded-lg transition-colors"
                >
                  {item.label}
                </Link>
              ))}
            </nav>

            {/* Mobile Menu Footer */}
            <div className="absolute bottom-0 left-0 right-0 border-t border-gray-200 p-6">
              <Link
                to="/login"
                className="btn btn-primary w-full justify-center"
                onClick={() => setMobileMenuOpen(false)}
              >
                Sign In
              </Link>
            </div>
          </div>
        </div>
      )}
    </header>
  )
}