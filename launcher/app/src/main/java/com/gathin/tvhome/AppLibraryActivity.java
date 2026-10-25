package com.gathin.tvhome;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.Collator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Full-screen, vertically scrolling app catalogue, separate from Home's single-row layout. */
public final class AppLibraryActivity extends Activity {
    private final List<ResolveInfo> apps = new ArrayList<>();
    private final Set<String> tvApps = new HashSet<>();
    private GridView grid;
    private FrameLayout scene;
    private GlassView focusGlass;
    private androidx.dynamicanimation.animation.SpringAnimation focusX, focusY;
    private boolean glassVisible;
    private TextView count;
    private Config config;
    private Set<String> added, removed;
    private int selected;
    private View selectedView;
    private LinearLayout actionBar;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        config = Config.load(this);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setClipChildren(false);
        page.setPadding(dp(36), dp(18), dp(36), dp(12));
        page.setBackgroundColor(0xFF101722);
        count = new TextView(this);
        count.setTextColor(Color.WHITE);
        count.setTextSize(27);
        page.addView(count, new LinearLayout.LayoutParams(-1, dp(42)));
        TextView hint = new TextView(this);
        hint.setText(R.string.all_apps_page_hint);
        hint.setTextColor(0xB3FFFFFF);
        hint.setTextSize(13);
        page.addView(hint, new LinearLayout.LayoutParams(-1, dp(28)));
        grid = new GridView(this);
        grid.setNumColumns(6);
        grid.setHorizontalSpacing(dp(12));
        grid.setVerticalSpacing(dp(12));
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        grid.setClipToPadding(false);
        grid.setPadding(0, dp(14), 0, dp(14));
        grid.setSmoothScrollbarEnabled(true);
        grid.setSelector(android.R.color.transparent);
        grid.setClipChildren(false);
        grid.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (selectedView != null) styleSelection(selectedView, false);
                selectedView = view;
                if (view != null) styleSelection(view, true);
            }
            public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });
        page.addView(grid, new LinearLayout.LayoutParams(-1, 0, 1));
        actionBar = new LinearLayout(this);
        actionBar.setGravity(Gravity.CENTER);
        actionBar.setVisibility(View.GONE);
        page.addView(actionBar, new LinearLayout.LayoutParams(-1, dp(48)));
        scene = new FrameLayout(this);
        scene.addView(page, new FrameLayout.LayoutParams(-1, -1));
        focusGlass = new GlassView(this, null);
        focusGlass.setStyle(config.glassStyle);
        focusGlass.setAlpha(0f);
        focusGlass.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        scene.addView(focusGlass, new FrameLayout.LayoutParams(1, 1));
        focusX = new androidx.dynamicanimation.animation.SpringAnimation(focusGlass,
                androidx.dynamicanimation.animation.SpringAnimation.TRANSLATION_X);
        focusY = new androidx.dynamicanimation.animation.SpringAnimation(focusGlass,
                androidx.dynamicanimation.animation.SpringAnimation.TRANSLATION_Y);
        for (androidx.dynamicanimation.animation.SpringAnimation spring : new androidx.dynamicanimation.animation.SpringAnimation[]{focusX, focusY}) {
            if (config.reducedMotion) Motion.reduce(spring);
            else spring.setSpring(new androidx.dynamicanimation.animation.SpringForce()
                    .setStiffness(!config.focusOvershoot ? (config.advancedEffects ? 520f : 440f) : (config.advancedEffects ? 320f : 260f))
                    .setDampingRatio(!config.focusOvershoot ? 1f : config.advancedEffects ? 0.76f : 0.58f));
        }
        grid.setOnScrollListener(new android.widget.AbsListView.OnScrollListener() {
            public void onScrollStateChanged(android.widget.AbsListView view, int state) { }
            public void onScroll(android.widget.AbsListView view, int first, int visible, int total) {
                if (selectedView != null && selectedView.getParent() == grid) moveGlass(selectedView.findViewById(R.id.card), true);
            }
        });
        setContentView(scene);
        page.setAlpha(0f);
        page.setTranslationX(dp(36));
        page.post(() -> page.animate().alpha(1f).translationX(0f).setDuration(260)
                .setInterpolator(new android.view.animation.DecelerateInterpolator(1.5f)).start());
        grid.setOnItemClickListener((parent, view, position, id) -> open(apps.get(position)));
        grid.setOnItemLongClickListener((parent, view, position, id) -> { actions(apps.get(position)); return true; });
        setResult(RESULT_OK);
    }

    @Override protected void onResume() {
        super.onResume();
        reload();
    }

    private void reload() {
        selected = Math.max(0, grid.getSelectedItemPosition());
        config = Config.load(this);
        added = new HashSet<>(LibraryApps.load(this));
        added.addAll(config.libraryApps);
        removed = LibraryApps.loadRemoved(this);
        PackageManager pm = getPackageManager();
        Map<String, ResolveInfo> unique = new LinkedHashMap<>();
        tvApps.clear();
        for (String category : new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
            for (ResolveInfo info : pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(category), 0)) {
                if (info.activityInfo == null || !info.activityInfo.exported) continue;
                String pkg = info.activityInfo.packageName;
                if (pkg.equals(getPackageName())) continue;
                if (Intent.CATEGORY_LEANBACK_LAUNCHER.equals(category)) tvApps.add(pkg);
                if (!unique.containsKey(pkg)) unique.put(pkg, info);
            }
        }
        apps.clear();
        apps.addAll(unique.values());
        Collator collator = Collator.getInstance();
        apps.sort((a, b) -> {
            int recent = Long.compare(LastOpened.get(this, b.activityInfo.packageName), LastOpened.get(this, a.activityInfo.packageName));
            return recent != 0 ? recent : collator.compare(label(a), label(b));
        });
        count.setText(getString(R.string.app_library) + "  ·  " + apps.size());
        selectedView = null;
        grid.setAdapter(new Catalogue());
        grid.setSelection(Math.min(selected, Math.max(0, apps.size() - 1)));
        grid.requestFocus();
    }

    private String label(ResolveInfo info) {
        String pkg = info.activityInfo.packageName;
        return config.appLabels.containsKey(pkg) ? config.appLabels.get(pkg)
                : Cards.cleanLabel(String.valueOf(info.loadLabel(getPackageManager())), pkg);
    }

    private boolean onHome(String pkg) {
        return LibraryApps.visible(tvApps.contains(pkg), config.hiddenApps.contains(pkg), added.contains(pkg), removed.contains(pkg));
    }

    private void open(ResolveInfo info) {
        Intent intent = new Intent(Intent.ACTION_MAIN).setClassName(info.activityInfo.packageName, info.activityInfo.name)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        if (tvApps.contains(info.activityInfo.packageName)) intent.addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        try { startActivity(intent); LastOpened.record(this, info.activityInfo.packageName); }
        catch (RuntimeException unavailable) { Toast.makeText(this, R.string.not_available, Toast.LENGTH_SHORT).show(); }
    }

    private void actions(ResolveInfo info) {
        String pkg = info.activityInfo.packageName;
        List<String> labels = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        labels.add(getString(R.string.library_open)); actions.add(() -> open(info));
        boolean home = onHome(pkg);
        labels.add(getString(R.string.home_visibility_state, getString(home ? R.string.on : R.string.off)));
        actions.add(() -> {
            if (!LibraryApps.save(this, pkg, !home)) Toast.makeText(this, R.string.library_save_failed, Toast.LENGTH_LONG).show();
            reload();
        });
        Intent uninstall = SystemSettings.uninstall(this, pkg);
        if (uninstall != null) {
            labels.add(getString(R.string.library_uninstall));
            actions.add(() -> startActivity(uninstall));
        }
        actionBar.removeAllViews();
        for (int index = 0; index < labels.size(); index++) {
            final Runnable action = actions.get(index);
            TextView button = new TextView(this);
            button.setText(labels.get(index));
            button.setTextSize(17);
            button.setTextColor(Color.WHITE);
            button.setGravity(Gravity.CENTER);
            button.setFocusable(true);
            button.setPadding(dp(18), 0, dp(18), 0);
            button.setOnFocusChangeListener((v, focused) -> {
                GradientDrawable bg = new GradientDrawable();
                bg.setCornerRadius(dp(16));
                bg.setColor(focused ? 0x44FFFFFF : Color.TRANSPARENT);
                v.setBackground(bg);
            });
            button.setOnClickListener(v -> { actionBar.setVisibility(View.GONE); action.run(); });
            actionBar.addView(button, new LinearLayout.LayoutParams(-2, -1));
        }
        actionBar.setVisibility(View.VISIBLE);
        actionBar.getChildAt(0).requestFocus();
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_MENU && !apps.isEmpty()) {
            if (event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled())
                actions(apps.get(Math.max(0, grid.getSelectedItemPosition())));
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override public void onBackPressed() {
        if (actionBar.getVisibility() == View.VISIBLE) {
            actionBar.setVisibility(View.GONE);
            grid.requestFocus();
        } else super.onBackPressed();
    }

    @Override public void finish() {
        super.finish();
        if (config == null || config.animations && !config.reducedMotion)
            overridePendingTransition(R.anim.panel_backdrop, R.anim.panel_exit_right);
    }

    private void styleSelection(View tile, boolean focused) {
        View card = tile.findViewById(R.id.card);
        card.setForeground(null);
        float scale = !config.animations ? 1f : focused ? (config.advancedEffects ? 1.12f : 1.10f) : 1f;
        card.animate().cancel();
        if (!config.animations) { card.setScaleX(1f); card.setScaleY(1f); }
        else card.animate().scaleX(scale).scaleY(scale)
                .translationY(focused && config.advancedEffects ? -dp(4) : 0)
                .translationZ(focused && config.advancedEffects ? dp(8) : 0)
                .setDuration(config.reducedMotion ? Motion.REDUCED_MS : config.advancedEffects ? (focused ? 220 : 170) : (focused ? 260 : 180))
                .setInterpolator(focused && !config.focusOvershoot
                        ? new android.view.animation.DecelerateInterpolator(1.5f)
                        : new android.view.animation.OvershootInterpolator(focused ? (config.advancedEffects ? 0.7f : 1.6f) : 0f))
                .withLayer().start();
        if (focused) card.post(() -> moveGlass(card, false));
    }

    private void moveGlass(View card, boolean scrolling) {
        if (focusGlass == null || card.getWidth() == 0 || card.getParent() == null) return;
        android.graphics.Rect rect = new android.graphics.Rect(0, 0, card.getWidth(), card.getHeight());
        scene.offsetDescendantRectToMyCoords(card, rect);
        float scale = !config.animations ? 1f : config.advancedEffects ? 1.12f : 1.10f;
        int width = Math.round(card.getWidth() * scale + dp(12));
        int height = Math.round(card.getHeight() * scale + dp(12));
        ViewGroup.LayoutParams params = focusGlass.getLayoutParams();
        if (params.width != width || params.height != height) {
            params.width = width; params.height = height; focusGlass.setLayoutParams(params);
            focusGlass.setRadius(dp(24) * scale);
        }
        float x = rect.exactCenterX() - width / 2f;
        float y = rect.exactCenterY() - height / 2f - (config.advancedEffects ? dp(4) : 0);
        if (!glassVisible || scrolling || !config.animations) {
            focusX.cancel(); focusY.cancel();
            focusGlass.setTranslationX(x); focusGlass.setTranslationY(y);
            focusGlass.setAlpha(1f); glassVisible = true;
        } else { focusX.animateToFinalPosition(x); focusY.animateToFinalPosition(y); }
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private final class Catalogue extends BaseAdapter {
        @Override public int getCount() { return apps.size(); }
        @Override public Object getItem(int position) { return apps.get(position); }
        @Override public long getItemId(int position) { return position; }
        @Override public View getView(int position, View convert, ViewGroup parent) {
            LinearLayout tile;
            if (convert == null) {
                tile = new LinearLayout(AppLibraryActivity.this);
                tile.setOrientation(LinearLayout.VERTICAL);
                tile.setGravity(Gravity.CENTER);
                tile.setPadding(dp(5), dp(5), dp(5), dp(3));
                tile.setLayoutParams(new GridView.LayoutParams(-1, dp(144)));
                FrameLayout card = new FrameLayout(AppLibraryActivity.this);
                card.setId(R.id.card);
                card.setBackground(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
                card.setOutlineProvider(new android.view.ViewOutlineProvider() {
                    public void getOutline(View view, android.graphics.Outline outline) {
                        outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dp(24));
                    }
                });
                card.setClipToOutline(true);
                tile.addView(card, new LinearLayout.LayoutParams(dp(100), dp(100)));
                ImageView icon = new ImageView(AppLibraryActivity.this);
                icon.setId(R.id.icon);
                icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
                card.addView(icon, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
                TextView name = new TextView(AppLibraryActivity.this);
                name.setId(R.id.title);
                name.setTextSize(15);
                name.setTextColor(Color.WHITE);
                name.setGravity(Gravity.CENTER);
                name.setSingleLine(true);
                name.setEllipsize(TextUtils.TruncateAt.END);
                tile.addView(name, new LinearLayout.LayoutParams(-1, dp(30)));
            } else tile = (LinearLayout) convert;
            ResolveInfo app = apps.get(position);
            ImageView image = tile.findViewById(R.id.icon);
            image.setImageDrawable(app.loadIcon(getPackageManager()));
            ((TextView) tile.findViewById(R.id.title)).setText(label(app));
            return tile;
        }
    }
}
