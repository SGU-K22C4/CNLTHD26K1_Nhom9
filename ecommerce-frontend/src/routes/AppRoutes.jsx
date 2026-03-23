import { Routes, Route } from 'react-router-dom'
import HomePage from '../modules/home/pages/HomePage'
import Layout from '../shared/components/layout/Layout'
import SearchPage from '../modules/product/pages/SearchPage'
import ProductListPage from '../modules/product/pages/ProductListPage'
import ProductDetailPage from '../modules/product/pages/ProductDetailPage'

export default function AppRoutes() {
  return (
    <Layout>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/search" element={<SearchPage />} />
        <Route path="/products" element={<ProductListPage />} />
        <Route path="/products/:id" element={<ProductDetailPage />} />
      </Routes>
    </Layout>
  )
}