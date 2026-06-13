package com.jumpreset.ui;

import com.jumpreset.JumpResetMod;
import com.jumpreset.config.FeedbackStyle;
import com.jumpreset.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

/**
 * ConfigScreen — v2.0.0
 *
 * Changes from v1.9.0:
 *  - General tab: "Reset Stats" button clears the SessionStats aggregate.
 *  - General tab: session summary (attempts, hit rate, streak) drawn as text
 *    in the gap between the last control and the HUD preview area.
 *  - Title bar updated to v2.0.
 *
 * Layout (tabs):
 *   [General] [Display] [Timing] [Indicator]
 *   ── tab content area (scrollable if needed) ──
 *   [HUD preview — draggable]
 *   [Save]  [Reset]  [Cancel]
 *
 * Each tab shows a focused subset of settings, avoiding the wall-of-buttons
 * problem from v1.7.0. The panel is visually compact and professional.
 */
public class ConfigScreen extends Screen {

    private final Screen parent;

    // ── Panel geometry (computed from screen size in init) ────────────────────
    private int panelX, panelY, panelW, panelH;

    // ── Tab state ─────────────────────────────────────────────────────────────
    private int activeTab = 0; // 0=General, 1=Display, 2=Timing, 3=Indicator
    private static final String[] TAB_LABELS = {"General", "Display", "Timing", "Indicator"};

    // ── Drag state ────────────────────────────────────────────────────────────
    private boolean dragging = false;
    private double  dragOffX = 0, dragOffY = 0;

    // ── Colors ────────────────────────────────────────────────────────────────
    private static final int C_PANEL     = 0xF00A0A1A;
    private static final int C_TITLEBAR  = 0xFF0D0D22;
    private static final int C_TABBAR    = 0xF0080814;
    private static final int C_BORDER    = 0xFF1E1E3A;
    private static final int C_TAB_ACT   = 0xFF1A1A38;
    private static final int C_TAB_HOV   = 0xFF141428;
    private static final int C_TITLE     = 0xFFDDDDFF;
    private static final int C_SECTION   = 0xFF6070A8;
    private static final int C_DIVIDER   = 0xFF181828;
    private static final int C_HINT      = 0xFF404060;
    private static final int C_FOOTER    = 0xF0050510;

    // ── Layout constants ──────────────────────────────────────────────────────
    private static final int PANEL_W     = 340;  // fixed panel width
    private static final int TITLE_H     = 22;
    private static final int TAB_H       = 20;
    private static final int CONTENT_PAD = 8;
    private static final int BH          = 17;   // button height
    private static final int GAP         = 20;   // vertical gap between rows
    private static final int FOOTER_H    = 28;
    private static final int PREVIEW_H   = 44;   // HUD preview area height

    // Content start Y (inside panel, relative to panelY)
    private int contentY;          // set in init()
    private int generalTabEndY;    // Y just below the last General-tab widget

