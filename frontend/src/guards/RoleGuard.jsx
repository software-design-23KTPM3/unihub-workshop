import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth.js';

export default function RoleGuard({ allowedRoles, children }) {
  const location = useLocation();
  const { currentUser, hasRole, isLoading } = useAuth();

  if (isLoading) {
    return <div className="loading-overlay">Đang kiểm tra quyền truy cập...</div>;
  }

  if (!currentUser) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  if (!hasRole(allowedRoles)) {
    return <Navigate to="/403" replace />;
  }

  return children;
}
