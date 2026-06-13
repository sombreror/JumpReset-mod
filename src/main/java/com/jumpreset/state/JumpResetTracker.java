package com.jumpreset.state;

import com.jumpreset.JumpResetMod;
import com.jumpreset.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

/**
 * JumpResetTracker — core detection state machine.
 *
 * <p>Each client tick {@link #tick(MinecraftClient)} runs a fixed pipeline:
 * <ol>
 *   <li>{@link #sample} — read this tick's player state in one shot.</li>
 *   <li>{@link #detectJump} — delta-vy jump impulse detection.</li>
 *   <li>{@link #detectHit} / {@link #handleHit} — confirmed combat hit handling
 *       (pre-hit jump, same-tick, or opening a timing window).</li>
 *   <li>{@link #checkWindowExpiry} — close a stale window.</li>
 *   <li>{@link #checkJumpInWindow} — score a jump that lands inside an open window.</li>
 * </ol>
 * Every classified attempt is dispatched through {@link #dispatchResult}, the
 * single point that feeds the HUD, the crosshair indicator and {@link SessionStats}.
 */
public class JumpResetTracker {

    private static final int COMBAT_TIMEOUT_TICKS = 40;
    private static final int HUD_COOLDOWN_TICKS   = 4;
    private static final int JUMP_LOOKBACK_TICKS  = 2;

    /** Minimum pre-move vy for a delta to count as a jump (filters tiny bumps). */
    private static final double JUMP_MIN_VY = 0.10;

    // ── State ─────────────────────────────────────────────────────────────────
    private TrackingState state       = TrackingState.IDLE;
    private HitSnapshot   hitSnapshot = null;

    private int    currentTick   = 0;
    private int    lastHitTick   = Integer.MIN_VALUE / 2;
    private int    lastHudTick   = Integer.MIN_VALUE / 2;
    private int    lastJumpTick  = Integer.MIN_VALUE / 2;
    private long   lastJumpNano  = 0L;
    private double lastKnownPing = 0.0;

    // Per-tick carry-overs
    private int     prevHurtTime  = 0;
    private boolean prevOnGround  = true;
    private double  prevVelocityY = 0.0;

    // ── Public observable state (used by crosshair indicator) ─────────────────
    /** Most recent result shown; null if no result or display expired. */
    public volatile JumpResetResult lastResult          = null;
    /** Wall-clock time (ms) when lastResult was set. */
    public volatile long            lastResultTimestamp = 0L;

    // ── Session statistics ────────────────────────────────────────────────────
    /** Aggregate stats for the current play session. Never reset automatically. */
    public final SessionStats sessionStats = new SessionStats();

    // ─────────────────────────────────────────────────────────────────────────

    public void tick(MinecraftClient client) {
        currentTick++;

        ClientPlayerEntity player = client.player;
        if (player == null) { resetAll(); return; }

        ModConfig cfg = ModConfig.get();
        if (!cfg.enabled)   { resetAll(); return; }

        final long   tickNano = System.nanoTime();
        final Sample s        = sample(player, client);

        // ── Jump detection (delta-vy; fires on the impulse tick only) ─────────
        final boolean jumpNow = detectJump(s, cfg);
        if (jumpNow) {
            lastJumpTick = currentTick;
            lastJumpNano = tickNano;
        }

        // ── Hit detection ─────────────────────────────────────────────────────
        if (detectHit(s, cfg)) {
            lastHitTick = currentTick;
            handleHit(s, cfg, tickNano);
        }

        // ── Window lifecycle ────────────────────────────────────────────────
        checkWindowExpiry(cfg, s.ping());
        if (jumpNow) checkJumpInWindow(s.ping());

        // ── Carry-over for next tick ──────────────────────────────────────────
        prevHurtTime  = s.hurtTime();
        prevOnGround  = s.onGround();
        prevVelocityY = s.preMoveVy();
    }

    // ── Pipeline steps ──────────────────────────────────────────────────────────

    /** Immutable snapshot of everything read from the player this tick. */
    private record Sample(int hurtTime, int timeRegen, double horizMag,
                          boolean onGround, double ping, double preMoveVy) {}

    private Sample sample(ClientPlayerEntity player, MinecraftClient client) {
        double vx = player.getVelocity().x;
        double vz = player.getVelocity().z;
        return new Sample(
                player.hurtTime,
                player.timeUntilRegen,
                Math.sqrt(vx * vx + vz * vz),
                player.isOnGround(),
                measurePing(client),
                JumpResetMod.preMoveVelocityY);
    }

    /**
     * Delta-based jump detection: vy rose by at least the configured threshold
     * into a positive impulse while airborne. No hurtTime guard — we must detect
     * jumps even immediately after a hit.
     */
    private boolean detectJump(Sample s, ModConfig cfg) {
        double vyDelta = s.preMoveVy() - prevVelocityY;
        return vyDelta >= cfg.jumpDeltaThreshold
                && s.preMoveVy() >= JUMP_MIN_VY
                && !s.onGround();
    }

