/**
 * /register → POST /api/auth/register, then POST /api/auth/login.
 *
 * Server side: RegisterRequest is validated with bean validation (same rules as below),
 * the user is saved with a BCrypt hash and role CUSTOMER, and after the transaction commits
 * a `homefin.auth.user-registered.v1` event goes to Kafka. customer-service picks it up and
 * creates the customer profile — so the dashboard may briefly show "Setting up your profile…".
 */
import { useState, type ChangeEvent, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router';
import { register } from '../api/auth';
import { ApiError } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import { ErrorBanner } from '../components/ErrorBanner';
import { TextField } from '../components/FormField';
import * as rules from '../lib/validation';

const EMPTY = { email: '', password: '', confirmPassword: '', firstName: '', lastName: '' };

export function RegisterPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [values, setValues] = useState(EMPTY);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [serverError, setServerError] = useState<unknown>();
  const [submitting, setSubmitting] = useState(false);

  const set = (field: keyof typeof EMPTY) => (e: ChangeEvent<HTMLInputElement>) =>
    setValues((v) => ({ ...v, [field]: e.target.value }));

  function validate(): Record<string, string> {
    return rules.compact({
      email: rules.email(values.email) ?? rules.maxLength(values.email, 255),
      password: rules.password(values.password),
      confirmPassword: values.confirmPassword === values.password ? undefined : 'Passwords do not match',
      firstName: rules.required(values.firstName, 'First name') ?? rules.maxLength(values.firstName, 100),
      lastName: rules.required(values.lastName, 'Last name') ?? rules.maxLength(values.lastName, 100),
    });
  }

  async function onSubmit(e: FormEvent): Promise<void> {
    e.preventDefault();
    const clientErrors = validate();
    setErrors(clientErrors);
    setServerError(undefined);
    if (Object.keys(clientErrors).length) return;

    setSubmitting(true);
    try {
      const email = values.email.trim();
      await register({ email, password: values.password, firstName: values.firstName.trim(), lastName: values.lastName.trim() });
      // auth-service doesn't return a token on register, so sign in straight away
      await login(email, values.password);
      navigate('/', { replace: true });
    } catch (err) {
      setServerError(err);
      if (err instanceof ApiError) {
        const fieldErrors = err.fieldErrors();
        if (err.code === 'email-taken') fieldErrors.email = 'An account with this email already exists';
        setErrors(fieldErrors);
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section className="card narrow">
      <h1>Create your account</h1>
      <ErrorBanner error={serverError} />
      <form onSubmit={onSubmit} noValidate className="form">
        <div className="form-row">
          <TextField label="First name" autoComplete="given-name" value={values.firstName} error={errors.firstName} onChange={set('firstName')} />
          <TextField label="Last name" autoComplete="family-name" value={values.lastName} error={errors.lastName} onChange={set('lastName')} />
        </div>
        <TextField label="Email" type="email" autoComplete="email" value={values.email} error={errors.email} onChange={set('email')} />
        <TextField
          label="Password"
          type="password"
          autoComplete="new-password"
          value={values.password}
          error={errors.password}
          hint="10–72 characters with an uppercase letter, a lowercase letter, a digit and a symbol."
          onChange={set('password')}
        />
        <TextField
          label="Confirm password"
          type="password"
          autoComplete="new-password"
          value={values.confirmPassword}
          error={errors.confirmPassword}
          onChange={set('confirmPassword')}
        />
        <button type="submit" className="btn btn-primary btn-block" disabled={submitting}>
          {submitting ? 'Creating account…' : 'Create account'}
        </button>
      </form>
      <p className="muted">
        Already registered? <Link to="/login">Sign in</Link>
      </p>
    </section>
  );
}
