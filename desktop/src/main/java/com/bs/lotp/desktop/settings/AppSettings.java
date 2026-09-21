package com.bs.lotp.desktop.settings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Shape of {@code settings.yaml}. All fields optional; missing means unset. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AppSettings(String pluginsDir, WindowBounds window, String theme) {

    public AppSettings() {
        this(null, null, null);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WindowBounds(double x, double y, double w, double h) {
    }

    public AppSettings withPluginsDir(String dir) {
        return new AppSettings(dir, window, theme);
    }

    public AppSettings withWindow(double x, double y, double w, double h) {
        return new AppSettings(pluginsDir, new WindowBounds(x, y, w, h), theme);
    }

    public AppSettings withTheme(String theme) {
        return new AppSettings(pluginsDir, window, theme);
    }
}
