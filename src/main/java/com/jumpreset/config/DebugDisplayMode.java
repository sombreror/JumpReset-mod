package com.jumpreset.config;

import com.google.gson.annotations.SerializedName;

/**
 * Verbosity of the debug overlay.
 *
 * <p>{@link SerializedName} values match the pre-2.5 string config
 * ({@code "compact"} / {@code "full"}) for backwards compatibility.
 */
public enum DebugDisplayMode {
    @SerializedName("compact") COMPACT("Compact"),
    @SerializedName("full")    FULL   ("Full");

    private final String displayName;

    DebugDisplayMode(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    /** The other mode — used by the config screen's toggle button. */
    public DebugDisplayMode toggled() {
        return this == FULL ? COMPACT : FULL;
    }
}
