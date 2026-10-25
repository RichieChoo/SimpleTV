package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class AppMenuPolicyTest {
    @Test public void downloadedAppsCanUseTheSystemUninstallConfirmation() {
        assertTrue(AppMenuPolicy.canUninstall("com.example.player", "com.ts.launcher", 0, true));
    }

    @Test public void theCurrentLauncherAndSystemAppsAreProtected() {
        assertFalse(AppMenuPolicy.canUninstall("com.ts.launcher", "com.ts.launcher", 0, true));
        assertFalse(AppMenuPolicy.canUninstall("com.mitv.tvhome", "com.ts.launcher", 1, true));
        assertFalse(AppMenuPolicy.canUninstall("com.example.updatedsystem", "com.ts.launcher", 128, true));
        assertFalse(AppMenuPolicy.canUninstall(null, "com.ts.launcher", 0, true));
    }

    @Test public void noUninstallActionIsOfferedWithoutASystemHandler() {
        assertFalse(AppMenuPolicy.canUninstall("com.example.player", "com.ts.launcher", 0, false));
    }
}
