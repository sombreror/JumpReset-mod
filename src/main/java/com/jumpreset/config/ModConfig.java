package com.jumpreset.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * ModConfig — v2.0.0
 *
 * Added in v1.9.0:
 *  - showCrosshairIndicator (triangle above crosshair)
 *  - crosshairTriangleSize (pixels, default 5)
 *  - crosshairIndicatorY   (vertical offset from crosshair center, default 12)
 *
 * Note: jumpDeltaThreshold is still active — it gates the delta-vy jump
 * detection in JumpResetTracker (vyDelta >= cfg.jumpDeltaThreshold).
 */
public class ModConfig {

    private static final Logger LOGGER   = LoggerFactory.getLogger("JumpReset");
    private static final Gson   GSON     = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILENAME = "jumpreset.json";
    private static ModConfig    INSTANCE = null;

    // ── General ───────────────────────────────────────────────────────────────
    public boolean          enabled          = true;
    public boolean          debugMode        = false;
    public DebugDisplayMode debugDisplayMode = DebugDisplayMode.FULL;

    // ── Feedback style ────────────────────────────────────────────────────────
    public FeedbackStyle feedbackStyle = FeedbackStyle.DETAILED;

    // ── Timing thresholds (ms) ────────────────────────────────────────────────
    public double tooEarlyMs   = 15.0;
    public double perfectMs    = 80.0;
    public double perfectMaxMs = 150.0;
    public double goodMaxMs    = 250.0;
    public double lateMaxMs    = 380.0;
    public double scoreSigma   = 60.0;

    // ── Ping compensation ─────────────────────────────────────────────────────
    public boolean autoPingAdjust = true;
    public double  pingCompFactor = 0.5;

    // ── Detection ─────────────────────────────────────────────────────────────
    public double knockbackThreshold  = 0.065;
    public double jumpDeltaThreshold  = 0.25;
    public int    windowTicksGround   = 6;
    public int    windowTicksAir     = 10;

    // ── HUD position & appearance ─────────────────────────────────────────────
    public float   hudX          = 0.5f;
    public float   hudY          = 0.65f;
    public float   hudScale      = 1.0f;
    public float   hudOpacity    = 1.0f;
    public boolean hudLocked     = false;

    // ── Visual toggles ────────────────────────────────────────────────────────
    public boolean showLabel       = true;
    public boolean showMs          = true;
    public boolean showHint        = true;
    public boolean showScoreBar    = true;
    public boolean animateSlideIn  = true;
    public boolean textShadow      = false;
    public boolean showHistory     = true;
    public int     historyCount    = 5;
    public boolean showMissed      = false;
    public int     displayDurationMs = 1800;

    // ── Crosshair indicator ───────────────────────────────────────────────────
    /** Whether to show the colored triangle above the crosshair. */
    public boolean showCrosshairIndicator = true;
    /** Half-width of the triangle base in pixels. 5 → 11px wide total. */
    public int     crosshairTriangleSize  = 5;
    /** Distance from crosshair centre to triangle tip, in pixels. */
    public int     crosshairIndicatorY    = 14;

    // ── Colors (ARGB) ─────────────────────────────────────────────────────────
    public int colorPerfect  = 0xFF00E87A;
    public int colorGood     = 0xFF00DDEE;
    public int colorLate     = 0xFFFF8800;
    public int colorTooEarly = 0xFFFF3333;
    public int colorBad      = 0xFFFF3333;

    // ─────────────────────────────────────────────────────────────────────────

    public static ModConfig get() {
        if (INSTANCE == null) load();
        return INSTANCE;
    }

    public static void load() {
        Path path = configPath();
        if (Files.exists(path)) {
            try (Reader r = Files.newBufferedReader(path)) {
                INSTANCE = GSON.fromJson(r, ModConfig.class);
                if (INSTANCE == null) INSTANCE = new ModConfig();
            } catch (IOException | com.google.gson.JsonSyntaxException e) {
                LOGGER.warn("Config load failed, using defaults: {}", e.getMessage());
                INSTANCE = new ModConfig();
            }
        } else {
            INSTANCE = new ModConfig();
            save();
        }
        INSTANCE.sanitize();
    }

    public static void save() {
        if (INSTANCE == null) return;
        try {
            Path path = configPath();
            Files.createDirectories(path.getParent());
            try (Writer w = Files.newBufferedWriter(path)) {
                GSON.toJson(INSTANCE, w);
            }
        } catch (IOException e) {
            LOGGER.error("Config save failed: {}", e.getMessage());
        }
    }

    /**
     * Clamp values to sane ranges and enforce the monotonic ordering the timing
     * model relies on ({@code tooEarlyMs < perfectMs ≤ perfectMaxMs ≤ goodMaxMs
     * ≤ lateMaxMs}). Runs after every {@link #load()} so a hand-edited or
     * out-of-date config file can never break classification or crash the HUD.
     */
    public void sanitize() {
        // Timing thresholds: positive, and strictly/weakly increasing in order.
        tooEarlyMs   = clamp(tooEarlyMs,   1,   500);
        perfectMs    = Math.max(tooEarlyMs + 1, clamp(perfectMs,    1, 1000));
        perfectMaxMs = Math.max(perfectMs,      clamp(perfectMaxMs, 1, 1500));
        goodMaxMs    = Math.max(perfectMaxMs,   clamp(goodMaxMs,    1, 2000));
        lateMaxMs    = Math.max(goodMaxMs,      clamp(lateMaxMs,    1, 3000));
        scoreSigma   = clamp(scoreSigma, 1, 1000);

        pingCompFactor     = clamp(pingCompFactor, 0, 1);
        knockbackThreshold = clamp(knockbackThreshold, 0, 1);
        jumpDeltaThreshold = clamp(jumpDeltaThreshold, 0.01, 1);
        windowTicksGround  = (int) clamp(windowTicksGround, 1, 40);
        windowTicksAir     = (int) clamp(windowTicksAir,    1, 40);

        // HUD geometry / appearance.
        hudX       = (float) clamp(hudX, 0, 1);
        hudY       = (float) clamp(hudY, 0, 1);
        hudScale   = (float) clamp(hudScale,   0.5, 2.0);
        hudOpacity = (float) clamp(hudOpacity, 0.0, 1.0);
        historyCount          = (int) clamp(historyCount, 1, 10);
        displayDurationMs     = (int) clamp(displayDurationMs, 200, 10_000);
        crosshairTriangleSize = (int) clamp(crosshairTriangleSize, 2, 12);
        crosshairIndicatorY   = (int) clamp(crosshairIndicatorY, 0, 100);

        // Gson leaves these null if the JSON holds an unrecognised value.
        if (feedbackStyle == null)    feedbackStyle    = FeedbackStyle.DETAILED;
        if (debugDisplayMode == null) debugDisplayMode = DebugDisplayMode.FULL;
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : v > hi ? hi : v;
    }

    /**
     * Ping offset added to all timing thresholds when autoPingAdjust is on.
     *   0–40 ms  → +0
     *   40–100   → +30
     *   100–180  → +60
     *   180+     → +100
     */
    public static double pingOffset(double pingMs) {
        if (!get().autoPingAdjust) return 0.0;
        if (pingMs < 40)  return 0.0;
        if (pingMs < 100) return 30.0;
        if (pingMs < 180) return 60.0;
        return 100.0;
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILENAME);
    }
}
