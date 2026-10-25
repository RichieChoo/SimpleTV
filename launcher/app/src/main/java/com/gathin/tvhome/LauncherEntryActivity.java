package com.gathin.tvhome;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/** The icon on the original launcher's app list re-enables this Home experience. */
public final class LauncherEntryActivity extends Activity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        Intent intent = new Intent(this, MainActivity.class).setAction(OriginalHome.RESUME_CUSTOM)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        OriginalHome.resumeCustomIfAsked(this, intent);
        startActivity(intent);
        finish();
    }
}
