package com.gathin.tvhome;
final class WakeHomePolicy {
    private WakeHomePolicy() { }
    static boolean shouldResume(boolean enabled, boolean restored, boolean stockHome, long screenOn, long now) {
        return enabled && !restored && stockHome && screenOn >= 0 && now >= screenOn && now - screenOn <= 6000;
    }
}
