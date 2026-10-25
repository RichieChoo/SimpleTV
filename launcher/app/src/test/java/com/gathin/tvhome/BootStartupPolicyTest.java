package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class BootStartupPolicyTest {
    @Test public void startupRespectsBothTheSwitchAndOriginalHomeRestoration() {
        assertTrue(BootStartupPolicy.shouldStart(true, false));
        assertFalse(BootStartupPolicy.shouldStart(false, false));
        assertFalse(BootStartupPolicy.shouldStart(true, true));
        assertFalse(BootStartupPolicy.shouldStart(false, true));
    }
    @Test public void switchingOffReturnsToOriginalHomeOnlyOncePerKnownBoot() {
        assertTrue(BootStartupPolicy.originalDue(false, 10, 9));
        assertFalse(BootStartupPolicy.originalDue(false, 10, 10));
        assertFalse(BootStartupPolicy.originalDue(false, -1, 9));
        assertFalse(BootStartupPolicy.originalDue(true, 10, 9));
    }
}
