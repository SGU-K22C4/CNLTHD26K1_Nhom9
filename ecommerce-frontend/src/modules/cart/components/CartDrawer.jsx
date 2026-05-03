import { useEffect, useCallback } from 'react'
import { X } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { useCartContext } from '../hooks/useCartContext'
import CartItem from './CartItem'
import { formatCurrency } from '../../../shared/utils/format'

export default function CartDrawer() {
  const {
    items,
    isDrawerOpen,
    drawerAnchor,
    closeDrawer,
    removeItem,
    updateQuantity,
    subtotal,
  } = useCartContext()

  const navigate = useNavigate()
  const isEmpty = items.length === 0

  /* ── Close on Escape ──────────────────────────────────────────────────── */
  const handleKeyDown = useCallback(
    (e) => { if (e.key === 'Escape') closeDrawer() },
    [closeDrawer],
  )
  useEffect(() => {
    window.addEventListener('keydown', handleKeyDown)
    return () => window.removeEventListener('keydown', handleKeyDown)
  }, [handleKeyDown])

  const handleCheckout = () => {
    closeDrawer()
    navigate('/cart')
  }

  return (
    <>
      {/* Blurred backdrop — starts below the header so the header stays clean */}
      <div
        aria-hidden="true"
        onClick={closeDrawer}
        style={{ top: `${drawerAnchor.top}px` }}
        className={`fixed inset-x-0 bottom-0 bg-black/40 backdrop-blur-sm z-[1099] transition-opacity duration-300 ${
          isDrawerOpen ? 'opacity-100 pointer-events-auto' : 'opacity-0 pointer-events-none'
        }`}
      />

      {/* Panel — drops down directly below the cart icon, right-aligned */}
      <div
        role="dialog"
        aria-modal="true"
        aria-label="Shopping cart"
        style={{
          top: `${drawerAnchor.top}px`,
          right: `${drawerAnchor.right}px`,
        }}
        className={`fixed w-[calc(100vw-2rem)] sm:w-[380px] max-w-[380px] h-[660px] bg-white z-[9999] flex flex-col shadow-2xl overflow-hidden transition-all duration-300 ${
          isDrawerOpen
            ? 'opacity-100 translate-y-0 pointer-events-auto'
            : 'opacity-0 -translate-y-4 pointer-events-none'
        }`}
      >
        {/* Header row — X close button */}
        <div className="flex items-center justify-end px-5 py-4 shrink-0">
          <button
            onClick={closeDrawer}
            aria-label="Close cart"
            className="p-1 text-[#0C0C0C] hover:opacity-60 transition-opacity"
          >
            <X size={20} strokeWidth={1.5} />
          </button>
        </div>

        {/* Scrollable content */}
        <div className="flex-1 overflow-y-auto px-6 [&::-webkit-scrollbar]:hidden [-ms-overflow-style:none] [scrollbar-width:none]">
          {isEmpty ? (
            <EmptyState onClose={closeDrawer} />
          ) : (
            items.map((item) => (
              <CartItem
                key={item.id}
                item={item}
                onRemove={removeItem}
                onUpdateQuantity={updateQuantity}
              />
            ))
          )}
        </div>

        {/* Footer — subtotal + checkout, shown only when cart has items */}
        {!isEmpty && (
          <div className="mt-auto shrink-0 px-6 py-5 border-t border-[#CBCBCB] bg-white">
            <div className="flex items-center justify-between mb-4">
              <span className="font-[Montserrat] text-sm text-[#404040]">Subtotal</span>
              <span className="font-[Montserrat] text-base font-bold text-[#0C0C0C]">
                {formatCurrency(subtotal)}
              </span>
            </div>
            <button
              onClick={handleCheckout}
              className="w-full h-12 bg-[#5A6D57] hover:bg-[#748C70] font-[Montserrat] text-sm text-white tracking-wide transition-colors"
            >
              Check Out
            </button>
          </div>
        )}
      </div>
    </>
  )
}

/* ─── Empty state — matches Figma node 3493-17328 exactly ──────────────────── */
function EmptyState({ onClose }) {
  const navigate = useNavigate()

  const goTo = (path) => {
    onClose()
    navigate(path)
  }

  return (
    <div className="flex flex-1 flex-col items-center justify-center text-center py-10">
      {/* Title — H6: Montserrat Bold 16px */}
      <p className="font-[Montserrat] font-bold text-base leading-[1.4] text-[#0C0C0C] capitalize mb-4">
        Your Shopping Bag Is Empty
      </p>

      {/* Description — bodySM: Montserrat Regular 14px */}
      <p className="font-[Montserrat] text-sm leading-[1.8] text-[#0C0C0C] mb-8">
        Discover Modimal
        <br />
        And Add Products To Your Bag
      </p>

      {/* Navigation buttons */}
      <div className="flex flex-col gap-3 w-full">
        {[
          { path: '/collection', label: 'Collection' },
          { path: '/new-in', label: 'New In' },
          { path: '/best-sellers', label: 'Best Sellers' },
        ].map(({ path, label }) => (
          <button
            key={path}
            onClick={() => goTo(path)}
            className="w-full h-10 bg-[#5A6D57] hover:bg-[#748C70] font-[Montserrat] text-sm text-white capitalize transition-colors"
          >
            {label}
          </button>
        ))}
      </div>
    </div>
  )
}
