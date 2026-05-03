import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../modules/auth/hooks/useAuth';

/**
 * PrivateRoute — Chỉ user đã đăng nhập mới vào được.
 * Nếu chưa đăng nhập, đá về /login và ghi nhớ URL để sau login quay lại.
 * Hỗ trợ 2 cách dùng:
 *   1. <Route element={<PrivateRoute />}><Route path="/cart" .../></Route>   (Outlet pattern)
 *   2. <PrivateRoute><CartPage /></PrivateRoute>                              (Children pattern)
 */
export default function PrivateRoute({ children }) {
  const { user } = useAuth();
  const location = useLocation();
  const hasAccessToken = Boolean(localStorage.getItem('accessToken'));

  if (!user && !hasAccessToken) {
    return <Navigate to="/login" state={{ from: location.pathname }} replace />;
  }

  // Hỗ trợ cả children và Outlet
  return children ?? <Outlet />;
}

/**
 * GuestOnlyRoute — Chỉ khách chưa đăng nhập mới vào được (login/register).
 * Nếu đã đăng nhập mà cố vào, đá về trang chủ.
 */
export function GuestOnlyRoute() {
  const { user } = useAuth();
  const hasAccessToken = Boolean(localStorage.getItem('accessToken'));

  if (user || hasAccessToken) {
    return <Navigate to="/" replace />;
  }

  return <Outlet />;
}
