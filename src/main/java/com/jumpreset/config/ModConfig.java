package com.jumpreset.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * ModConfig — v1.9.0
 *
 * Added:
 *  - showCrosshairIndicator (triangle above crosshair)
 *  - crosshairTriangleSize (pixels, default 5)
 *  - crosshairIndicatorY   (vertical offset from crosshair center, default 12)
 * Removed:
 *  - jumpDeltaThreshold (dead field, was never used in detection logic)
 */
public class ModConfig {

    private static final Gson   GSON     = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILENAME = "jumpreset.json";
    private static ModConfig    INSTANCE = null;

    // ── General ───────────────────────────────────────────────────────────────
    public boolean enabled         = true;
    public boolean debugMode       = false;
    public String  debugDisplayMode = "full"; // "compact" | "full"

    // ── Feedback style ────────────────────────────────────────────────────────
    // "minimal" | "detailed" | "bar"
    public String feedbackStyle = "detailed";

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
                System.err.println("[JumpReset] Config load failed, using defaults: " + e.getMessage());
                INSTANCE = new ModConfig();
            }
        } else {
            INSTANCE = new ModConfig();
            save();
        }
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
            System.err.println("[JumpReset] Config save failed: " + e.getMessage());
        }
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
