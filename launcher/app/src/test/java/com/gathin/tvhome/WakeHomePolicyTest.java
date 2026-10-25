package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class WakeHomePolicyTest {
    @Test public void recentWakeRestoresCustomHomeOnlyWhenFirmwareResumesStockHome() {
        assertTrue(WakeHomePolicy.shouldResume(true, false, true, 1000, 2000));
        assertFalse(WakeHomePolicy.shouldResume(true, false, false, 1000, 2000));
    }
    @Test public void neverStealsAnotherAppOrAnIntentionalOriginalHome() {
        assertFalse(WakeHomePolicy.shouldResume(false, false, true, 1000, 2000));
        assertFalse(WakeHomePolicy.shouldResume(true, true, true, 1000, 2000));
        assertFalse(WakeHomePolicy.shouldResume(true, false, true, -1, 2000));
        assertFalse(WakeHomePolicy.shouldResume(true, false, true, 1000, 9000));
        assertFalse(WakeHomePolicy.shouldResume(true, false, true, 3000, 2000));
    }
}
