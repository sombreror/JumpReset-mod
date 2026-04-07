# JumpReset v1.9.0

Minecraft 1.21.11 · Fabric Loader ≥ 0.18.0 · Java 21

Client-side Fabric mod that improves jump reset detection in PvP by accurately distinguishing real jumps from knockback-induced movement.

---

## Features

- Precise jump detection using pre-physics velocity capture (Mixin-based)
- Tick + nanosecond hybrid timing system
- Gaussian-based performance scoring (centered at 115ms optimal window)
- Dynamic timing window depending on player state (ground / air)
- Ping compensation for network fairness
- Anti-spam HUD system with cooldown control
- Combat-only filtering for relevant feedback
- Smooth animated HUD (fade + slide transitions)
- Color-coded results: PERFECT / GOOD / BAD
- Real-time score visualization bar

---

## How it works

The mod captures player velocity **before Minecraft physics are applied**, allowing it to detect the exact tick where a jump impulse occurs.

This avoids false positives caused by:
- knockback
- fall decay
- slope movement
- post-physics velocity updates

The system then evaluates timing precision using a Gaussian scoring model centered on optimal jump reset timing.

---

## Building

### Requirements
- Java 21
- Internet (first Gradle setup only)

### Build

```bash
chmod +x gradlew
./gradlew build
