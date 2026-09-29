/** Display helpers. Money is AED; the UAE locale groups digits as 1,450,000.00. */

const aed = new Intl.NumberFormat('en-AE', {
  style: 'currency',
  currency: 'AED',
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

const aedWhole = new Intl.NumberFormat('en-AE', {
  style: 'currency',
  currency: 'AED',
  maximumFractionDigits: 0,
});

export function formatMoney(amount: number, opts: { whole?: boolean } = {}): string {
  return (opts.whole ? aedWhole : aed).format(amount);
}

const percent = new Intl.NumberFormat('en-AE', { style: 'percent', maximumFractionDigits: 2 });
export function formatPercent(fraction: number): string {
  return percent.format(fraction);
}

const dateTime = new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' });
const dateOnly = new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeZone: 'UTC' });

/** For Java `Instant`s (ISO-8601 with Z). */
export function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return '—';
  const d = new Date(iso);
  return Number.isNaN(d.getTime()) ? iso : dateTime.format(d);
}

/** For Java `LocalDate`s ("YYYY-MM-DD") — formatted in UTC so the day never shifts. */
export function formatDate(isoDate: string | null | undefined): string {
  if (!isoDate) return '—';
  const d = new Date(`${isoDate}T00:00:00Z`);
  return Number.isNaN(d.getTime()) ? isoDate : dateOnly.format(d);
}

/** "UNDER_REVIEW" -> "Under review" */
export function humanize(enumValue: string): string {
  const s = enumValue.toLowerCase().replace(/_/g, ' ');
  return s.charAt(0).toUpperCase() + s.slice(1);
}

export function shortId(uuid: string): string {
  return uuid.slice(0, 8);
}
