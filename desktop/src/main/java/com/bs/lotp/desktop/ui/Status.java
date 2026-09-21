package com.bs.lotp.desktop.ui;

public enum Status {
    NOT_INSTALLED("Not installed"),
    INSTALLED("Installed"),
    UPDATE_AVAILABLE("Update available");

    public final String text;

    Status(String text) {
        this.text = text;
    }
}
