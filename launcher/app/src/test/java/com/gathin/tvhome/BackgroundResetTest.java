package com.gathin.tvhome;

import org.junit.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class BackgroundResetTest {
    @Test public void restoringBackgroundKeepsLayoutMotionAndStartupChoices() {
        Map<String, Object> preferences = new HashMap<>();
        preferences.put("columns", 6);
        preferences.put("rows_below_dock", 0);
        preferences.put("animations", true);
        preferences.put("advanced_effects", false);
        preferences.put("dock_opacity", 70);
        preferences.put("open_on_start", "com.example.iptv");
        preferences.put("start_launcher_on_boot", true);
        Map<String, Object> expected = new HashMap<>(preferences);
        preferences.put("wallpaper_choice", "video:night.mp4");
        BackgroundReset.reset(preferences::remove);
        assertEquals(expected, preferences);
    }
}
