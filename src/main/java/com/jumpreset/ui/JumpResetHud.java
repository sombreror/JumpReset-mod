package com.jumpreset.ui;

import com.jumpreset.config.ModConfig;
import com.jumpreset.state.JumpResetResult;
import com.jumpreset.state.TimingResult;
import com.jumpreset.util.Easing;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * JumpResetHud — v1.7.0
 *
 * Visual improvements:
 *  - Cleaner color palette with proper alpha layering
 *  - Rounded-feel panel with subtle inner highlight line
 *  - Accent bar is now a gradient-feel gradient (3-px accent + 1-px highlight)
 *  - Bar style: smoother zone gradients, cleaner marker
 *  - Minimal style: small pill-shaped label with fade
 *  - Debug overlay: cleaner layout with icon prefix
 *  - All styles: consistent typography and spacing
 */
public class JumpResetHud {

    // Panel geometry
    public static final int BASE_W = 128;
    public static final int BASE_H = 52;
    private static final int BAR_W = 148;
    private static final int BAR_H = 32;

    private static final int ACCENT_W  = 3;
    private static final int PAD_X     = ACCENT_W + 8;
    private static final int PAD_Y     = 6;
    private static final int LINE_H    = 11;
    private static final int SCORE_H   = 3;

    // Color palette — dark navy theme
    private static final int C_BG          = 0xEA060612;   // near-black navy
    private static final int C_BORDER      = 0xFF1C1C35;   // subtle border
    private static final int C_HIGHLIGHT   = 0x18FFFFFF;   // inner top highlight
    private static final int C_BAR_TRACK   = 0xFF111122;   // score bar track
    private static final int C_MS          = 0xFFCCCCDD;   // ms text
    private static final int C_HINT        = 0xFF606080;   // hint text
    private static final int C_DBG_BG      = 0xD0050510;
    private static final int C_DBG_GOOD    = 0xFF00E890;
    private static final int C_DBG_BAD     = 0xFFFF5566;
    private static final int C_DBG_NEUT    = 0xFF8899BB;
    private static final int C_DBG_BORDER  = 0xFF1A1A40;

    // State
    private JumpResetResult activeResult = null;
    private long            showStartMs  = 0L;
    public  TimingHistory   history      = new TimingHistory();

    // ── Public API ────────────────────────────────────────────────────────────

    public void showResult(JumpResetResult result) {
        this.activeResult = result;
        this.showStartMs  = System.currentTimeMillis();
        history.record(result.classification());
    }

    public void render(DrawContext ctx, MinecraftClient client) {
        ModConfig cfg     = ModConfig.get();
        float     opacity = clamp01(cfg.hudOpacity);

        if (cfg.debugMode) renderDebug(ctx, client, cfg);

        clearIfExpired(cfg);
        if (activeResult == null) {
            renderHistoryAlone(ctx, client, cfg, opacity);
            return;
        }

        long  elapsed  = System.currentTimeMillis() - showStartMs;
        float progress = Math.min(1f, (float) elapsed / cfg.displayDurationMs);
        int   alpha    = (int)(Easing.computeAlpha(progress) * opacity);
        if (alpha <= 0) { activeResult = null; return; }

        switch (cfg.feedbackStyle) {
            case "minimal"  -> renderMinimal (ctx, client, cfg, elapsed, alpha);
            case "bar"      -> renderBar     (ctx, client, cfg, elapsed, alpha);
            default         -> renderDetailed(ctx, client, cfg, elapsed, alpha);
        }
    }

    // ── MINIMAL ───────────────────────────────────────────────────────────────

