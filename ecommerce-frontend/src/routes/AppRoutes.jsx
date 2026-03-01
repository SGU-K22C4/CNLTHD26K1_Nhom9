import { Routes, Route } from 'react-router-dom'
import HomePage from '../modules/home/pages/HomePage'
import CartPage from '../modules/cart/pages/CartPage'
import CheckoutPage from '../modules/order/pages/CheckoutPage'
import CheckoutShippingPage from '../modules/order/pages/CheckoutShippingPage'
import CheckoutPaymentPage from '../modules/order/pages/CheckoutPaymentPage'
import PaymentSuccessPage from '../modules/order/pages/PaymentSuccessPage'
import PaymentFailedPage from '../modules/order/pages/PaymentFailedPage'
import Layout from '../shared/components/layout/Layout'

function WithLayout({ children }) {
  return <Layout>{children}</Layout>
}

export default function AppRoutes() {
  return (
    <Routes>
      {/* Full-screen checkout — no shared header/footer */}
      <Route path="/checkout" element={<CheckoutPage />} />
      <Route path="/checkout/shipping" element={<CheckoutShippingPage />} />
      <Route path="/checkout/payment" element={<CheckoutPaymentPage />} />

      {/* Pages wrapped in the shared Layout */}
      <Route path="/" element={<WithLayout><HomePage /></WithLayout>} />
      <Route path="/cart" element={<WithLayout><CartPage /></WithLayout>} />
      <Route path="/checkout/success" element={<WithLayout><PaymentSuccessPage /></WithLayout>} />
      <Route path="/checkout/failed" element={<WithLayout><PaymentFailedPage /></WithLayout>} />
    </Routes>
  )
}