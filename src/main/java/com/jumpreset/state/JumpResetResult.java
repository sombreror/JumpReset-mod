package com.jumpreset.state;

/**
 * Fully-evaluated result of a single jump-reset attempt.
 *
 * @param millis         raw elapsed ms between hit and jump
 * @param tickDelta      ticks between hit and jump
 * @param score          Gaussian score [0,1]
 * @param classification PERFECT / GOOD / LATE / TOO_EARLY / MISSED
 * @param pingMs         ping at time of hit (for display in debug mode)
 */
public record JumpResetResult(
        double       millis,
        int          tickDelta,
        double       score,
        TimingResult classification,
        double       pingMs
) {
    public static JumpResetResult evaluate(HitSnapshot hit, long jumpNano, int jumpTick) {
        double ms        = (jumpNano - hit.nanoTime()) / 1_000_000.0;
        int    tickDelta = jumpTick - hit.tick();
        double ping      = hit.pingMs();
        double sc        = TimingResult.score(ms, ping);
        TimingResult cls = TimingResult.fromMillis(ms, ping);
        return new JumpResetResult(ms, tickDelta, sc, cls, ping);
    }
}
