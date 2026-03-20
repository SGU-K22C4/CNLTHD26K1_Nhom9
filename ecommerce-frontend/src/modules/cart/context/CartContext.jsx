import { createContext, useContext, useState, useCallback, useMemo } from 'react'

const CartContext = createContext(null)

/**
 * Custom hook to consume cart state.
 * Throws if used outside <CartProvider>.
 */
export const useCartContext = () => {
  const ctx = useContext(CartContext)
  if (!ctx) throw new Error('useCartContext must be used inside <CartProvider>')
  return ctx
}

/* ── Mock data — replace with real API integration ────────────────────────── */
const MOCK_ITEMS = [
  {
    id: '1',
    name: 'Linen Midi Dress',
    color: 'Sage Green',
    size: 'S',
    price: 89.99,
    quantity: 1,
    image: 'https://images.unsplash.com/photo-1515372039744-b8f02a3ae446?w=400&q=80',
  },
  {
    id: '2',
    name: 'Relaxed Linen Shirt',
    color: 'Ivory',
    size: 'M',
    price: 59.99,
    quantity: 2,
    image: 'https://images.unsplash.com/photo-1594938298603-c8148c4b4f56?w=400&q=80',
  },
  {
    id: '3',
    name: 'Wide-Leg Trousers',
    color: 'Ecru',
    size: 'S',
    price: 74.99,
    quantity: 1,
    image: 'https://images.unsplash.com/photo-1551163943-3f7253a97b2c?w=400&q=80',
  },
]

export function CartProvider({ children }) {
  const [items, setItems] = useState(MOCK_ITEMS)
  const [isDrawerOpen, setIsDrawerOpen] = useState(false)

  /* ── Drawer controls ─────────────────────────────────────────────────────── */
  const openDrawer = useCallback(() => setIsDrawerOpen(true), [])
  const closeDrawer = useCallback(() => setIsDrawerOpen(false), [])

  /* ── Item CRUD ───────────────────────────────────────────────────────────── */
  const addItem = useCallback((product) => {
    setItems((prev) => {
      const existing = prev.find((i) => i.id === product.id)
      if (existing) {
        return prev.map((i) =>
          i.id === product.id
            ? { ...i, quantity: i.quantity + (product.quantity ?? 1) }
            : i,
        )
      }
      return [...prev, { ...product, quantity: product.quantity ?? 1 }]
    })
  }, [])

  const removeItem = useCallback(
    (id) => setItems((prev) => prev.filter((i) => i.id !== id)),
    [],
  )

  const updateQuantity = useCallback((id, qty) => {
    setItems((prev) =>
      qty <= 0
        ? prev.filter((i) => i.id !== id)
        : prev.map((i) => (i.id === id ? { ...i, quantity: qty } : i)),
    )
  }, [])

  /* ── Derived values (memoised) ───────────────────────────────────────────── */
  const subtotal = useMemo(
    () => items.reduce((sum, i) => sum + i.price * i.quantity, 0),
    [items],
  )

  const totalItems = useMemo(
    () => items.reduce((sum, i) => sum + i.quantity, 0),
    [items],
  )

  /* ── Context value (stable reference when deps unchanged) ────────────────── */
  const value = useMemo(
    () => ({
      items,
      isDrawerOpen,
      openDrawer,
      closeDrawer,
      addItem,
      removeItem,
      updateQuantity,
      subtotal,
      totalItems,
    }),
    [items, isDrawerOpen, openDrawer, closeDrawer, addItem, removeItem, updateQuantity, subtotal, totalItems],
  )

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>
}

export default CartContext