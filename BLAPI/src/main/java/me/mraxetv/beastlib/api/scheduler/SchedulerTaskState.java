package me.mraxetv.beastlib.api.scheduler;

/**
 * Lifecycle state of a task submitted through {@link SchedulerService}.
 */
public enum SchedulerTaskState {

    /** The task is waiting for its first or next execution. */
    SCHEDULED,

    /** The task callback is currently running. */
    RUNNING,

    /** A one-shot task finished normally. */
    COMPLETED,

    /** The task was cancelled before all of its executions completed. */
    CANCELLED,

    /** An entity task could no longer run because its entity was retired. */
    RETIRED,

    /** The platform rejected the task before it could be scheduled. */
    REJECTED;

    /**
     * Returns whether no future execution can occur.
     *
     * @return {@code true} for a terminal state
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED || this == RETIRED || this == REJECTED;
    }
}
