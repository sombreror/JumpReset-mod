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
 * ClientPlayerEntityMixin
 *
 * <p>Captures the player's vertical velocity at the {@code HEAD} of
 * {@link net.minecraft.entity.Entity#move} — i.e. <em>before</em> the physics
 * engine resolves the movement for the tick. This is the raw impulse value:
 * a vanilla ground jump sets {@code vy ≈ 0.42} (×jump-boost) and that value is
 * present here before gravity/drag are applied.
 *
 * <p>The actual jump detection lives in {@link com.jumpreset.state.JumpResetTracker},
 * which compares this captured value against the previous tick's value
 * (delta-vy) so it can fire reliably even on the same tick a hit lands.
 *
 * <p>This injection is deliberately minimal:
 * <ul>
 *   <li>No {@code @Shadow} fields → no refMap required → cannot crash on mapping mismatch.</li>
 *   <li>Only public API ({@code getVelocity()}) is used.</li>
 *   <li>{@code move()} runs every physics tick, so the sample is guaranteed.</li>
 * </ul>
 */
@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {

    @Inject(method = "move", at = @At("HEAD"))
    private void jumpreset$capturePreMove(MovementType type, Vec3d movement, CallbackInfo ci) {
        ClientPlayerEntity self = (ClientPlayerEntity) (Object) this;
        JumpResetMod.preMoveVelocityY = self.getVelocity().y;
    }
}
