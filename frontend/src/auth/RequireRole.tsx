/**
 * Route guards. These are UX only — the real authorisation happens in each service's
 * Spring Security config (`hasRole("ADMIN")` etc.), which returns 401/403 regardless of
 * what the browser does.
 */
import { Navigate, Outlet, useLocation } from 'react-router';
import type { Role } from '../api/types';
import { useAuth } from './AuthContext';

/** Layout route: renders child routes only for a signed-in user (optionally with a role). */
export function RequireRole({ role }: { role?: Role }) {
  const { user } = useAuth();
  const location = useLocation();

  if (!user) {
    // remember where the user wanted to go; LoginPage sends them back after sign-in
    return <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />;
  }
  if (role && !user.roles.includes(role)) {
    return (
      <section className="card narrow">
        <h1>Not allowed</h1>
        <p className="muted">This page requires the {role} role. You are signed in as {user.email}.</p>
      </section>
    );
  }
  return <Outlet />;
}

/** Layout route for /login and /register: signed-in users are sent home. */
export function GuestOnly() {
  const { user } = useAuth();
  return user ? <Navigate to="/" replace /> : <Outlet />;
}
