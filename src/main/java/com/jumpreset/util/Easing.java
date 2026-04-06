package com.jumpreset.util;

/**
 * Lightweight easing functions for HUD animations.
 * All methods accept a normalised progress value t ∈ [0, 1].
 */
public final class Easing {

    private Easing() {}

    /** Cubic ease-out: fast start, slow finish. */
    public static float easeOutCubic(float t) {
        float f = 1f - t;
        return 1f - f * f * f;
    }

    /** Quartic ease-in: slow start, accelerates. Good for fade-outs. */
    public static float easeInQuart(float t) {
        return t * t * t * t;
    }

    /** Smooth-step (Ken Perlin style). */
    public static float smoothStep(float t) {
        return t * t * (3f - 2f * t);
    }

    /**
     * Computes an alpha (0–255) for a given normalised progress value.
     *
     * Timeline (total = DISPLAY_DURATION_MS):
     *   0 % – 12 %  → ease-out fade IN  (~190 ms at 1600 ms total)
     *  12 % – 70 %  → fully opaque hold
     *  70 % – 100 % → ease-in  fade OUT (~480 ms)
     *
     * @param progress 0.0 = first frame, 1.0 = fully expired
     */
    public static int computeAlpha(float progress) {
        if (progress < 0.12f) {
            // Fade in
            float t = progress / 0.12f;
            return Math.min(255, (int)(easeOutCubic(t) * 255));
        } else if (progress > 0.70f) {
            // Fade out
            float t = (progress - 0.70f) / 0.30f;
            return Math.max(0, (int)((1f - easeInQuart(t)) * 255));
        } else {
            return 255;
        }
    }
}
