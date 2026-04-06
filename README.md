# JumpReset v1.2.0
### Minecraft **1.21.11** · Fabric Loader ≥ 0.18.0 · Java 21

Client-side Fabric mod that detects and visually displays **jump reset timing** in PvP.
Feedback is shown **only when the player jumps after being hit** — never on the hit itself.

---

## Features

- Dual-precision timing — tick-based (20 TPS) + `System.nanoTime()` nanosecond resolution
- Gaussian scoring centred at 115 ms ideal window
- Dynamic timing window — 5 ticks on ground, 8 ticks airborne
- Pre-physics velocity capture via Mixin — detects jump impulse on the exact tick it is applied
- Ping compensation — subtracts half the server RTT from the hit timestamp
- Anti-spam guard — 8-tick minimum between HUD displays
- Combat filter — only shows feedback if hit within the last ~2 seconds
- Smooth HUD — ease-out slide-in, fade-in/hold/fade-out animation (1.6 s total)
- Color coded — green (PERFECT), blue (GOOD), red (BAD)
- Score bar — Gaussian score as animated progress bar

---

## Building

### Prerequisites
- **JDK 21** (Temurin / Eclipse Adoptium recommended)
- Internet connection (Gradle downloads Fabric toolchain on first run)

### Steps

```bash
# macOS / Linux — make wrapper executable first
chmod +x gradlew

# Build the mod JAR
./gradlew build          # Windows: gradlew.bat build

# Output: build/libs/jumpreset-1.2.0.jar
```

> First build downloads ~300 MB of Minecraft mappings. Subsequent builds are fast.

### Installing

1. Install [Fabric Loader 0.18.x](https://fabricmc.net/use/) for Minecraft **1.21.11**
2. Install [Fabric API 0.140.x+1.21.11](https://modrinth.com/mod/fabric-api)
3. Copy `build/libs/jumpreset-1.2.0.jar` into `.minecraft/mods/`
4. Launch — the mod is active immediately, no config needed

---

## 1.21.11 API Changes vs 1.21.4

| Area | 1.21.4 | 1.21.11 |
|---|---|---|
| HUD registration | `HudRenderCallback.EVENT.register(...)` (deprecated) | `HudElementRegistry.attachElementAfter(...)` |
| HUD callback type | `(DrawContext, RenderTickCounter)` | `(DrawContext, RenderTickCounter)` (unchanged) |
| Fabric Loader | 0.16.9 | **0.18.1** |
| Fabric API | 0.111.0+1.21.4 | **0.140.2+1.21.11** |
| Yarn mappings | 1.21.4+build.8 | **1.21.11+build.4** |
| Loom | 1.9-SNAPSHOT | **1.14-SNAPSHOT** |
| Gradle | 8.10 | **8.12** |
| `Entity#getWorld` | `getWorld()` | `getEntityWorld()` (since 1.21.9 Yarn) |

> **Note on mapping names:** Yarn still names the rendering context `DrawContext`
> (`net.minecraft.client.gui.DrawContext`). `GuiGraphics` is the Mojang official name
> for the same class. Since this mod uses Yarn, `DrawContext` is the correct import.

---

## How It Works

### State Machine

```
IDLE
 │  hit confirmed (hurtTime 0→+, timeUntilRegen > 0, horizontal vel > threshold)
 ▼
WINDOW_ACTIVE  ←─── new hit while open: restart snapshot
 │  window expired (> 5 or 8 ticks) → IDLE silently
 │  jump detected (vy delta ≥ 0.25 from mixin, preMoveVy ≥ 0.10, not on ground)
 ▼
 ── evaluate timing ──→ dispatch result to HUD ──→ IDLE
```

### Jump Detection (Key Fix)

Vanilla applies the jump impulse (+0.42 m/t) in a single tick, then immediately
runs collision/drag inside the same `move()` call. Reading `getVelocity().y` at
`END_CLIENT_TICK` sees the post-physics value, which may already be decayed or
zeroed on landing frames.

The mixin captures `getVelocity().y` at `HEAD` of `move()` — before any physics —
and stores it in `JumpResetMod.preMoveVelocityY`. The tracker computes the delta
between this tick's and last tick's pre-physics vy. The jump impulse creates a
delta ≥ 0.25 on exactly one tick, which is what the tracker detects.

### Scoring

```
score(ms) = exp( -(ms - 115)² / (2 × 60²) )

≥ 0.78 → PERFECT  (≈ 70–160 ms)
≥ 0.40 → GOOD     (≈ 35–195 ms)
 < 0.40 → BAD
```

---

## Tuning Constants (`JumpResetTracker.java`)

| Constant | Default | Effect |
|---|---|---|
| `KNOCKBACK_THRESHOLD` | `0.065` | Min horizontal velocity to accept as PvP hit |
| `JUMP_VY_DELTA_THRESHOLD` | `0.25` | Min vy increase in one tick to count as jump |
| `JUMP_VY_MIN` | `0.10` | Min absolute preMoveVy (guards slope edges) |
| `WINDOW_TICKS_GROUNDED` | `5` | Window size when on ground at hit time |
| `WINDOW_TICKS_AIR` | `8` | Window size when airborne at hit time |
| `COMBAT_TIMEOUT_TICKS` | `40` | Combat flag duration (~2 s) |
| `HUD_COOLDOWN_TICKS` | `8` | Min ticks between HUD displays |
| `PING_COMPENSATION_FACTOR` | `0.5` | Fraction of RTT subtracted from hit time |

---

## File Structure

```
src/main/java/com/jumpreset/
├── JumpResetMod.java                   Entrypoint; registers tick + HUD events
├── mixin/
│   └── ClientPlayerEntityMixin.java    Captures pre-physics vy via move() hook
├── state/
│   ├── TrackingState.java              IDLE / WINDOW_ACTIVE / … enum
│   ├── HitSnapshot.java                Immutable record: tick, nanoTime, grounded
│   ├── JumpResetResult.java            Evaluated result: ms, score, classification
│   ├── JumpResetTracker.java           Core state machine (tick logic)
│   └── TimingResult.java               PERFECT / GOOD / BAD + Gaussian scoring
├── ui/
│   └── JumpResetHud.java               HUD overlay with slide + fade animation
└── util/
    └── Easing.java                     easeOutCubic, easeInQuart, computeAlpha

src/main/resources/
├── fabric.mod.json                     Mod metadata (MC ~1.21.11)
└── jumpreset.mixins.json               Mixin config (JAVA_21)
```

---

## Compatibility

- **Side:** Client-only — safe on any server (vanilla, Paper, Purpur, etc.)
- **Conflicts:** None known. Purely observational — no game mechanics modified.
- **Sodium / Iris:** Compatible — uses only standard Fabric rendering APIs.

---

## License

MIT — free to use, modify, and redistribute.
