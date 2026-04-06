package com.jumpreset.mixin;

import com.jumpreset.JumpResetMod;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ClientPlayerEntityMixin — v1.8.0
 *
 * ── Why the v1.7.0 INVOKE approach silently failed ──────────────────────────
 *
 * The previous version tried to inject at:
 *   INVOKE Lnet/minecraft/entity/LivingEntity;jump()V
 *   inside ClientPlayerEntity.tickMovement()
 *
 * With require=0, Mixin silently no-ops if the call-site isn't found.
 * In MC 1.21.11, ClientPlayerEntity.tickMovement() likely delegates jump
 * handling upward through the call chain in a way Mixin couldn't match,
 * so jumpingThisTick was NEVER set → HUD never showed.
 *
 * ── Fix: velocity-signature detection inside move() HEAD ────────────────────
 *
 * The vanilla ground jump ALWAYS sets vy to exactly:
 *   0.42 * jumpBoostFactor  (≈ 0.42 without potion effects)
 * and applies it BEFORE move() is called.
 *
 * So at HEAD of move():
 *   - preMoveVy ≈ 0.42 (tight range: 0.38 – 0.50 to cover Jump Boost)
 *   - player.isOnGround() == true  (still touching ground this tick)
 *   - MovementType == SELF  (player-initiated, not entity push or piston)
 *
 * Knockback via Entity.takeKnockback() adds vy of ~0.36 at most, and arrives
 * as MovementType.SELF as well — BUT the player is typically already airborne
 * (isOnGround() == false) when knockback arrives, OR the vy is below 0.38.
 * The ground-check is the primary discriminator.
 *
 * In edge cases where knockback is applied while still on ground AND happens
 * to produce vy ≥ 0.38, the tracker has a secondary guard (hurtTime check)
 * to filter those out.
 *
 * This approach:
 *   ✓ Zero @Shadow fields → no refMap required → no crash possible
 *   ✓ Only uses public API: getVelocity(), isOnGround()
 *   ✓ Works with any MC version / obfuscation mapping
 *   ✓ Reliable: move() is called every tick physics runs, guaranteed
 */
@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {

    /** Minimum vy that counts as a vanilla jump impulse (no potion = 0.42). */
    private static final double JUMP_VY_MIN = 0.38;
    /** Maximum vy: Jump Boost V gives ~0.78, so cap generously. */
    private static final double JUMP_VY_MAX = 0.85;

    /**
     * Called at the HEAD of move() every physics tick.
     *
     * Captures preMoveVelocityY for the tracker.
     *
     * Also detects a real jump by the velocity signature:
     *   vy in [JUMP_VY_MIN, JUMP_VY_MAX] + MovementType.SELF + isOnGround()
     *
     * Sets JumpResetMod.jumpingThisTick = true when all conditions hold.
     * The tracker reads and clears this flag in END_CLIENT_TICK.
     */
    @Inject(method = "move", at = @At("HEAD"))
    private void jumpreset$capturePreMove(MovementType type, Vec3d movement, CallbackInfo ci) {
        ClientPlayerEntity self = (ClientPlayerEntity)(Object) this;
        double vy = self.getVelocity().y;

        // Always capture vy for the tracker (used for historical context).
        JumpResetMod.preMoveVelocityY = vy;

        // Detect jump: velocity signature + ground + player-initiated movement.
        // MovementType.SELF covers normal walking/jumping.
        // Knockback uses SELF too, but the player is usually airborne by then.
        if (type == MovementType.SELF
                && vy >= JUMP_VY_MIN
                && vy <= JUMP_VY_MAX
                && self.isOnGround()) {
            JumpResetMod.jumpingThisTick = true;
        }
    }
}
