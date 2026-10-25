package com.gathin.tvhome;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

/** Optional boot entry, with a foreground Home fallback for firmware that launches its selected HOME first. */
public final class BootStartup extends BroadcastReceiver {
    static final String KEY = "start_launcher_on_boot";
    private static final String HANDLED = "launcher_boot_handled";

    static boolean enabled(Context context) {
        return context.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE).getBoolean(KEY, true);
    }

    static void setEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY, enabled).commit();
        // Choosing Off affects the next boot, not the Home that follows closing this settings page.
        skipThisBoot(context);
        if (enabled) OriginalHome.useCustom(context);
    }

    static void skipThisBoot(Context context) {
        context.getSharedPreferences(OpenOnStart.BOOT_PREFS, Context.MODE_PRIVATE).edit()
                .putInt(HANDLED, OpenOnStart.boot(context)).commit();
    }

    static boolean originalOnFirstHome(Activity activity) {
        int boot = OpenOnStart.boot(activity);
        SharedPreferences prefs = activity.getSharedPreferences(OpenOnStart.BOOT_PREFS, Context.MODE_PRIVATE);
        int handled = prefs.getInt(HANDLED, -1);
        if (boot >= 0 && boot != handled && !prefs.edit().putInt(HANDLED, boot).commit()) return false;
        return BootStartupPolicy.originalDue(enabled(activity), boot, handled) && OriginalHome.openOriginalOnce(activity);
    }

    @Override public void onReceive(Context context, Intent intent) {
        if (intent == null || !Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())
                || !BootStartupPolicy.shouldStart(enabled(context), OriginalHome.isRestored(context))) return;
        int boot = OpenOnStart.boot(context);
        AerialDiagnostics.record(context, "startup boot broadcast count=" + boot + " enabled=" + enabled(context));
        if (boot >= 0 && context.getSharedPreferences(OpenOnStart.BOOT_PREFS, Context.MODE_PRIVATE)
                .getInt(HANDLED, -1) == boot) return;  // Home already handled this boot, possibly opening IPTV.
        try {
            context.startActivity(new Intent(context, MainActivity.class).addCategory(Intent.CATEGORY_HOME)
                    .setAction(Intent.ACTION_MAIN).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (RuntimeException blocked) {
            // Some firmware requires the default-HOME role or explicit vendor auto-start permission.
            Log.w("TvHome.Startup", "Firmware blocked boot start; default Home remains available", blocked);
        }
    }
}
