package com.jumpreset.state;

/**
 * Immutable snapshot captured at the moment a valid combat hit is detected.
 *
 * @param tick         game tick when the hit was registered
 * @param nanoTime     ping-adjusted System.nanoTime() at that moment
 * @param velocityMag  horizontal knockback magnitude (diagnostic / debug)
 * @param wasGrounded  whether the player was on the ground at hit time
 * @param pingMs       measured round-trip ping at hit time (used for window sizing)
 */
public record HitSnapshot(
        int     tick,
        long    nanoTime,
        double  velocityMag,
        boolean wasGrounded,
        double  pingMs
) {}
