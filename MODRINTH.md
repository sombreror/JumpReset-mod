# JumpReset

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-brightgreen?logo=minecraft&logoColor=white)](#)
[![Fabric](https://img.shields.io/badge/Loader-Fabric%200.18%2B-ffb347)](#)
[![Fabric API](https://img.shields.io/badge/Requires-Fabric%20API-dbb8ff)](#)
[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk&logoColor=white)](#)
[![Environment](https://img.shields.io/badge/Environment-Client--side-blue)](#)
[![Version](https://img.shields.io/badge/Version-v3.0.0-blueviolet)](#)
[![License](https://img.shields.io/badge/License-MIT-lightgrey)](#)

> **Accurate jump-reset feedback for PvP — powered by pre-physics velocity analysis.**

**JumpReset** is a client-side Fabric mod that gives you accurate, real-time feedback on your jump resets during PvP combat.

A *jump reset* is the technique of jumping immediately after taking a hit to cancel the knockback. Its effectiveness depends entirely on timing — jump too early and the game hasn't registered the hit yet; jump too late and the knockback has already been applied. The ideal window is roughly 80 ms after impact.

Most mods read velocity *after* the physics engine has already processed it, making it impossible to reliably tell the difference between an intentional jump, knockback-induced motion, and terrain. JumpReset hooks into the engine **before physics resolution**, capturing the raw velocity impulse at the exact tick it is produced — so every result you see is honest and consistent.

---

## Features

### Detection
- **Pre-physics velocity capture** — never confuses knockback, slopes, or fall decay with a real jump
- **Dynamic timing windows** — separate windows for ground and air state
- **Automatic ping compensation** — both the hit timestamp and all classification thresholds shift based on your measured latency
- **Combat filtering** — falls, fire, and poison damage are filtered out; only knockback-qualifying hits count

### Feedback
- **Five classifications**: `PERFECT` · `GOOD` · `LATE` · `TOO EARLY` · `MISSED`
- **Gaussian scoring curve** — every attempt gets a 0–1 score, not just a label
- **Crosshair indicator** — a colored triangle above your crosshair shows when the window is open (yellow) and the result of your last attempt (green / red), so you never look away from the fight
- **Coaching hints** — short tips ("slightly late", "perfect!") tell you how to adjust

### HUD — three display styles
- **Minimal** — a small pill label, lowest visual footprint
- **Detailed** — full panel with classification, ms readout, coaching hint, score bar, and a history strip of your recent attempts
- **Timing Bar** — a horizontal bar with zone markers and a moving marker showing exactly where your jump landed relative to the perfect zone

All styles feature smooth slide-in animations, drop shadows for readability on bright backgrounds, and adjustable scale, opacity, and display duration.

### Session statistics
Your performance is tracked across the whole play session:
- Hit rate (percentage of attempts rated PERFECT or GOOD)
- Current and best streak
- Per-classification counts and average score
- One-click reset button in the settings screen

### Configuration
- Press **`K`** anytime to open the in-game settings screen (toggle the mod with **`J`**)
- **Style-aware settings** — the Display tab only shows options relevant to your active HUD style
- **Live, draggable preview** — the settings screen renders your actual HUD style and lets you drag it anywhere on screen
- All timing thresholds, colors, indicator size, and debug options are adjustable in-game
- Sensible defaults tuned for a typical 30–80 ms connection — no setup required

---

## Compatibility

| | |
|---|---|
| **Minecraft** | 1.21.11 |
| **Loader** | Fabric ≥ 0.18.0 |
| **Fabric API** | Required |
| **Java** | 21 |
| **Side** | Client-side only |

Works on any server — vanilla, Paper, Fabric, or otherwise. No server installation needed. JumpReset is read-only: it observes your movement but never modifies packets or gameplay, so it is safe to use with standard anti-cheats.

---

## Getting Started

1. Install [Fabric Loader](https://fabricmc.net/use/) 0.18.0+ and [Fabric API](https://modrinth.com/mod/fabric-api)
2. Drop the jar into your `mods` folder and launch the game
3. Take a hit, jump right after — the HUD shows your result instantly
4. Press **`K`** to fine-tune everything to your taste

---

## Links

- **Source code**: [GitHub](https://github.com/sombreror/JumpReset-mod)
- **Issues & suggestions**: [Issue tracker](https://github.com/sombreror/JumpReset-mod/issues)
- **License**: MIT
