/**
 * /profile — customer-service.
 *  - GET  /api/customers/me       load (with retry while the Kafka-created profile appears)
 *  - PUT  /api/customers/me       save UpdateProfileRequest (bean validation incl. custom @Adult)
 *  - POST /api/customers/me/kyc   run identity verification against the 3rd-party KYC provider
 *                                 (WireMock) via a Resilience4j retry + circuit breaker
 */
import { useEffect, useState, type ChangeEvent, type FormEvent } from 'react';
import { ApiError } from '../api/client';
import { updateMyProfile, verifyMyIdentity } from '../api/customers';
import type { CustomerResponse } from '../api/types';
import { ErrorBanner, SuccessBanner } from '../components/ErrorBanner';
import { DemoHint, Loading } from '../components/Feedback';
import { TextField } from '../components/FormField';
import { StatusBadge } from '../components/StatusBadge';
import { useMyCustomer } from '../hooks/useMyCustomer';
import { formatDateTime } from '../lib/format';
import * as rules from '../lib/validation';

interface FormValues {
  firstName: string;
  lastName: string;
  phoneNumber: string;
  dateOfBirth: string;
  nationalId: string;
}

function toForm(c: CustomerResponse): FormValues {
  return {
    firstName: c.firstName,
    lastName: c.lastName,
    phoneNumber: c.phoneNumber ?? '',
    dateOfBirth: c.dateOfBirth ?? '',
    // the API only returns a masked value, so the user re-enters it (the server needs it on every PUT)
    nationalId: '',
  };
}

export function ProfilePage() {
  const me = useMyCustomer();

  if (me.provisioning) return <Loading label="Setting up your profile…" />;
  if (me.loading) return <Loading />;
  if (me.error || !me.customer) return <ErrorBanner error={me.error} onRetry={me.reload} />;

  return (
    <>
      <h1>Your profile</h1>
      <div className="grid-2 grid-wide-left">
        <ProfileForm customer={me.customer} onSaved={me.setCustomer} />
        <KycPanel customer={me.customer} onChecked={me.setCustomer} />
      </div>
    </>
  );
}

function ProfileForm({ customer, onSaved }: { customer: CustomerResponse; onSaved: (c: CustomerResponse) => void }) {
  const [values, setValues] = useState<FormValues>(() => toForm(customer));
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [serverError, setServerError] = useState<unknown>();
  const [saved, setSaved] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  // keep names in sync if the profile is replaced from outside (e.g. after KYC)
  useEffect(() => setValues((v) => ({ ...toForm(customer), nationalId: v.nationalId })), [customer]);

  const set = (field: keyof FormValues) => (e: ChangeEvent<HTMLInputElement>) => {
    // national IDs are upper-case per the server regex; normalise as the user types
    const value = field === 'nationalId' ? e.target.value.toUpperCase() : e.target.value;
    setValues((v) => ({ ...v, [field]: value }));
    setSaved(false);
  };

  async function onSubmit(e: FormEvent): Promise<void> {
    e.preventDefault();
    const clientErrors = rules.compact({
      firstName: rules.required(values.firstName, 'First name') ?? rules.maxLength(values.firstName, 100),
      lastName: rules.required(values.lastName, 'Last name') ?? rules.maxLength(values.lastName, 100),
      phoneNumber: rules.e164Phone(values.phoneNumber),
      dateOfBirth: rules.adultDateOfBirth(values.dateOfBirth),
      nationalId: rules.nationalId(values.nationalId),
    });
    setErrors(clientErrors);
    setServerError(undefined);
    setSaved(false);
    if (Object.keys(clientErrors).length) return;

    setSubmitting(true);
    try {
      const updated = await updateMyProfile({ ...values, firstName: values.firstName.trim(), lastName: values.lastName.trim() });
      onSaved(updated);
      setSaved(true);
    } catch (err) {
      setServerError(err);
      if (err instanceof ApiError) {
        const fieldErrors = err.fieldErrors();
        if (err.code === 'national-id-locked') {
          fieldErrors.nationalId = 'Your national ID is locked after successful verification.';
        }
        setErrors(fieldErrors);
      }
    } finally {
      setSubmitting(false);
    }
  }

  const today = new Date().toISOString().slice(0, 10);

  return (
    <section className="card">
      <h2>Personal details</h2>
      {saved && <SuccessBanner>Profile saved.</SuccessBanner>}
      <ErrorBanner error={serverError} />
      <form onSubmit={onSubmit} noValidate className="form">
        <div className="form-row">
          <TextField label="First name" autoComplete="given-name" value={values.firstName} error={errors.firstName} onChange={set('firstName')} />
          <TextField label="Last name" autoComplete="family-name" value={values.lastName} error={errors.lastName} onChange={set('lastName')} />
        </div>
        <TextField label="Email" value={customer.email} disabled hint="Managed by auth-service; cannot be changed here." />
        <TextField
          label="Mobile number"
          type="tel"
          autoComplete="tel"
          placeholder="+971501234567"
          value={values.phoneNumber}
          error={errors.phoneNumber}
          hint="International E.164 format, e.g. +971501234567"
          onChange={set('phoneNumber')}
        />
        <TextField
          label="Date of birth"
          type="date"
          max={today}
          value={values.dateOfBirth}
          error={errors.dateOfBirth}
          hint="You must be at least 18."
          onChange={set('dateOfBirth')}
        />
        <TextField
          label="National ID"
          autoComplete="off"
          placeholder={customer.nationalIdMasked ?? '784-1990-1234567-1'}
          value={values.nationalId}
          error={errors.nationalId}
          hint={
            customer.nationalIdMasked
              ? `On file: ${customer.nationalIdMasked}. Re-enter it to save (the server never sends it back in full).`
              : '6–20 characters: digits, capital letters or "-".'
          }
          onChange={set('nationalId')}
        />
        <button type="submit" className="btn btn-primary" disabled={submitting}>
          {submitting ? 'Saving…' : 'Save profile'}
        </button>
      </form>
    </section>
  );
}