    public ConfigScreen(Screen parent) {
        super(Text.literal("JumpReset"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        // Panel dimensions: fixed width, height fitted to screen
        panelW = Math.min(PANEL_W, width - 20);
        panelH = Math.min(320, height - 20);
        panelX = (width  - panelW) / 2;
        panelY = (height - panelH) / 2;
        contentY = panelY + TITLE_H + TAB_H + CONTENT_PAD;

        int bW  = panelW - CONTENT_PAD * 2; // full-width element inside content
        int lx  = panelX + CONTENT_PAD;     // left X of content elements
        int ry  = contentY;                  // rolling Y for widget placement

        buildTabButtons();
        buildTabContent(lx, ry, bW);
        buildFooterButtons();
    }

    // ── Tab bar ───────────────────────────────────────────────────────────────

    private void buildTabButtons() {
        int tabW = panelW / TAB_LABELS.length;
        int tabY = panelY + TITLE_H;
        for (int i = 0; i < TAB_LABELS.length; i++) {
            final int tab = i;
            addDrawableChild(ButtonWidget.builder(
                    Text.literal(TAB_LABELS[i]),
                    b -> { activeTab = tab; clearChildren(); init(); })
                    .dimensions(panelX + tab * tabW, tabY, tabW, TAB_H)
                    .build());
        }
    }

    // ── Tab content ───────────────────────────────────────────────────────────

    private void buildTabContent(int lx, int startY, int bW) {
        int slW = bW; // slider width = full content width
        switch (activeTab) {
            case 0 -> buildGeneralTab(lx, startY, bW, slW);
            case 1 -> buildDisplayTab(lx, startY, bW, slW);
            case 2 -> buildTimingTab(lx, startY, bW, slW);
            case 3 -> buildIndicatorTab(lx, startY, bW, slW);
        }
    }

    private void buildGeneralTab(int lx, int y, int bW, int slW) {
        tog("Mod Enabled",    lx, y, bW, ModConfig.get().enabled,        v -> ModConfig.get().enabled = v);        y += GAP;
        tog("Auto Ping Adj",  lx, y, bW, ModConfig.get().autoPingAdjust,  v -> ModConfig.get().autoPingAdjust = v);  y += GAP;
        tog("Show Missed",    lx, y, bW, ModConfig.get().showMissed,      v -> ModConfig.get().showMissed = v);      y += GAP;
        tog("Lock HUD",       lx, y, bW, ModConfig.get().hudLocked,       v -> ModConfig.get().hudLocked = v);       y += GAP;
        tog("Debug Mode",     lx, y, bW, ModConfig.get().debugMode,       v -> ModConfig.get().debugMode = v);       y += GAP;
        // Debug verbosity toggle (Full / Compact)
        addDrawableChild(ButtonWidget.builder(
                Text.literal("Debug: " + ModConfig.get().debugDisplayMode.displayName()),
                b -> {
                    ModConfig.get().debugDisplayMode = ModConfig.get().debugDisplayMode.toggled();
                    b.setMessage(Text.literal("Debug: " + ModConfig.get().debugDisplayMode.displayName()));
                }).dimensions(lx, y, bW, BH).build()); y += GAP;
        sld("Knockback", lx, y, slW, ModConfig.get().knockbackThreshold, 0.01, 0.20,
                v -> ModConfig.get().knockbackThreshold = v,
                v -> String.format("KB threshold: %.3f", v)); y += GAP;
        // Session stats reset (v2.0.0)
        addDrawableChild(ButtonWidget.builder(
                Text.literal("§cReset Session Stats"),
                b -> JumpResetMod.tracker.sessionStats.reset())
                .dimensions(lx, y, bW, BH).build());
        generalTabEndY = y + BH;
    }

    private void buildDisplayTab(int lx, int y, int bW, int slW) {
        FeedbackStyle style = ModConfig.get().feedbackStyle;
        boolean isMinimal  = style == FeedbackStyle.MINIMAL;
        boolean isDetailed = style == FeedbackStyle.DETAILED;

        // Style cycle — always visible; rebuilds tab on change to show/hide options
        addDrawableChild(ButtonWidget.builder(
                Text.literal("Style: " + style.displayName()),
                b -> {
                    ModConfig.get().feedbackStyle = ModConfig.get().feedbackStyle.next();
                    clearChildren(); init();
                }).dimensions(lx, y, bW, BH).build()); y += GAP;

        // ── Options available per style ───────────────────────────────────────
        //   minimal  : Slide Anim, Text Shadow, Scale, Opacity, Duration
        //   bar      : Show Label, Show MS, Slide Anim, Text Shadow, Scale, Opacity, Duration
        //   detailed : all options

        if (!isMinimal) {
            tog("Show Label", lx, y, bW, ModConfig.get().showLabel,
                    v -> ModConfig.get().showLabel = v); y += GAP;
            tog("Show MS",    lx, y, bW, ModConfig.get().showMs,
                    v -> ModConfig.get().showMs = v);    y += GAP;
        }
        if (isDetailed) {
            tog("Show Hint",     lx, y, bW, ModConfig.get().showHint,
                    v -> ModConfig.get().showHint = v);      y += GAP;
            tog("Score Bar",     lx, y, bW, ModConfig.get().showScoreBar,
                    v -> ModConfig.get().showScoreBar = v);  y += GAP;
        }
        tog("Slide Anim",  lx, y, bW, ModConfig.get().animateSlideIn,
                v -> ModConfig.get().animateSlideIn = v); y += GAP;
        tog("Text Shadow", lx, y, bW, ModConfig.get().textShadow,
                v -> ModConfig.get().textShadow = v);     y += GAP;
        if (isDetailed) {
            tog("History Strip", lx, y, bW, ModConfig.get().showHistory,
                    v -> ModConfig.get().showHistory = v); y += GAP;
        }

        // ── Sliders — always visible ──────────────────────────────────────────
        sld("Scale",   lx, y, slW, ModConfig.get().hudScale,   0.5, 2.0,
                v -> ModConfig.get().hudScale = (float)(double)v,
                v -> String.format("Scale: %.1fx", v)); y += GAP;
        sld("Opacity", lx, y, slW, ModConfig.get().hudOpacity, 0.1, 1.0,
                v -> ModConfig.get().hudOpacity = (float)(double)v,
                v -> String.format("Opacity: %.0f%%", v * 100)); y += GAP;
        sld("Duration", lx, y, slW, ModConfig.get().displayDurationMs / 1000.0, 0.5, 5.0,
                v -> ModConfig.get().displayDurationMs = (int)(v * 1000),
                v -> String.format("Duration: %.1fs", v)); y += GAP;
        if (isDetailed) {
            sld("History", lx, y, slW, ModConfig.get().historyCount, 2, 10,
                    v -> ModConfig.get().historyCount = (int)Math.round(v),
                    v -> String.format("History: %.0f entries", v));
        }
    }

    private void buildTimingTab(int lx, int y, int bW, int slW) {
        sld("Ideal",    lx, y, slW, ModConfig.get().perfectMs,     30, 200,
                v -> ModConfig.get().perfectMs = v,
                v -> String.format("Ideal: %.0f ms", v)); y += GAP;
        sld("Perfect≤", lx, y, slW, ModConfig.get().perfectMaxMs,  50, 250,
                v -> ModConfig.get().perfectMaxMs = v,
                v -> String.format("Perfect ≤ %.0f ms", v)); y += GAP;
        sld("Good≤",    lx, y, slW, ModConfig.get().goodMaxMs,     80, 400,
                v -> ModConfig.get().goodMaxMs = v,
                v -> String.format("Good ≤ %.0f ms", v)); y += GAP;
        sld("Late≤",    lx, y, slW, ModConfig.get().lateMaxMs,    150, 600,
                v -> ModConfig.get().lateMaxMs = v,
                v -> String.format("Late ≤ %.0f ms", v)); y += GAP;
        sld("Early<",   lx, y, slW, ModConfig.get().tooEarlyMs,     5,  80,
                v -> ModConfig.get().tooEarlyMs = v,
                v -> String.format("Too early < %.0f ms", v)); y += GAP;
        sld("Sigma",    lx, y, slW, ModConfig.get().scoreSigma,    10, 200,
                v -> ModConfig.get().scoreSigma = v,
                v -> String.format("Score sigma: %.0f", v)); y += GAP;
        sld("PingFac",  lx, y, slW, ModConfig.get().pingCompFactor,  0, 1.0,
                v -> ModConfig.get().pingCompFactor = v,
                v -> String.format("Ping factor: %.1f", v)); y += GAP;
        sld("WinGnd",   lx, y, slW, ModConfig.get().windowTicksGround, 2, 15,
                v -> ModConfig.get().windowTicksGround = (int)Math.round(v),
                v -> String.format("Window (grnd): %.0f ticks", v)); y += GAP;
        sld("WinAir",   lx, y, slW, ModConfig.get().windowTicksAir,    4, 20,
                v -> ModConfig.get().windowTicksAir = (int)Math.round(v),
                v -> String.format("Window (air): %.0f ticks", v));
    }

    private void buildIndicatorTab(int lx, int y, int bW, int slW) {
        tog("Show Indicator", lx, y, bW, ModConfig.get().showCrosshairIndicator,
                v -> ModConfig.get().showCrosshairIndicator = v); y += GAP;
        sld("Size",   lx, y, slW, ModConfig.get().crosshairTriangleSize, 2, 12,
                v -> ModConfig.get().crosshairTriangleSize = (int)Math.round(v),
                v -> String.format("Triangle size: %.0f px", v)); y += GAP;
        sld("Offset", lx, y, slW, ModConfig.get().crosshairIndicatorY,   6, 30,
                v -> ModConfig.get().crosshairIndicatorY = (int)Math.round(v),
                v -> String.format("Offset: %.0f px above", v));
    }

    // ── Footer buttons ────────────────────────────────────────────────────────

    private void buildFooterButtons() {
        int bW  = 80;
        int gap = 6;
        int tot = bW * 3 + gap * 2;
        int bx  = (width - tot) / 2;
        int by  = panelY + panelH - FOOTER_H + (FOOTER_H - BH) / 2;

        addDrawableChild(ButtonWidget.builder(Text.literal("✓ Save"),
                b -> { ModConfig.save(); close(); })
                .dimensions(bx, by, bW, BH).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("↺ Reset"),
                b -> { ModConfig.load(); clearChildren(); init(); })
                .dimensions(bx + bW + gap, by, bW, BH).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("✗ Cancel"),
                b -> { ModConfig.load(); close(); })
                .dimensions(bx + (bW + gap) * 2, by, bW, BH).build());
    }