    private void renderMinimal(DrawContext ctx, MinecraftClient client,
                               ModConfig cfg, long elapsed, int alpha) {
        int   sw    = client.getWindow().getScaledWidth();
        int   sh    = client.getWindow().getScaledHeight();
        float scale = clampScale(cfg.hudScale);
        TextRenderer tr = client.textRenderer;

        TimingResult cls   = activeResult.classification();
        int          color = blendA(cls.configColor(), alpha);
        String       label = cls.label;
        int          lw    = (int)(tr.getWidth(label) * scale);

        int cx = (int)(cfg.hudX * sw);
        int cy = (int)(cfg.hudY * sh);
        if (cfg.animateSlideIn) {
            float t = Math.min(1f, elapsed / 120f);
            cy += (int)((1f - Easing.easeOutCubic(t)) * 8 * scale);
        }

        // Pill background
        int pillPadX = (int)(6 * scale), pillPadY = (int)(3 * scale);
        int pillW = lw + pillPadX * 2, pillH = (int)(10 * scale) + pillPadY * 2;
        int pillX = cx - pillW / 2, pillY = cy - pillH / 2;
        fill(ctx, pillX, pillY, pillW, pillH, blendA(C_BG, alpha));
        border(ctx, pillX, pillY, pillW, pillH, blendA(cls.configColor(), alpha / 3));

        drawText(ctx, tr, label, pillX + pillPadX, pillY + pillPadY, color, scale, cfg.textShadow);

        history.render(ctx, client, cx, cy + pillH / 2 + 5, cfg.hudOpacity);
    }

    // ── DETAILED ──────────────────────────────────────────────────────────────

    private void renderDetailed(DrawContext ctx, MinecraftClient client,
                                ModConfig cfg, long elapsed, int alpha) {
        int   sw     = client.getWindow().getScaledWidth();
        int   sh     = client.getWindow().getScaledHeight();
        float scale  = clampScale(cfg.hudScale);
        int   panelW = (int)(BASE_W * scale);
        int   panelH = computePanelH(cfg, scale);
        int   panelX = clampX((int)(cfg.hudX * sw) - panelW / 2, panelW, sw);
        int   panelY = clampY((int)(cfg.hudY * sh) - panelH / 2, panelH, sh);

        if (cfg.animateSlideIn) {
            float t = Math.min(1f, elapsed / 140f);
            panelY += (int)((1f - Easing.easeOutCubic(t)) * 10 * scale);
        }

        TimingResult cls    = activeResult.classification();
        int          accent = blendA(cls.configColor(), alpha);
        int          bgA    = blendA(C_BG, alpha);
        int          borA   = blendA(C_BORDER, alpha);

        // Panel body
        fill(ctx, panelX, panelY, panelW, panelH, bgA);
        // Inner top highlight (glass feel)
        fill(ctx, panelX, panelY, panelW, Math.max(1, (int)(1 * scale)), blendA(C_HIGHLIGHT, alpha));
        // Accent bar
        int aw = Math.max(1, (int)(ACCENT_W * scale));
        fill(ctx, panelX, panelY, aw, panelH, accent);
        // Faint accent glow (1px wider, half alpha)
        fill(ctx, panelX + aw, panelY, 1, panelH, blendA(cls.configColor(), alpha / 6));
        border(ctx, panelX, panelY, panelW, panelH, borA);

        TextRenderer tr   = client.textRenderer;
        int tx = panelX + (int)(PAD_X * scale);
        int ty = panelY + (int)(PAD_Y * scale);
        int st = (int)(LINE_H * scale);

        if (cfg.showLabel) {
            drawText(ctx, tr, cls.label, tx, ty, accent, scale, cfg.textShadow); ty += st;
        }
        if (cfg.showMs) {
            String ms = String.format("%.0f ms", Math.max(0, activeResult.millis()));
            drawText(ctx, tr, ms, tx, ty, blendA(C_MS, alpha), scale, cfg.textShadow); ty += st;
        }
        if (cfg.showHint) {
            String h = TimingResult.hint(activeResult.millis(), activeResult.pingMs());
            drawText(ctx, tr, h, tx, ty, blendA(C_HINT, alpha), scale, cfg.textShadow);
        }
        if (cfg.showScoreBar) {
            int bx = panelX + aw;
            int by = panelY + panelH - Math.max(1, (int)(SCORE_H * scale)) - 1;
            int bw = panelW - aw;
            int bh = Math.max(1, (int)(SCORE_H * scale));
            int fw = Math.max(1, (int)(bw * activeResult.score()));
            fill(ctx, bx, by, bw, bh, blendA(C_BAR_TRACK, alpha));
            fill(ctx, bx, by, fw, bh, accent);
        }

        history.render(ctx, client, panelX + panelW / 2, panelY + panelH + 4, cfg.hudOpacity);
    }

    // ── BAR ───────────────────────────────────────────────────────────────────

