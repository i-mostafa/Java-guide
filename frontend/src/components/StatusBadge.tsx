import type { ApplicationStatus, KycStatus } from '../api/types';
import { humanize } from '../lib/format';

type Tone = 'neutral' | 'info' | 'success' | 'danger' | 'warning';

const TONES: Record<ApplicationStatus | KycStatus, Tone> = {
  SUBMITTED: 'info',
  UNDER_REVIEW: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger',
  PENDING: 'neutral',
  VERIFIED: 'success',
};

export function StatusBadge({ status }: { status: ApplicationStatus | KycStatus }) {
  return <span className={`badge badge-${TONES[status]}`}>{humanize(status)}</span>;
}
