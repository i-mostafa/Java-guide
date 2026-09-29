/**
 * Client-side validation that mirrors the Jakarta Bean Validation annotations on the Java
 * request records. It exists for fast feedback only — the server re-validates everything
 * and its `errors[]` are always shown too (see useForm / ApiError.fieldErrors()).
 * Each function returns an error message or undefined.
 */
import {
  MAX_FINANCE_TO_VALUE,
  MAX_TENURE_MONTHS,
  MIN_FINANCE_AMOUNT,
  MIN_PROPERTY_VALUE,
  MIN_TENURE_MONTHS,
} from './finance';

type Check = string | undefined;

export function required(value: string, label = 'This field'): Check {
  return value.trim() ? undefined : `${label} is required`;
}

/** @Size(max = n) */
export function maxLength(value: string, max: number): Check {
  return value.length > max ? `must be at most ${max} characters` : undefined;
}

/** @Email — deliberately loose, like Hibernate Validator's default. */
export function email(value: string): Check {
  return required(value, 'Email') ?? (/^[^\s@]+@[^\s@]+$/.test(value) ? undefined : 'must be a valid email address');
}

/** RegisterRequest.password: @Size(min=10,max=72) + @Pattern(lower, upper, digit, symbol). */
export function password(value: string): Check {
  if (!value) return 'Password is required';
  if (value.length < 10 || value.length > 72) return 'must be between 10 and 72 characters';
  const missing: string[] = [];
  if (!/[a-z]/.test(value)) missing.push('a lowercase letter');
  if (!/[A-Z]/.test(value)) missing.push('an uppercase letter');
  if (!/\d/.test(value)) missing.push('a digit');
  if (!/[^A-Za-z0-9]/.test(value)) missing.push('a symbol');
  return missing.length ? `must contain ${missing.join(', ')}` : undefined;
}

/** UpdateProfileRequest.phoneNumber: ^\+[1-9]\d{7,14}$ */
export function e164Phone(value: string): Check {
  return (
    required(value, 'Phone number') ??
    (/^\+[1-9]\d{7,14}$/.test(value) ? undefined : 'must be in E.164 format, e.g. +971501234567')
  );
}

/** UpdateProfileRequest.dateOfBirth: @Past @Adult (18+). */
export function adultDateOfBirth(value: string, today = new Date()): Check {
  if (!value) return 'Date of birth is required';
  const [y, m, d] = value.split('-').map(Number);
  if (!y || !m || !d) return 'must be a valid date';
  const eighteenth = new Date(Date.UTC(y + 18, m - 1, d));
  const todayUtc = new Date(Date.UTC(today.getFullYear(), today.getMonth(), today.getDate()));
  return eighteenth <= todayUtc ? undefined : 'you must be at least 18 years old';
}

/** UpdateProfileRequest.nationalId: ^[0-9A-Z-]{6,20}$ */
export function nationalId(value: string): Check {
  return (
    required(value, 'National ID') ??
    (/^[0-9A-Z-]{6,20}$/.test(value) ? undefined : "must be 6-20 characters: digits, capitals or '-'")
  );
}

/** CreateApplicationRequest.propertyReference: @Size(max=50) @Pattern(^[A-Z0-9-]+$) */
export function propertyReference(value: string): Check {
  return (
    required(value, 'Property reference') ??
    maxLength(value, 50) ??
    (/^[A-Z0-9-]+$/.test(value) ? undefined : "use capitals, digits and '-'")
  );
}

export function propertyValue(value: number): Check {
  if (!Number.isFinite(value)) return 'Property value is required';
  return value >= MIN_PROPERTY_VALUE ? undefined : `must be at least ${MIN_PROPERTY_VALUE.toLocaleString('en-AE')}`;
}

/** @DecimalMin("50000") + class-level FinanceRatioValidator (<= 80% of declared value). */
export function financeAmount(value: number, propertyVal: number): Check {
  if (!Number.isFinite(value)) return 'Finance amount is required';
  if (value < MIN_FINANCE_AMOUNT) return `must be at least ${MIN_FINANCE_AMOUNT.toLocaleString('en-AE')}`;
  if (propertyVal > 0 && value / propertyVal > MAX_FINANCE_TO_VALUE) {
    return `cannot exceed ${MAX_FINANCE_TO_VALUE * 100}% of the property value`;
  }
  return undefined;
}

export function tenureMonths(value: number): Check {
  if (!Number.isInteger(value)) return 'Tenure is required';
  return value >= MIN_TENURE_MONTHS && value <= MAX_TENURE_MONTHS
    ? undefined
    : `must be between ${MIN_TENURE_MONTHS} and ${MAX_TENURE_MONTHS} months`;
}

/** Drop undefined entries so `Object.keys(errors).length === 0` means "valid". */
export function compact(errors: Record<string, Check>): Record<string, string> {
  const out: Record<string, string> = {};
  for (const [k, v] of Object.entries(errors)) if (v) out[k] = v;
  return out;
}
