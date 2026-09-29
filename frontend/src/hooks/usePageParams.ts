import { useSearchParams } from 'react-router';

/**
 * Keeps `page` (zero-based, like Spring's Pageable) and one optional filter in the URL
 * query string, so pagination/filters survive reloads and are shareable.
 */
export function usePageParams<F extends string = string>(filterName = 'filter') {
  const [params, setParams] = useSearchParams();
  const page = Math.max(0, Number(params.get('page') ?? 0) || 0);
  const filter = (params.get(filterName) ?? '') as F | '';

  function setPage(next: number): void {
    setParams((prev) => {
      const p = new URLSearchParams(prev);
      if (next > 0) p.set('page', String(next));
      else p.delete('page');
      return p;
    });
  }

  function setFilter(next: F | ''): void {
    setParams((prev) => {
      const p = new URLSearchParams(prev);
      if (next) p.set(filterName, next);
      else p.delete(filterName);
      p.delete('page'); // new filter -> back to first page
      return p;
    });
  }

  return { page, filter, setPage, setFilter };
}
