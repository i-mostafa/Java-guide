/**
 * /admin/customers → GET /api/customers?kycStatus=&page=&size=&sort=createdAt,desc
 * (customer-service, role ADMIN). National IDs are masked by the server's CustomerMapper.
 */
import { listCustomers } from '../../api/customers';
import { KYC_STATUSES, type KycStatus } from '../../api/types';
import { ErrorBanner } from '../../components/ErrorBanner';
import { EmptyState, Loading } from '../../components/Feedback';
import { SelectField } from '../../components/FormField';
import { Pagination } from '../../components/Pagination';
import { StatusBadge } from '../../components/StatusBadge';
import { useAsync } from '../../hooks/useAsync';
import { usePageParams } from '../../hooks/usePageParams';
import { formatDate, formatDateTime, humanize } from '../../lib/format';

const PAGE_SIZE = 20;

export function AdminCustomersPage() {
  const { page, filter: kycStatus, setPage, setFilter } = usePageParams<KycStatus>('kycStatus');
  const { data, error, loading, reload } = useAsync(
    (signal) => listCustomers({ kycStatus, page, size: PAGE_SIZE }, signal),
    [kycStatus, page],
  );

  return (
    <>
      <div className="page-header">
        <h1>Customers</h1>
        <div className="filter">
          <SelectField
            label="KYC status"
            value={kycStatus}
            onChange={(e) => setFilter(e.target.value as KycStatus | '')}
            options={[{ value: '', label: 'All' }, ...KYC_STATUSES.map((s) => ({ value: s, label: humanize(s) }))]}
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
            <table className="table responsive">
              <thead>
                <tr>
                  <th scope="col">Name</th>
                  <th scope="col">Email</th>
                  <th scope="col">Phone</th>
                  <th scope="col">Date of birth</th>
                  <th scope="col">National ID</th>
                  <th scope="col">KYC</th>
                  <th scope="col">Joined</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((c) => (
                  <tr key={c.id}>
                    <td data-label="Name">
                      {c.firstName} {c.lastName}
                    </td>
                    <td data-label="Email" className="break">
                      {c.email}
                    </td>
                    <td data-label="Phone">{c.phoneNumber ?? '—'}</td>
                    <td data-label="Date of birth">{formatDate(c.dateOfBirth)}</td>
                    <td data-label="National ID">{c.nationalIdMasked ?? '—'}</td>
                    <td data-label="KYC">
                      <StatusBadge status={c.kycStatus} />
                      {c.kycCheckedAt && <div className="muted small">{formatDateTime(c.kycCheckedAt)}</div>}
                    </td>
                    <td data-label="Joined">{formatDateTime(c.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            <Pagination page={data.page} totalPages={data.totalPages} totalElements={data.totalElements} onChange={setPage} />
          </>
        ) : (
          <EmptyState title="No customers match this filter" />
        )}
      </section>
    </>
  );
}
