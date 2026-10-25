package com.gathin.tvhome;

import android.app.Application;
import android.content.*;
import android.os.PowerManager;
import android.os.SystemClock;

/** Covers MiTV standby wake-ups, which do not emit BOOT_COMPLETED. No service or polling. */
public final class DesktopApplication extends Application {
    private long screenOn = -1;
    private final BroadcastReceiver wake = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) { screenOn = -1; return; }
            if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                screenOn = SystemClock.elapsedRealtime();
                AerialDiagnostics.record(context, "startup screen-on");
                return;
            }
            boolean stock = "com.mitv.tvhome".equals(intent.getStringExtra("package"));
            if (!WakeHomePolicy.shouldResume(BootStartup.enabled(context), OriginalHome.isRestored(context), stock,
                    screenOn, SystemClock.elapsedRealtime())) return;
            screenOn = -1; // one redirect per wake; never fight another app or a deliberate restore
            try {
                context.startActivity(new Intent(context, MainActivity.class).setAction(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                AerialDiagnostics.record(context, "startup restored custom home after MiTV wake");
            } catch (RuntimeException blocked) {
                AerialDiagnostics.record(context, "startup wake blocked " + blocked);
            }
        }
    };
    @Override public void onCreate() {
        super.onCreate();
        PowerManager power = getSystemService(PowerManager.class);
        if (power != null && power.isInteractive()) screenOn = SystemClock.elapsedRealtime();
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction("mitv.action.resume.activity");
        if (android.os.Build.VERSION.SDK_INT >= 33) registerReceiver(wake, filter, Context.RECEIVER_EXPORTED);
        else registerLegacy(filter);
    }
    @android.annotation.SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void registerLegacy(IntentFilter filter) { registerReceiver(wake, filter); }
}
