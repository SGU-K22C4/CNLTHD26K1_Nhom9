import { Routes, Route } from 'react-router-dom'
import HomePage from '../modules/home/pages/HomePage'
import PrivateRoute, { GuestOnlyRoute } from './PrivateRoute'
import CartPage from '../modules/cart/pages/CartPage'
import CheckoutPage from '../modules/order/pages/CheckoutPage'
import PaymentSuccessPage from '../modules/order/pages/PaymentSuccessPage'
import PaymentFailedPage from '../modules/order/pages/PaymentFailedPage'
import VNPayReturnPage from '../modules/order/pages/VNPayReturnPage'
import OrderDetailPage from '../modules/order/pages/OrderDetailPage'
import OrderHistoryPage from '../modules/order/pages/OrderHistoryPage'
import LoyaltyWalletPage from '../modules/order/pages/LoyaltyWalletPage'
import Layout from '../shared/components/layout/Layout'
import ProductListPage from '../modules/product/pages/ProductListPage'
import ProductDetailPage from '../modules/product/pages/ProductDetailPage'
import WishlistPage from '../modules/wishlist/pages/WishlistPage'
import LoginPage from '../modules/auth/pages/LoginPage'
import RegisterPage from '../modules/auth/pages/RegisterPage'
import VerifyEmailPage from '../modules/auth/pages/VerifyEmailPage'

function WithLayout({ children }) {
  return <Layout>{children}</Layout>
}

export default function AppRoutes() {
  return (
    <Routes>
      {/* -------------------------------------------------- */}
      {/* NHÓM 1: PUBLIC — Ai cũng vào được                  */}
      {/* -------------------------------------------------- */}
      <Route path="/" element={<WithLayout><HomePage /></WithLayout>} />
      <Route path="/products" element={<WithLayout><ProductListPage /></WithLayout>} />
      <Route path="/collection/:gender" element={<WithLayout><ProductListPage /></WithLayout>} />
      <Route path="/products/:id" element={<WithLayout><ProductDetailPage /></WithLayout>} />
      <Route path="/verify-email" element={<WithLayout><VerifyEmailPage /></WithLayout>} />

      {/* -------------------------------------------------- */}
      {/* NHÓM 2: GUEST ONLY — Đã login thì về trang chủ     */}
      {/* -------------------------------------------------- */}
      <Route element={<GuestOnlyRoute />}>
        <Route path="/login" element={<WithLayout><LoginPage /></WithLayout>} />
        <Route path="/register" element={<WithLayout><RegisterPage /></WithLayout>} />
      </Route>

      {/* -------------------------------------------------- */}
      {/* NHÓM 3: PRIVATE — Phải đăng nhập mới vào được      */}
      {/* -------------------------------------------------- */}
      <Route path="/cart" element={<PrivateRoute><WithLayout><CartPage /></WithLayout></PrivateRoute>} />
      <Route path="/wishlist" element={<PrivateRoute><WithLayout><WishlistPage /></WithLayout></PrivateRoute>} />
      <Route path="/checkout/success" element={<PrivateRoute><WithLayout><PaymentSuccessPage /></WithLayout></PrivateRoute>} />
      <Route path="/checkout/failed" element={<PrivateRoute><WithLayout><PaymentFailedPage /></WithLayout></PrivateRoute>} />
      <Route path="/payment/vnpay-return" element={<PrivateRoute><WithLayout><VNPayReturnPage /></WithLayout></PrivateRoute>} />
      <Route path="/orders" element={<PrivateRoute><WithLayout><OrderHistoryPage /></WithLayout></PrivateRoute>} />
      <Route path="/wallet" element={<PrivateRoute><WithLayout><LoyaltyWalletPage /></WithLayout></PrivateRoute>} />
      <Route path="/orders/:orderId" element={<PrivateRoute><WithLayout><OrderDetailPage /></WithLayout></PrivateRoute>} />
      <Route path="/checkout" element={<PrivateRoute><CheckoutPage /></PrivateRoute>} />
    </Routes>
  )
}