    private void renderBar(DrawContext ctx, MinecraftClient client,
                           ModConfig cfg, long elapsed, int alpha) {
        int   sw     = client.getWindow().getScaledWidth();
        int   sh     = client.getWindow().getScaledHeight();
        float scale  = clampScale(cfg.hudScale);
        int   panelW = (int)(BAR_W * scale);
        int   panelH = (int)(BAR_H * scale);
        int   panelX = clampX((int)(cfg.hudX * sw) - panelW / 2, panelW, sw);
        int   panelY = clampY((int)(cfg.hudY * sh) - panelH / 2, panelH, sh);

        if (cfg.animateSlideIn) {
            float t = Math.min(1f, elapsed / 140f);
            panelY += (int)((1f - Easing.easeOutCubic(t)) * 8 * scale);
        }

        TimingResult cls    = activeResult.classification();
        double       ms     = activeResult.millis();
        double       ping   = activeResult.pingMs();
        double       offset = ModConfig.pingOffset(ping);

        fill(ctx, panelX, panelY, panelW, panelH, blendA(C_BG, alpha));
        fill(ctx, panelX, panelY, panelW, 1, blendA(C_HIGHLIGHT, alpha));
        border(ctx, panelX, panelY, panelW, panelH, blendA(C_BORDER, alpha));

        // ── Timing bar ────────────────────────────────────────────────────
        int bPad = (int)(8 * scale);
        int bx   = panelX + bPad;
        int bw   = panelW - bPad * 2;
        int bh   = (int)(7 * scale);
        int by   = panelY + (int)(7 * scale);

        double rangeStart   = cfg.tooEarlyMs  + offset;
        double rangeEnd     = cfg.lateMaxMs   + offset;
        double range        = Math.max(1, rangeEnd - rangeStart);
        double perfectStart = cfg.perfectMs   + offset;
        double perfectEnd   = cfg.perfectMaxMs + offset;
        double goodEnd      = cfg.goodMaxMs   + offset;

        int xPerfStart = bx + (int)(bw * ((perfectStart - rangeStart) / range));
        int xPerfEnd   = bx + (int)(bw * ((perfectEnd   - rangeStart) / range));
        int xGoodEnd   = bx + (int)(bw * ((goodEnd      - rangeStart) / range));
        xPerfStart = clamp(xPerfStart, bx, bx + bw);
        xPerfEnd   = clamp(xPerfEnd,   bx, bx + bw);
        xGoodEnd   = clamp(xGoodEnd,   bx, bx + bw);

        // Zone fills
        fill(ctx, bx,         by, xPerfStart - bx,       bh, blendA(0xCC5C1111, alpha)); // early
        fill(ctx, xPerfStart, by, xPerfEnd - xPerfStart,  bh, blendA(0xCC0F5C2E, alpha)); // perfect
        fill(ctx, xPerfEnd,   by, xGoodEnd  - xPerfEnd,   bh, blendA(0xCC0F3050, alpha)); // good
        fill(ctx, xGoodEnd,   by, bx + bw   - xGoodEnd,   bh, blendA(0xCC5C3000, alpha)); // late

        // Zone borders
        int sepA = blendA(0xFF2A3550, alpha);
        fill(ctx, xPerfStart - 1, by, 1, bh, sepA);
        fill(ctx, xPerfEnd   - 1, by, 1, bh, sepA);
        fill(ctx, xGoodEnd   - 1, by, 1, bh, sepA);

        // Bar outer border
        border(ctx, bx, by, bw, bh, blendA(0xFF1A2030, alpha));

        // ── Marker ────────────────────────────────────────────────────────
        double clampedMs = Math.max(rangeStart, Math.min(rangeEnd, ms));
        int    markerX   = bx + (int)(bw * ((clampedMs - rangeStart) / range));
        markerX = clamp(markerX, bx, bx + bw - 1);

        int markerCol = blendA(cls.configColor(), alpha);
        // Drop shadow
        fill(ctx, markerX - 1, by - 1, 4, bh + 2, blendA(0xFF000000, alpha / 2));
        // Marker line
        fill(ctx, markerX,     by, 2, bh, markerCol);
        // Cap triangle above
        fill(ctx, markerX - 1, by - 3, 4, 3, markerCol);
        fill(ctx, markerX,     by - 4, 2, 1, blendA(cls.configColor(), alpha / 2));

        // ── Direction arrows ──────────────────────────────────────────────
        TextRenderer tr = client.textRenderer;
        if (cls == TimingResult.TOO_EARLY) {
            ctx.drawText(tr, Text.literal("◄"), bx - 8, by + 1, blendA(cfg.colorTooEarly, alpha), false);
        } else if (cls == TimingResult.LATE) {
            ctx.drawText(tr, Text.literal("►"), bx + bw + 2, by + 1, blendA(cfg.colorLate, alpha), false);
        }

        // ── Label + ms below bar ──────────────────────────────────────────
        int   ly  = by + bh + (int)(4 * scale);
        int   col = blendA(cls.configColor(), alpha);
        if (cfg.showLabel) {
            String label = cls.label;
            int    lw    = tr.getWidth(label);
            drawText(ctx, tr, label, panelX + panelW / 2 - lw / 2, ly, col, scale, cfg.textShadow);
        }
        if (cfg.showMs) {
            String msStr = String.format("%.0f ms", Math.max(0, ms));
            int    mw    = (int)(tr.getWidth(msStr) * scale);
            drawText(ctx, tr, msStr, panelX + panelW - bPad - mw, ly,
                    blendA(C_MS, alpha), scale, cfg.textShadow);
        }

        history.render(ctx, client, panelX + panelW / 2, panelY + panelH + 4, cfg.hudOpacity);
    }

