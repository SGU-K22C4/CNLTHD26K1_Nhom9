import { useContext } from 'react'
import CartContext from '../context/CartContext'

/**
 * Custom hook to consume cart state.
 * Throws if used outside <CartProvider>.
 */
export const useCartContext = () => {
  const ctx = useContext(CartContext)
  if (!ctx) throw new Error('useCartContext must be used inside <CartProvider>')
  return ctx
}

export default useCartContext
