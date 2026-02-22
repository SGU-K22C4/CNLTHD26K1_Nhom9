import { createContext, useContext } from 'react'

const CartContext = createContext(null)

export const useCartContext = () => useContext(CartContext)

export default CartContext