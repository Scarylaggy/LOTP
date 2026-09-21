package com.bs.lotp.desktop.ui;

import java.util.Locale;
import java.util.Objects;

/**
 * Light/dark UI themes. Each maps to a JavaFX stylesheet bundled next to this class.
 */
public enum UiTheme {
    LIGHT("light", "theme-light.css", "☾ Dark"),
    DARK("dark", "theme-dark.css", "☀ Light");

    private final String id;
    private final String cssFile;
    private final String toggleText;

    UiTheme(String id, String cssFile, String toggleText) {
        this.id = id;
        this.cssFile = cssFile;
        this.toggleText = toggleText;
    }

    public static UiTheme from(String id) {
        if (id != null && id.toLowerCase(Locale.ROOT).startsWith("dark")) {
            return DARK;
        }
        return LIGHT;
    }

    public String id() {
        return id;
    }

    /**
     * Text (with sun/moon glyph) shown on the toggle button while this theme is active.
     */
    public String toggleText() {
        return toggleText;
    }

    /**
     * External-form URL of the stylesheet, for {@code Scene.getStylesheets()}.
     */
    public String stylesheet() {
        var url = UiTheme.class.getResource(cssFile);
        Objects.requireNonNull(url, "missing theme css: " + cssFile);
        return url.toExternalForm();
    }
}
