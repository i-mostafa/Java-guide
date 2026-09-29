/**
 * /applications/:id → GET /api/applications/{id} (application-service).
 * The service allows the owner (customerUserId == JWT sub) or an ADMIN; others get 403.
 * Admins also get the status buttons here (PATCH /api/applications/{id}/status).
 */
import { Link, useLocation, useParams } from 'react-router';
import { getApplication } from '../api/applications';
import { useAuth } from '../auth/AuthContext';
import { ErrorBanner, SuccessBanner } from '../components/ErrorBanner';
import { Loading } from '../components/Feedback';
import { Money } from '../components/Money';
import { StatusActions } from '../components/StatusActions';
import { StatusBadge } from '../components/StatusBadge';
import { StatusTimeline } from '../components/StatusTimeline';
import { useAsync } from '../hooks/useAsync';
import { formatDateTime, formatPercent, humanize } from '../lib/format';

export function ApplicationDetailPage() {
  const { id = '' } = useParams();
  const { isAdmin } = useAuth();
  const location = useLocation();
  const justCreated = (location.state as { created?: boolean } | null)?.created === true;
  const { data: app, error, loading, reload, setData } = useAsync((signal) => getApplication(id, signal), [id]);

  const backLink = isAdmin ? (
    <Link to="/admin/applications">← All applications</Link>
  ) : (
    <Link to="/applications">← My applications</Link>
  );

  if (loading && !app) return <Loading />;
  if (error || !app) {
    return (
      <>
        {backLink}
        <ErrorBanner error={error} onRetry={reload} />
      </>
    );
  }

  const usedValue = Math.min(app.declaredValue, app.valuationAmount);
  const ftv = usedValue > 0 ? app.financeAmount / usedValue : null;

  return (
    <>
      {backLink}
      <div className="page-header">
        <h1>
          {app.propertyReference} <StatusBadge status={app.status} />
        </h1>
      </div>
      {justCreated && (
        <SuccessBanner>
          Application submitted. application-service saved it and published an <code>application-submitted</code> event
          to Kafka.
        </SuccessBanner>
      )}

      <section className="card">
        <h2>Status</h2>
        <StatusTimeline application={app} />
        {app.statusReason && (
          <p>
            <strong>Reason:</strong> {app.statusReason}
          </p>
        )}
        {isAdmin && <StatusActions application={app} onUpdated={setData} />}
      </section>

      <div className="grid-2">
        <section className="card">
          <h2>Property</h2>
          <dl className="dl">
            <dt>Reference</dt>
            <dd>{app.propertyReference}</dd>
            <dt>City</dt>
            <dd>{app.city}</dd>
            <dt>Type</dt>
            <dd>{humanize(app.propertyType)}</dd>
            <dt>Declared value</dt>
            <dd>
              <Money amount={app.declaredValue} />
            </dd>
            <dt>Independent valuation</dt>
            <dd>
              <Money amount={app.valuationAmount} />
            </dd>
          </dl>
        </section>

        <section className="card">
          <h2>Finance</h2>
          <dl className="dl">
            <dt>Finance amount</dt>
            <dd>
              <Money amount={app.financeAmount} />
            </dd>
            <dt>Finance-to-value</dt>
            <dd>{ftv === null ? '—' : `${formatPercent(ftv)} of ${usedValue === app.valuationAmount ? 'valuation' : 'declared value'}`}</dd>
            <dt>Tenure</dt>
            <dd>
              {app.tenureMonths} months ({(app.tenureMonths / 12).toFixed(1)} years)
            </dd>
            <dt>Profit rate</dt>
            <dd>{formatPercent(app.profitRate)} p.a.</dd>
            <dt>Monthly installment</dt>
            <dd>
              <strong>
                <Money amount={app.monthlyInstallment} />
              </strong>
            </dd>
          </dl>
        </section>
      </div>

      <p className="muted small">
        Created {formatDateTime(app.createdAt)} · Updated {formatDateTime(app.updatedAt)} · ID <code>{app.id}</code>
      </p>
    </>
  );
}
