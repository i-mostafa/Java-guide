/**
 * customer-service — com.homefin.customer.customer.CustomerController (/api/customers).
 */
import { request } from './client';
import type {
  CustomerResponse,
  KycStatus,
  PageQuery,
  PageResponse,
  UpdateProfileRequest,
} from './types';

/**
 * GET /api/customers/me (role CUSTOMER). Looks the profile up by the JWT `sub` claim.
 * 404 right after registration: the profile is created by UserRegisteredListener
 * (Kafka consumer) a moment after auth-service publishes the event.
 */
export function getMyProfile(signal?: AbortSignal): Promise<CustomerResponse> {
  return request<CustomerResponse>('/api/customers/me', { signal });
}

/**
 * PUT /api/customers/me. Server validates E.164 phone, 18+ date of birth (@Adult custom
 * constraint) and national id format. The national id is never returned in full (the
 * CustomerMapper masks it), so the form must re-send it on every save; once KYC is VERIFIED
 * the Customer entity refuses a *different* value -> 422 "national-id-locked".
 */
export function updateMyProfile(body: UpdateProfileRequest): Promise<CustomerResponse> {
  return request<CustomerResponse>('/api/customers/me', { method: 'PUT', body });
}

/**
 * POST /api/customers/me/kyc (no body). customer-service calls the 3rd-party KYC provider
 * (WireMock in dev) through a Resilience4j retry + circuit breaker.
 *  - 422 "profile-incomplete" if phone / date of birth / national id are missing
 *  - national id starting "000" -> provider says REJECTED (200 with kycStatus REJECTED)
 *  - national id starting "999" -> provider outage -> 503 after retries / open circuit
 */
export function verifyMyIdentity(): Promise<CustomerResponse> {
  return request<CustomerResponse>('/api/customers/me/kyc', { method: 'POST' });
}

/** GET /api/customers?kycStatus=&page=&size=&sort= (role ADMIN). */
export function listCustomers(
  params: PageQuery & { kycStatus?: KycStatus | '' },
  signal?: AbortSignal,
): Promise<PageResponse<CustomerResponse>> {
  return request<PageResponse<CustomerResponse>>('/api/customers', {
    query: { sort: 'createdAt,desc', ...params },
    signal,
  });
}

/** GET /api/customers/{id} (role ADMIN). */
export function getCustomer(id: string, signal?: AbortSignal): Promise<CustomerResponse> {
  return request<CustomerResponse>(`/api/customers/${encodeURIComponent(id)}`, { signal });
}
