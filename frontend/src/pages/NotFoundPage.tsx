import { Link, isRouteErrorResponse, useRouteError } from 'react-router';

export function NotFoundPage() {
  return (
    <section className="card narrow center">
      <h1>Page not found</h1>
      <p className="muted">There's nothing at this address.</p>
      <Link to="/" className="btn btn-primary">
        Go home
      </Link>
    </section>
  );
}

/** `errorElement` for the router: catches render errors so users never see a blank page. */
export function RouteErrorPage() {
  const error = useRouteError();
  const message = isRouteErrorResponse(error)
    ? `${error.status} ${error.statusText}`
    : error instanceof Error
      ? error.message
      : 'Unknown error';
  return (
    <main className="container">
      <section className="card narrow">
        <h1>Something went wrong</h1>
        <p className="muted">{message}</p>
        <a href="/" className="btn">
          Reload the app
        </a>
      </section>
    </main>
  );
}