    /**
     * Confirmed combat hit: hurtTime went 0→positive with the regen timer
     * active, and horizontal knockback exceeds the threshold (filters
     * fall/fire/poison damage, which has no horizontal push).
     */
    private boolean detectHit(Sample s, ModConfig cfg) {
        return prevHurtTime == 0
                && s.hurtTime() > 0
                && s.timeRegen() > 0
                && s.horizMag() > cfg.knockbackThreshold;
    }

    private void handleHit(Sample s, ModConfig cfg, long tickNano) {
        double ping       = s.ping();
        double pingOneWay = ping * cfg.pingCompFactor;
        long   hitNano    = tickNano - (long) (pingOneWay * 1_000_000.0);

        int ticksSinceJump = currentTick - lastJumpTick;

        // PRE-HIT JUMP: jumped 1–2 ticks before this hit registered → too early.
        if (ticksSinceJump > 0 && ticksSinceJump <= JUMP_LOOKBACK_TICKS && readyForResult()) {
            double displayMs = Math.abs((hitNano - lastJumpNano) / 1_000_000.0);
            dispatchResult(new JumpResetResult(
                    displayMs, ticksSinceJump, 0.0, TimingResult.TOO_EARLY, ping));
            return;
        }

        // SAME-TICK: jump and hit on the same tick → simultaneous, perfect (0 ms).
        if (currentTick == lastJumpTick && readyForResult()) {
            JumpResetResult result = new JumpResetResult(0.0, 0, 1.0, TimingResult.PERFECT, ping);
            if (result.classification().shouldShow()) dispatchResult(result);
            return;
        }

        // NORMAL: open (or restart) a timing window for an upcoming jump.
        HitSnapshot snap = new HitSnapshot(currentTick, hitNano, s.horizMag(), prevOnGround, ping);
        if (state == TrackingState.WINDOW_ACTIVE) {
            hitSnapshot = snap; // back-to-back hit: restart the window
        } else if (state == TrackingState.IDLE && readyForResult()) {
            hitSnapshot = snap;
            state       = TrackingState.WINDOW_ACTIVE;
        }
    }

    /** Close the window if no jump arrived within its tick budget. */
    private void checkWindowExpiry(ModConfig cfg, double ping) {
        if (state != TrackingState.WINDOW_ACTIVE || hitSnapshot == null) return;

        int maxTicks = hitSnapshot.wasGrounded() ? cfg.windowTicksGround : cfg.windowTicksAir;
        if (currentTick - hitSnapshot.tick() > maxTicks) {
            // shouldShow() already honours the showMissed config for MISSED.
            JumpResetResult missed = new JumpResetResult(9999, 99, 0.0, TimingResult.MISSED, ping);
            if (missed.classification().shouldShow()
                    && currentTick - lastHudTick >= HUD_COOLDOWN_TICKS) {
                dispatchResult(missed);
            }
            transitionIdle();
        }
    }

    /** Score a jump that lands inside an open window, then close it. */
    private void checkJumpInWindow(double ping) {
        if (state != TrackingState.WINDOW_ACTIVE || hitSnapshot == null) return;

        if (isInCombat()) {
            JumpResetResult result = JumpResetResult.evaluate(hitSnapshot, lastJumpNano, currentTick);
            if (result.classification().shouldShow()) dispatchResult(result);
        }
        transitionIdle();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** True once the per-result HUD cooldown has elapsed and we're idle-eligible. */
    private boolean readyForResult() {
        return state == TrackingState.IDLE
                && currentTick - lastHudTick >= HUD_COOLDOWN_TICKS;
    }

    /**
     * Single point where results are dispatched to the HUD and stored
     * in the publicly observable lastResult field.
     */
    private void dispatchResult(JumpResetResult result) {
        lastHudTick         = currentTick;
        lastResult          = result;
        lastResultTimestamp = System.currentTimeMillis();
        sessionStats.record(result);
        JumpResetMod.hud.showResult(result);
    }

    private void transitionIdle() {
        state       = TrackingState.IDLE;
        hitSnapshot = null;
    }

    private void resetAll() {
        transitionIdle();
        prevHurtTime  = 0;
        prevOnGround  = true;
        prevVelocityY = 0.0;
        lastJumpTick  = Integer.MIN_VALUE / 2;
        lastJumpNano  = 0L;
        lastHitTick   = Integer.MIN_VALUE / 2;
        lastResult    = null;
    }

    private boolean isInCombat() {
        return currentTick - lastHitTick <= COMBAT_TIMEOUT_TICKS;
    }

    private double measurePing(MinecraftClient client) {
        if (client.getNetworkHandler() == null || client.player == null)
            return lastKnownPing;
        var e = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
        if (e == null) return lastKnownPing;
        lastKnownPing = Math.max(1.0, e.getLatency());
        return lastKnownPing;
    }

    public TrackingState getState() { return state; }
}
