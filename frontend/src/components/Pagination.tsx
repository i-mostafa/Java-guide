interface Props {
  /** zero-based, as returned by PageResponse.page */
  page: number;
  totalPages: number;
  totalElements: number;
  onChange: (page: number) => void;
}

export function Pagination({ page, totalPages, totalElements, onChange }: Props) {
  if (totalPages <= 1) {
    return <p className="pagination muted">{totalElements} result{totalElements === 1 ? '' : 's'}</p>;
  }
  return (
    <nav className="pagination" aria-label="Pagination">
      <button type="button" className="btn btn-small" disabled={page <= 0} onClick={() => onChange(page - 1)}>
        ← Previous
      </button>
      <span aria-live="polite">
        Page {page + 1} of {totalPages} · {totalElements} results
      </span>
      <button
        type="button"
        className="btn btn-small"
        disabled={page >= totalPages - 1}
        onClick={() => onChange(page + 1)}
      >
        Next →
      </button>
    </nav>
  );
}
