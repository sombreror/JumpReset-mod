package com.jumpreset.config;

import com.google.gson.annotations.SerializedName;

/**
 * HUD feedback presentation style.
 *
 * <p>The {@link SerializedName} values match the lowercase strings used by
 * pre-2.5 config files, so existing {@code jumpreset.json} files deserialize
 * unchanged (and continue to serialize to the same strings).
 */
public enum FeedbackStyle {
    @SerializedName("minimal")  MINIMAL ("Minimal"),
    @SerializedName("detailed") DETAILED("Detailed"),
    @SerializedName("bar")      BAR     ("Timing Bar");

    private final String displayName;

    FeedbackStyle(String displayName) {
        this.displayName = displayName;
    }

    /** Human-readable name for the config screen's cycle button. */
    public String displayName() {
        return displayName;
    }

    /** Next style in the cycle used by the config screen: minimal → detailed → bar → … */
    public FeedbackStyle next() {
        return switch (this) {
            case MINIMAL  -> DETAILED;
            case DETAILED -> BAR;
            case BAR      -> MINIMAL;
        };
    }
}
