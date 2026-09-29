package com.homefin.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
// Project Reactor types. Mono<T> = an async value of 0 or 1 items, roughly a lazy Promise<T>.
// (Flux<T> is the 0..N version, similar to an RxJS Observable.)
import reactor.core.publisher.Mono;

/**
 * A GlobalFilter runs for every routed request - similar to an Express middleware
 * registered with app.use(). Note the reactive style: we return a Mono and hook
 * into its completion instead of blocking.
 *
 * <p>Express analogy:
 * {@code app.use((req, res, next) => { const t = Date.now(); res.on('finish', () => log(...)); next(); })}.
 * Spring Cloud Gateway finds this bean and calls {@code filter(...)} for every proxied request.
 */
// @Slf4j (Lombok, compile time): generates a "log" field (an SLF4J logger) for this class.
@Slf4j
// @Component (Spring, runtime): "create one instance of this class and put it in the DI container".
// Found by component scanning. The gateway then picks up every GlobalFilter bean automatically.
@Component
// "implements" = this class fulfils these interfaces (like TS "implements"). A Java class can
// implement several interfaces but extend only one class.
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    // Mono<Void> = "an async operation with no result value", like Promise<void>.
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // System.nanoTime() is a monotonic clock, like process.hrtime.bigint().
        long start = System.nanoTime();
        // "var" = local type inference (like TS "const x = ..." without an annotation).
        // The type is still static - it's inferred as ServerHttpRequest.
        var request = exchange.getRequest();
        // chain.filter(exchange) is "next()": it forwards the request downstream. doFinally runs the
        // lambda when the response completes, errors or is cancelled (like promise.finally).
        return chain.filter(exchange).doFinally(signal -> {
            // Underscores in number literals are just digit separators, as in JS (1_000_000).
            // Integer division: long / long truncates, like Math.trunc(a / b).
            long ms = (System.nanoTime() - start) / 1_000_000;
            log.info("{} {} -> {} ({} ms)", request.getMethod(), request.getPath(),
                    exchange.getResponse().getStatusCode(), ms);
        });
    }

    // From the Ordered interface: lower value = runs earlier. HIGHEST_PRECEDENCE runs this filter
    // first, so the measured time includes every other filter.
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
