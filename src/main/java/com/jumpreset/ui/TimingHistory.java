package com.jumpreset.ui;

import com.jumpreset.config.ModConfig;
import com.jumpreset.state.JumpResetResult;
import com.jumpreset.state.TimingResult;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * TimingHistory — v1.5.0
 *
 * Displays a strip of the last N jump reset results as colored dots/squares.
 * Positioned just below the main HUD panel, or independently.
 *
 * Layout example (5 attempts, left = oldest):
 *   [■] [■] [■] [■] [■]
 *    G   P   L   P   G
 *
 * Each cell is a colored square using the result's color.
 * Letter abbreviation shown below (P/G/L/E).
 *
 * This is lightweight: no allocations per frame, just iterating a small deque.
 */
public class TimingHistory {

    private static final int MAX_HISTORY = 10;
    private static final int CELL_SIZE   = 8;
    private static final int CELL_GAP    = 3;
    private static final int LABEL_H     = 8; // small font label below each cell

    // Ring buffer of last results
    private final Deque<TimingResult> history = new ArrayDeque<>();

    /** Called by JumpResetHud.showResult() for every new result. */
    public void record(TimingResult result) {
        if (result == TimingResult.MISSED) return; // don't clutter history with misses
        if (history.size() >= MAX_HISTORY) history.pollFirst();
        history.addLast(result);
    }

    public void clear() { history.clear(); }

    /**
     * Render below the main panel anchor.
     * anchorX = centre X of the main panel
     * anchorY = bottom Y of the main panel + gap
     */
    public void render(DrawContext ctx, MinecraftClient client, int anchorX, int anchorY, float opacity) {
        ModConfig cfg = ModConfig.get();
        if (!cfg.showHistory || history.isEmpty()) return;

        int n     = Math.min(history.size(), cfg.historyCount);
        int totalW = n * CELL_SIZE + (n - 1) * CELL_GAP;
        int startX = anchorX - totalW / 2;
        int bgW   = totalW + 8;
        int bgH   = CELL_SIZE + LABEL_H + 6;
        int bgX   = startX - 4;
        int bgY   = anchorY;

        int baseAlpha = (int)(opacity * 200); // slightly more transparent than main panel

        // Background
        fill(ctx, bgX, bgY, bgW, bgH, blendA(0xCC080810, baseAlpha));
        border(ctx, bgX, bgY, bgW, bgH, blendA(0xFF1A1A30, baseAlpha));

        TextRenderer tr = client.textRenderer;
        int i = 0;
        // Iterate from oldest to newest (pollFirst gives oldest)
        TimingResult[] arr = history.toArray(new TimingResult[0]);
        int start = Math.max(0, arr.length - n); // show only last n
        for (int j = start; j < arr.length; j++) {
            TimingResult r   = arr[j];
            int           cx = startX + i * (CELL_SIZE + CELL_GAP);
            int           cy = bgY + 3;

            // Colored square
            fill(ctx, cx, cy, CELL_SIZE, CELL_SIZE, blendA(r.configColor(), baseAlpha));

            // 1-letter abbreviation below
            String abbr = abbrev(r);
            int    aw   = tr.getWidth(abbr);
            ctx.drawText(tr, Text.literal(abbr),
                    cx + CELL_SIZE / 2 - aw / 2,
                    cy + CELL_SIZE + 1,
                    blendA(r.configColor(), Math.min(255, baseAlpha + 40)),
                    false);
            i++;
        }
    }

    private static String abbrev(TimingResult r) {
        return switch (r) {
            case PERFECT   -> "P";
            case GOOD      -> "G";
            case LATE      -> "L";
            case TOO_EARLY -> "E";
            default        -> "?";
        };
    }

    /** Returns width of the history strip at current historyCount. */
    public static int stripWidth(int n) {
        return n * CELL_SIZE + (n - 1) * CELL_GAP + 8;
    }

    public int size() { return history.size(); }

    private static void fill(DrawContext ctx, int x, int y, int w, int h, int color) {
        if (w > 0 && h > 0) ctx.fill(x, y, x+w, y+h, color);
    }
    private static void border(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x,     y,     x+w,   y+1,   color);
        ctx.fill(x,     y+h-1, x+w,   y+h,   color);
        ctx.fill(x,     y+1,   x+1,   y+h-1, color);
        ctx.fill(x+w-1, y+1,   x+w,   y+h-1, color);
    }
    private static int blendA(int argb, int alpha) {
        int ex = (argb >>> 24) & 0xFF;
        return ((ex * alpha / 255) << 24) | (argb & 0x00FFFFFF);
    }
}
