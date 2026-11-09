package me.mraxetv.beastlib.scheduler;

import me.mraxetv.beastlib.api.scheduler.SchedulerTask;
import me.mraxetv.beastlib.api.scheduler.SchedulerTaskState;
import org.bukkit.plugin.Plugin;

final class TrackedSchedulerTask implements SchedulerTask {

    interface TerminalListener {
        void onTerminal(TrackedSchedulerTask task);
    }

    private final Plugin owner;
    private final boolean repeating;
    private final TerminalListener terminalListener;

    private volatile SchedulerTaskState state = SchedulerTaskState.SCHEDULED;
    private Runnable cancellation;

    TrackedSchedulerTask(Plugin owner, boolean repeating, TerminalListener terminalListener) {
        this.owner = owner;
        this.repeating = repeating;
        this.terminalListener = terminalListener;
    }

    @Override
    public Plugin getOwner() {
        return owner;
    }

    @Override
    public boolean isRepeating() {
        return repeating;
    }

    @Override
    public SchedulerTaskState getState() {
        return state;
    }

    @Override
    public boolean cancel() {
        Runnable cancellationToRun;
        synchronized (this) {
            if (state.isTerminal()) {
                return false;
            }
            state = SchedulerTaskState.CANCELLED;
            cancellationToRun = cancellation;
        }

        runCancellation(cancellationToRun);
        notifyTerminal();
        return true;
    }

    void bindCancellation(Runnable platformCancellation) {
        if (platformCancellation == null) {
            return;
        }

        boolean cancelImmediately = false;
        synchronized (this) {
            if (cancellation == null) {
                cancellation = platformCancellation;
                cancelImmediately = state == SchedulerTaskState.CANCELLED;
            }
        }

        if (cancelImmediately) {
            runCancellation(platformCancellation);
        }
    }

    void execute(Runnable task) {
        if (!beginExecution()) {
            return;
        }
        try {
            task.run();
        } finally {
            finishExecution();
        }
    }

    boolean retire() {
        synchronized (this) {
            if (state.isTerminal()) {
                return false;
            }
            state = SchedulerTaskState.RETIRED;
        }
        notifyTerminal();
        return true;
    }

    void reject() {
        synchronized (this) {
            if (state.isTerminal()) {
                return;
            }
            state = SchedulerTaskState.REJECTED;
        }
        notifyTerminal();
    }

    private boolean beginExecution() {
        synchronized (this) {
            if (state.isTerminal()) {
                return false;
            }
            state = SchedulerTaskState.RUNNING;
            return true;
        }
    }

    private void finishExecution() {
        boolean terminal = false;
        synchronized (this) {
            if (state.isTerminal()) {
                return;
            }
            if (repeating) {
                state = SchedulerTaskState.SCHEDULED;
            } else {
                state = SchedulerTaskState.COMPLETED;
                terminal = true;
            }
        }
        if (terminal) {
            notifyTerminal();
        }
    }

    private void notifyTerminal() {
        if (terminalListener != null) {
            terminalListener.onTerminal(this);
        }
    }

    private static void runCancellation(Runnable cancellation) {
        if (cancellation == null) {
            return;
        }
        try {
            cancellation.run();
        } catch (RuntimeException ignored) {
            // The BeastLib handle is already terminal. Platform shutdown must
            // continue even if the native scheduler rejects a late cancel.
        } catch (LinkageError ignored) {
            // A scheduler implementation may disappear while a server is
            // tearing down. The task is still terminal from BeastLib's view.
        }
    }
}
