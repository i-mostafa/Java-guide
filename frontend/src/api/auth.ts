/**
 * auth-service — com.homefin.auth.auth.AuthController (mapped at /api/auth).
 * Users live in auth-service's own Postgres schema; passwords are BCrypt-hashed.
 * Tokens are RS256 JWTs signed with a key pair the service generates at startup in dev
 * (published at /.well-known/jwks.json), which is why a restart invalidates every token.
 */
import { request } from './client';
import type { LoginRequest, RegisterRequest, TokenResponse, UserResponse } from './types';

/**
 * POST /api/auth/register -> 201 UserResponse.
 * Server side: bean validation (password strength etc.), 409 "email-taken" if the email exists, then after
 * the DB commit a `homefin.auth.user-registered.v1` Kafka event is published. customer-service
 * consumes it and creates the customer profile *asynchronously* (see useMyCustomer).
 */
export function register(body: RegisterRequest): Promise<UserResponse> {
  return request<UserResponse>('/api/auth/register', { method: 'POST', body });
}

/** POST /api/auth/login -> 200 TokenResponse; 401 "bad credentials" (not a session expiry). */
export function login(body: LoginRequest): Promise<TokenResponse> {
  return request<TokenResponse>('/api/auth/login', { method: 'POST', body, skipAuthHandling: true });
}

/** GET /api/auth/me -> the user behind the bearer token. */
export function me(signal?: AbortSignal): Promise<UserResponse> {
  return request<UserResponse>('/api/auth/me', { signal });
}
