package com.gathin.tvhome;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.widget.Toast;

/** Delegates recognition to the installed assistant; this launcher never records audio itself. */
final class VoiceAssistant {
    private VoiceAssistant() { }

    private static Intent resolve(Context context) {
        PackageManager pm = context.getPackageManager();
        // AOSP's voice-first action is not a public SDK constant; only an exported handler is eligible.
        String[] actions = {"android.intent.action.VOICE_ASSIST", Intent.ACTION_ASSIST, Intent.ACTION_VOICE_COMMAND};
        for (String pkg : new String[]{"com.xiaomi.voicecontrol", null}) {
            for (String action : actions) {
                Intent candidate = new Intent(action);
                if (pkg != null) candidate.setPackage(pkg);
                ResolveInfo info = pm.resolveActivity(candidate, PackageManager.MATCH_DEFAULT_ONLY);
                if (info != null && info.activityInfo != null && info.activityInfo.exported) return candidate;
            }
        }
        return null;
    }

    static void open(Activity activity) {
        Intent intent = resolve(activity);
        if (intent != null) {
            try { activity.startActivity(intent); return; } catch (RuntimeException unavailable) { }
        }
        Toast.makeText(activity, R.string.voice_unavailable, Toast.LENGTH_LONG).show();
    }

}
