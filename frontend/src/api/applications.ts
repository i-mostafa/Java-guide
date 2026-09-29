/**
 * application-service — com.homefin.application.finance.FinanceApplicationController
 * (/api/applications). Business rules live in FinanceApplicationService.
 */
import { request } from './client';
import type {
  ApplicationResponse,
  ApplicationStatus,
  CreateApplicationRequest,
  PageQuery,
  PageResponse,
  UpdateStatusRequest,
} from './types';

/**
 * POST /api/applications (role CUSTOMER) -> 201. What happens server-side, in order:
 *  1. bean validation (+ class-level FinanceRatioValidator: finance <= 80% of declared value)
 *  2. Feign call to customer-service: KYC must be VERIFIED, else 422 "kyc-required"
 *  3. call to the valuation provider (WireMock returns 1,450,000):
 *     "UNKNOWN..." -> 422 "property-not-found", "SLOW..." -> timeout -> 503
 *  4. finance / min(declared, valuation) > 0.80 -> 422 "ftv-exceeded"
 *  5. an open application for the same property -> 409 "duplicate-application"
 *  6. installment via InstallmentCalculator, row saved, then after commit a
 *     `homefin.application.submitted.v1` Kafka event is published.
 */
export function createApplication(body: CreateApplicationRequest): Promise<ApplicationResponse> {
  return request<ApplicationResponse>('/api/applications', { method: 'POST', body });
}

/** GET /api/applications (role CUSTOMER) — only the caller's own applications. */
export function listMyApplications(
  params: PageQuery,
  signal?: AbortSignal,
): Promise<PageResponse<ApplicationResponse>> {
  return request<PageResponse<ApplicationResponse>>('/api/applications', {
    query: { sort: 'createdAt,desc', ...params },
    signal,
  });
}

/** GET /api/applications/{id} — owner or ADMIN; anyone else gets 403. */
export function getApplication(id: string, signal?: AbortSignal): Promise<ApplicationResponse> {
  return request<ApplicationResponse>(`/api/applications/${encodeURIComponent(id)}`, { signal });
}

/** GET /api/applications/admin?status=&page=&size= (role ADMIN) — everyone's applications. */
export function listAllApplications(
  params: PageQuery & { status?: ApplicationStatus | '' },
  signal?: AbortSignal,
): Promise<PageResponse<ApplicationResponse>> {
  return request<PageResponse<ApplicationResponse>>('/api/applications/admin', {
    query: { sort: 'createdAt,desc', ...params },
    signal,
  });
}

/**
 * PATCH /api/applications/{id}/status (role ADMIN). The entity enforces the state machine
 * (422 "invalid-status-transition"); `reason` is required for REJECTED (400). On success a
 * `homefin.application.status-changed.v1` Kafka event is published after commit;
 * customer-service's ApplicationStatusListener consumes it.
 */
export function updateApplicationStatus(
  id: string,
  body: UpdateStatusRequest,
): Promise<ApplicationResponse> {
  return request<ApplicationResponse>(`/api/applications/${encodeURIComponent(id)}/status`, {
    method: 'PATCH',
    body,
  });
}
