import AppRoutes from './routes/AppRoutes'
import { CartProvider } from './modules/cart/context/CartContext'

function App() {
  return (
    <CartProvider>
      <AppRoutes />
    </CartProvider>
  )
}

export default App