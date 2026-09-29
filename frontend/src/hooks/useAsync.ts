import { useCallback, useEffect, useState, type DependencyList } from 'react';
import { isAbortError } from '../api/client';

export interface AsyncState<T> {
  data: T | undefined;
  error: unknown;
  loading: boolean;
  /** Re-run the loader (e.g. after a mutation). */
  reload: () => void;
  /** Replace the data locally (e.g. with the response of a PUT) without refetching. */
  setData: (data: T) => void;
}

/**
 * Minimal data-fetching hook: runs `load` whenever `deps` change, aborts the in-flight
 * request on change/unmount (so StrictMode's double effect and fast pagination clicks
 * never race), and exposes loading / error / data.
 */
export function useAsync<T>(load: (signal: AbortSignal) => Promise<T>, deps: DependencyList): AsyncState<T> {
  const [data, setData] = useState<T>();
  const [error, setError] = useState<unknown>();
  const [loading, setLoading] = useState(true);
  const [tick, setTick] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError(undefined);
    load(controller.signal)
      .then((result) => {
        if (!controller.signal.aborted) setData(result);
      })
      .catch((err: unknown) => {
        if (!controller.signal.aborted && !isAbortError(err)) setError(err);
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
    // `load` is intentionally not a dependency: callers pass an inline arrow + explicit deps.
  }, [...deps, tick]);

  const reload = useCallback(() => setTick((t) => t + 1), []);
  return { data, error, loading, reload, setData };
}
