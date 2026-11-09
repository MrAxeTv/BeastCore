package me.mraxetv.beastlib.api.scheduler;

/**
 * Process-wide access point for the scheduler supplied by the active
 * BeastLib plugin instance.
 */
public final class SchedulerServices {

    private static volatile SchedulerService service;

    private SchedulerServices() {
    }

    /**
     * Returns the active scheduler.
     *
     * @return active scheduler
     * @throws IllegalStateException when BeastLib is not enabled
     */
    public static SchedulerService get() {
        SchedulerService current = service;
        if (current == null) {
            throw new IllegalStateException("BeastLib scheduler is not available.");
        }
        return current;
    }

    public static boolean isAvailable() {
        SchedulerService current = service;
        return current != null && !current.isShutdown();
    }

    public static void set(SchedulerService schedulerService) {
        if (schedulerService == null) {
            throw new IllegalArgumentException("schedulerService cannot be null");
        }
        service = schedulerService;
    }

    public static void clear() {
        service = null;
    }
}
