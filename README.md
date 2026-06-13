# JumpReset

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-brightgreen?logo=minecraft&logoColor=white)](#)
[![Fabric](https://img.shields.io/badge/Loader-Fabric%200.18%2B-ffb347)](#)
[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)](#)
[![Environment](https://img.shields.io/badge/Environment-Client--side-blue)](#)
[![Version](https://img.shields.io/badge/Version-v2.5.0-blueviolet)](#)
[![License](https://img.shields.io/badge/License-MIT-lightgrey)](#)

> **Accurate jump-reset feedback for PvP — powered by pre-physics velocity analysis.**

---

## Table of Contents

- [What is JumpReset?](#what-is-jumpreset)
- [User Guide](#user-guide)
  - [Installation](#installation)
  - [Requirements](#requirements)
  - [Getting Started](#getting-started)
  - [HUD Styles](#hud-styles)
  - [Crosshair Indicator](#crosshair-indicator)
  - [Session Statistics](#session-statistics)
  - [Configuration](#configuration)
  - [Keybinds](#keybinds)
  - [FAQ](#faq)
- [Technical Reference](#technical-reference)
  - [Architecture](#architecture)
  - [Detection Pipeline](#detection-pipeline)
  - [Scoring Model](#scoring-model)
  - [Configuration Reference](#configuration-reference)
  - [Building from Source](#building-from-source)
- [Changelog](#changelog)

---

## What is JumpReset?

JumpReset is a **client-side Fabric mod** that gives you accurate, real-time feedback on your jump resets during PvP combat.

A *jump reset* is the technique of jumping immediately after taking a hit to cancel the knockback. Its effectiveness depends entirely on timing — jump too early and the game hasn't registered the hit yet; jump too late and the knockback has already been applied to your trajectory. The ideal window is roughly 80 ms after impact.

The problem with vanilla Minecraft and most existing mods is that they read velocity *after* the physics engine has already processed it, making it impossible to reliably distinguish between an intentional jump, knockback-induced motion, and terrain-caused velocity changes.

JumpReset solves this by hooking into the engine **before physics resolution**, capturing the raw velocity impulse at the exact tick it is produced. The result is a detector that never confuses knockback with a jump, never fires on slopes, and gives you consistent feedback every single attempt.

---

# User Guide

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) **0.18.0 or newer**
2. Install [Fabric API](https://modrinth.com/mod/fabric-api) for Minecraft 1.21.11
3. Download the latest `jumpreset-vX.X.X.jar` from [Modrinth](#) or [GitHub Releases](#)
4. Place the `.jar` file inside your `.minecraft/mods` folder
5. Launch Minecraft — no further setup is required

> **Server note:** JumpReset is client-side only. It does not need to be installed on the server and works on any server (vanilla, Paper, Fabric, etc.).

---

## Requirements

| Component     | Required version    |
|---------------|---------------------|
| Minecraft     | 1.21.11             |
| Fabric Loader | ≥ 0.18.0            |
| Fabric API    | Latest for 1.21.11  |
| Java          | 21                  |

---

## Getting Started

Once installed, the mod is active immediately. Join any server or world and start a fight:

1. Take a hit from another player
2. Jump within ~250 ms of the impact
3. The HUD appears with your result: **PERFECT**, **GOOD**, **LATE**, or **TOO EARLY**

No configuration is needed out of the box. Default settings are tuned for a typical 30–80 ms connection. If you play on a higher-ping server, enable **Auto Ping Adj** in General settings to shift all thresholds automatically.

Press **`K`** at any time to open the settings screen.

---

## HUD Styles

Three display styles are available. Switch between them in **Settings → Display → Style**.

### Minimal
A small pill-shaped label that fades in and out. Shows only the result classification (PERFECT, GOOD, etc.) with no additional text. Ideal for players who want feedback without visual clutter.

### Detailed
A full panel with all available information:
- Classification label with accent color
- Elapsed time in milliseconds
- Coaching hint ("perfect!", "slightly late", etc.)
- Score bar — a gradient bar showing where your timing fell in the scoring curve
- History strip — the last N results as colored dots

### Timing Bar
A horizontal bar visualising the timing window. A moving marker shows where your jump landed relative to the perfect zone. Label and ms readout are shown below the bar. Best for players who want to understand their timing pattern at a glance.

> **Display tab options are filtered per style.** Irrelevant toggles are hidden automatically when you switch styles, so you only see settings that actually apply.

| Option | Minimal | Bar | Detailed |
|--------|:-------:|:---:|:--------:|
| Show Label | — | ✓ | ✓ |
| Show MS | — | ✓ | ✓ |
| Show Hint | — | — | ✓ |
| Score Bar | — | — | ✓ |
| Slide Anim | ✓ | ✓ | ✓ |
| Text Shadow | ✓ | ✓ | ✓ |
| History Strip | — | — | ✓ |
| History count | — | — | ✓ |
| Scale / Opacity / Duration | ✓ | ✓ | ✓ |

---

## Crosshair Indicator

A small colored triangle appears above your crosshair to give real-time window feedback without looking away from the fight:

| Color | Meaning |
|-------|---------|
| **Yellow** | Timing window is currently open — jump now |
| **Green** | Last attempt was PERFECT or GOOD |
| **Red** | Last attempt was LATE or TOO EARLY |

Configure size and vertical offset in **Settings → Indicator**.

---

## Session Statistics

JumpReset tracks your performance across the entire play session:

| Stat | Description |
|------|-------------|
| Total attempts | All classified resets (excludes MISSED) |
| Per-class counts | Breakdown of PERFECT / GOOD / LATE / TOO EARLY |
| Hit rate | Percentage of attempts rated PERFECT or GOOD |
| Current streak | Consecutive PERFECT or GOOD attempts |
| Best streak | Highest streak reached this session |
| Average score | Mean of all Gaussian scores (0–1) |

Stats are visible in:
- **Config → General tab** — full color-coded summary (hit rate green ≥ 70%, yellow ≥ 40%, red below)
- **Debug overlay** — compact one-line summary: `sess N  hit X%  streak S (best B)`

Stats reset on game restart, or manually via the **Reset Session Stats** button in the General tab.

---

## Configuration

Press **`K`** to open the settings screen.

### General tab

| Setting | Default | Description |
|---------|---------|-------------|
| Mod Enabled | On | Master on/off switch |
| Auto Ping Adj | On | Shifts all ms thresholds by `ping × pingCompFactor × 0.5` |
| Show Missed | Off | Show HUD when the timing window expires without a jump |
| Lock HUD | Off | Prevent accidental dragging of the HUD preview |
| Debug Mode | Off | Enables the timing overlay with raw ms, ping, and session stats |
| Debug Layout | Full | Toggle between full and compact debug overlay |
| Knockback threshold | 0.065 | Minimum horizontal velocity to qualify as combat knockback |
| Reset Session Stats | — | Clears all session counters immediately |

### Timing tab

| Setting | Default | Description |
|---------|---------|-------------|
| Too early < | 15 ms | Below this = TOO EARLY |
| Ideal (peak) | 80 ms | Center of Gaussian curve; score = 1.0 here |
| Perfect ≤ | 150 ms | Upper bound for PERFECT |
| Good ≤ | 250 ms | Upper bound for GOOD |
| Late ≤ | 380 ms | Upper bound for LATE (beyond = MISSED) |
| Score sigma | 60 | Standard deviation of the Gaussian tail |
| Ping factor | 0.5 | Fraction of measured ping applied as threshold offset |
| Window (ground) | 6 ticks | Ticks to wait for a jump after a hit while grounded |
| Window (air) | 10 ticks | Ticks to wait for a jump after a hit while airborne |

### Indicator tab

| Setting | Default | Description |
|---------|---------|-------------|
| Show Indicator | On | Enable/disable the crosshair triangle |
| Size | 5 px | Half-width of the triangle base (total width = 2×size+1) |
| Offset | 14 px | Distance from crosshair centre to triangle tip |

---

## Keybinds

| Key | Action |
|-----|--------|
| `K` | Open settings |
| `J` | Toggle mod on / off |

Both can be rebound in **Options → Controls → Key Binds**.

---

## FAQ

**Does this work on servers with anti-cheat?**  
Yes. JumpReset is purely client-side and read-only. It observes your movement but never modifies packets or server-side state, and is not detectable by any standard anti-cheat.

**Why does it sometimes not fire?**  
The mod only evaluates jumps while in active combat — it requires that you took knockback-qualifying damage within the last 40 ticks (~2 seconds). Falls, fire damage, and poison are filtered out by the horizontal velocity threshold.

**The HUD never appears.**  
Check that the mod is enabled (`J` key) and that `feedbackStyle` is not set to a blank value in the config file. Enable Debug Mode — if the overlay draws but no result ever fires, the issue is with jump or hit detection; check `knockbackThreshold` in the Timing tab.

**Can I use it on versions older than 1.21.11?**  
Not with this release. This build targets 1.21.11 specifically. Older builds may be available on the releases page.

---

# Technical Reference

## Architecture

```
com.jumpreset
├── JumpResetMod            ClientModInitializer; singleton HUD/tracker refs;
│                           volatile preMoveVelocityY written by Mixin
├── config
│   └── ModConfig           GSON-backed config; .minecraft/config/jumpreset.json
├── mixin
│   └── PlayerMoveMixin     @Inject into LivingEntity.move() HEAD;
│                           writes JumpResetMod.preMoveVelocityY
├── state
│   ├── JumpResetTracker    Main tick loop; hit/jump detection; state machine
│   ├── JumpResetResult     Immutable record: ms, ticks, score, classification, ping
│   ├── TimingResult        Enum (PERFECT/GOOD/LATE/TOO_EARLY/MISSED) + scoring
│   ├── SessionStats        In-memory session aggregate; never persisted
│   ├── TimingHistory       Ring buffer of the last N JumpResetResult values
│   ├── HitSnapshot         Snapshot of state at hit registration tick
│   └── TrackingState       IDLE | WINDOW_ACTIVE
└── ui
    ├── JumpResetHud        HUD rendering — minimal / detailed / bar
    ├── ConfigScreen        In-game 4-tab settings screen
    ├── CrosshairIndicator  Triangle overlay above crosshair
    └── util
        └── Easing          Quadratic-out interpolation for slide-in animation
```

---

## Detection Pipeline

Runs every client tick via `ClientTickEvents.END_CLIENT_TICK`.

```
[END_CLIENT_TICK]
      │
      ├─ 1. Read preMoveVelocityY (written by Mixin at move() HEAD this tick)
      │
      ├─ 2. JUMP DETECTION
      │      vyDelta  = preMoveVelocityY − prevVelocityY
      │      jumpNow  = vyDelta ≥ jumpDeltaThreshold (0.25)
      │                 AND preMoveVelocityY ≥ 0.10
      │                 AND !isOnGround()
      │      → if true: lastJumpTick = currentTick, lastJumpNano = System.nanoTime()
      │
      ├─ 3. HIT DETECTION
      │      hitNow = prevHurtTime == 0
      │               AND hurtTime > 0
      │               AND timeUntilRegen > 0
      │               AND horizMag > knockbackThreshold (0.065)
      │
      ├─ 4. ON HIT — three branches
      │      a. PRE-HIT JUMP (lastJumpTick within 1–2 ticks before current)
      │         → dispatch TOO_EARLY immediately, skip window
      │      b. SAME-TICK (lastJumpTick == currentTick)
      │         → dispatch PERFECT with displayMs = 0.0
      │      c. NORMAL → open WINDOW_ACTIVE, store HitSnapshot
      │         (back-to-back hit while window active: replace snapshot, stay in WINDOW_ACTIVE)
      │
      ├─ 5. WINDOW EXPIRY (state == WINDOW_ACTIVE)
      │      elapsed ticks > windowTicksGround (6) or windowTicksAir (10)
      │      → optional MISSED dispatch (if showMissed = true)
      │      → IDLE
      │
      ├─ 6. JUMP WITHIN WINDOW (jumpNow AND WINDOW_ACTIVE AND isInCombat())
      │      result = JumpResetResult.evaluate(snapshot, lastJumpNano, currentTick)
      │      → dispatchResult() → sessionStats.record() + hud.showResult()
      │      → IDLE
      │
      └─ 7. Carry-over
             prevHurtTime  ← hurtTime
             prevOnGround  ← onGround
             prevVelocityY ← preMoveVelocityY
```

**Ping compensation — timestamp correction:**
```
hitNano = tickNano − (long)(ping × pingCompFactor × 1_000_000)
```

**Ping compensation — threshold shift** (when `autoPingAdjust = true`):
```
offset = ping × pingCompFactor × 0.5   (ms)
all thresholds += offset at classification time
```

---

## Scoring Model

A two-segment curve maps elapsed milliseconds to a score in [0, 1]:

```
ms < tooEarlyMs              → 0.0           (hard floor)
tooEarlyMs ≤ ms < perfectMs  → (ms − tooEarlyMs) / (perfectMs − tooEarlyMs)
ms ≥ perfectMs               → exp(−(ms − perfectMs)² / (2 × scoreSigma²))
```

Classification thresholds (after ping offset):

| Score | Result |
|-------|--------|
| ≥ 0.78 | PERFECT |
| ≥ 0.40 | GOOD |
| < 0.40 and ms ≤ lateMaxMs | LATE |
| ms > lateMaxMs | MISSED |

Default approximate windows at zero ping:

| Result | ms range |
|--------|---------|
| TOO EARLY | < 15 ms |
| PERFECT | ~62 – 150 ms |
| GOOD | 150 – 250 ms |
| LATE | 250 – 380 ms |
| MISSED | > 380 ms |

---

## Configuration Reference

Config file: `.minecraft/config/jumpreset.json`

```json
{
  "enabled": true,
  "debugMode": false,
  "debugDisplayMode": "full",
  "feedbackStyle": "detailed",
  "tooEarlyMs": 15.0,
  "perfectMs": 80.0,
  "perfectMaxMs": 150.0,
  "goodMaxMs": 250.0,
  "lateMaxMs": 380.0,
  "scoreSigma": 60.0,
  "autoPingAdjust": true,
  "pingCompFactor": 0.5,
  "knockbackThreshold": 0.065,
  "jumpDeltaThreshold": 0.25,
  "windowTicksGround": 6,
  "windowTicksAir": 10,
  "hudX": 0.5,
  "hudY": 0.65,
  "hudScale": 1.0,
  "hudOpacity": 1.0,
  "hudLocked": false,
  "showLabel": true,
  "showMs": true,
  "showHint": true,
  "showScoreBar": true,
  "animateSlideIn": true,
  "textShadow": false,
  "showHistory": true,
  "historyCount": 5,
  "showMissed": false,
  "displayDurationMs": 1800,
  "showCrosshairIndicator": true,
  "crosshairTriangleSize": 5,
  "crosshairIndicatorY": 14,
  "colorPerfect": -16741254,
  "colorGood": -16265490,
  "colorLate": -32768,
  "colorTooEarly": -52429,
  "colorBad": -52429
}
```

---

## Building from Source

**Prerequisites:** JDK 21, internet access on first build.

```bash
git clone https://github.com/your-username/JumpReset-mod.git
cd JumpReset-mod
chmod +x gradlew
./gradlew build
```

Output JAR: `build/libs/jumpreset-v2.5.0.jar`

Uses Fabric Loom 1.14.10 and Gradle 9.2. Java toolchain is fixed to JDK 21.

---

## Changelog

### v2.5.0

A maintenance and code-quality release. No change to the detection pipeline or
default feel; behaviour is the same except where noted as a fix.

#### Bug Fixes

**"Show Missed" now actually works**  
The option was dead: `TimingResult.MISSED.shouldShow()` always returned `false`, so the
missed-window result was built and then discarded. `shouldShow()` now returns `true` for
MISSED when `showMissed` is enabled, and `SessionStats.record()` explicitly ignores MISSED
so it never inflates totals or breaks streaks. With the option off (the default) behaviour
is unchanged.

**Timing sliders now drive classification**  
`TimingResult.fromMillis()` previously labelled results from the Gaussian *score*
(PERFECT ≥ 0.78, GOOD ≥ 0.40), so the *Perfect ≤* / *Good ≤* / *Late ≤* sliders and
the on-screen "bar" zones did not actually control the PERFECT/GOOD/LATE verdict.
Classification is now threshold-based and matches both the bar zones and the
`hint()` text exactly: `tooEarlyMs → perfectMs → perfectMaxMs → goodMaxMs → lateMaxMs`.
The Gaussian `score()` is retained solely for the score bar and the session average.

**`score()` division-by-zero guard**  
A config with `tooEarlyMs ≥ perfectMs` produced `NaN`/`∞` in the linear ramp.
The denominator is now clamped to ≥ 1 ms.

**Config validation on load**  
`ModConfig.sanitize()` runs after every load, clamping all values to safe ranges
and enforcing `tooEarlyMs < perfectMs ≤ perfectMaxMs ≤ goodMaxMs ≤ lateMaxMs`, plus
validating the `feedbackStyle` / `debugDisplayMode` enums. A hand-edited or
out-of-date config can no longer break classification or the HUD.

**Config screen — session summary placement**  
The General-tab session summary was positioned with a hardcoded row count and could
overlap the HUD-preview area on smaller screens. It is now anchored to the actual
end of the tab's controls.

#### Refactoring

- **`JumpResetTracker.tick()` decomposed.** The ~120-line method is now a small,
  readable pipeline (`sample` → `detectJump` → `detectHit`/`handleHit` →
  `checkWindowExpiry` → `checkJumpInWindow`) backed by an immutable per-tick
  `Sample` record. Runtime behaviour is unchanged.
- **Type-safe config enums.** The stringly-typed `feedbackStyle` (`"minimal"` /
  `"detailed"` / `"bar"`) and `debugDisplayMode` (`"compact"` / `"full"`) are now
  the `FeedbackStyle` and `DebugDisplayMode` enums, removing magic-string `switch`/
  `equals` checks scattered across `JumpResetHud`, `ConfigScreen` and `ModConfig`.
  `@SerializedName` keeps existing `jumpreset.json` files fully compatible.
- **`TimingResult` slimmed.** Removed the dead `color` field and its constructor
  argument (colours come from `configColor()` / `ModConfig`); dropped three unused
  tracker getters.
- **`ConfigScreen` drag handling** rewritten to use the idiomatic `Screen` mouse events
  (`mouseClicked` / `mouseDragged` / `mouseReleased`) instead of polling raw GLFW state
  every tick. Coordinates come already in GUI space, and widgets get first refusal on
  every click, so buttons and sliders keep working even under the preview.
- **HUD placement de-duplicated.** The repeated screen-size / clamp / slide-in code in
  the three render styles and the preview now goes through shared `panelOrigin()` and
  `slideInY()` helpers.

#### Code Quality

- **Removed dead code:** the `jumpingThisTick` flag and the mixin's velocity-signature
  branch (cleared every tick and never read — detection uses delta-vy in the tracker),
  and the unused `prevHorizMag` carry-over field. The mixin is now a minimal
  velocity-capture injection.
- **New `com.jumpreset.util.RenderUtil`:** the `fill` / `border` / `blendA` / scaled-text
  primitives that were copy-pasted across `JumpResetHud`, `TimingHistory` and
  `CrosshairIndicator` now live in one place.
- **Logging:** replaced `System.err.println` with an SLF4J logger.
- **`SessionStats`** counters are now `volatile` (read from the render thread), with the
  thread-safety note corrected.
- Added the `LICENSE` file (MIT, as declared in `fabric.mod.json`) and refreshed stale
  build-script comments.

---

### v2.0.0

#### New Features

**SessionStats** (`com.jumpreset.state.SessionStats`)  
New class tracking aggregate performance for the current play session. Fields: `total` (int), `perfect` / `good` / `late` / `tooEarly` (int), `streak` / `bestStreak` (int), `scoreSum` (double). All writes happen on the client tick thread; reads from the render thread are safe because all fields are primitive word-sized values. Never persisted to disk — resets on game restart. Owned by `JumpResetTracker` as `public final SessionStats sessionStats`.

`SessionStats.record(JumpResetResult)` is called inside `JumpResetTracker.dispatchResult()`, making it the single dispatch point for both the HUD and the stats system. MISSED results are filtered by `TimingResult.shouldShow()` before `dispatchResult()` is reached and are therefore never counted in session totals.

**Debug overlay — session line**  
The full debug overlay (`debugDisplayMode = "full"`) now renders a third text line: `sess N  hit X%  streak S (best B)`. Color: green when hit rate ≥ 70%, neutral otherwise. Compact mode (`"compact"`) does not include this line.

**Config → General — session summary panel**  
Two text lines are drawn below the last button in the General tab:
- Line 1: `P: N  G: N  L: N  E: N` (per-classification counts)
- Line 2: `Hit: X%  Avg: Y  Streak: S (best B)` with color threshold on hit rate

**Config → General — Reset Session Stats button**  
Calls `JumpResetMod.tracker.sessionStats.reset()` on click. Immediately zeroes all counters; the summary panel refreshes on the next render frame.

**Style-aware Display tab**  
`ConfigScreen.buildDisplayTab()` now reads `ModConfig.feedbackStyle` at build time and conditionally adds controls. On style change the button callback calls `clearChildren(); init()` to fully rebuild the screen. Previously all controls were always added regardless of active style, causing options like *Score Bar* and *History Strip* to appear in Minimal and Bar styles where they have no effect.

#### Bug Fixes

**CrosshairIndicator rendered in F1 / hide-HUD mode**  
The render callback in `CrosshairIndicatorRenderer` had an empty guard block that was compiled away. The check `MinecraftClient.getInstance().options.hudHidden` is now evaluated correctly before any draw call, suppressing the triangle whenever the HUD is hidden.

**ModConfig Javadoc — incorrect removal notice**  
The class-level Javadoc stated `jumpDeltaThreshold` had been removed. The field is still read by `JumpResetTracker.tick()` on every tick (`vyDelta >= cfg.jumpDeltaThreshold`). Comment corrected to reflect actual usage.

---

### v1.9.0

#### New Features

**CrosshairIndicator** (`com.jumpreset.ui.CrosshairIndicator`)  
A filled triangle rendered above the crosshair via `HudRenderCallback`. Color logic: yellow (`0xFFFFCC00`) while `JumpResetTracker.state == WINDOW_ACTIVE`; green (`colorPerfect`) when `lastResult` is PERFECT or GOOD and within `displayDurationMs`; red (`colorTooEarly`) on LATE or TOO_EARLY. Reads `JumpResetTracker.lastResult` (volatile `JumpResetResult`) and `lastResultTimestamp` (volatile `long`) from the render thread.

**Exposed observable state on JumpResetTracker**  
`lastResult` and `lastResultTimestamp` added as `public volatile` fields to allow lock-free render-thread reads. Previously there was no safe way for `CrosshairIndicator` to read the most recent result without accessing internal tracker state.

**Config: Indicator tab**  
New tab added to `ConfigScreen` with three controls mapped to `showCrosshairIndicator`, `crosshairTriangleSize`, and `crosshairIndicatorY`.

**ModConfig — three new fields**  
`showCrosshairIndicator` (boolean, default `true`), `crosshairTriangleSize` (int, default `5`), `crosshairIndicatorY` (int, default `14`). All three are persisted to `jumpreset.json` and handled by the existing GSON serializer with no migration needed.

#### Bug Fixes

**Same-tick ping delay (BUG 2)**  
When `lastJumpTick == currentTick` the previous code fell through to the general `JumpResetResult.evaluate()` path, which computed `(lastJumpNano - hitNano)` in nanoseconds. Because both nanos were recorded within the same tick's execution, instruction ordering produced a small nonzero delta (typically 1–5 µs), displayed as `0.00 ms` but occasionally as `1 ms`. The same-tick branch now constructs the result directly with `displayMs = 0.0` and hardcodes `TimingResult.PERFECT`, bypassing the nanosecond math entirely.

---

### v1.8.0

#### New Features / Rewrites

**Velocity-signature Mixin** (replaces `LivingEntity.jump()` hook)  
Previous Mixin targeted `jump()` directly. This missed jumps processed via `PlayerMoveC2SPacket` integration and failed silently when the method was inlined by the JIT. New Mixin targets `LivingEntity.move()` HEAD and validates the three-condition signature:
1. `movementType == MovementType.SELF`
2. `isOnGround()` at entry
3. `velocity.y ∈ [0.38, 0.85]` (the exact range produced by a ground jump impulse)

Knockback passes condition 1 but fails condition 3 (knockback vy is typically < 0.1 or negative). Terrain-induced velocity changes fail condition 1 (they use `PISTON` or `SHULKER_BOX`). Fall decay fails condition 2. Only a genuine player-initiated ground jump satisfies all three simultaneously.

**`preMoveVelocityY`**  
`public static volatile double preMoveVelocityY` added to `JumpResetMod`. The Mixin writes it unconditionally at `move()` HEAD. `JumpResetTracker.tick()` reads it at the start of `END_CLIENT_TICK` and stores it into `prevVelocityY` at the end, giving a clean per-tick delta.

#### Bug Fixes

**Silent no-op Mixin**  
v1.7 Mixin compiled without errors and injected successfully (confirmed by Mixin debug log), but the `@At` target used an incorrect method descriptor for the `jump()` overload present in 1.21.x mappings. The injection point was never reached at runtime. `jumpDetected` was never set to `true`, causing the HUD to never fire in normal play. Fixed by switching to the `move()` HEAD target, which has a stable descriptor across all 1.21.x versions.
