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
 * Thread safety: all writes happen on the client tick thread; the render thread
 * reads these counters for display. Fields are {@code volatile} so the renderer
 * always observes the latest published value. A render may catch the counters
 * mid-update (e.g. {@code total} bumped before {@code perfect}); that is harmless
 * for a transient on-screen readout and avoids any locking on the hot path.
 */
public class SessionStats {

    private volatile int    total      = 0;
    private volatile int    perfect    = 0;
    private volatile int    good       = 0;
    private volatile int    late       = 0;
    private volatile int    tooEarly   = 0;
    private volatile int    streak     = 0;
    private volatile int    bestStreak = 0;
    private volatile double scoreSum   = 0.0;

    // ── Write API (tick thread) ───────────────────────────────────────────────

    /**
     * Record a new result. Called from JumpResetTracker.dispatchResult().
     * MISSED is never counted — it represents a non-attempt, and including it
     * would inflate {@code total} and reset streaks whenever "Show Missed" is on.
     */
    public void record(JumpResetResult result) {
        if (result.classification() == TimingResult.MISSED) return;

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
