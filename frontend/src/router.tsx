/**
 * Route table (react-router v8 "data mode": createBrowserRouter + RouterProvider).
 * Guards are pathless layout routes; see auth/RequireRole.tsx.
 */
import { Navigate, createBrowserRouter } from 'react-router';
import { GuestOnly, RequireRole } from './auth/RequireRole';
import { Layout } from './components/Layout';
import { ApplicationDetailPage } from './pages/ApplicationDetailPage';
import { ApplicationsPage } from './pages/ApplicationsPage';
import { HomePage } from './pages/HomePage';
import { LoginPage } from './pages/LoginPage';
import { NewApplicationPage } from './pages/NewApplicationPage';
import { NotFoundPage, RouteErrorPage } from './pages/NotFoundPage';
import { ProfilePage } from './pages/ProfilePage';
import { RegisterPage } from './pages/RegisterPage';
import { AdminApplicationsPage } from './pages/admin/AdminApplicationsPage';
import { AdminCustomersPage } from './pages/admin/AdminCustomersPage';

export const router = createBrowserRouter([
  {
    element: <Layout />,
    errorElement: <RouteErrorPage />,
    children: [
      {
        element: <GuestOnly />,
        children: [
          { path: 'login', element: <LoginPage /> },
          { path: 'register', element: <RegisterPage /> },
        ],
      },
      {
        // any signed-in user
        element: <RequireRole />,
        children: [
          { index: true, element: <HomePage /> },
          // owner or admin — application-service enforces it (403 otherwise)
          { path: 'applications/:id', element: <ApplicationDetailPage /> },
        ],
      },
      {
        element: <RequireRole role="CUSTOMER" />,
        children: [
          { path: 'profile', element: <ProfilePage /> },
          { path: 'applications', element: <ApplicationsPage /> },
          { path: 'applications/new', element: <NewApplicationPage /> },
        ],
      },
      {
        path: 'admin',
        element: <RequireRole role="ADMIN" />,
        children: [
          { index: true, element: <Navigate to="applications" replace /> },
          { path: 'applications', element: <AdminApplicationsPage /> },
          { path: 'customers', element: <AdminCustomersPage /> },
        ],
      },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
]);
