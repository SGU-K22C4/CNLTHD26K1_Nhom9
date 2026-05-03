import { useContext } from 'react'
import { WishlistContext } from './wishlistContextDef'

export const useWishlistContext = () => {
  const ctx = useContext(WishlistContext)
  if (!ctx) throw new Error('useWishlistContext must be used inside <WishlistProvider>')
  return ctx
}
