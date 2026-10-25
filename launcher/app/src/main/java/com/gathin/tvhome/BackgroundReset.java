package com.gathin.tvhome;

import java.util.function.Consumer;

final class BackgroundReset {
    private BackgroundReset() { }
    static void reset(Consumer<String> remove) {
        remove.accept("wallpaper_choice");
    }
}