function KycPanel({ customer, onChecked }: { customer: CustomerResponse; onChecked: (c: CustomerResponse) => void }) {
  const [running, setRunning] = useState(false);
  const [error, setError] = useState<unknown>();
  const [justChecked, setJustChecked] = useState(false);
  const incomplete = !customer.phoneNumber || !customer.dateOfBirth || !customer.nationalIdMasked;

  async function runKyc(): Promise<void> {
    setRunning(true);
    setError(undefined);
    setJustChecked(false);
    try {
      onChecked(await verifyMyIdentity());
      setJustChecked(true);
    } catch (err) {
      setError(err);
    } finally {
      setRunning(false);
    }
  }

  const profileIncomplete = error instanceof ApiError && error.code === 'profile-incomplete';

  return (
    <section className="card">
      <h2>Identity verification (KYC)</h2>
      <p>
        Status: <StatusBadge status={customer.kycStatus} />
        {customer.kycCheckedAt && <span className="muted small"> · checked {formatDateTime(customer.kycCheckedAt)}</span>}
      </p>

      {justChecked && customer.kycStatus === 'VERIFIED' && <SuccessBanner>Your identity has been verified.</SuccessBanner>}
      {justChecked && customer.kycStatus === 'REJECTED' && (
        <div className="banner banner-warning" role="status">
          The KYC provider rejected the verification.
        </div>
      )}
      {profileIncomplete ? (
        <div className="banner banner-warning" role="alert">
          Please save your phone number, date of birth and national ID first.
        </div>
      ) : (
        <ErrorBanner error={error} />
      )}

      {customer.kycStatus === 'VERIFIED' ? (
        <p className="muted">You're verified. Your national ID can no longer be changed.</p>
      ) : (
        <>
          {incomplete && <p className="muted">Save your phone, date of birth and national ID before verifying.</p>}
          <button type="button" className="btn btn-primary" onClick={() => void runKyc()} disabled={running}>
            {running ? 'Contacting KYC provider…' : 'Verify my identity (KYC)'}
          </button>
        </>
      )}

      <DemoHint>
        <ul>
          <li>Any other national ID → <strong>VERIFIED</strong>.</li>
          <li>
            National ID starting with <code>000</code> → provider returns <strong>REJECTED</strong>.
          </li>
          <li>
            Starting with <code>999</code> → simulated provider outage: customer-service retries, the circuit
            breaker may open, and you get a <strong>503</strong>.
          </li>
          <li>Missing phone / date of birth / national ID → 422 <code>profile-incomplete</code>.</li>
        </ul>
      </DemoHint>
    </section>
  );
}
