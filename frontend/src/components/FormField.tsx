import { useId, type InputHTMLAttributes, type ReactNode, type SelectHTMLAttributes } from 'react';

interface FieldShellProps {
  label: string;
  error?: string | undefined;
  hint?: ReactNode;
  children: (ids: { inputId: string; describedBy: string | undefined }) => ReactNode;
}

/** Label + control + hint + error, with the aria wiring done once. */
function FieldShell({ label, error, hint, children }: FieldShellProps) {
  const inputId = useId();
  const hintId = `${inputId}-hint`;
  const errorId = `${inputId}-error`;
  const describedBy = [hint ? hintId : null, error ? errorId : null].filter(Boolean).join(' ') || undefined;

  return (
    <div className={`field${error ? ' field-invalid' : ''}`}>
      <label htmlFor={inputId}>{label}</label>
      {children({ inputId, describedBy })}
      {hint && (
        <p id={hintId} className="field-hint">
          {hint}
        </p>
      )}
      {error && (
        <p id={errorId} className="field-error">
          {error}
        </p>
      )}
    </div>
  );
}

type InputProps = Omit<InputHTMLAttributes<HTMLInputElement>, 'id'> & {
  label: string;
  error?: string | undefined;
  hint?: ReactNode;
};

export function TextField({ label, error, hint, ...inputProps }: InputProps) {
  return (
    <FieldShell label={label} error={error} hint={hint}>
      {({ inputId, describedBy }) => (
        <input id={inputId} aria-invalid={error ? true : undefined} aria-describedby={describedBy} {...inputProps} />
      )}
    </FieldShell>
  );
}

type SelectProps = Omit<SelectHTMLAttributes<HTMLSelectElement>, 'id'> & {
  label: string;
  error?: string | undefined;
  hint?: ReactNode;
  options: readonly { value: string; label: string }[];
};

export function SelectField({ label, error, hint, options, ...selectProps }: SelectProps) {
  return (
    <FieldShell label={label} error={error} hint={hint}>
      {({ inputId, describedBy }) => (
        <select id={inputId} aria-invalid={error ? true : undefined} aria-describedby={describedBy} {...selectProps}>
          {options.map((o) => (
            <option key={o.value} value={o.value}>
              {o.label}
            </option>
          ))}
        </select>
      )}
    </FieldShell>
  );
}
