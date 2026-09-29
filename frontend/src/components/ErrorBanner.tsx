import type { ReactNode } from 'react';
import { ApiError } from '../api/client';

const JAEGER_URL = 'http://localhost:16686';

/** Human-friendly headline per HTTP status, before the server's own `detail`. */
function headline(err: ApiError): string {
  switch (err.status) {
    case 0:
      return 'Network error';
    case 403:
      return "You're not allowed to do that";
    case 404:
      return 'Not found';
    case 409:
      return 'Conflict';
    case 422:
      return 'Request not accepted';
    case 502:
    case 503:
    case 504:
      return 'A dependency is unavailable, please try again';
    default:
      return err.status >= 500 ? 'Something went wrong on the server' : 'Request failed';
  }
}

/**
 * Renders any thrown error. For ApiError (RFC 9457 ProblemDetail) it shows the server's
 * `detail`, any `globalErrors` (class-level constraint failures such as FinanceRatioValidator),
 * and the `traceId` so the request can be found in Jaeger.
 */
export function ErrorBanner({ error, onRetry }: { error: unknown; onRetry?: () => void }) {
  if (!error) return null;

  if (!(error instanceof ApiError)) {
    const message = error instanceof Error ? error.message : String(error);
    return (
      <div className="banner banner-error" role="alert">
        <strong>Unexpected error</strong>
        <p>{message}</p>
      </div>
    );
  }

  const { problem } = error;
  return (
    <div className="banner banner-error" role="alert">
      <strong>{headline(error)}</strong>
      {problem.detail && <p>{problem.detail}</p>}
      {problem.globalErrors && problem.globalErrors.length > 0 && (
        <ul>
          {problem.globalErrors.map((g) => (
            <li key={g}>{g}</li>
          ))}
        </ul>
      )}
      <p className="banner-meta">
        <code>{problem.title}</code>
        {problem.status > 0 && <> · HTTP {problem.status}</>}
        {problem.traceId && (
          <>
            {' '}
            · Reference:{' '}
            <a href={`${JAEGER_URL}/trace/${problem.traceId}`} target="_blank" rel="noreferrer">
              <code>{problem.traceId}</code>
            </a>
          </>
        )}
      </p>
      {onRetry && (
        <button type="button" className="btn btn-small" onClick={onRetry}>
          Try again
        </button>
      )}
    </div>
  );
}

export function SuccessBanner({ children }: { children: ReactNode }) {
  return (
    <div className="banner banner-success" role="status">
      {children}
    </div>
  );
}
