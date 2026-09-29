/**
 * /applications/new → POST /api/applications (application-service).
 *
 * The estimate panel re-implements InstallmentCalculator in the browser for instant
 * feedback. The server's result can differ because it finances against the *independent
 * valuation*: finance / min(declared value, valuation) must stay ≤ 80% (422 "ftv-exceeded").
 * The KYC gate here is UX; the server double-checks it by calling customer-service
 * (Feign client) and answers 422 "kyc-required".
 */
import { useMemo, useState, type ChangeEvent, type FormEvent } from 'react';
import { Link, useNavigate } from 'react-router';
import { createApplication } from '../api/applications';
import { ApiError } from '../api/client';
import { PROPERTY_TYPES, type PropertyType } from '../api/types';
import { ErrorBanner } from '../components/ErrorBanner';
import { DemoHint, Loading } from '../components/Feedback';
import { SelectField, TextField } from '../components/FormField';
import { Money } from '../components/Money';
import { StatusBadge } from '../components/StatusBadge';
import { useMyCustomer } from '../hooks/useMyCustomer';
import {
  ANNUAL_PROFIT_RATE,
  MAX_FINANCE_TO_VALUE,
  MAX_TENURE_MONTHS,
  MIN_TENURE_MONTHS,
  financeToValue,
  monthlyInstallment,
} from '../lib/finance';
import { formatPercent, humanize } from '../lib/format';
import * as rules from '../lib/validation';

interface FormValues {
  propertyReference: string;
  city: string;
  propertyType: PropertyType;
  propertyValue: string;
  financeAmount: string;
  tenureMonths: string;
}

const INITIAL: FormValues = {
  propertyReference: '',
  city: 'Dubai',
  propertyType: 'APARTMENT',
  propertyValue: '1500000',
  financeAmount: '1000000',
  tenureMonths: '240',
};

/** "1,500,000" -> 1500000; empty -> NaN so validation can say "required". */
function parseAmount(raw: string): number {
  const cleaned = raw.replace(/[,\s]/g, '');
  return cleaned === '' ? Number.NaN : Number(cleaned);
}

export function NewApplicationPage() {
  const me = useMyCustomer();

  if (me.provisioning) return <Loading label="Setting up your profile…" />;
  if (me.loading) return <Loading />;
  if (me.error || !me.customer) return <ErrorBanner error={me.error} onRetry={me.reload} />;

  if (me.customer.kycStatus !== 'VERIFIED') {
    return (
      <section className="card narrow">
        <h1>New application</h1>
        <div className="banner banner-warning" role="status">
          <strong>Identity verification required.</strong>
          <p>
            Your KYC status is <StatusBadge status={me.customer.kycStatus} />. application-service will refuse new
            applications (422 <code>kyc-required</code>) until it is Verified.
          </p>
        </div>
        <Link to="/profile" className="btn btn-primary">
          Go to profile to verify
        </Link>
      </section>
    );
  }
  return <ApplicationForm />;
}

