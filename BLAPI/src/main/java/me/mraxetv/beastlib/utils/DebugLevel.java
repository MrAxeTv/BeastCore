package me.mraxetv.beastlib.utils;



public enum DebugLevel {
    LOWEST(0, new String[] { "LOWEST" }),
    LOW(1, new String[] { "LOW" }),
    MEDIUM(2, new String[] { "MEDIUM" }),
    HIGH(3, new String[] { "HIGH" }),
    HIGHEST(4, new String[] { "HIGHEST" });


    private final String[] names;
    private final int priority;

    DebugLevel(int priority, String... names) {
        this.priority = priority;
        this.names = names;
    }

    public int getPriority() {
        return this.priority;
    }


    static {}

}