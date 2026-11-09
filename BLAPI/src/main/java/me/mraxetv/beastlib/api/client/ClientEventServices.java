package me.mraxetv.beastlib.api.client;

/** Access to the client event service supplied by the enabled BeastLib instance. */
public final class ClientEventServices {
    private static volatile ClientEventService service;

    private ClientEventServices() { }

    public static ClientEventService get() {
        ClientEventService current = service;
        if (current == null || current.isShutdown())
            throw new IllegalStateException("BeastLib client event service is not available.");
        return current;
    }

    public static boolean isAvailable() {
        ClientEventService current = service;
        return current != null && !current.isShutdown();
    }

    public static void set(ClientEventService clientEventService) {
        if (clientEventService == null) throw new IllegalArgumentException("clientEventService cannot be null");
        service = clientEventService;
    }

    public static void clear() { service = null; }
}
