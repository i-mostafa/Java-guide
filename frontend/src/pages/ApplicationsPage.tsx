/**
 * /applications → GET /api/applications?page=&size=&sort=createdAt,desc (application-service).
 * The service filters by the JWT `sub` claim, so customers only ever see their own rows.
 */
import { Link } from 'react-router';
import { listMyApplications } from '../api/applications';
import { ApplicationsTable } from '../components/ApplicationsTable';
import { ErrorBanner } from '../components/ErrorBanner';
import { EmptyState, Loading } from '../components/Feedback';
import { Pagination } from '../components/Pagination';
import { useAsync } from '../hooks/useAsync';
import { usePageParams } from '../hooks/usePageParams';

const PAGE_SIZE = 10;

export function ApplicationsPage() {
  const { page, setPage } = usePageParams();
  const { data, error, loading, reload } = useAsync((signal) => listMyApplications({ page, size: PAGE_SIZE }, signal), [page]);

  return (
    <>
      <div className="page-header">
        <h1>My applications</h1>
        <Link to="/applications/new" className="btn btn-primary">
          New application
        </Link>
      </div>
      <section className="card">
        {loading && !data ? (
          <Loading />
        ) : error ? (
          <ErrorBanner error={error} onRetry={reload} />
        ) : data && data.content.length > 0 ? (
          <>
            <ApplicationsTable applications={data.content} />
            <Pagination page={data.page} totalPages={data.totalPages} totalElements={data.totalElements} onChange={setPage} />
          </>
        ) : (
          <EmptyState title="You haven't applied yet">
            <p className="muted">Complete KYC on your profile, then start an application.</p>
          </EmptyState>
        )}
      </section>
    </>
  );
}
