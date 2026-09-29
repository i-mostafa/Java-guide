import { NavLink, Outlet, useNavigate } from 'react-router';
import { useAuth } from '../auth/AuthContext';

const CUSTOMER_LINKS = [
  { to: '/', label: 'Dashboard', end: true },
  { to: '/profile', label: 'Profile', end: false },
  { to: '/applications', label: 'Applications', end: true },
  { to: '/applications/new', label: 'Apply', end: false },
];

const ADMIN_LINKS = [
  { to: '/admin/applications', label: 'Applications', end: false },
  { to: '/admin/customers', label: 'Customers', end: false },
];

/** App shell: top nav (links depend on the role claim in the JWT) + routed content. */
export function Layout() {
  const { user, isAdmin, logout } = useAuth();
  const navigate = useNavigate();
  const links = !user ? [] : isAdmin ? ADMIN_LINKS : CUSTOMER_LINKS;

  return (
    <div className="app">
      <a className="skip-link" href="#main">
        Skip to content
      </a>
      <header className="topbar">
        <div className="topbar-inner">
          <NavLink to="/" className="brand">
            <span className="brand-mark" aria-hidden="true">
              ⌂
            </span>
            HomeFin
          </NavLink>

          {user && (
            <nav className="nav" aria-label="Main">
              {links.map((l) => (
                <NavLink key={l.to} to={l.to} end={l.end}>
                  {l.label}
                </NavLink>
              ))}
            </nav>
          )}

          <div className="topbar-user">
            {user ? (
              <>
                <span className="user-email" title={user.email}>
                  {user.email}
                </span>
                {isAdmin && <span className="badge badge-info">Admin</span>}
                <button
                  type="button"
                  className="btn btn-small"
                  onClick={() => {
                    logout();
                    navigate('/login');
                  }}
                >
                  Log out
                </button>
              </>
            ) : (
              <>
                <NavLink to="/login">Sign in</NavLink>
                <NavLink to="/register" className="btn btn-small btn-primary">
                  Register
                </NavLink>
              </>
            )}
          </div>
        </div>
      </header>

      <main id="main" className="container">
        <Outlet />
      </main>

      <footer className="footer muted">
        HomeFin demo · Browser → Vite/nginx → API gateway :8080 → Spring Boot services
      </footer>
    </div>
  );
}
