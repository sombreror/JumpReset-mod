package com.jumpreset.state;

/**
 * SessionStats — v2.0.0
 *
 * Tracks aggregate statistics for the current play session.
 * Owned by JumpResetTracker; never persisted to disk (resets on game restart).
 *
 * Tracked data:
 *  - total attempts (excluding MISSED, which never reaches dispatchResult)
 *  - per-classification counts (PERFECT / GOOD / LATE / TOO_EARLY)
 *  - current consecutive streak (PERFECT or GOOD)
 *  - best streak this session
 *  - cumulative score sum (for average computation)
 *
 * Thread safety: all writes happen on the client tick thread; reads from the
 * render thread are volatile-safe because primitives are word-sized or smaller.
 */
public class SessionStats {

    private int    total      = 0;
    private int    perfect    = 0;
    private int    good       = 0;
    private int    late       = 0;
    private int    tooEarly   = 0;
    private int    streak     = 0;
    private int    bestStreak = 0;
    private double scoreSum   = 0.0;

    // ── Write API (tick thread) ───────────────────────────────────────────────

    /**
     * Record a new result. Called from JumpResetTracker.dispatchResult().
     * MISSED results never reach here (filtered before dispatchResult).
     */
    public void record(JumpResetResult result) {
        total++;
        scoreSum += result.score();

        boolean hit = false;
        switch (result.classification()) {
            case PERFECT   -> { perfect++;  hit = true; }
            case GOOD      -> { good++;     hit = true; }
            case LATE      -> late++;
            case TOO_EARLY -> tooEarly++;
            default        -> {}
        }

        if (hit) {
            streak++;
            if (streak > bestStreak) bestStreak = streak;
        } else {
            streak = 0;
        }
    }

    /** Reset all counters to zero (e.g. via "Reset Stats" button in ConfigScreen). */
    public void reset() {
        total      = 0;
        perfect    = 0;
        good       = 0;
        late       = 0;
        tooEarly   = 0;
        streak     = 0;
        bestStreak = 0;
        scoreSum   = 0.0;
    }

    // ── Read API (render thread) ──────────────────────────────────────────────

    public int    total()      { return total; }
    public int    perfect()    { return perfect; }
    public int    good()       { return good; }
    public int    late()       { return late; }
    public int    tooEarly()   { return tooEarly; }
    public int    streak()     { return streak; }
    public int    bestStreak() { return bestStreak; }

    /** Average score in [0, 1]. Returns 0 if no attempts recorded. */
    public double avgScore() {
        return total > 0 ? scoreSum / total : 0.0;
    }

    /**
     * Percentage of attempts classified as PERFECT or GOOD.
     * Range: [0, 100]. Returns 0 if no attempts recorded.
     */
    public double hitRate() {
        return total > 0 ? (perfect + good) * 100.0 / total : 0.0;
    }
}
