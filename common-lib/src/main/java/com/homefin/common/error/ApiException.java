// "package" declares the namespace this file lives in. It MUST match the folder path
// (com/homefin/common/error). There is no default export in Java: other files refer to this
// class as com.homefin.common.error.ApiException (or import it, see below).
package com.homefin.common.error;

// "import" is like a named TS import, but it only shortens names - it does not load or run code.
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base class for exceptions that map cleanly to an HTTP status.
 * Unchecked (extends RuntimeException) - the idiomatic choice in Spring apps.
 *
 * <p>Role: services throw subclasses of this (NotFoundException, ConflictException...) anywhere in
 * their code, and {@code GlobalExceptionHandler} turns them into a JSON error response.
 * TS analogy: {@code class ApiError extends Error { constructor(public status: number, public code: string) } }
 * that an Express error middleware inspects.
 *
 * <p>Checked vs unchecked exceptions: Java forces callers to either catch or declare
 * ({@code throws X}) "checked" exceptions (subclasses of Exception). Subclasses of RuntimeException
 * are "unchecked" - they behave like JS errors: no declaration needed, they just bubble up.
 */
// @Getter (Lombok, compile time): generates a public getter for every field, here
// getStatus() and getCode(). Lombok rewrites the class while javac compiles it.
@Getter
// public = visible from any package. abstract = cannot be instantiated with "new", only extended
// (same as a TS abstract class). "extends" = single class inheritance, like in TS.
public abstract class ApiException extends RuntimeException {

    // private = only visible inside this class. final = assigned exactly once (in the constructor),
    // like a TS "readonly" field. Types come BEFORE names in Java: "HttpStatus status".
    private final HttpStatus status;
    private final String code;

    // A constructor: same name as the class, no return type. "protected" = callable from
    // subclasses (and from classes in the same package), so only subclasses can create one.
    protected ApiException(HttpStatus status, String code, String message) {
        // super(...) calls the parent (RuntimeException) constructor, just like in TS.
        super(message);
        // "this.status" is the field; "status" alone is the parameter that shadows it.
        this.status = status;
        this.code = code;
    }

    // Overloading: Java allows several constructors/methods with the same name but different
    // parameter lists (TS would need optional params or overload signatures).
    protected ApiException(HttpStatus status, String code, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.code = code;
    }
}
