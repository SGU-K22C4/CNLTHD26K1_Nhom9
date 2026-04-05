import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../modules/auth/hooks/useAuth';

export default function PrivateRoute({ children }) {
  const { user } = useAuth();
  const location = useLocation();

  if (!user) {
    // Chuyển hướng về login nhưng nhớ lại trang hiện tại để login xong back lại cho tiện
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  return children;
}