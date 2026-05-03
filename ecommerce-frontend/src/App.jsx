import AppRoutes from './routes/AppRoutes'
import { CartProvider } from './modules/cart/context/CartContext'
import { WishlistProvider } from './modules/wishlist/context/WishlistContext'
import ChatWidget from './modules/chatbot/components/ChatWidget'

function App() {
  return (
    <WishlistProvider>
      <CartProvider>
        <AppRoutes />
        <ChatWidget />
      </CartProvider>
    </WishlistProvider>
  )
}

export default App