    // ── DEBUG OVERLAY ─────────────────────────────────────────────────────────

    private void renderDebug(DrawContext ctx, MinecraftClient client, ModConfig cfg) {
        double ping = 0;
        if (client.getNetworkHandler() != null && client.player != null) {
            var e = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
            if (e != null) ping = e.getLatency();
        }
        double offset = ModConfig.pingOffset(ping);
        boolean full  = "full".equals(cfg.debugDisplayMode);

        TextRenderer tr = client.textRenderer;
        String l1, l2 = null;
        int col1, col2 = C_DBG_NEUT;

        if (activeResult != null) {
            double ms    = activeResult.millis();
            double ideal = cfg.perfectMs + offset;
            double delta = ms - ideal;
            col1  = (ms >= cfg.tooEarlyMs + offset && ms <= cfg.goodMaxMs + offset)
                    ? C_DBG_GOOD : C_DBG_BAD;
            l1    = String.format("⏱ %.0f ms  Δ%+.0f  ping %.0f ms", ms, delta, ping);
            if (full) {
                l2   = String.format("win %.0f–%.0f ms  adj +%.0f ms",
                        cfg.tooEarlyMs + offset, cfg.goodMaxMs + offset, offset);
                col2 = offset > 0 ? 0xFFFFCC55 : C_DBG_NEUT;
            }
        } else {
            col1 = C_DBG_NEUT;
            l1   = String.format("⏱ ping %.0f ms  adj +%.0f ms", ping, offset);
            if (full) {
                l2 = String.format("ideal %.0f ms  win %.0f–%.0f ms",
                        cfg.perfectMs + offset, cfg.tooEarlyMs + offset, cfg.goodMaxMs + offset);
            }
        }

        int dw = Math.max(tr.getWidth(l1), l2 != null ? tr.getWidth(l2) : 0) + 12;
        int dh = l2 != null ? 28 : 16;
        int dx = 4, dy = 4;

        fill  (ctx, dx, dy, dw, dh, C_DBG_BG);
        border(ctx, dx, dy, dw, dh, C_DBG_BORDER);
        ctx.drawText(tr, Text.literal(l1), dx + 5, dy + 4, col1, false);
        if (l2 != null) ctx.drawText(tr, Text.literal(l2), dx + 5, dy + 16, col2, false);
    }

    // ── PREVIEW (ConfigScreen) ────────────────────────────────────────────────

