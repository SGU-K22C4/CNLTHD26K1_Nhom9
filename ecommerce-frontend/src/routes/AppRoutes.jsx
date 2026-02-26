import { Route, Routes } from 'react-router-dom';
import HomePage from '../modules/home/pages/HomePage';
import RegisterPage from '../modules/auth/pages/RegisterPage';
import LoginPage from '../modules/auth/pages/LoginPage';
import Layout from '../shared/components/layout/Layout';

export default function AppRoutes() {
  return (
    <Routes>
      <Route
        path="/"
        element={
          <Layout>
            <HomePage />
          </Layout>
        }
      />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/login" element={<LoginPage />} />
    </Routes>
  );
}