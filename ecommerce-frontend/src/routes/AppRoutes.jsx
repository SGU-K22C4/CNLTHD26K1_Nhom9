import { Routes, Route } from 'react-router-dom';
import HomePage from '../modules/home/pages/HomePage';
import ProductListPage from '../modules/product/pages/ProductListPage';
import ProductDetailPage from '../modules/product/pages/ProductDetailPage';
import Layout from '../shared/components/layout/Layout';

export default function AppRoutes() {
  return (
    <Layout>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/collection/:category" element={<ProductListPage />} />
        <Route path="/shop" element={<ProductListPage />} />
        <Route path="/product/:slug" element={<ProductDetailPage />} />
        {/* xử lý sau: thêm các routes khác */}
      </Routes>
    </Layout>
  );
}