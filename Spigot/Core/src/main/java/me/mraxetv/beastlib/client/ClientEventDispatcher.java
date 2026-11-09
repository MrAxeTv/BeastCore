package me.mraxetv.beastlib.client;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Bounded, one-task-per-observation admission to the native packet FIFO. */
final class ClientEventDispatcher {
    private final int limit;
    private final Consumer<Runnable> executor;
    private final BooleanSupplier active;
    private final Consumer<Throwable> failure;
    private final AtomicInteger pending = new AtomicInteger();

    ClientEventDispatcher(int limit, Consumer<Runnable> executor, BooleanSupplier active, Consumer<Throwable> failure) {
        if (limit < 1) throw new IllegalArgumentException("limit must be positive");
        this.limit = limit;
        this.executor = executor;
        this.active = active;
        this.failure = failure;
    }

    void submit(final Runnable callback) {
        if (!active.getAsBoolean()) return;
        if (pending.incrementAndGet() > limit) {
            pending.decrementAndGet();
            failure.accept(new IllegalStateException("Client event queue overflow; input stream stopped"));
            return;
        }
        try {
            executor.accept(() -> {
                pending.decrementAndGet();
                if (active.getAsBoolean()) callback.run();
            });
        } catch (RuntimeException error) {
            pending.decrementAndGet();
            failure.accept(error);
        }
    }
}
