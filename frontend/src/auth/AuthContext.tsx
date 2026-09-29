/**
 * Holds the access token issued by auth-service (POST /api/auth/login) and the claims
 * decoded from it.
 *
 * Storage trade-off: the token lives in sessionStorage so a page refresh keeps you logged
 * in, while closing the tab logs you out. Anything in Web Storage is readable by JavaScript,
 * so an XSS bug could steal it. The more robust pattern is an httpOnly, Secure, SameSite
 * cookie set by the backend (JS can't read it; needs CSRF protection) or a BFF that keeps
 * tokens server-side. The backend here issues bearer tokens in the response body, so for
 * this demo sessionStorage + a short 15-minute TTL is the pragmatic choice.
 */
import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react';
import * as authApi from '../api/auth';
import { configureApiClient } from '../api/client';
import type { Role } from '../api/types';
import { decodeJwt, isExpired, type JwtClaims } from '../lib/jwt';

const STORAGE_KEY = 'homefin.accessToken';

export interface AuthUser {
  id: string;
  email: string;
  roles: Role[];
  expiresAt: Date;
}

interface AuthContextValue {
  user: AuthUser | null;
  isAdmin: boolean;
  login: (email: string, password: string) => Promise<AuthUser>;
  logout: (notice?: string) => void;
  /** A one-off message for the login page ("your session expired", ...). */
  notice: string | null;
  clearNotice: () => void;
}

const AuthContext = createContext<AuthContextValue | null>(null);

interface Session {
  token: string;
  claims: JwtClaims;
}

function readStoredSession(): Session | null {
  try {
    const token = sessionStorage.getItem(STORAGE_KEY);
    if (!token) return null;
    const claims = decodeJwt(token);
    if (!claims || isExpired(claims)) {
      sessionStorage.removeItem(STORAGE_KEY);
      return null;
    }
    return { token, claims };
  } catch {
    return null; // storage blocked (privacy mode) -> just start logged out
  }
}

function toUser(claims: JwtClaims): AuthUser {
  return { id: claims.sub, email: claims.email, roles: claims.roles, expiresAt: new Date(claims.exp * 1000) };
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(readStoredSession);
  const [notice, setNotice] = useState<string | null>(null);

  // The API client reads the token through this ref, so it always sees the latest value
  // without us re-configuring it on every render.
  const tokenRef = useRef<string | null>(session?.token ?? null);
  tokenRef.current = session?.token ?? null;

  const logout = useCallback((message?: string) => {
    try {
      sessionStorage.removeItem(STORAGE_KEY);
    } catch {
      /* ignore */
    }
    setSession(null);
    setNotice(message ?? null);
  }, []);

  useEffect(() => {
    configureApiClient({
      getToken: () => tokenRef.current,
      onUnauthorized: () => {
        // Only react if we *thought* we were logged in (avoids loops on public pages).
        if (!tokenRef.current) return;
        tokenRef.current = null;
        logout(
          'Your session is no longer valid, please sign in again. ' +
            '(Tokens expire after 15 minutes, and in dev they are also invalidated when auth-service restarts.)',
        );
      },
    });
  }, [logout]);

  // Auto-logout exactly when the JWT `exp` claim is reached.
  useEffect(() => {
    if (!session) return;
    const ms = session.claims.exp * 1000 - Date.now();
    const id = setTimeout(() => logout('Your session expired, please sign in again.'), Math.max(0, ms));
    return () => clearTimeout(id);
  }, [session, logout]);

  const login = useCallback(async (email: string, password: string) => {
    const { accessToken } = await authApi.login({ email, password });
    const claims = decodeJwt(accessToken);
    if (!claims) throw new Error('Received an unreadable token from auth-service');
    try {
      sessionStorage.setItem(STORAGE_KEY, accessToken);
    } catch {
      /* storage blocked: still works for this page view */
    }
    tokenRef.current = accessToken;
    setSession({ token: accessToken, claims });
    setNotice(null);
    return toUser(claims);
  }, []);

  const value = useMemo<AuthContextValue>(() => {
    const user = session ? toUser(session.claims) : null;
    return {
      user,
      isAdmin: user?.roles.includes('ADMIN') ?? false,
      login,
      logout,
      notice,
      clearNotice: () => setNotice(null),
    };
  }, [session, login, logout, notice]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside <AuthProvider>');
  return ctx;
}
