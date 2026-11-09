package me.mraxetv.beastlib.api.client;

/** Access to the display transport owned by the active BeastLib instance. */
public final class ClientDisplayServices {
    private static volatile ClientDisplayService service;

    private ClientDisplayServices() { }

    public static ClientDisplayService get() {
        ClientDisplayService current = service;
        if (current == null) throw new IllegalStateException("BeastLib client display service is not available.");
        return current;
    }

    public static boolean isAvailable() { return service != null; }

    public static void set(ClientDisplayService displayService) {
        if (displayService == null) throw new IllegalArgumentException("displayService cannot be null");
        service = displayService;
    }

    public static void clear() { service = null; }
}
