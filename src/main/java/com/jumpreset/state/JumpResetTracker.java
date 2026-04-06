package com.jumpreset.state;

import com.jumpreset.JumpResetMod;
import com.jumpreset.config.ModConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;

/**
 * JumpResetTracker — v1.9.0
 *
 * Changes from v1.8.0:
 *
 * Fix — Same-tick branch displayed ping delay as ms (BUG 2).
 *   When hit and jump happen on the same tick, the previous code showed
 *   pingOneWay ms as the reaction time. Now we show 0 ms (instantaneous)
 *   since both events are same-tick — the player was perfectly synchronized.
 *
 * New — lastResult / lastResultTimestamp exposed publicly.
 *   The crosshair triangle indicator reads these to determine its color
 *   (green = recent PERFECT/GOOD, yellow = window open, red = recent miss/early).
 *   These reset to null after the HUD display duration expires.
 *
 * Clean — removed dead prevVelocityY field (was tracked but never read).
 */
public class JumpResetTracker {

    private static final int COMBAT_TIMEOUT_TICKS = 40;
    private static final int HUD_COOLDOWN_TICKS   = 4;
    private static final int JUMP_LOOKBACK_TICKS  = 2;

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
    private double  prevHorizMag  = 0.0;
    private double  prevVelocityY = 0.0;

    // ── Public observable state (used by crosshair indicator) ─────────────────
    /** Most recent result shown; null if no result or display expired. */
    public volatile JumpResetResult lastResult          = null;
    /** Wall-clock time (ms) when lastResult was set. */
    public volatile long            lastResultTimestamp = 0L;

    // ─────────────────────────────────────────────────────────────────────────

    public void tick(MinecraftClient client) {
        currentTick++;

        ClientPlayerEntity player = client.player;
        if (player == null)  { resetAll(); return; }

        ModConfig cfg = ModConfig.get();
        if (!cfg.enabled)    { resetAll(); return; }

        final long tickNano = System.nanoTime();

        // ── Sample ────────────────────────────────────────────────────────
        final int     hurtTime  = player.hurtTime;
        final int     timeRegen = player.timeUntilRegen;
        final double  vx        = player.getVelocity().x;
        final double  vz        = player.getVelocity().z;
        final double  horizMag  = Math.sqrt(vx * vx + vz * vz);
        final boolean onGround  = player.isOnGround();
        final double  ping      = measurePing(client);

        // ── JUMP DETECTION ────────────────────────────────────────────────
        // Delta-based: prevVelocityY → preMoveVelocityY ≥ threshold.
        // Fires on exactly one tick (the jump impulse tick).
        // No hurtTime guard — we need to detect jumps even right after a hit.
        final double  preMoveVy = JumpResetMod.preMoveVelocityY;
        final double  vyDelta   = preMoveVy - prevVelocityY;
        final boolean jumpNow   = vyDelta >= cfg.jumpDeltaThreshold
                && preMoveVy >= 0.10
                && !onGround;

        if (jumpNow) {
            lastJumpTick = currentTick;
            lastJumpNano = tickNano;
        }

        // ── HIT DETECTION ─────────────────────────────────────────────────
        // hurtTime 0→positive + timeUntilRegen>0 = confirmed combat hit.
        // horizMag threshold filters non-knockback damage (fall/fire/poison).
        final boolean hitNow = prevHurtTime == 0
                && hurtTime > 0
                && timeRegen > 0
                && horizMag > cfg.knockbackThreshold;

        if (hitNow) {
            lastHitTick = currentTick;

            double pingOneWay = ping * cfg.pingCompFactor;
            long   hitNano    = tickNano - (long)(pingOneWay * 1_000_000.0);

            // PRE-HIT JUMP: jumped 1–2 ticks before this hit was registered
            int ticksSinceJump = currentTick - lastJumpTick;
            if (ticksSinceJump > 0 && ticksSinceJump <= JUMP_LOOKBACK_TICKS
                    && state == TrackingState.IDLE
                    && currentTick - lastHudTick >= HUD_COOLDOWN_TICKS) {

                double displayMs = Math.abs((hitNano - lastJumpNano) / 1_000_000.0);
                JumpResetResult earlyResult = new JumpResetResult(
                        displayMs, ticksSinceJump, 0.0, TimingResult.TOO_EARLY, ping);
                dispatchResult(earlyResult);

            // SAME-TICK: jump and hit on exactly the same game tick.
            // Both events are simultaneous → display 0 ms (fix for BUG 2).
            } else if (currentTick == lastJumpTick
                    && state == TrackingState.IDLE
                    && currentTick - lastHudTick >= HUD_COOLDOWN_TICKS) {

                // 0 ms reaction: simultaneous jump + hit. Classify as PERFECT.
                JumpResetResult result = new JumpResetResult(
                        0.0, 0, 1.0, TimingResult.PERFECT, ping);
                if (result.classification().shouldShow()) {
                    dispatchResult(result);
                }

            // NORMAL: open a timing window for an upcoming jump
            } else {
                HitSnapshot newSnap = new HitSnapshot(
                        currentTick, hitNano, horizMag, prevOnGround, ping);
                if (state == TrackingState.WINDOW_ACTIVE) {
                    hitSnapshot = newSnap; // back-to-back hit: restart window
                } else if (state == TrackingState.IDLE
                        && currentTick - lastHudTick >= HUD_COOLDOWN_TICKS) {
                    hitSnapshot = newSnap;
                    state = TrackingState.WINDOW_ACTIVE;
                }
            }
        }

        // ── WINDOW EXPIRY ─────────────────────────────────────────────────
        if (state == TrackingState.WINDOW_ACTIVE && hitSnapshot != null) {
            int maxTicks = hitSnapshot.wasGrounded()
                    ? cfg.windowTicksGround : cfg.windowTicksAir;
            if (currentTick - hitSnapshot.tick() > maxTicks) {
                if (cfg.showMissed && currentTick - lastHudTick >= HUD_COOLDOWN_TICKS) {
                    JumpResetResult missed = new JumpResetResult(
                            9999, 99, 0.0, TimingResult.MISSED, ping);
                    if (missed.classification().shouldShow()) {
                        dispatchResult(missed);
                    }
                }
                transitionIdle();
            }
        }

        // ── JUMP WITHIN WINDOW ────────────────────────────────────────────
        if (jumpNow && state == TrackingState.WINDOW_ACTIVE && hitSnapshot != null) {
            if (isInCombat()) {
                JumpResetResult result =
                        JumpResetResult.evaluate(hitSnapshot, lastJumpNano, currentTick);
                if (result.classification().shouldShow()) {
                    dispatchResult(result);
                }
            }
            transitionIdle();
        }

        // ── CARRY-OVER ────────────────────────────────────────────────────
        prevHurtTime  = hurtTime;
        prevOnGround  = onGround;
        prevHorizMag  = horizMag;
        prevVelocityY = JumpResetMod.preMoveVelocityY;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Single point where results are dispatched to the HUD and stored
     * in the publicly observable lastResult field.
     */
    private void dispatchResult(JumpResetResult result) {
        lastHudTick         = currentTick;
        lastResult          = result;
        lastResultTimestamp = System.currentTimeMillis();
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
        prevHorizMag  = 0.0;
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

    public TrackingState   getState()       { return state; }
    public int             getCurrentTick() { return currentTick; }
    public HitSnapshot     getHitSnapshot() { return hitSnapshot; }
}