function ApplicationForm() {
  const navigate = useNavigate();
  const [values, setValues] = useState<FormValues>(INITIAL);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [serverError, setServerError] = useState<unknown>();
  const [submitting, setSubmitting] = useState(false);

  const set = (field: keyof FormValues) => (e: ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const value = field === 'propertyReference' ? e.target.value.toUpperCase() : e.target.value;
    setValues((v) => ({ ...v, [field]: value }));
  };

  const propertyValue = parseAmount(values.propertyValue);
  const financeAmount = parseAmount(values.financeAmount);
  const tenureMonths = Number(values.tenureMonths);

  const estimate = useMemo(() => {
    const installment = monthlyInstallment(financeAmount, tenureMonths);
    const ftv = financeToValue(financeAmount, propertyValue);
    return {
      installment,
      ftv,
      total: installment * (Number.isFinite(tenureMonths) ? tenureMonths : 0),
      maxFinance: Number.isFinite(propertyValue) ? propertyValue * MAX_FINANCE_TO_VALUE : Number.NaN,
    };
  }, [financeAmount, tenureMonths, propertyValue]);

  async function onSubmit(e: FormEvent): Promise<void> {
    e.preventDefault();
    const clientErrors = rules.compact({
      propertyReference: rules.propertyReference(values.propertyReference),
      city: rules.required(values.city, 'City') ?? rules.maxLength(values.city, 100),
      propertyValue: rules.propertyValue(propertyValue),
      financeAmount: rules.financeAmount(financeAmount, propertyValue),
      tenureMonths: rules.tenureMonths(tenureMonths),
    });
    setErrors(clientErrors);
    setServerError(undefined);
    if (Object.keys(clientErrors).length) return;

    setSubmitting(true);
    try {
      const created = await createApplication({
        propertyReference: values.propertyReference,
        city: values.city.trim(),
        propertyType: values.propertyType,
        propertyValue,
        financeAmount,
        tenureMonths,
      });
      navigate(`/applications/${created.id}`, { state: { created: true } });
    } catch (err) {
      setServerError(err);
      if (err instanceof ApiError) {
        const fieldErrors = err.fieldErrors();
        if (err.code === 'property-not-found') fieldErrors.propertyReference = 'The valuation provider does not know this property.';
        if (err.code === 'duplicate-application') fieldErrors.propertyReference = 'You already have an open application for this property.';
        if (err.code === 'ftv-exceeded') fieldErrors.financeAmount = 'Too high for the independent valuation (max 80%).';
        setErrors(fieldErrors);
      }
    } finally {
      setSubmitting(false);
    }
  }

  const ftvTooHigh = estimate.ftv !== null && estimate.ftv > MAX_FINANCE_TO_VALUE;

  return (
    <>
      <h1>New home finance application</h1>
      <div className="grid-2 grid-wide-left">
        <section className="card">
          <ErrorBanner error={serverError} />
          <form onSubmit={onSubmit} noValidate className="form">
            <TextField
              label="Property reference"
              value={values.propertyReference}
              error={errors.propertyReference}
              placeholder="DXB-MARINA-0042"
              maxLength={50}
              hint="Capitals, digits and '-' (max 50)."
              onChange={set('propertyReference')}
            />
            <div className="form-row">
              <TextField label="City" value={values.city} error={errors.city} onChange={set('city')} />
              <SelectField
                label="Property type"
                value={values.propertyType}
                error={errors.propertyType}
                options={PROPERTY_TYPES.map((t) => ({ value: t, label: humanize(t) }))}
                onChange={set('propertyType')}
              />
            </div>
            <div className="form-row">
              <TextField
                label="Property value (AED)"
                inputMode="decimal"
                value={values.propertyValue}
                error={errors.propertyValue}
                hint="Minimum 100,000"
                onChange={set('propertyValue')}
              />
              <TextField
                label="Finance amount (AED)"
                inputMode="decimal"
                value={values.financeAmount}
                error={errors.financeAmount}
                hint="Minimum 50,000 and at most 80% of the value"
                onChange={set('financeAmount')}
              />
            </div>
            <TextField
              label="Tenure (months)"
              type="number"
              min={MIN_TENURE_MONTHS}
              max={MAX_TENURE_MONTHS}
              step={1}
              value={values.tenureMonths}
              error={errors.tenureMonths}
              hint={`${MIN_TENURE_MONTHS}–${MAX_TENURE_MONTHS} months${
                Number.isFinite(tenureMonths) && tenureMonths > 0 ? ` (${(tenureMonths / 12).toFixed(1)} years)` : ''
              }`}
              onChange={set('tenureMonths')}
            />
            <button type="submit" className="btn btn-primary" disabled={submitting}>
              {submitting ? 'Submitting (valuation in progress)…' : 'Submit application'}
            </button>
          </form>
        </section>

        <aside className="card estimate" aria-live="polite">
          <h2>Live estimate</h2>
          <p className="estimate-figure">
            <Money amount={estimate.installment || null} />
            <span className="muted small"> / month</span>
          </p>
          <dl className="dl">
            <dt>Profit rate</dt>
            <dd>{formatPercent(ANNUAL_PROFIT_RATE)} p.a.</dd>
            <dt>Finance-to-value</dt>
            <dd className={ftvTooHigh ? 'text-danger' : undefined}>{estimate.ftv === null ? '—' : formatPercent(estimate.ftv)}</dd>
            <dt>Max finance (80%)</dt>
            <dd>
              <Money amount={Number.isFinite(estimate.maxFinance) ? estimate.maxFinance : null} />
            </dd>
            <dt>Total repayments</dt>
            <dd>
              <Money amount={estimate.total || null} />
            </dd>
          </dl>
          {ftvTooHigh && (
            <div className="banner banner-warning" role="status">
              Finance-to-value is above {formatPercent(MAX_FINANCE_TO_VALUE)}; the server will reject this.
            </div>
          )}
          <p className="muted small">
            Indicative only. The server finances against the lower of your declared value and an independent valuation
            it fetches from the valuation provider, so the approved figures may differ.
          </p>
          <DemoHint>
            <ul>
              <li>The valuation provider values every property at AED 1,450,000.</li>
              <li>
                Reference starting <code>UNKNOWN</code> → 422 <code>property-not-found</code>.
              </li>
              <li>
                Reference starting <code>SLOW</code> → provider timeout → 503.
              </li>
              <li>Same reference twice while open → 409 <code>duplicate-application</code>.</li>
            </ul>
          </DemoHint>
        </aside>
      </div>
    </>
  );
}
