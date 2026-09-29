/**
 * / (customer dashboard). Two parallel calls:
 *  - GET /api/customers/me (customer-service), retried while the Kafka-created profile appears
 *  - GET /api/applications?size=5 (application-service), the latest own applications
 * Admins have no customer profile (customer-service would 403), so they're redirected.
 */
import { Link, Navigate } from 'react-router';
import { listMyApplications } from '../api/applications';
import { useAuth } from '../auth/AuthContext';
import { ApplicationsTable } from '../components/ApplicationsTable';
import { ErrorBanner } from '../components/ErrorBanner';
import { EmptyState, Loading } from '../components/Feedback';
import { StatusBadge } from '../components/StatusBadge';
import { useAsync } from '../hooks/useAsync';
import { useMyCustomer } from '../hooks/useMyCustomer';
import type { CustomerResponse } from '../api/types';
import { formatDate, formatDateTime } from '../lib/format';

export function HomePage() {
  const { isAdmin } = useAuth();
  if (isAdmin) return <Navigate to="/admin/applications" replace />;
  return <CustomerDashboard />;
}

function CustomerDashboard() {
  const me = useMyCustomer();
  const apps = useAsync((signal) => listMyApplications({ page: 0, size: 5 }, signal), []);

  return (
    <>
      <h1>{me.customer ? `Welcome, ${me.customer.firstName}` : 'Dashboard'}</h1>
      <div className="grid-2">
        <section className="card">
          <h2>Your profile</h2>
          {me.provisioning ? (
            <Loading label="Setting up your profile…" />
          ) : me.loading ? (
            <Loading />
          ) : me.error ? (
            <ErrorBanner error={me.error} onRetry={me.reload} />
          ) : (
            me.customer && <ProfileSummary customer={me.customer} />
          )}
        </section>

        <section className="card">
          <h2>Identity verification</h2>
          {me.customer ? <KycCallToAction customer={me.customer} /> : <p className="muted">Waiting for your profile…</p>}
        </section>
      </div>

      <section className="card">
        <div className="card-header">
          <h2>Latest applications</h2>
          <Link to="/applications">View all</Link>
        </div>
        {apps.loading ? (
          <Loading />
        ) : apps.error ? (
          <ErrorBanner error={apps.error} onRetry={apps.reload} />
        ) : apps.data && apps.data.content.length > 0 ? (
          <ApplicationsTable applications={apps.data.content} />
        ) : (
          <EmptyState title="No applications yet">
            <Link to="/applications/new" className="btn btn-primary">
              Start an application
            </Link>
          </EmptyState>
        )}
      </section>
    </>
  );
}

function ProfileSummary({ customer }: { customer: CustomerResponse }) {
  return (
    <>
      <dl className="dl">
        <dt>Name</dt>
        <dd>
          {customer.firstName} {customer.lastName}
        </dd>
        <dt>Email</dt>
        <dd>{customer.email}</dd>
        <dt>Phone</dt>
        <dd>{customer.phoneNumber ?? <span className="muted">Not set</span>}</dd>
        <dt>Date of birth</dt>
        <dd>{customer.dateOfBirth ? formatDate(customer.dateOfBirth) : <span className="muted">Not set</span>}</dd>
        <dt>National ID</dt>
        <dd>{customer.nationalIdMasked ?? <span className="muted">Not set</span>}</dd>
      </dl>
      <Link to="/profile">Edit profile</Link>
    </>
  );
}

function KycCallToAction({ customer }: { customer: CustomerResponse }) {
  const status = <StatusBadge status={customer.kycStatus} />;
  switch (customer.kycStatus) {
    case 'VERIFIED':
      return (
        <>
          <p>{status} Verified {formatDateTime(customer.kycCheckedAt)}. You can apply for home finance.</p>
          <Link to="/applications/new" className="btn btn-primary">
            New application
          </Link>
        </>
      );
    case 'REJECTED':
      return (
        <>
          <p>{status} The KYC provider could not verify your identity. Check your details and try again.</p>
          <Link to="/profile" className="btn">
            Review profile
          </Link>
        </>
      );
    case 'PENDING':
      return (
        <>
          <p>{status} Complete your profile and verify your identity before applying.</p>
          <Link to="/profile" className="btn btn-primary">
            Verify my identity
          </Link>
        </>
      );
  }
}
