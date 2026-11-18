# MANU Launcher 3.2 — Bloxstrap-inspired (Android)

Bloxstrap is **Windows-only**. This port brings everything that maps to Android:

## From Bloxstrap → Android
| Bloxstrap | Android MANU |
|-----------|----------------|
| Engine Settings / FastFlags | Full FastFlag builder + JSON editor |
| FPS unlock | SeekBar 60–240 → DFIntTaskSchedulerTargetFps |
| Prefer Vulkan | Toggle |
| Presets (potato / low / quality) | Presets tab |
| Telemetry flags | Optional toggles |
| Shadows / PostFX / Grass | Toggles |
| Discord RPC | ❌ desktop only |
| Content mods (oof, cursor) | ❌ needs APK content replace |
| Exclusive fullscreen / DPI | ❌ desktop only |
| Server geolocation | ❌ not ported |

## Android 16
- `compileSdk = 36` (Android 16)
- `targetSdk = 35` (broad install + GH Actions stable)
- minSdk 26

## Need Shizuku or Root
Flags write to `/data/local/tmp` + Roblox ClientSettings paths.

## Build
GitHub Actions → artifact `manu-launcher-apk`