    public int[] renderPreview(DrawContext ctx, MinecraftClient client, boolean highlighted) {
        ModConfig cfg = ModConfig.get();
        int   sw     = client.getWindow().getScaledWidth();
        int   sh     = client.getWindow().getScaledHeight();
        float scale  = clampScale(cfg.hudScale);

        int panelW, panelH;
        if ("bar".equals(cfg.feedbackStyle)) {
            panelW = (int)(BAR_W * scale);
            panelH = (int)(BAR_H * scale);
        } else {
            panelW = (int)(BASE_W * scale);
            panelH = computePanelH(cfg, scale);
        }
        int panelX = clampX((int)(cfg.hudX * sw) - panelW / 2, panelW, sw);
        int panelY = clampY((int)(cfg.hudY * sh) - panelH / 2, panelH, sh);

        int bc = highlighted ? 0xFFFFFFAA : 0x8800CCEE;
        fill  (ctx, panelX, panelY, panelW, panelH, C_BG);
        fill  (ctx, panelX, panelY, panelW, 1, C_HIGHLIGHT);
        fill  (ctx, panelX, panelY, Math.max(1, (int)(ACCENT_W * scale)), panelH, cfg.colorPerfect);
        border(ctx, panelX, panelY, panelW, panelH, bc);

        TextRenderer tr = client.textRenderer;
        int tx = panelX + (int)(PAD_X * scale);
        int ty = panelY + (int)(PAD_Y * scale);
        int st = (int)(LINE_H * scale);
        if (cfg.showLabel) { drawText(ctx, tr, "PERFECT", tx, ty, cfg.colorPerfect, scale, false); ty += st; }
        if (cfg.showMs)    { drawText(ctx, tr, "82 ms",   tx, ty, C_MS,             scale, false); ty += st; }
        if (cfg.showHint)  { drawText(ctx, tr, "perfect!", tx, ty, C_HINT,          scale, false); }

        if (highlighted) {
            ctx.drawText(tr, Text.literal("§e⇥ drag to move"), panelX + 2, panelY - 12, 0xFFFFFFFF, false);
        }
        return new int[]{ panelX, panelY, panelW, panelH };
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void renderHistoryAlone(DrawContext ctx, MinecraftClient client,
                                    ModConfig cfg, float opacity) {
        if (!cfg.showHistory || history.size() == 0) return;
        int   sw    = client.getWindow().getScaledWidth();
        int   sh    = client.getWindow().getScaledHeight();
        float scale = clampScale(cfg.hudScale);
        int   cx    = (int)(cfg.hudX * sw);
        int   cy    = (int)(cfg.hudY * sh) + (int)(BASE_H * scale / 2) + 4;
        history.render(ctx, client, cx, cy, opacity);
    }

    private void clearIfExpired(ModConfig cfg) {
        if (activeResult != null
                && System.currentTimeMillis() - showStartMs >= cfg.displayDurationMs) {
            activeResult = null;
        }
    }

    private int computePanelH(ModConfig cfg, float scale) {
        int lines = 0;
        if (cfg.showLabel) lines++;
        if (cfg.showMs)    lines++;
        if (cfg.showHint)  lines++;
        int text = (int)((PAD_Y * 2 + lines * LINE_H) * scale);
        int bar  = cfg.showScoreBar ? (int)((SCORE_H + 2) * scale) : 0;
        return Math.max((int)(BASE_H * scale), text + bar);
    }

    private static void drawText(DrawContext ctx, TextRenderer tr, String text,
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

    private static void fill(DrawContext ctx, int x, int y, int w, int h, int c) {
        if (w > 0 && h > 0) ctx.fill(x, y, x + w, y + h, c);
    }

    private static void border(DrawContext ctx, int x, int y, int w, int h, int c) {
        ctx.fill(x,     y,     x + w, y + 1, c);
        ctx.fill(x,     y + h - 1, x + w, y + h, c);
        ctx.fill(x,     y + 1, x + 1,     y + h - 1, c);
        ctx.fill(x + w - 1, y + 1, x + w, y + h - 1, c);
    }

    private static int blendA(int argb, int alpha) {
        return (((argb >>> 24) & 0xFF) * alpha / 255 << 24) | (argb & 0x00FFFFFF);
    }

    private static float clampScale(float s) { return Math.max(0.5f, Math.min(2.0f, s)); }
    private static float clamp01(float v)    { return Math.max(0f, Math.min(1f, v)); }
    private static int   clamp(int v, int lo, int hi) { return v < lo ? lo : v > hi ? hi : v; }
    private static int   clampX(int x, int w, int sw) { return Math.max(0, Math.min(sw - w, x)); }
    private static int   clampY(int y, int h, int sh) { return Math.max(0, Math.min(sh - h, y)); }

    public JumpResetResult getActiveResult() { return activeResult; }
}
