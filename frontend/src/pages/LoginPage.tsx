/**
 * /login → POST /api/auth/login (auth-service).
 * auth-service authenticates with Spring Security's AuthenticationManager (BCrypt check)
 * and returns a signed RS256 JWT. 401 here means bad credentials, not an expired session.
 */
import { useState, type FormEvent } from 'react';
import { Link, useLocation, useNavigate } from 'react-router';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { ErrorBanner } from '../components/ErrorBanner';
import { DemoHint } from '../components/Feedback';
import { TextField } from '../components/FormField';
import { compact, email as emailRule, required } from '../lib/validation';

export function LoginPage() {
  const { login, notice, clearNotice } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as { from?: string } | null)?.from;

  const [values, setValues] = useState({ email: '', password: '' });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [serverError, setServerError] = useState<unknown>();
  const [submitting, setSubmitting] = useState(false);

  async function onSubmit(e: FormEvent): Promise<void> {
    e.preventDefault();
    const clientErrors = compact({ email: emailRule(values.email), password: required(values.password, 'Password') });
    setErrors(clientErrors);
    setServerError(undefined);
    if (Object.keys(clientErrors).length) return;

    setSubmitting(true);
    try {
      const user = await login(values.email.trim(), values.password);
      clearNotice();
      navigate(from ?? (user.roles.includes('ADMIN') ? '/admin/applications' : '/'), { replace: true });
    } catch (err) {
      setServerError(err);
      if (err instanceof ApiError) setErrors(err.fieldErrors());
    } finally {
      setSubmitting(false);
    }
  }

  const badCredentials = serverError instanceof ApiError && serverError.status === 401;

  return (
    <section className="card narrow">
      <h1>Sign in</h1>
      {notice && (
        <div className="banner banner-warning" role="status">
          {notice}
        </div>
      )}
      {badCredentials ? (
        <div className="banner banner-error" role="alert">
          Invalid email or password.
        </div>
      ) : (
        <ErrorBanner error={serverError} />
      )}

      <form onSubmit={onSubmit} noValidate className="form">
        <TextField
          label="Email"
          type="email"
          autoComplete="username"
          value={values.email}
          error={errors.email}
          onChange={(e) => setValues({ ...values, email: e.target.value })}
        />
        <TextField
          label="Password"
          type="password"
          autoComplete="current-password"
          value={values.password}
          error={errors.password}
          onChange={(e) => setValues({ ...values, password: e.target.value })}
        />
        <button type="submit" className="btn btn-primary btn-block" disabled={submitting}>
          {submitting ? 'Signing in…' : 'Sign in'}
        </button>
      </form>

      <p className="muted">
        No account yet? <Link to="/register">Register</Link>
      </p>
      <DemoHint>
        <p>
          Admin account seeded by auth-service's AdminBootstrap: <code>admin@homefin.local</code> /{' '}
          <code>Admin#12345</code>
        </p>
      </DemoHint>
    </section>
  );
}
