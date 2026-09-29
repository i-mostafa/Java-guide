/**
 * Tiny typed wrapper around `fetch`.
 *
 * Every request goes to a *relative* URL (`/api/...`). In dev, Vite proxies it to the
 * API gateway on :8080; in Docker, nginx does. The gateway (Spring Cloud Gateway)
 * routes by path prefix:
 *    /api/auth/**          -> auth-service
 *    /api/customers/**     -> customer-service
 *    /api/applications/**  -> application-service
 * Each service validates the JWT itself (Spring Security resource server, keys fetched
 * from auth-service's /.well-known/jwks.json), so the gateway just passes the header on.
 */
import type { FieldError, ProblemDetail } from './types';

/** Error thrown for any non-2xx response. Wraps the server's RFC 9457 ProblemDetail. */
export class ApiError extends Error {
  readonly status: number;
  readonly problem: ProblemDetail;

  constructor(problem: ProblemDetail) {
    super(problem.detail ?? problem.title);
    this.name = 'ApiError';
    this.status = problem.status;
    this.problem = problem;
  }

  /** The machine-readable code, e.g. "kyc-required", "validation-failed". */
  get code(): string {
    return this.problem.title;
  }

  get traceId(): string | undefined {
    return this.problem.traceId;
  }

  /** Bean-validation errors keyed by the Java field name (same names as our form fields). */
  fieldErrors(): Record<string, string> {
    const out: Record<string, string> = {};
    for (const e of this.problem.errors ?? ([] as FieldError[])) {
      // keep the first message per field; Spring can emit several (e.g. @NotBlank + @Pattern)
      out[e.field] ??= e.message;
    }
    return out;
  }
}

// --- wiring set up by AuthContext -------------------------------------------------

interface ClientConfig {
  getToken: () => string | null;
  /** Called on any 401 from an authenticated call (expired / invalid token). */
  onUnauthorized: () => void;
}

let config: ClientConfig = { getToken: () => null, onUnauthorized: () => {} };

export function configureApiClient(next: ClientConfig): void {
  config = next;
}

// --- request ------------------------------------------------------------------------

type Query = Record<string, string | number | boolean | null | undefined>;

export interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  body?: unknown;
  query?: Query;
  signal?: AbortSignal;
  /**
   * Don't treat 401 as "session expired". Used by /api/auth/login where 401 simply
   * means wrong email/password.
   */
  skipAuthHandling?: boolean;
}

function buildUrl(path: string, query?: Query): string {
  if (!query) return path;
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(query)) {
    // skip empty filters so we don't send `?status=` (Spring would fail to bind "" to an enum)
    if (value !== undefined && value !== null && value !== '') params.set(key, String(value));
  }
  const qs = params.toString();
  return qs ? `${path}?${qs}` : path;
}

async function toProblem(res: Response): Promise<ProblemDetail> {
  const contentType = res.headers.get('content-type') ?? '';
  if (contentType.includes('json')) {
    try {
      const body = (await res.json()) as Partial<ProblemDetail>;
      return { ...body, title: body.title ?? res.statusText, status: body.status ?? res.status };
    } catch {
      /* fall through: malformed body */
    }
  }
  // e.g. the gateway returning a plain 502/504 page while a service is starting
  return {
    title: res.status === 502 || res.status === 504 ? 'gateway-error' : 'http-error',
    status: res.status,
    detail: res.statusText || `Request failed with status ${res.status}`,
  };
}

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', body, query, signal, skipAuthHandling = false } = options;

  const headers: Record<string, string> = { Accept: 'application/json, application/problem+json' };
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  const token = config.getToken();
  if (token) headers.Authorization = `Bearer ${token}`;

  let res: Response;
  try {
    res = await fetch(buildUrl(path, query), {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal,
    });
  } catch (err) {
    if (err instanceof DOMException && err.name === 'AbortError') throw err;
    // fetch only rejects on network failure: gateway not running, proxy can't connect...
    throw new ApiError({
      title: 'network-error',
      status: 0,
      detail: 'Could not reach the server. Is the backend running (docker compose up)?',
    });
  }

  if (!res.ok) {
    const problem = await toProblem(res);
    if (res.status === 401 && !skipAuthHandling) config.onUnauthorized();
    throw new ApiError(problem);
  }

  if (res.status === 204 || res.headers.get('content-length') === '0') {
    return undefined as T;
  }
  return (await res.json()) as T;
}

/** True for errors caused by our own AbortController (component unmounted, deps changed). */
export function isAbortError(err: unknown): boolean {
  return err instanceof DOMException && err.name === 'AbortError';
}
