/**
 * /admin/applications (role ADMIN)
 *  - GET   /api/applications/admin?status=&page=&size=   all customers' applications
 *  - PATCH /api/applications/{id}/status                  state-machine transition
 * Each successful PATCH publishes `homefin.application.status-changed.v1` to Kafka.
 */
import { listAllApplications } from '../../api/applications';
import { APPLICATION_STATUSES, type ApplicationStatus } from '../../api/types';
import { ApplicationsTable } from '../../components/ApplicationsTable';
import { ErrorBanner } from '../../components/ErrorBanner';
import { EmptyState, Loading } from '../../components/Feedback';
import { SelectField } from '../../components/FormField';
import { Pagination } from '../../components/Pagination';
import { StatusActions } from '../../components/StatusActions';
import { useAsync } from '../../hooks/useAsync';
import { usePageParams } from '../../hooks/usePageParams';
import { humanize } from '../../lib/format';

const PAGE_SIZE = 10;

export function AdminApplicationsPage() {
  const { page, filter: status, setPage, setFilter } = usePageParams<ApplicationStatus>('status');
  const { data, error, loading, reload } = useAsync(
    (signal) => listAllApplications({ status, page, size: PAGE_SIZE }, signal),
    [status, page],
  );

  return (
    <>
      <div className="page-header">
        <h1>Applications</h1>
        <div className="filter">
          <SelectField
            label="Status"
            value={status}
            onChange={(e) => setFilter(e.target.value as ApplicationStatus | '')}
            options={[{ value: '', label: 'All statuses' }, ...APPLICATION_STATUSES.map((s) => ({ value: s, label: humanize(s) }))]}
          />
        </div>
      </div>

      <section className="card">
        {loading && !data ? (
          <Loading />
        ) : error ? (
          <ErrorBanner error={error} onRetry={reload} />
        ) : data && data.content.length > 0 ? (
          <>
            {/* After a transition we refetch: the row may no longer match the status filter. */}
            <ApplicationsTable applications={data.content} actions={(app) => <StatusActions application={app} onUpdated={reload} />} />
            <Pagination page={data.page} totalPages={data.totalPages} totalElements={data.totalElements} onChange={setPage} />
          </>
        ) : (
          <EmptyState title={status ? `No ${humanize(status).toLowerCase()} applications` : 'No applications yet'} />
        )}
      </section>
    </>
  );
}
