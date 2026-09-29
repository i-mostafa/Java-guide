import type { ReactNode } from 'react';
import { Link } from 'react-router';
import type { ApplicationResponse } from '../api/types';
import { formatDateTime, humanize } from '../lib/format';
import { Money } from './Money';
import { StatusBadge } from './StatusBadge';

/**
 * Table of ApplicationResponse rows. On narrow screens CSS turns each row into a card
 * using the `data-label` attributes (no horizontal scrolling).
 */
export function ApplicationsTable({
  applications,
  actions,
}: {
  applications: ApplicationResponse[];
  /** optional extra column (admin status buttons) */
  actions?: (app: ApplicationResponse) => ReactNode;
}) {
  return (
    <table className="table responsive">
      <thead>
        <tr>
          <th scope="col">Property</th>
          <th scope="col">Type</th>
          <th scope="col" className="num">Finance</th>
          <th scope="col" className="num">Monthly</th>
          <th scope="col">Status</th>
          <th scope="col">Submitted</th>
          {actions && <th scope="col">Actions</th>}
        </tr>
      </thead>
      <tbody>
        {applications.map((a) => (
          <tr key={a.id}>
            <td data-label="Property">
              <Link to={`/applications/${a.id}`}>{a.propertyReference}</Link>
              <div className="muted small">{a.city}</div>
            </td>
            <td data-label="Type">{humanize(a.propertyType)}</td>
            <td data-label="Finance" className="num">
              <Money amount={a.financeAmount} />
            </td>
            <td data-label="Monthly" className="num">
              <Money amount={a.monthlyInstallment} />
            </td>
            <td data-label="Status">
              <StatusBadge status={a.status} />
            </td>
            <td data-label="Submitted">{formatDateTime(a.createdAt)}</td>
            {actions && <td data-label="Actions">{actions(a)}</td>}
          </tr>
        ))}
      </tbody>
    </table>
  );
}
