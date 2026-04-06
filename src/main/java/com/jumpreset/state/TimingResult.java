package com.jumpreset.state;

import com.jumpreset.config.ModConfig;

/**
 * TimingResult — v1.4.0
 *
 * Five classifications, each with actionable meaning:
 *
 *  PERFECT   — ideal timing, full score                 → green
 *  GOOD      — acceptable, slightly off                 → cyan
 *  LATE      — jumped too late, knockback not cancelled → orange
 *  TOO_EARLY — jumped before hit registered             → red
 *  MISSED    — window expired before any jump           → (no HUD, silent)
 *
 * Scoring curve (asymmetric):
 *  ms < tooEarlyMs              → score = 0   (hard floor)
 *  tooEarlyMs ≤ ms < perfectMs  → linear ramp 0→1
 *  ms == perfectMs              → score = 1.0  (peak)
 *  ms > perfectMs               → Gaussian decay with sigma from config
 *
 * Ping adjustment: all thresholds shift up by pingOffset(ping).
 */
public enum TimingResult {

    PERFECT  ("PERFECT",   0xFF00E87A),  // green
    GOOD     ("GOOD",      0xFF00DDEE),  // cyan / celestino
    LATE     ("LATE",      0xFFFF8800),  // orange
    TOO_EARLY("TOO EARLY", 0xFFFF3333),  // red
    MISSED   ("MISSED",    0xFF666666);  // grey — silent, rarely shown

    public final String label;
    public final int    color;

    TimingResult(String label, int color) {
        this.label = label;
        this.color = color;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Classify a timing value given the current ping.
     *
     * @param ms      elapsed milliseconds between hit and jump
     * @param pingMs  current measured ping (round-trip ms)
     */
    public static TimingResult fromMillis(double ms, double pingMs) {
        ModConfig cfg = ModConfig.get();
        double offset = ModConfig.pingOffset(pingMs);

        double early   = cfg.tooEarlyMs  + offset;
        double perfect = cfg.perfectMs   + offset;
        double goodMax = cfg.goodMaxMs   + offset;
        double lateMax = cfg.lateMaxMs   + offset;

        if (ms < early)   return TOO_EARLY;
        if (ms > lateMax) return MISSED;

        double s = score(ms, pingMs);
        if (s >= 0.78) return PERFECT;
        if (s >= 0.40) return GOOD;
        return LATE;
    }

    /**
     * Numeric score in [0, 1]. 1.0 = perfect timing.
     * Used for the score bar and for classification thresholds.
     */
    public static double score(double ms, double pingMs) {
        ModConfig cfg    = ModConfig.get();
        double offset    = ModConfig.pingOffset(pingMs);
        double early     = cfg.tooEarlyMs + offset;
        double perfect   = cfg.perfectMs  + offset;
        double sigma     = cfg.scoreSigma;

        if (ms < early)  return 0.0;
        if (ms < perfect) return (ms - early) / (perfect - early); // linear ramp
        double delta = ms - perfect;
        return Math.exp(-(delta * delta) / (2.0 * sigma * sigma)); // Gaussian tail
    }

    /**
     * Short one-line coaching hint shown below the ms readout.
     */
    public static String hint(double ms, double pingMs) {
        ModConfig cfg  = ModConfig.get();
        double offset  = ModConfig.pingOffset(pingMs);
        if (ms < cfg.tooEarlyMs + offset)  return "jumped too early";
        if (ms < cfg.perfectMs  + offset)  return "a bit early";
        if (ms <= cfg.perfectMaxMs + offset) return "perfect!";
        if (ms <= cfg.goodMaxMs  + offset) return "slightly late";
        if (ms <= cfg.lateMaxMs  + offset) return "too late";
        return "missed window";
    }

    /** Color from config (allows runtime theme changes). */
    public int configColor() {
        ModConfig cfg = ModConfig.get();
        return switch (this) {
            case PERFECT   -> cfg.colorPerfect;
            case GOOD      -> cfg.colorGood;
            case LATE      -> cfg.colorLate;
            case TOO_EARLY -> cfg.colorTooEarly;
            case MISSED    -> cfg.colorBad;
        };
    }

    /** True if this result should be shown in the HUD. MISSED is silent. */
    public boolean shouldShow() {
        return this != MISSED;
    }
}
