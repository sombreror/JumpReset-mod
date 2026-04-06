package com.jumpreset.ui;

import com.jumpreset.JumpResetMod;
import com.jumpreset.config.ModConfig;
import com.jumpreset.state.JumpResetResult;
import com.jumpreset.state.TimingResult;
import com.jumpreset.state.TrackingState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * CrosshairIndicator — v1.0.0
 *
 * Renders a small downward-pointing triangle above the crosshair.
 * The triangle color reflects the current jump reset system state:
 *
 *   ● GREEN  — recent result was PERFECT or GOOD (you nailed it)
 *   ● YELLOW — timing window is OPEN (you were hit, jump now!)
 *   ● RED    — recent result was LATE, TOO_EARLY, or MISSED
 *   ● HIDDEN — IDLE with no recent result → nothing shown
 *
 * The triangle fades out smoothly after the result display duration expires.
 * It's drawn as a filled pixel triangle (no textures needed):
 *
 *      ▼
 *   [crosshair center]
 *
 * Triangle is a downward-pointing solid shape:
 *   row 0 (top): full base width  (2*size + 1 px)
 *   row 1:       narrowed by 1 each side
 *   ...
 *   row size:    1 px tip
 *
 * Lightweight: pure pixel fills, no matrix transforms, no texture binds.
 */
public class CrosshairIndicator {

    /**
     * Render the triangle above the crosshair if appropriate.
     * Called from JumpResetHud.render() on every frame.
     */
    public void render(DrawContext ctx, MinecraftClient client) {
        ModConfig cfg = ModConfig.get();
        if (!cfg.enabled || !cfg.showCrosshairIndicator) return;
        if (client.player == null || client.inGameHud.getDebugHud().shouldShowDebugHud()) return;
        // Don't show while any screen (inventory, config, etc.) is open
        if (client.currentScreen != null) return;
        // Don't show while in F1 (hide HUD) mode
        if (!client.options.hudHidden) {
            // hudHidden==false means HUD IS visible, which is what we want
        }

        int sw = client.getWindow().getScaledWidth();
        int sh = client.getWindow().getScaledHeight();
        int cx = sw / 2; // crosshair center X
        int cy = sh / 2; // crosshair center Y

        TrackingState  state       = JumpResetMod.tracker.getState();
        JumpResetResult lastResult = JumpResetMod.tracker.lastResult;
        long lastResultTs          = JumpResetMod.tracker.lastResultTimestamp;
        long now                   = System.currentTimeMillis();
        long resultAge             = now - lastResultTs;

        int color;
        int alpha;

        if (state == TrackingState.WINDOW_ACTIVE) {
            // Window open — pulsing yellow to draw attention
            color = 0xFFFFCC00;
            alpha = pulseAlpha(now);

        } else if (lastResult != null && resultAge < cfg.displayDurationMs) {
            // Recent result — show appropriate color with fade
            TimingResult cls = lastResult.classification();
            color = switch (cls) {
                case PERFECT, GOOD -> cfg.colorPerfect; // green
                default            -> cfg.colorTooEarly; // red
            };
            // Fade: full opacity for 60% of duration, then fade out
            float progress = (float) resultAge / cfg.displayDurationMs;
            alpha = progress < 0.60f ? 220 : Math.max(0, (int)(220 * (1f - (progress - 0.60f) / 0.40f)));

        } else {
            // IDLE, no recent result → nothing to show
            return;
        }

        if (alpha <= 0) return;

        int size   = Math.max(2, cfg.crosshairTriangleSize);
        int tipY   = cy - cfg.crosshairIndicatorY;    // tip of triangle (lowest point)
        int baseY  = tipY - size;                      // widest row (topmost)

        drawTriangleDown(ctx, cx, baseY, size, blendA(color, alpha));
    }

    /**
     * Draw a downward-pointing filled triangle.
     *
     * @param ctx     draw context
     * @param cx      horizontal center
     * @param baseY   Y coordinate of the widest row (top of triangle)
     * @param size    half-width of base; total height = size rows
     * @param color   ARGB color
     */
    private static void drawTriangleDown(DrawContext ctx, int cx, int baseY, int size, int color) {
        // Draw row by row: row 0 = full base, row size-1 = 1px tip
        for (int row = 0; row < size; row++) {
            int halfW = size - row;               // half-width at this row
            int x     = cx - halfW;
            int y     = baseY + row;
            int w     = halfW * 2 + 1;
            if (w > 0) ctx.fill(x, y, x + w, y + 1, color);
        }
        // Single-pixel tip
        ctx.fill(cx, baseY + size, cx + 1, baseY + size + 1, color);
    }

    /** Smooth sinusoidal pulse: α oscillates between 140 and 255 at ~1.5 Hz. */
    private static int pulseAlpha(long nowMs) {
        double phase = (nowMs % 666) / 666.0; // 0→1 over 666ms (~1.5 Hz)
        double sine  = Math.sin(phase * 2 * Math.PI);
        return 140 + (int)(sine * 57.5 + 57.5); // range [140, 255]
    }

    private static int blendA(int argb, int alpha) {
        return (((argb >>> 24) & 0xFF) * alpha / 255 << 24) | (argb & 0x00FFFFFF);
    }
}
