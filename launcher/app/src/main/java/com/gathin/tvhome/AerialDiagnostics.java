package com.gathin.tvhome;

import android.content.Context;
import android.os.SystemClock;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Bounded device-local diagnostics; records playback stages, never network or account data. */
final class AerialDiagnostics {
    private static final ExecutorService WRITER = Executors.newSingleThreadExecutor();
    private AerialDiagnostics() { }
    static void record(Context context, String event) {
        Context app = context.getApplicationContext();
        String line = SystemClock.uptimeMillis() + " " + Thread.currentThread().getName() + " " + event + "\n";
        WRITER.execute(() -> {
            File directory = app.getExternalFilesDir(null);
            if (directory == null) return;
            File file = new File(directory, "aerial-state.log");
            try {
                if (file.length() > 256 * 1024) {
                    File old = new File(directory, "aerial-state.previous.log");
                    old.delete();
                    file.renameTo(old);
                }
                try (FileOutputStream out = new FileOutputStream(file, true)) {
                    out.write(line.getBytes(StandardCharsets.UTF_8));
                }
            } catch (java.io.IOException ignored) { }
        });
    }
}
