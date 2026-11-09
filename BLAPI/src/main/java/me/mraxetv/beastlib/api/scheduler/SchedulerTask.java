package me.mraxetv.beastlib.api.scheduler;

import org.bukkit.plugin.Plugin;

/**
 * Platform-neutral handle for a task scheduled by BeastLib.
 *
 * <p>The handle remains safe to cancel after completion and while BeastLib is
 * shutting down. Cancellation never interrupts a callback which is already
 * running, but prevents later executions of a repeating task.</p>
 */
public interface SchedulerTask {

    /**
     * Returns the plugin which owns this task.
     *
     * @return task owner
     */
    Plugin getOwner();

    /**
     * Returns whether this task has a fixed repeat period.
     *
     * @return {@code true} for a repeating task
     */
    boolean isRepeating();

    /**
     * Returns the current task lifecycle state.
     *
     * @return current state
     */
    SchedulerTaskState getState();

    /**
     * Attempts to cancel this task.
     *
     * @return {@code true} when this call changed the task to cancelled
     */
    boolean cancel();

    /**
     * Returns whether this task was cancelled.
     *
     * @return {@code true} when cancelled
     */
    default boolean isCancelled() {
        return getState() == SchedulerTaskState.CANCELLED;
    }

    /**
     * Returns whether this task can no longer execute.
     *
     * @return {@code true} for any terminal state
     */
    default boolean isDone() {
        return getState().isTerminal();
    }
}
