import AppRoutes from './routes/AppRoutes'
import { CartProvider } from './modules/cart/context/CartContext'
import { WishlistProvider } from './modules/wishlist/context/WishlistContext'

function App() {
  return (
    <WishlistProvider>
      <CartProvider>
        <AppRoutes />
      </CartProvider>
    </WishlistProvider>
  )
}

export default App