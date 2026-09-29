package com.homefin.common.web;

import org.springframework.data.domain.Page;

import java.util.List;
// Function<A, B> is the JDK's type for a one-argument function, i.e. TS (a: A) => B.
import java.util.function.Function;

/**
 * Stable JSON shape for paginated responses. Don't return Spring's Page/PageImpl directly:
 * its JSON structure is an implementation detail (Spring Data warns about it).
 *
 * <p>Generic record: {@code PageResponse<T>} is like TS {@code type PageResponse<T> = { content: T[]; ... }}.
 * Controllers return it and Jackson serializes it as
 * {@code {"content":[...],"page":0,"size":20,"totalElements":42,"totalPages":3}}.
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    // A generic static method: "<E, T>" before the return type declares the method's own type
    // parameters, like TS function of<E, T>(page: Page<E>, mapper: (e: E) => T): PageResponse<T>.
    // Typical call: PageResponse.of(customerPage, mapper::toResponse) - "mapper::toResponse" is a
    // method reference, shorthand for the lambda c -> mapper.toResponse(c).
    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        // new PageResponse<>(...): the empty "<>" (diamond) lets Java infer T from the context.
        return new PageResponse<>(page.getContent().stream().map(mapper).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
