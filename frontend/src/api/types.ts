/**
 * TypeScript mirrors of the Java DTOs (Java `record`s) returned by the services.
 * Jackson serialises records field-by-field, so these shapes match 1:1.
 *
 * Conventions used by the backend:
 *  - UUIDs, `Instant`s (ISO-8601, e.g. "2026-09-29T10:15:30Z") and `LocalDate`s ("YYYY-MM-DD")
 *    arrive as strings.
 *  - `BigDecimal` money values arrive as JSON numbers (fine for display; the server does
 *    the authoritative maths with BigDecimal).
 *  - Java enums arrive as their constant names ("UNDER_REVIEW").
 */

// ----------------------------------------------------------------- common-lib

/** com.homefin.common.web.PageResponse<T> — a trimmed-down Spring Data `Page`. */
export interface PageResponse<T> {
  content: T[];
  /** zero-based page index */
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

/** A single bean-validation failure (from @Valid on a request body). */
export interface FieldError {
  field: string;
  message: string;
}

/**
 * RFC 9457 `ProblemDetail` built by com.homefin.common.error.GlobalExceptionHandler
 * (content-type `application/problem+json`). `title` is a stable machine code such as
 * "validation-failed", "kyc-required", "ftv-exceeded".
 */
export interface ProblemDetail {
  type?: string;
  title: string;
  status: number;
  detail?: string;
  instance?: string;
  /** OpenTelemetry trace id — look it up in Jaeger (http://localhost:16686). */
  traceId?: string;
  errors?: FieldError[];
  globalErrors?: string[];
}

// ----------------------------------------------------------------- auth-service

export type Role = 'CUSTOMER' | 'ADMIN';

/** com.homefin.auth.auth.dto.RegisterRequest */
export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}

/** com.homefin.auth.auth.dto.LoginRequest */
export interface LoginRequest {
  email: string;
  password: string;
}

/** com.homefin.auth.auth.dto.TokenResponse */
export interface TokenResponse {
  accessToken: string;
  tokenType: 'Bearer';
  /** lifetime in seconds (15 min in dev) */
  expiresIn: number;
}

/** com.homefin.auth.auth.dto.UserResponse */
export interface UserResponse {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: Role;
}

// ----------------------------------------------------------------- customer-service

/** com.homefin.customer.customer.KycStatus */
export type KycStatus = 'PENDING' | 'VERIFIED' | 'REJECTED';
export const KYC_STATUSES: readonly KycStatus[] = ['PENDING', 'VERIFIED', 'REJECTED'];

/** com.homefin.customer.customer.dto.CustomerResponse */
export interface CustomerResponse {
  id: string;
  /** the auth-service user id (JWT `sub`) this profile belongs to */
  userId: string;
  email: string;
  firstName: string;
  lastName: string;
  phoneNumber: string | null;
  dateOfBirth: string | null;
  /** server never returns the full national id, only e.g. "****5678" */
  nationalIdMasked: string | null;
  kycStatus: KycStatus;
  kycCheckedAt: string | null;
  createdAt: string;
}

/** com.homefin.customer.customer.dto.UpdateProfileRequest */
export interface UpdateProfileRequest {
  firstName: string;
  lastName: string;
  phoneNumber: string;
  dateOfBirth: string;
  nationalId: string;
}

// ----------------------------------------------------------------- application-service

/** com.homefin.application.finance.PropertyType */
export type PropertyType = 'APARTMENT' | 'VILLA' | 'TOWNHOUSE';
export const PROPERTY_TYPES: readonly PropertyType[] = ['APARTMENT', 'VILLA', 'TOWNHOUSE'];

/** com.homefin.application.finance.ApplicationStatus */
export type ApplicationStatus = 'SUBMITTED' | 'UNDER_REVIEW' | 'APPROVED' | 'REJECTED';
export const APPLICATION_STATUSES: readonly ApplicationStatus[] = [
  'SUBMITTED',
  'UNDER_REVIEW',
  'APPROVED',
  'REJECTED',
];

/**
 * Mirrors the state machine enforced server-side (ApplicationStatus / FinanceApplication).
 * The UI uses it only to decide which buttons to show; the server still rejects anything
 * else with 422 "invalid-status-transition".
 */
export const ALLOWED_TRANSITIONS: Record<ApplicationStatus, readonly ApplicationStatus[]> = {
  SUBMITTED: ['UNDER_REVIEW', 'REJECTED'],
  UNDER_REVIEW: ['APPROVED', 'REJECTED'],
  APPROVED: [],
  REJECTED: [],
};

/** com.homefin.application.finance.dto.CreateApplicationRequest */
export interface CreateApplicationRequest {
  propertyReference: string;
  city: string;
  propertyType: PropertyType;
  propertyValue: number;
  financeAmount: number;
  tenureMonths: number;
}

/** com.homefin.application.finance.dto.UpdateStatusRequest */
export interface UpdateStatusRequest {
  status: ApplicationStatus;
  reason?: string;
}

/** com.homefin.application.finance.dto.ApplicationResponse */
export interface ApplicationResponse {
  id: string;
  customerUserId: string;
  propertyReference: string;
  city: string;
  propertyType: PropertyType;
  /** what the customer typed as `propertyValue` */
  declaredValue: number;
  /** what the (WireMock) valuation provider said the property is worth */
  valuationAmount: number;
  financeAmount: number;
  tenureMonths: number;
  /** annual rate as a fraction, e.g. 0.0499 */
  profitRate: number;
  monthlyInstallment: number;
  status: ApplicationStatus;
  statusReason: string | null;
  createdAt: string;
  updatedAt: string;
}

/** Query params accepted by Spring's `Pageable` resolver. */
export interface PageQuery {
  page?: number;
  size?: number;
  /** e.g. "createdAt,desc" */
  sort?: string;
}
