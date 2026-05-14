import { createBrowserRouter, Navigate } from 'react-router-dom';
import AuthLayout from '../layouts/AuthLayout.jsx';
import StudentLayout from '../layouts/StudentLayout.jsx';
import AdminLayout from '../layouts/AdminLayout.jsx';
import LoginPage from '../pages/auth/LoginPage.jsx';
import UnauthorizedPage from '../pages/auth/UnauthorizedPage.jsx';
import StudentWorkshopsPage from '../pages/student/StudentWorkshopsPage.jsx';
import StudentWorkshopDetailPage from '../pages/student/StudentWorkshopDetailPage.jsx';
import StudentRegistrationsPage from '../pages/student/StudentRegistrationsPage.jsx';
import StudentTicketPage from '../pages/student/StudentTicketPage.jsx';
import StudentPaymentPage from '../pages/student/StudentPaymentPage.jsx';
import AdminDashboardPage from '../pages/admin/AdminDashboardPage.jsx';
import AdminWorkshopsPage from '../pages/admin/AdminWorkshopsPage.jsx';
import AdminWorkshopCreatePage from '../pages/admin/AdminWorkshopCreatePage.jsx';
import AdminWorkshopEditPage from '../pages/admin/AdminWorkshopEditPage.jsx';
import AdminRegistrationsPage from '../pages/admin/AdminRegistrationsPage.jsx';
import AdminStatisticsPage from '../pages/admin/AdminStatisticsPage.jsx';
import NotFoundPage from '../pages/auth/NotFoundPage.jsx';
import RoleGuard from '../guards/RoleGuard.jsx';
import { useAuth } from '../hooks/useAuth.js';

const defaultRouteByRole = {
  STUDENT: '/student/workshops',
  ORGANIZER: '/admin/dashboard',
};

function RootRedirect() {
  const { currentUser, isLoading } = useAuth();

  if (isLoading) {
    return <div className="loading-overlay">Đang kiểm tra quyền truy cập...</div>;
  }

  if (!currentUser) {
    return <Navigate to="/login" replace />;
  }

  return <Navigate to={defaultRouteByRole[currentUser.role] || '/login'} replace />;
}

export const router = createBrowserRouter([
  {
    path: '/',
    element: <RootRedirect />,
  },
  {
    element: <AuthLayout />,
    children: [
      { path: '/login', element: <LoginPage /> },
      { path: '/403', element: <UnauthorizedPage /> },
    ],
  },
  {
    path: '/student',
    element: (
      <RoleGuard allowedRoles={['STUDENT']}>
        <StudentLayout />
      </RoleGuard>
    ),
    children: [
      { index: true, element: <Navigate to="workshops" replace /> },
      { path: 'workshops', element: <StudentWorkshopsPage /> },
      { path: 'workshops/:id', element: <StudentWorkshopDetailPage /> },
      { path: 'my-registrations', element: <StudentRegistrationsPage /> },
      { path: 'tickets/:registrationId', element: <StudentTicketPage /> },
      { path: 'tickets/:registrationId/payment', element: <StudentPaymentPage /> },
    ],
  },
  {
    path: '/admin',
    element: (
      <RoleGuard allowedRoles={['ORGANIZER', 'STAFF']}>
        <AdminLayout />
      </RoleGuard>
    ),
    children: [
      { index: true, element: <Navigate to="dashboard" replace /> },
      { path: 'dashboard', element: <AdminDashboardPage /> },
      { path: 'workshops', element: <AdminWorkshopsPage /> },
      { path: 'workshops/create', element: <AdminWorkshopCreatePage /> },
      { path: 'workshops/:id/edit', element: <AdminWorkshopEditPage /> },
      { path: 'registrations', element: <AdminRegistrationsPage /> },
      { path: 'statistics', element: <AdminStatisticsPage /> },
    ],
  },
  {
    path: '*',
    element: <NotFoundPage />,
  },
]);
