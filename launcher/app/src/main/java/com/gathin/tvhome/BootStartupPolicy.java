package com.gathin.tvhome;

final class BootStartupPolicy {
    private BootStartupPolicy() { }
    static boolean shouldStart(boolean enabled, boolean originalRestored) { return enabled && !originalRestored; }
    static boolean originalDue(boolean enabled, int boot, int handledBoot) {
        return !enabled && boot >= 0 && boot != handledBoot;
    }
}
