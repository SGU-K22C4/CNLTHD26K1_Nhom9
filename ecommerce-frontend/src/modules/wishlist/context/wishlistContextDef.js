import { createContext } from 'react'

/**
 * WishlistContext — separated into its own file so WishlistContext.jsx
 * only exports React components (fixing react-refresh/only-export-components).
 */
export const WishlistContext = createContext(null)
