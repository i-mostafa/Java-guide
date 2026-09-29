import { formatMoney } from '../lib/format';

/** AED amount; tabular digits so columns line up. */
export function Money({ amount, whole = false }: { amount: number | null | undefined; whole?: boolean }) {
  if (amount === null || amount === undefined || !Number.isFinite(amount)) return <span className="num">—</span>;
  return <span className="num">{formatMoney(amount, { whole })}</span>;
}