    // ── Drag handling (HUD preview repositioning) ─────────────────────────────
    //
    // Uses the Screen mouse events (coordinates already in scaled GUI space)
    // instead of polling raw GLFW state every tick. Widgets get first refusal on
    // every click, so buttons and sliders keep working even when they overlap the
    // preview.

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) return true;
        double mx = click.x(), my = click.y();
        if (click.button() == 0 && !ModConfig.get().hudLocked && inside((int) mx, (int) my, hudBounds())) {
            int[] b = hudBounds();
            dragging = true;
            dragOffX = mx - b[0];
            dragOffY = my - b[1];
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (dragging && click.button() == 0 && !ModConfig.get().hudLocked) {
            ModConfig cfg = ModConfig.get();
            float s  = cfg.hudScale;
            int   pw = (int) (JumpResetHud.BASE_W * s);
            int   ph = (int) (JumpResetHud.BASE_H * s);
            cfg.hudX = clampF((float) ((click.x() - dragOffX + pw * 0.5f) / width),  0.02f, 0.98f);
            cfg.hudY = clampF((float) ((click.y() - dragOffY + ph * 0.5f) / height), 0.02f, 0.98f);
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (click.button() == 0) dragging = false;
        return super.mouseReleased(click);
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        // Dim the world behind the panel
        ctx.fill(0, 0, width, height, 0x88000000);

        // Panel background
        ctx.fill(panelX, panelY, panelX + panelW, panelY + panelH, C_PANEL);
        drawBorder(ctx, panelX, panelY, panelW, panelH, C_BORDER);

        // Title bar
        ctx.fill(panelX, panelY, panelX + panelW, panelY + TITLE_H, C_TITLEBAR);
        drawBorder(ctx, panelX, panelY, panelW, TITLE_H, C_BORDER);

        // Tab bar background
        int tabY = panelY + TITLE_H;
        ctx.fill(panelX, tabY, panelX + panelW, tabY + TAB_H, C_TABBAR);
        drawBorder(ctx, panelX, tabY, panelW, TAB_H, C_BORDER);

        // Highlight active tab
        int tabW = panelW / TAB_LABELS.length;
        ctx.fill(panelX + activeTab * tabW, tabY, panelX + (activeTab + 1) * tabW, tabY + TAB_H, C_TAB_ACT);

        // Footer bar
        int footerY = panelY + panelH - FOOTER_H;
        ctx.fill(panelX, footerY, panelX + panelW, panelY + panelH, C_FOOTER);
        ctx.fill(panelX, footerY, panelX + panelW, footerY + 1, C_BORDER);

        // Preview area (above footer)
        int previewY = footerY - PREVIEW_H - 4;
        ctx.fill(panelX, previewY, panelX + panelW, footerY, 0x44000000);
        ctx.fill(panelX, previewY, panelX + panelW, previewY + 1, C_DIVIDER);

        // Title text
        String title = "⚙  JumpReset  v2.0";
        int tw = textRenderer.getWidth(title);
        ctx.drawText(textRenderer, Text.literal(title),
                width / 2 - tw / 2, panelY + (TITLE_H - 8) / 2, C_TITLE, true);

        // Content section label
        ctx.drawText(textRenderer, Text.literal(TAB_LABELS[activeTab].toUpperCase()),
                panelX + CONTENT_PAD, contentY - CONTENT_PAD + 1, C_SECTION, false);

        // Draw all widgets
        super.render(ctx, mx, my, delta);

        // Session stats summary (v2.0.0) — shown in General tab below controls
        if (activeTab == 0) renderSessionStats(ctx);

        // HUD preview in the preview area (draggable when not locked)
        boolean locked  = ModConfig.get().hudLocked;
        int[]   bounds  = hudBounds();
        boolean hovered = inside(mx, my, bounds);
        JumpResetMod.hud.renderPreview(ctx, MinecraftClient.getInstance(),
                !locked && (dragging || hovered));

        // Drag hint
        String hint = locked
                ? "§cHUD locked"
                : (dragging ? "§eMoving HUD..." : "§7Drag preview to reposition");
        int hw = textRenderer.getWidth(hint.replaceAll("§.", ""));
        ctx.drawText(textRenderer, Text.literal(hint),
                width / 2 - hw / 2, previewY + 2, C_HINT, false);
    }

    @Override public boolean shouldPause() { return false; }
    @Override public void    close()       { if (client != null) client.setScreen(parent); }

    // ── Session stats display (v2.0.0) ────────────────────────────────────────

    /**
     * Draw a compact session summary in the gap between the last General-tab
     * control and the HUD preview area.
     *
     * Anchored just below the last General-tab widget ({@code generalTabEndY},
     * captured in {@link #buildGeneralTab}) rather than a hardcoded row count, so
     * it stays correct if the tab's controls change. We draw two lines of small
     * text (no widgets needed — pure drawText).
     */
    private void renderSessionStats(DrawContext ctx) {
        var stats = JumpResetMod.tracker.sessionStats;

        int statsY = generalTabEndY + 8;
        int lx     = panelX + CONTENT_PAD;

        if (stats.total() == 0) {
            ctx.drawText(textRenderer,
                    Text.literal("§7No attempts recorded this session."),
                    lx, statsY, 0xFF404060, false);
            return;
        }

        // Line 1: counts
        String line1 = String.format("§7Attempts: §f%d   §aP:%d §bG:%d §6L:%d §cE:%d",
                stats.total(), stats.perfect(), stats.good(), stats.late(), stats.tooEarly());
        ctx.drawText(textRenderer, Text.literal(line1), lx, statsY, 0xFFFFFFFF, false);

        // Line 2: hit rate, avg score, streak
        int hitCol = stats.hitRate() >= 70 ? 0xFF00E87A : stats.hitRate() >= 40 ? 0xFFFFCC00 : 0xFFFF5566;
        String line2 = "§7Hit: ";
        ctx.drawText(textRenderer, Text.literal(line2), lx, statsY + 10, 0xFFFFFFFF, false);
        int offsetX = lx + textRenderer.getWidth(line2.replaceAll("§.", ""));
        String hitStr = String.format("%.0f%%", stats.hitRate());
        ctx.drawText(textRenderer, Text.literal(hitStr), offsetX, statsY + 10, hitCol, false);
        offsetX += textRenderer.getWidth(hitStr);
        String rest = String.format("  §7Avg: §f%.0f%%  §7Streak: §f%d §7(best §f%d§7)",
                stats.avgScore() * 100, stats.streak(), stats.bestStreak());
        ctx.drawText(textRenderer, Text.literal(rest), offsetX, statsY + 10, 0xFFFFFFFF, false);
    }

    // ── Widget helpers ────────────────────────────────────────────────────────

    private void tog(String lbl, int x, int y, int w, boolean init,
                     java.util.function.Consumer<Boolean> cb) {
        boolean[] s = {init};
        addDrawableChild(ButtonWidget.builder(
                Text.literal(lbl + ": " + (s[0] ? "§aON" : "§cOFF")),
                b -> {
                    s[0] = !s[0];
                    cb.accept(s[0]);
                    b.setMessage(Text.literal(lbl + ": " + (s[0] ? "§aON" : "§cOFF")));
                }).dimensions(x, y, w, BH).build());
    }

    private void sld(String id, int x, int y, int w, double val, double mn, double mx2,
                     java.util.function.Consumer<Double> cb,
                     java.util.function.Function<Double, String> fmt) {
        addDrawableChild(new SliderWidget(x, y, w, BH, Text.literal(fmt.apply(val)),
                (val - mn) / (mx2 - mn)) {
            @Override protected void updateMessage() {
                setMessage(Text.literal(fmt.apply(mn + value * (mx2 - mn))));
            }
            @Override protected void applyValue() {
                cb.accept(mn + value * (mx2 - mn));
            }
        });
    }

    // ── Geometry helpers ──────────────────────────────────────────────────────

    private int[] hudBounds() {
        ModConfig cfg = ModConfig.get();
        float s  = cfg.hudScale;
        int   pw = (int)(JumpResetHud.BASE_W * s);
        int   ph = (int)(JumpResetHud.BASE_H * s);
        return new int[]{ (int)(cfg.hudX * width) - pw/2, (int)(cfg.hudY * height) - ph/2, pw, ph };
    }

    private static boolean inside(int x, int y, int[] b) {
        return x >= b[0] && x <= b[0]+b[2] && y >= b[1] && y <= b[1]+b[3];
    }

    private static float clampF(float v, float lo, float hi) {
        return v < lo ? lo : v > hi ? hi : v;
    }

    private static void drawBorder(DrawContext ctx, int x, int y, int w, int h, int c) {
        ctx.fill(x, y, x+w, y+1, c);
        ctx.fill(x, y+h-1, x+w, y+h, c);
        ctx.fill(x, y+1, x+1, y+h-1, c);
        ctx.fill(x+w-1, y+1, x+w, y+h-1, c);
    }
}
