package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/** Restores the original Home experience without shell access or changing system settings. */
final class OriginalHome {
    static final String RESUME_CUSTOM = "com.gathin.tvhome.action.RESUME_CUSTOM_HOME";
    private static final String TARGET = "original_home_target";

    private OriginalHome() { }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences("home_restore", Context.MODE_PRIVATE);
    }

    static boolean isRestored(Context context) {
        return preferences(context).getString(TARGET, null) != null;
    }

    static void useCustom(Context context) {
        preferences(context).edit().remove(TARGET).commit();
    }

    /** Boot-only fallback: does not change the user's normal Home routing preference. */
    static boolean openOriginalOnce(Activity activity) {
        List<ResolveInfo> homes = activity.getPackageManager().queryIntentActivities(
                new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0);
        ComponentName only = null;
        int count = 0;
        for (ResolveInfo home : homes) {
            ActivityInfo info = home.activityInfo;
            if (info == null || !HomeRestorePolicy.canRestore(activity.getPackageName(), info.packageName,
                    info.enabled && info.applicationInfo.enabled, info.exported)) continue;
            ComponentName candidate = new ComponentName(info.packageName, info.name);
            if ("com.mitv.tvhome".equals(info.packageName) && "com.mitv.tvhome.MainActivityUserMode".equals(info.name))
                return open(activity, candidate);
            only = candidate;
            count++;
        }
        return count == 1 && open(activity, only);
    }

    static void resumeCustomIfAsked(Context context, Intent intent) {
        if (intent != null && RESUME_CUSTOM.equals(intent.getAction())) {
            preferences(context).edit().remove(TARGET).commit();
            BootStartup.skipThisBoot(context); // An explicit app launch must work even with boot auto-start off.
        }
    }

    static boolean redirect(Activity activity) {
        String saved = preferences(activity).getString(TARGET, null);
        ComponentName target = saved == null ? null : ComponentName.unflattenFromString(saved);
        if (target == null) return false;
        return open(activity, target);
    }

    private static boolean open(Activity activity, ComponentName target) {
        try {
            ActivityInfo info = activity.getPackageManager().getActivityInfo(target, 0);
            if (!HomeRestorePolicy.canRestore(activity.getPackageName(), target.getPackageName(),
                    info.enabled && info.applicationInfo.enabled, info.exported)) throw new IllegalStateException();
            activity.startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                    .setComponent(target).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED));
            return true;
        } catch (Exception unavailable) {
            // A removed/disabled original launcher must never trap Home in a redirect loop.
            preferences(activity).edit().remove(TARGET).commit();
            Toast.makeText(activity, R.string.restore_home_unavailable, Toast.LENGTH_LONG).show();
            return false;
        }
    }

    static void showRestoreDialog(Activity activity) {
        PackageManager pm = activity.getPackageManager();
        List<ResolveInfo> homes = new ArrayList<>();
        for (ResolveInfo home : pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)) {
            ActivityInfo info = home.activityInfo;
            if (info != null && HomeRestorePolicy.canRestore(activity.getPackageName(), info.packageName,
                    info.enabled && info.applicationInfo.enabled, info.exported)) homes.add(home);
        }
        // This TV's saved original entry; do not pick a factory, setup or alternate user-mode activity.
        for (ResolveInfo home : homes) {
            if ("com.mitv.tvhome".equals(home.activityInfo.packageName)
                    && "com.mitv.tvhome.MainActivityUserMode".equals(home.activityInfo.name)) {
                confirm(activity, home);
                return;
            }
        }
        if (homes.isEmpty()) {
            Toast.makeText(activity, R.string.restore_home_unavailable, Toast.LENGTH_LONG).show();
        } else if (homes.size() == 1) {
            confirm(activity, homes.get(0));
        } else {
            String[] labels = new String[homes.size()];
            for (int i = 0; i < labels.length; i++) labels[i] = homes.get(i).loadLabel(pm).toString();
            new AlertDialog.Builder(activity).setTitle(R.string.restore_original_home)
                    .setItems(labels, (dialog, which) -> confirm(activity, homes.get(which)))
                    .setNegativeButton(android.R.string.cancel, null).show();
        }
    }

    private static void confirm(Activity activity, ResolveInfo home) {
        new AlertDialog.Builder(activity).setTitle(R.string.restore_original_home)
                .setMessage(activity.getString(R.string.restore_original_home_message, home.loadLabel(activity.getPackageManager())))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.restore_original_home, (dialog, which) -> {
                    ComponentName target = new ComponentName(home.activityInfo.packageName, home.activityInfo.name);
                    if (preferences(activity).edit().putString(TARGET, target.flattenToString()).commit()) open(activity, target);
                    else Toast.makeText(activity, R.string.restore_home_unavailable, Toast.LENGTH_LONG).show();
                }).show();
    }
}
