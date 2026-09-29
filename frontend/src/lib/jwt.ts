/**
 * Decode (NOT verify) a JWT payload. Verification is the backend's job: every service
 * checks the RS256 signature against auth-service's JWKS. The browser only reads the
 * claims to drive the UI (who am I, which role, when does it expire).
 */
import type { Role } from '../api/types';

/** Claims auth-service puts in the access token (com.homefin.auth.auth.TokenService). */
export interface JwtClaims {
  /** user uuid */
  sub: string;
  email: string;
  roles: Role[];
  /** expiry, seconds since epoch */
  exp: number;
  iss?: string;
  aud?: string | string[];
  iat?: number;
}

function base64UrlDecode(segment: string): string {
  const base64 = segment.replace(/-/g, '+').replace(/_/g, '/');
  const padded = base64 + '='.repeat((4 - (base64.length % 4)) % 4);
  // atob gives a "binary string"; convert to bytes then UTF-8 decode (names may be non-ASCII)
  const bytes = Uint8Array.from(atob(padded), (c) => c.charCodeAt(0));
  return new TextDecoder().decode(bytes);
}

export function decodeJwt(token: string): JwtClaims | null {
  const payload = token.split('.')[1];
  if (!payload) return null;
  try {
    const claims = JSON.parse(base64UrlDecode(payload)) as Partial<JwtClaims>;
    if (typeof claims.sub !== 'string' || typeof claims.exp !== 'number') return null;
    return {
      ...claims,
      sub: claims.sub,
      exp: claims.exp,
      email: claims.email ?? '',
      roles: Array.isArray(claims.roles) ? claims.roles : [],
    };
  } catch {
    return null;
  }
}

export function isExpired(claims: JwtClaims, now = Date.now()): boolean {
  return claims.exp * 1000 <= now;
}
