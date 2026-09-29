import type { ReactNode } from 'react';

export function Loading({ label = 'Loading…' }: { label?: string }) {
  return (
    <div className="loading" role="status" aria-live="polite">
      <span className="spinner" aria-hidden="true" />
      {label}
    </div>
  );
}

export function EmptyState({ title, children }: { title: string; children?: ReactNode }) {
  return (
    <div className="empty">
      <p className="empty-title">{title}</p>
      {children}
    </div>
  );
}

/** Callout used for "this is a demo, try these magic values" notes. */
export function DemoHint({ children }: { children: ReactNode }) {
  return (
    <aside className="demo-hint">
      <strong>Demo tips</strong>
      {children}
    </aside>
  );
}
