package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Muted local aerial behind Home. Releases its decoder before another app needs video playback. */
final class VideoWallpaper implements TextureView.SurfaceTextureListener {
    private final FrameLayout layer;
    private final TextureView texture;
    private final List<File> files;
    private final boolean rotate;
    private final SharedPreferences choices;
    private static final android.os.HandlerThread MEDIA_THREAD;
    private static final android.os.Handler MEDIA;
    static {
        MEDIA_THREAD = new android.os.HandlerThread("SimpleTV-Aerial");
        MEDIA_THREAD.start();
        MEDIA = new android.os.Handler(MEDIA_THREAD.getLooper());
    }
    private final int diagnosticId = DIAGNOSTIC_IDS.incrementAndGet();
    private static final java.util.concurrent.atomic.AtomicInteger DIAGNOSTIC_IDS = new java.util.concurrent.atomic.AtomicInteger();
    private int renderedFrames;
    private MediaPlayer player; // accessed only on MEDIA, never on the UI thread
    private volatile boolean active, closed;
    private boolean prepared, failed;
    private volatile SurfaceTexture availableSurface;
    private int position, index;

    static List<File> videos(File dir) {
        File[] found = dir == null ? null : new File(dir, "videos").listFiles(
                file -> file.isFile() && file.length() > 0 && file.getName().endsWith(".mp4"));
        List<File> result = new ArrayList<>();
        if (found != null) { Arrays.sort(found); result.addAll(Arrays.asList(found)); }
        for (String name : new String[]{"01-dubai.mp4", "02-greenland.mp4", "03-hong-kong.mp4", "04-china.mp4", "05-new-york.mp4"}) {
            boolean local = false;
            for (File file : result) if (name.equals(file.getName())) local = true;
            if (!local) result.add(new File("/android_asset/aerial/" + name));
        }
        return result;
    }

    static String title(File file) {
        String name = file.getName();
        if (name.contains("dubai")) return "迪拜";
        if (name.contains("greenland")) return "格陵兰";
        if (name.contains("hong-kong")) return "香港";
        if (name.contains("china")) return "中国山水";
        if (name.contains("new-york")) return "纽约";
        return name;
    }

