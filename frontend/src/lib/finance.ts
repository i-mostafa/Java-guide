/**
 * Client-side copy of the maths in application-service, used ONLY for the live estimate
 * on the "new application" form. The server recomputes everything with BigDecimal
 * (InstallmentCalculator, MathContext.DECIMAL64, HALF_EVEN to 2 dp) and its numbers win.
 */

/** FinanceProperties.annualProfitRate (config-server: 0.0499). */
export const ANNUAL_PROFIT_RATE = 0.0499;
/** FinanceProperties.maxFinanceToValue (0.80). */
export const MAX_FINANCE_TO_VALUE = 0.8;

export const MIN_PROPERTY_VALUE = 100_000;
export const MIN_FINANCE_AMOUNT = 50_000;
export const MIN_TENURE_MONTHS = 12;
export const MAX_TENURE_MONTHS = 300;

/**
 * Annuity formula: P * r * (1+r)^n / ((1+r)^n - 1), with r = annualRate / 12.
 * Returns 0 for incomplete input so the UI can just render "—".
 */
export function monthlyInstallment(principal: number, months: number, annualRate = ANNUAL_PROFIT_RATE): number {
  if (!(principal > 0) || !(months > 0)) return 0;
  const r = annualRate / 12;
  if (r === 0) return round2(principal / months);
  const growth = (1 + r) ** months;
  return round2((principal * r * growth) / (growth - 1));
}

/** finance / value, e.g. 0.75. Returns null when value is unknown. */
export function financeToValue(financeAmount: number, propertyValue: number): number | null {
  if (!(propertyValue > 0) || !(financeAmount >= 0)) return null;
  return financeAmount / propertyValue;
}

function round2(n: number): number {
  return Math.round(n * 100) / 100;
}
