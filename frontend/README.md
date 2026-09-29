# HomeFin web UI

React + TypeScript single-page app for the HomeFin Spring Boot microservices. It talks
**only** to the API gateway (`:8080`), which routes to `auth-service`, `customer-service`
and `application-service`.

Stack: Vite, React 19, TypeScript (strict), react-router v8 (data mode:
`createBrowserRouter` + `RouterProvider`), plain `fetch`, hand-written CSS (light/dark via
`prefers-color-scheme`). No UI kit, no react-query.

## Run in development

```bash
# 1. backend (from the repo root): infra + all services in containers
docker compose --profile apps up -d --build
#    ...or `docker compose up -d` for infra only and run the services from your IDE

# 2. frontend
cd frontend
npm install
npm run dev            # http://localhost:5173
```

The Vite dev server proxies `/api/**` and `/.well-known/**` to `http://localhost:8080`, so the
browser sees a single origin and no CORS configuration is needed (see `vite.config.ts`).

Dev admin: `admin@homefin.local` / `Admin#12345`. Register any other account to act as a customer.

| Script              | What it does                                   |
| ------------------- | ---------------------------------------------- |
| `npm run dev`       | Vite dev server on :5173 with the API proxy    |
| `npm run typecheck` | `tsc --noEmit` (strict)                        |
| `npm run build`     | type-check, then `vite build` into `dist/`     |
| `npm run preview`   | serve `dist/` on :4173 (same proxy as dev)     |

## Docker

```bash
cd frontend
docker build -t homefin-frontend .
# join the compose network so nginx can reach the `api-gateway` container
docker run --rm -p 3000:80 --network homefin_default homefin-frontend
# -> http://localhost:3000
```

The image is multi-stage: `node:22-alpine` builds the app, `nginx:1.27-alpine` serves `dist/`.
`nginx.conf` falls back to `index.html` for client-side routes and proxies `/api/` and
`/.well-known/` to `http://api-gateway:8080`, passing `Host` and `X-Forwarded-*` headers.

## Folder structure

```
src/
  api/          typed fetch client + one module per backend service
    client.ts     request<T>(), ApiError (wraps RFC 9457 ProblemDetail), 401 hook
    types.ts      TS mirrors of the Java DTO records (each names its Java class)
    auth.ts  customers.ts  applications.ts
  auth/         AuthContext (JWT in sessionStorage, auto-logout at exp), route guards
  hooks/        useAsync (fetch + abort), useMyCustomer (retry while Kafka creates the profile),
                usePageParams (page/filter in the URL)
  components/   Layout, FormField, StatusBadge, StatusActions, StatusTimeline, Pagination,
                ErrorBanner, Money, ApplicationsTable, Feedback (loading/empty/demo hint)
  lib/          finance.ts (installment formula), jwt.ts, format.ts, validation.ts
  pages/        one component per route (admin/ for ADMIN pages)
  router.tsx    route table   main.tsx  entry   styles.css  all styles
```

## Pages → backend endpoints

| Route                  | Role          | Endpoints (via gateway)                                                                                      | Server-side notes |
| ---------------------- | ------------- | ------------------------------------------------------------------------------------------------------------ | ----------------- |
| `/login`               | public        | `POST /api/auth/login`                                                                                       | BCrypt check, returns RS256 JWT (15 min). 401 = bad credentials. |
| `/register`            | public        | `POST /api/auth/register`, then `POST /api/auth/login`                                                       | Bean validation; 409 `email-taken`; publishes `homefin.auth.user-registered.v1` to Kafka. |
| `/`                    | CUSTOMER      | `GET /api/customers/me`, `GET /api/applications?size=5`                                                      | Profile is created asynchronously from the Kafka event, so the UI retries 404s with back-off. Admins are redirected to `/admin/applications`. |
| `/profile`             | CUSTOMER      | `GET` / `PUT /api/customers/me`, `POST /api/customers/me/kyc`                                                | E.164 phone, 18+ DOB, national-ID format; 422 `national-id-locked`, `profile-incomplete`. KYC calls the WireMock provider with retry + circuit breaker (`000…` → REJECTED, `999…` → 503). |
| `/applications`        | CUSTOMER      | `GET /api/applications?page=&size=&sort=createdAt,desc`                                                      | Filtered by JWT `sub`. |
| `/applications/new`    | CUSTOMER      | `GET /api/customers/me` (KYC gate), `POST /api/applications`                                                 | Feign call to customer-service for KYC (422 `kyc-required`); valuation provider (`UNKNOWN…` → 422 `property-not-found`, `SLOW…` → 503); 422 `ftv-exceeded`; 409 `duplicate-application`; publishes `homefin.application.submitted.v1`. |
| `/applications/:id`    | owner / ADMIN | `GET /api/applications/{id}` (+ `PATCH …/status` for admins)                                                 | 403 for anyone else. |
| `/admin/applications`  | ADMIN         | `GET /api/applications/admin?status=&page=&size=`, `PATCH /api/applications/{id}/status`                     | State machine SUBMITTED → UNDER_REVIEW → APPROVED/REJECTED (422 `invalid-status-transition`); `reason` required for REJECTED; publishes `homefin.application.status-changed.v1`. |
| `/admin/customers`     | ADMIN         | `GET /api/customers?kycStatus=&page=&size=&sort=createdAt,desc`                                              | National IDs are masked server-side. |
| `*`                    | any           | —                                                                                                            | 404 page. |

## Error handling

Every non-2xx response is parsed as a ProblemDetail and thrown as `ApiError`:

- `errors[]` (bean-validation failures) are mapped onto the form fields with the same names.
- `detail` is shown in a banner together with `Reference: <traceId>`, which links to Jaeger
  (http://localhost:16686).
- 401 on an authenticated call → logged out with a message. Tokens expire after 15 minutes,
  and in dev every token is invalidated when auth-service restarts (it generates new signing keys).
- 403 → "not allowed", 502/503/504 → "a dependency is unavailable, try again".
