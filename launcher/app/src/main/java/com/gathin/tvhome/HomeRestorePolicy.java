package com.gathin.tvhome;

final class HomeRestorePolicy {
    private HomeRestorePolicy() { }

    static boolean canRestore(String ownPackage, String targetPackage, boolean enabled, boolean exported) {
        return ownPackage != null && targetPackage != null && !targetPackage.isEmpty()
                && !ownPackage.equals(targetPackage) && enabled && exported;
    }
}
