package com.gathin.tvhome;

/** Decides whether a tile may offer the system's uninstall confirmation. */
final class AppMenuPolicy {
    private AppMenuPolicy() {}

    static boolean canUninstall(String pkg, String ownPackage, int applicationFlags, boolean hasHandler) {
        // Android ApplicationInfo: system=1, updated-system=128. Neither is a removable user app.
        return pkg != null && !pkg.equals(ownPackage)
                && (applicationFlags & (1 | 128)) == 0 && hasHandler;
    }
}
