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
 * Classification is threshold-based and matches both the {@link #hint} text
 * and the on-screen "bar" zones exactly (every Timing-tab slider affects it):
 *  ms < tooEarlyMs                 → TOO_EARLY ("jumped too early")
 *  tooEarlyMs ≤ ms < perfectMs     → GOOD      ("a bit early")
 *  perfectMs  ≤ ms ≤ perfectMaxMs  → PERFECT   ("perfect!")
 *  perfectMaxMs < ms ≤ goodMaxMs   → GOOD      ("slightly late")
 *  goodMaxMs    < ms ≤ lateMaxMs   → LATE      ("too late")
 *  ms > lateMaxMs                  → MISSED    (silent)
 *
 * The numeric {@link #score} (used only for the score bar and session average)
 * is a separate, smooth curve:
 *  ms < tooEarlyMs              → score = 0   (hard floor)
 *  tooEarlyMs ≤ ms < perfectMs  → linear ramp 0→1
 *  ms == perfectMs              → score = 1.0  (peak)
 *  ms > perfectMs               → Gaussian decay with sigma from config
 *
 * Ping adjustment: all thresholds shift up by pingOffset(ping).
 */
public enum TimingResult {

    PERFECT  ("PERFECT"),
    GOOD     ("GOOD"),
    LATE     ("LATE"),
    TOO_EARLY("TOO EARLY"),
    MISSED   ("MISSED");  // silent — never shown in the HUD

    public final String label;

    TimingResult(String label) {
        this.label = label;
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

        double early      = cfg.tooEarlyMs   + offset;
        double perfect    = cfg.perfectMs    + offset;
        double perfectMax = cfg.perfectMaxMs + offset;
        double goodMax    = cfg.goodMaxMs    + offset;
        double lateMax    = cfg.lateMaxMs    + offset;

        if (ms < early)        return TOO_EARLY;
        if (ms < perfect)      return GOOD;        // a bit early but acceptable
        if (ms <= perfectMax)  return PERFECT;
        if (ms <= goodMax)     return GOOD;        // slightly late
        if (ms <= lateMax)     return LATE;
        return MISSED;
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
        // Guard against a degenerate config where tooEarlyMs >= perfectMs.
        double ramp = Math.max(1.0, perfect - early);
        if (ms < perfect) return (ms - early) / ramp; // linear ramp
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

    /**
     * Whether this result should be shown in the HUD. Every classification is
     * shown except MISSED, which appears only when the user enables {@code showMissed}.
     */
    public boolean shouldShow() {
        return this != MISSED || ModConfig.get().showMissed;
    }
}
