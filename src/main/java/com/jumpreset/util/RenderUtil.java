package com.jumpreset.util;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * Shared 2D drawing primitives for the HUD and config screens.
 *
 * <p>Previously these helpers ({@code fill}, {@code border}, {@code blendA})
 * were copy-pasted across {@code JumpResetHud}, {@code TimingHistory} and
 * {@code CrosshairIndicator}. Centralising them removes the duplication and
 * guarantees consistent behaviour (e.g. the zero-size guard in {@link #fill}).
 */
public final class RenderUtil {

    private RenderUtil() {}

    /** Fill a rectangle, skipping degenerate (non-positive) sizes. */
    public static void fill(DrawContext ctx, int x, int y, int w, int h, int color) {
        if (w > 0 && h > 0) ctx.fill(x, y, x + w, y + h, color);
    }

    /** Draw a 1-px rectangular outline. */
    public static void border(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x,         y,         x + w,     y + 1,     color);
        ctx.fill(x,         y + h - 1, x + w,     y + h,     color);
        ctx.fill(x,         y + 1,     x + 1,     y + h - 1, color);
        ctx.fill(x + w - 1, y + 1,     x + w,     y + h - 1, color);
    }

    /**
     * Scale the alpha channel of an ARGB colour by {@code alpha} ∈ [0, 255].
     * The existing alpha is multiplied by {@code alpha/255}, so a fully opaque
     * colour blended with 128 becomes ~50 % transparent.
     */
    public static int blendA(int argb, int alpha) {
        int existing = (argb >>> 24) & 0xFF;
        return ((existing * alpha / 255) << 24) | (argb & 0x00FFFFFF);
    }

    /**
     * Draw text at an arbitrary scale. Uses the fast path (no matrix push) when
     * the scale is effectively 1.0 to avoid needless GPU state changes.
     */
    public static void drawScaledText(DrawContext ctx, TextRenderer tr, String text,
                                      int x, int y, int color, float scale, boolean shadow) {
        if (scale > 0.95f && scale < 1.05f) {
            ctx.drawText(tr, Text.literal(text), x, y, color, shadow);
        } else {
            ctx.getMatrices().pushMatrix();
            ctx.getMatrices().translate(x, y);
            ctx.getMatrices().scale(scale, scale);
            ctx.drawText(tr, Text.literal(text), 0, 0, color, shadow);
            ctx.getMatrices().popMatrix();
        }
    }
}
