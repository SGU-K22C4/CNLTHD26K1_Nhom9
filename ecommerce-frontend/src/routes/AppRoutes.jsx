import { Navigate, Route, Routes } from 'react-router-dom'
import LoginPage from '../modules/auth/pages/LoginPage'
import RegisterPage from '../modules/auth/pages/RegisterPage'
import VerifyEmailPage from '../modules/auth/pages/VerifyEmailPage'
import CartPage from '../modules/cart/pages/CartPage'
import ChatbotPage from '../modules/chatbot/pages/ChatbotPage'
import ContactPage from '../modules/contact/pages/ContactPage'
import HomePage from '../modules/home/pages/HomePage'
import CheckoutPage from '../modules/order/pages/CheckoutPage'
import LoyaltyWalletPage from '../modules/order/pages/LoyaltyWalletPage'
import OrderDetailPage from '../modules/order/pages/OrderDetailPage'
import OrderHistoryPage from '../modules/order/pages/OrderHistoryPage'
import VNPayReturnPage from '../modules/order/pages/VNPayReturnPage'
import ProductDetailPage from '../modules/product/pages/ProductDetailPage'
import ProductListPage from '../modules/product/pages/ProductListPage'
import ProfilePage from '../modules/user/pages/ProfilePage'
import WishlistPage from '../modules/wishlist/pages/WishlistPage'
import Layout from '../shared/components/layout/Layout'
import PrivateRoute, { GuestOnlyRoute } from './PrivateRoute'

function WithLayout({ children }) {
  return <Layout>{children}</Layout>
}

export default function AppRoutes() {
  return (
    <Routes>
      {/* Public routes */}
      <Route path="/" element={<WithLayout><HomePage /></WithLayout>} />
      <Route path="/products" element={<WithLayout><ProductListPage /></WithLayout>} />
      <Route path="/collection/:gender" element={<WithLayout><ProductListPage /></WithLayout>} />
      <Route path="/products/:id" element={<WithLayout><ProductDetailPage /></WithLayout>} />
      <Route path="/verify-email" element={<WithLayout><VerifyEmailPage /></WithLayout>} />
      <Route path="/contact" element={<WithLayout><ContactPage /></WithLayout>} />
      <Route path="/chatbot" element={<PrivateRoute><WithLayout><ChatbotPage /></WithLayout></PrivateRoute>} />
      <Route path="/index.html" element={<WithLayout><HomePage /></WithLayout>} />

      {/* Guest-only routes */}
      <Route element={<GuestOnlyRoute />}>
        <Route path="/login" element={<WithLayout><LoginPage /></WithLayout>} />
        <Route path="/register" element={<WithLayout><RegisterPage /></WithLayout>} />
      </Route>

      {/* Authenticated routes */}
      <Route path="/cart" element={<PrivateRoute><WithLayout><CartPage /></WithLayout></PrivateRoute>} />
      <Route path="/wishlist" element={<PrivateRoute><WithLayout><WishlistPage /></WithLayout></PrivateRoute>} />
      <Route path="/payment/vnpay-return" element={<PrivateRoute><WithLayout><VNPayReturnPage /></WithLayout></PrivateRoute>} />
      <Route path="/orders" element={<PrivateRoute><WithLayout><OrderHistoryPage /></WithLayout></PrivateRoute>} />
      <Route path="/wallet" element={<PrivateRoute><WithLayout><LoyaltyWalletPage /></WithLayout></PrivateRoute>} />
      <Route path="/orders/:orderId" element={<PrivateRoute><WithLayout><OrderDetailPage /></WithLayout></PrivateRoute>} />
      <Route path="/profile" element={<PrivateRoute><WithLayout><ProfilePage /></WithLayout></PrivateRoute>} />
      <Route path="/checkout" element={<PrivateRoute><CheckoutPage /></PrivateRoute>} />

      {/* Keep SPA resilient to direct deep links and unknown paths */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
