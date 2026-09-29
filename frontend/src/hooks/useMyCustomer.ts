import { useCallback, useEffect, useState } from 'react';
import { ApiError, isAbortError } from '../api/client';
import { getMyProfile } from '../api/customers';
import type { CustomerResponse } from '../api/types';

/** Back-off schedule while waiting for the Kafka-created profile (≈ 7.5 s in total). */
const RETRY_DELAYS_MS = [500, 1000, 2000, 4000];

function sleep(ms: number, signal: AbortSignal): Promise<void> {
  return new Promise((resolve, reject) => {
    const id = setTimeout(resolve, ms);
    signal.addEventListener(
      'abort',
      () => {
        clearTimeout(id);
        reject(new DOMException('Aborted', 'AbortError'));
      },
      { once: true },
    );
  });
}

export interface MyCustomerState {
  customer: CustomerResponse | undefined;
  loading: boolean;
  /** true while we're retrying a 404 — the profile is still being created. */
  provisioning: boolean;
  error: unknown;
  reload: () => void;
  setCustomer: (c: CustomerResponse) => void;
}

/**
 * Loads GET /api/customers/me with retry-on-404.
 *
 * Why 404 happens: registration is handled by auth-service, which publishes a
 * `homefin.auth.user-registered.v1` event to Kafka. customer-service's UserRegisteredListener
 * consumes it and inserts the customer row. That's eventual consistency — for a second or
 * two after registering, customer-service simply doesn't know the user yet.
 */
export function useMyCustomer(): MyCustomerState {
  const [customer, setCustomer] = useState<CustomerResponse>();
  const [loading, setLoading] = useState(true);
  const [provisioning, setProvisioning] = useState(false);
  const [error, setError] = useState<unknown>();
  const [tick, setTick] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    const { signal } = controller;

    async function run(): Promise<void> {
      setLoading(true);
      setError(undefined);
      for (let attempt = 0; ; attempt++) {
        try {
          const result = await getMyProfile(signal);
          setCustomer(result);
          return;
        } catch (err) {
          const delay = RETRY_DELAYS_MS[attempt];
          const notYetCreated = err instanceof ApiError && err.status === 404;
          if (!notYetCreated || delay === undefined) throw err;
          setProvisioning(true);
          await sleep(delay, signal);
        }
      }
    }

    run()
      .catch((err: unknown) => {
        if (!signal.aborted && !isAbortError(err)) setError(err);
      })
      .finally(() => {
        if (!signal.aborted) {
          setLoading(false);
          setProvisioning(false);
        }
      });
    return () => controller.abort();
  }, [tick]);

  const reload = useCallback(() => setTick((t) => t + 1), []);
  return { customer, loading, provisioning, error, reload, setCustomer };
}
