package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class HomeRestorePolicyTest {
    @Test public void anAvailableOriginalLauncherCanReceiveHome() {
        assertTrue(HomeRestorePolicy.canRestore("com.ts.launcher", "com.mitv.tvhome", true, true));
    }

    @Test public void theNewLauncherCannotRedirectHomeBackToItself() {
        assertFalse(HomeRestorePolicy.canRestore("com.ts.launcher", "com.ts.launcher", true, true));
    }

    @Test public void missingDisabledOrPrivateTargetsLeaveTheCurrentHomeAvailable() {
        assertFalse(HomeRestorePolicy.canRestore("com.ts.launcher", null, true, true));
        assertFalse(HomeRestorePolicy.canRestore("com.ts.launcher", "", true, true));
        assertFalse(HomeRestorePolicy.canRestore("com.ts.launcher", "com.mitv.tvhome", false, true));
        assertFalse(HomeRestorePolicy.canRestore("com.ts.launcher", "com.mitv.tvhome", true, false));
    }
}
