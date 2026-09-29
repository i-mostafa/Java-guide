import { useId, useState, type FormEvent } from 'react';
import { ApiError } from '../api/client';
import { updateApplicationStatus } from '../api/applications';
import { ALLOWED_TRANSITIONS, type ApplicationResponse, type ApplicationStatus } from '../api/types';
import { ErrorBanner } from './ErrorBanner';

const ACTION_LABEL: Record<ApplicationStatus, string> = {
  SUBMITTED: 'Submit',
  UNDER_REVIEW: 'Start review',
  APPROVED: 'Approve',
  REJECTED: 'Reject',
};

/**
 * Admin buttons for PATCH /api/applications/{id}/status. Only transitions allowed by the
 * server-side state machine are offered. Rejecting opens an inline form because the server
 * requires `reason` for REJECTED (400 validation-failed otherwise).
 */
export function StatusActions({
  application,
  onUpdated,
}: {
  application: ApplicationResponse;
  onUpdated: (updated: ApplicationResponse) => void;
}) {
  const next = ALLOWED_TRANSITIONS[application.status];
  const [busy, setBusy] = useState(false);
  const [rejecting, setRejecting] = useState(false);
  const [reason, setReason] = useState('');
  const [reasonError, setReasonError] = useState<string>();
  const [error, setError] = useState<unknown>();
  const reasonId = useId();

  if (next.length === 0) return <span className="muted small">Final</span>;

  async function transition(status: ApplicationStatus, why?: string): Promise<void> {
    setBusy(true);
    setError(undefined);
    try {
      const updated = await updateApplicationStatus(application.id, why ? { status, reason: why } : { status });
      setRejecting(false);
      setReason('');
      onUpdated(updated);
    } catch (err) {
      setError(err);
      if (err instanceof ApiError) setReasonError(err.fieldErrors().reason);
    } finally {
      setBusy(false);
    }
  }

  function submitReject(e: FormEvent): void {
    e.preventDefault();
    const trimmed = reason.trim();
    if (!trimmed) return setReasonError('A reason is required when rejecting');
    if (trimmed.length > 500) return setReasonError('must be at most 500 characters');
    setReasonError(undefined);
    void transition('REJECTED', trimmed);
  }

  return (
    <div className="status-actions">
      {!rejecting && (
        <div className="btn-row">
          {next.map((status) =>
            status === 'REJECTED' ? (
              <button key={status} type="button" className="btn btn-small btn-danger" disabled={busy} onClick={() => setRejecting(true)}>
                {ACTION_LABEL[status]}
              </button>
            ) : (
              <button
                key={status}
                type="button"
                className={`btn btn-small ${status === 'APPROVED' ? 'btn-success' : 'btn-primary'}`}
                disabled={busy}
                onClick={() => void transition(status)}
              >
                {ACTION_LABEL[status]}
              </button>
            ),
          )}
        </div>
      )}

      {rejecting && (
        <form className="reject-form" onSubmit={submitReject} noValidate>
          <label htmlFor={reasonId}>Reason for rejection</label>
          <textarea
            id={reasonId}
            rows={2}
            maxLength={500}
            value={reason}
            autoFocus
            aria-invalid={reasonError ? true : undefined}
            aria-describedby={reasonError ? `${reasonId}-err` : undefined}
            onChange={(e) => setReason(e.target.value)}
          />
          {reasonError && (
            <p id={`${reasonId}-err`} className="field-error">
              {reasonError}
            </p>
          )}
          <div className="btn-row">
            <button type="submit" className="btn btn-small btn-danger" disabled={busy}>
              {busy ? 'Rejecting…' : 'Confirm reject'}
            </button>
            <button
              type="button"
              className="btn btn-small"
              disabled={busy}
              onClick={() => {
                setRejecting(false);
                setReasonError(undefined);
              }}
            >
              Cancel
            </button>
          </div>
        </form>
      )}
      {error !== undefined && <ErrorBanner error={error} />}
    </div>
  );
}
