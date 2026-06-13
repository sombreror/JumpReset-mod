package com.jumpreset.state;

/**
 * State machine for jump-reset detection lifecycle.
 *
 * Valid transitions:
 *   IDLE          → WINDOW_ACTIVE  (confirmed combat hit received)
 *   WINDOW_ACTIVE → IDLE           (window expired OR jump processed)
 *
 * The state is also used by the crosshair indicator to determine triangle color:
 *   IDLE          → no triangle (or brief green/red after result)
 *   WINDOW_ACTIVE → yellow triangle (waiting for jump)
 */
public enum TrackingState {
    /** No active timing window. Waiting for the next hit. */
    IDLE,
    /** Player was hit; timing window is open. Waiting for a valid jump. */
    WINDOW_ACTIVE
}
