import { Routes, Route } from 'react-router-dom'
import HomePage from '../modules/home/pages/HomePage'
import CartPage from '../modules/cart/pages/CartPage'
import CheckoutPage from '../modules/order/pages/CheckoutPage'
import CheckoutShippingPage from '../modules/order/pages/CheckoutShippingPage'
import CheckoutPaymentPage from '../modules/order/pages/CheckoutPaymentPage'
import PaymentSuccessPage from '../modules/order/pages/PaymentSuccessPage'
import PaymentFailedPage from '../modules/order/pages/PaymentFailedPage'
import Layout from '../shared/components/layout/Layout'
import SearchPage from '../modules/product/pages/SearchPage'
import ProductListPage from '../modules/product/pages/ProductListPage'
import ProductDetailPage from '../modules/product/pages/ProductDetailPage'

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
      <Route path="/search" element={<WithLayout><SearchPage /></WithLayout>} />
      <Route path="/products" element={<WithLayout><ProductListPage /></WithLayout>} />
      <Route path="/collection/:gender" element={<WithLayout><ProductListPage /></WithLayout>} />
      <Route path="/products/:id" element={<WithLayout><ProductDetailPage /></WithLayout>} />
      <Route path="/cart" element={<WithLayout><CartPage /></WithLayout>} />
      <Route path="/checkout/success" element={<WithLayout><PaymentSuccessPage /></WithLayout>} />
      <Route path="/checkout/failed" element={<WithLayout><PaymentFailedPage /></WithLayout>} />

      {/* -------------------------------------------------- */}
      {/* NHÓM 2: CÁC TRANG CHECKOUT FULL MÀN HÌNH (NO LAYOUT) */}
      {/* -------------------------------------------------- */}
      <Route path="/checkout" element={<CheckoutPage />} />
      <Route path="/checkout/shipping" element={<CheckoutShippingPage />} />
      <Route path="/checkout/payment" element={<CheckoutPaymentPage />} />
    </Routes>
  )
}
  )
}