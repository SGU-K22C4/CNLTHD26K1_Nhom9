import { Routes, Route } from 'react-router-dom'
import HomePage from '../modules/home/pages/HomePage'
import CartPage from '../modules/cart/pages/CartPage'
import CheckoutPage from '../modules/order/pages/CheckoutPage'
import PaymentSuccessPage from '../modules/order/pages/PaymentSuccessPage'
import PaymentFailedPage from '../modules/order/pages/PaymentFailedPage'
import VNPayReturnPage from '../modules/order/pages/VNPayReturnPage'
import OrderDetailPage from '../modules/order/pages/OrderDetailPage'
import Layout from '../shared/components/layout/Layout'
import ProductListPage from '../modules/product/pages/ProductListPage'
import ProductDetailPage from '../modules/product/pages/ProductDetailPage'
import WishlistPage from '../modules/wishlist/pages/WishlistPage'
import LoginPage from '../modules/auth/pages/LoginPage'
import RegisterPage from '../modules/auth/pages/RegisterPage'
function WithLayout({ children }) {
  return <Layout>{children}</Layout>
}

export default function AppRoutes() {
  return (
    <Routes>
      {/* -------------------------------------------------- */}
      {/* NHÓM 1: CÁC TRANG CÓ LAYOUT (HEADER & FOOTER)        */}
      {/* -------------------------------------------------- */}
      <Route path="/" element={<WithLayout><HomePage /></WithLayout>} />
      <Route path="/login" element={<WithLayout><LoginPage /></WithLayout>} />
      <Route path="/register" element={<WithLayout><RegisterPage /></WithLayout>} />
      <Route path="/products" element={<WithLayout><ProductListPage /></WithLayout>} />
      <Route path="/collection/:gender" element={<WithLayout><ProductListPage /></WithLayout>} />
      <Route path="/products/:id" element={<WithLayout><ProductDetailPage /></WithLayout>} />
      <Route path="/cart" element={<WithLayout><CartPage /></WithLayout>} />
      <Route path="/wishlist" element={<WithLayout><WishlistPage /></WithLayout>} />
      <Route path="/checkout/success" element={<WithLayout><PaymentSuccessPage /></WithLayout>} />
      <Route path="/checkout/failed" element={<WithLayout><PaymentFailedPage /></WithLayout>} />
      <Route path="/payment/vnpay-return" element={<WithLayout><VNPayReturnPage /></WithLayout>} />
      <Route path="/orders/:orderId" element={<WithLayout><OrderDetailPage /></WithLayout>} />

      {/* -------------------------------------------------- */}
      {/* NHÓM 2: CÁC TRANG CHECKOUT FULL MÀN HÌNH (NO LAYOUT) */}
      {/* -------------------------------------------------- */}
      <Route path="/checkout" element={<CheckoutPage />} />
    </Routes>
  )
}
