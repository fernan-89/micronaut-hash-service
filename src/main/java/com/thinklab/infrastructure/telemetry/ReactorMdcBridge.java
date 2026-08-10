package com.thinklab.infrastructure.telemetry;

import org.reactivestreams.Subscription;
import org.slf4j.MDC;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Operators;
import reactor.util.context.Context;

/**
 * Infrastructure Component: Project Reactor to SLF4J MDC Bridge.
 *
 * <p><b>Architectural Role:</b>
 * Automatically synchronizes Project Reactor's reactive execution context ({@code Context}) with SLF4J's
 * thread-local Mapped Diagnostic Context ({@code MDC}) across asynchronous execution and thread boundaries
 * (e.g., from Netty event loops to worker thread pools like {@code andler-executor}).
 *
 * <p><b>Telemetry Continuity (ADR-003 & ADR-008):</b>
 * Solves the thread-hopping limitation of ThreadLocal-based loggers under non-blocking reactive streams,
 * guaranteeing that forensic correlation identifiers (`traceId`, `clientIp`, `userAgent`) remain persistent
 * and traceable in all application logs.
 *
 * <p><b>Contractual Obligations:</b>
 * <ul>
 * <li><b>Global Propagation:</b> Intercepts every reactive operator execution lifecycle stage via Reactor Hooks.</li>
 * <li><b>Thread-Context Isolation:</b> Ensures MDC keys are populated prior to signal delivery and safely purged or updated.</li>
 * <li><b>Zero-Allocation Footprint:</b> Lightweight wrapper implementation designed for high-throughput microservices.</li>
 * </ul>
 *
 * @author ThinkLab
 * @version 1.3.0
 * @since 1.0
 */
public final class ReactorMdcBridge {

    /**
     * Defines the exact telemetry matrix keys synchronized between Reactor and SLF4J MDC.
     */
    private static final String[] MDC_KEYS = {"traceId", "clientIp", "userAgent"};

    /**
     * Private constructor to prevent instantiation of utility class.
     * Throws an exception to enforce strict boundary constraints.
     */
    private ReactorMdcBridge() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated.");
    }

    /**
     * Registers the global Project Reactor operator hook to propagate tracing context into SLF4J MDC.
     * Must be invoked during the application bootstrap sequence.
     */
    public static void register() {
        Hooks.onEachOperator("thinklab-mdc-bridge", Operators.lift((scannable, subscriber) -> new MdcCoreSubscriber<>(subscriber)));
    }

    /**
     * Custom CoreSubscriber wrapper that synchronizes Reactor Context variables into SLF4J MDC
     * before processing reactive signals across thread boundaries.
     *
     * @param <T> The generic type of the upstream/downstream subscriber stream.
     */
    private static class MdcCoreSubscriber<T> implements CoreSubscriber<T> {

        private final CoreSubscriber<T> actual;

        /**
         * Constructs a new MDC-aware core subscriber wrapper.
         *
         * @param actual The downstream target subscriber instance. Must not be null.
         */
        MdcCoreSubscriber(CoreSubscriber<T> actual) {
            this.actual = actual;
        }

        /**
         * Passes the subscription lifecycle signal downstream.
         *
         * @param s The reactive subscription token.
         */
        @Override
        public void onSubscribe(Subscription s) {
            actual.onSubscribe(s);
        }

        /**
         * Synchronizes MDC context and forwards the next element signal.
         *
         * @param t The emitted data item.
         */
        @Override
        public void onNext(T t) {
            syncMdc(actual.currentContext());
            actual.onNext(t);
        }

        /**
         * Synchronizes MDC context and forwards the error signal downstream.
         *
         * @param t The caught exception or throwable.
         */
        @Override
        public void onError(Throwable t) {
            syncMdc(actual.currentContext());
            actual.onError(t);
        }

        /**
         * Synchronizes MDC context and forwards the completion signal downstream.
         */
        @Override
        public void onComplete() {
            syncMdc(actual.currentContext());
            actual.onComplete();
        }

        /**
         * Exposes the current reactive context chain.
         *
         * @return The active Reactor Context.
         */
        @Override
        public Context currentContext() {
            return actual.currentContext();
        }

        /**
         * Synchronizes all registered forensic telemetry keys from the Reactor Context
         * to the current physical thread's MDC. Clears missing keys to prevent data leakage across pooled threads.
         *
         * @param context The reactive execution context.
         */
        private void syncMdc(Context context) {
            for (String key : MDC_KEYS) {
                if (context.hasKey(key)) {
                    MDC.put(key, context.get(key));
                } else {
                    MDC.remove(key);
                }
            }
        }
    }
}