    static VideoWallpaper create(Context context, FrameLayout root, Config config) {
        File dir = Config.dir(context);
        if (!config.wallpaperEnabled || config.reducedMotion) return null;
        String choice = context.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE)
                .getString(Config.WALLPAPER_CHOICE_KEY, null);
        if (choice == null || (!choice.equals("videos") && !choice.startsWith("video:"))) return null;
        List<File> files = videos(dir);
        if (files.isEmpty() && dir != null) {
            File single = new File(dir, "wallpaper.mp4");
            if (single.isFile() && single.length() > 0) files = new ArrayList<>(Collections.singletonList(single));
        }
        return files.isEmpty() ? null : new VideoWallpaper(context, root, files, config.wallpaperDim, choice);
    }

    static VideoWallpaper preview(Context context, FrameLayout root, File file, int dim) {
        return new VideoWallpaper(context, root, new ArrayList<>(Collections.singletonList(file)), dim,
                "video:" + file.getName());
    }

    private VideoWallpaper(Context context, FrameLayout root, List<File> files, int dim, String choice) {
        this.files = files;
        choices = context.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE);
        rotate = choice == null || choice.equals("videos");
        if (choice != null && choice.startsWith("video:")) {
            for (int i = 0; i < files.size(); i++) if (files.get(i).getName().equals(choice.substring(6))) index = i;
        }
        layer = new FrameLayout(context);
        layer.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        layer.setAlpha(0f);  // Keep the image fallback visible until the first decoded frame.
        texture = new TextureView(context);
        texture.setOpaque(false);
        texture.setSurfaceTextureListener(this);
        layer.addView(texture, full());
        View shade = new View(context);
        shade.setBackgroundColor((dim * 255 / 100) << 24);
        layer.addView(shade, full());
        root.addView(layer, 1, full());
        trace("created " + files.get(index).getName());
        layer.postDelayed(() -> {
            if (closed) return;
            trace("watchdog active=" + active + " prepared=" + prepared + " failed=" + failed
                    + " surface=" + (availableSurface != null) + " texture=" + texture.isAvailable()
                    + " alpha=" + layer.getAlpha() + " frames=" + renderedFrames
                    + " worker=" + MEDIA_THREAD.getState() + " stack=" + Arrays.toString(MEDIA_THREAD.getStackTrace()));
        }, 3500);
    }

    private static FrameLayout.LayoutParams full() {
        return new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private void trace(String event) {
        AerialDiagnostics.record(layer.getContext(), "video=" + diagnosticId + " " + event);
    }

    void start() {
        active = true;
        trace("start surface=" + (availableSurface != null));
        SurfaceTexture surface = availableSurface;
        MEDIA.post(() -> prepare(surface));
    }

    /** All vendor codec calls, including release, stay on this worker Looper. */
    private void prepare(SurfaceTexture surfaceTexture) {
        trace("prepare active=" + active + " closed=" + closed + " failed=" + failed
                + " player=" + (player != null) + " surface=" + (surfaceTexture != null));
        if (!active || closed || failed || player != null || surfaceTexture == null
                || availableSurface != surfaceTexture) return;
        trace("native-create begin");
        MediaPlayer next = new MediaPlayer();
        trace("native-create end");
        player = next;
        try {
            Surface surface = new Surface(surfaceTexture);
            try { next.setSurface(surface); } finally { surface.release(); }
            File file = files.get(index);
            // Older TV firmware can stall on an APK fd with a large offset. Materialize just
            // the selected asset on the worker, then use the same local-file path already verified.
            trace("materialize begin " + file.getName());
            File playable = materialize(file);
            trace("materialize end bytes=" + playable.length());
            if (!active || closed || availableSurface != surfaceTexture) { release(false); return; }
            trace("set-data-source begin");
            next.setDataSource(playable.getAbsolutePath());
            trace("set-data-source end");
            next.setLooping(!rotate || files.size() == 1);
            next.setVolume(0f, 0f);
            next.setOnPreparedListener(mp -> {
                trace("prepared callback current=" + (player == mp) + " active=" + active + " closed=" + closed);
                if (player != mp || !active || closed) return;
                prepared = true;
                if (position > 0) mp.seekTo(position);
                mp.start();
                Log.i("TvHome.Video", "Playing " + file.getName() + " " + mp.getVideoWidth() + "x" + mp.getVideoHeight());
            });
            next.setOnCompletionListener(mp -> {
                if (player != mp || !active || closed) return;
                release(false);
                index = (index + 1) % files.size();
                position = 0;
                prepare(availableSurface);
            });
            next.setOnInfoListener((mp, what, extra) -> {
                if (what != 704) trace("info " + what + "/" + extra);
                if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START && player == mp)
                    layer.post(() -> { if (!closed && active) layer.animate().alpha(1f).setDuration(140).start(); });
                return false;
            });
            next.setOnErrorListener((mp, what, extra) -> {
                Log.w("TvHome.Video", "Image fallback: " + what + "/" + extra);
                trace("error " + what + "/" + extra);
                failed = true;
                release(true);
                return true;
            });
            next.prepareAsync();
        } catch (Exception failure) {
            Log.w("TvHome.Video", "Image fallback", failure);
            trace("exception " + failure);
            failed = active && !closed;
            release(failed);
        }
    }

    private File materialize(File file) throws java.io.IOException {
        if (!file.getAbsolutePath().startsWith("/android_asset/")) return file;
        String assetPath = file.getAbsolutePath().substring("/android_asset/".length());
        File directory = new File(layer.getContext().getCacheDir(), "aerial");
        directory.mkdirs();
        File cached = new File(directory, file.getName());
        long expected;
        try (android.content.res.AssetFileDescriptor asset = layer.getContext().getAssets().openFd(assetPath)) {
            expected = asset.getLength();
        }
        if (cached.isFile() && cached.length() == expected) return cached;
        File partial = new File(directory, file.getName() + ".part");
        try (java.io.InputStream in = layer.getContext().getAssets().open(assetPath);
             java.io.OutputStream out = new java.io.FileOutputStream(partial)) {
            byte[] bytes = new byte[65536]; int count;
            while ((count = in.read(bytes)) != -1) {
                if (closed || !active) throw new java.io.IOException("Preview superseded");
                out.write(bytes, 0, count);
            }
        } catch (java.io.IOException failure) { partial.delete(); throw failure; }
        if (partial.length() != expected || !partial.renameTo(cached))
            throw new java.io.IOException("Incomplete bundled aerial");
        return cached;
    }

    void stop() {
        trace("stop");
        active = false;
        MEDIA.post(() -> release(false));
    }

    void step(int direction) {
        MEDIA.post(() -> {
            release(false);
            index = Math.floorMod(index + direction, files.size());
            position = 0;
            failed = false;
            if (!rotate) choices.edit().putString(Config.WALLPAPER_CHOICE_KEY, "video:" + files.get(index).getName()).apply();
            prepare(availableSurface);
        });
    }

    void close() {
        trace("close");
        closed = true;
        active = false;
        MEDIA.post(() -> release(false));
        layer.animate().cancel();
        layer.setVisibility(View.GONE);
        if (layer.getParent() instanceof ViewGroup) ((ViewGroup) layer.getParent()).removeView(layer);
    }

    private void release(boolean hide) {
        MediaPlayer old = player;
        player = null;
        if (old != null) {
            if (prepared && !closed) {
                try { position = old.getCurrentPosition(); } catch (IllegalStateException ignored) { }
            }
            trace("native-release begin prepared=" + prepared);
            old.release();
            trace("native-release end");
            Log.i("TvHome.Video", "Decoder released on " + Thread.currentThread().getName());
        }
        prepared = false;
        if (hide) layer.post(() -> layer.setAlpha(0f));
    }

    @Override public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        trace("surface available " + width + "x" + height);
        availableSurface = surface;
        MEDIA.post(() -> prepare(surface));
    }
    @Override public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) { }
    @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        trace("surface destroyed");
        availableSurface = null;
        MEDIA.post(() -> release(false));
        // Let TextureView abandon its queue immediately. On MiTV, deferring SurfaceTexture.release()
        // until after MediaPlayer.release() on the same worker deadlocks that worker: the vendor
        // decoder waits for this queue to be abandoned before its release can finish.
        return true;
    }
    @Override public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        if (++renderedFrames == 1) trace("first texture frame alpha=" + layer.getAlpha());
    }
}
