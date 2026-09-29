import type { ApplicationResponse, ApplicationStatus } from '../api/types';
import { formatDateTime, humanize } from '../lib/format';

type StepState = 'done' | 'current' | 'upcoming' | 'rejected';

const STEP_INDEX: Record<ApplicationStatus, number> = {
  SUBMITTED: 0,
  UNDER_REVIEW: 1,
  APPROVED: 2,
  REJECTED: 2,
};

/**
 * SUBMITTED → UNDER_REVIEW → APPROVED | REJECTED.
 * The API returns only the current status plus createdAt/updatedAt (no history table), so
 * the timeline is derived: steps before the current one are "done". An application rejected
 * straight from SUBMITTED still shows the review step as passed-through.
 */
export function StatusTimeline({ application }: { application: ApplicationResponse }) {
  const { status } = application;
  const current = STEP_INDEX[status];
  const steps: ApplicationStatus[] = ['SUBMITTED', 'UNDER_REVIEW', status === 'REJECTED' ? 'REJECTED' : 'APPROVED'];

  function stateOf(index: number): StepState {
    if (index < current) return 'done';
    if (index > current) return 'upcoming';
    if (status === 'REJECTED') return 'rejected';
    return status === 'APPROVED' ? 'done' : 'current';
  }

  return (
    <ol className="timeline" aria-label="Application status">
      {steps.map((step, index) => {
        const state = stateOf(index);
        const when = index === 0 ? application.createdAt : index === current ? application.updatedAt : null;
        return (
          <li key={step} className={`timeline-step is-${state}`} aria-current={index === current ? 'step' : undefined}>
            <span className="timeline-dot" aria-hidden="true" />
            <span className="timeline-label">{humanize(step)}</span>
            <span className="timeline-meta">{when ? formatDateTime(when) : state === 'upcoming' ? 'Pending' : ''}</span>
          </li>
        );
      })}
    </ol>
  );
}
