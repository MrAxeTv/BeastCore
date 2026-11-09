package me.mraxetv.beastlib.api.client;

/** Owner-scoped client observation. Closing is idempotent and never blocks a network thread. */
public interface ClientEventRegistration extends AutoCloseable {
    boolean isClosed();

    @Override
    void close();
}
