# CaptureSpawn

A Paper plugin that lets you “capture mobs into a Pokéball” and release them via right-click or a physics-based throw. It preserves entity NBT as much as possible and simulates more realistic projectile behavior (gravity / bounces / rolling / lifetime).

- Runtime: Paper 1.21.1+, Java 21
- Required dependencies: PacketEvents, NBTAPI (Item-NBT-API plugin)
- License: MIT ([LICENSE](LICENSE))

## Features

- Capture & release with compressed NBT storage (GZIP + Base64) and necessary sanitization to reduce size risks
- THROW mode: behaves like a real projectile; only captures on collision; can bounce/roll on ground; auto-recovers on timeout
- Pokéball item supports custom player-head texture and configurable name/lore/glow/custom-model-data
- Lore enrichment for common mobs (villager profession/level, sheep color, cat variant, wolf collar/tamed, etc.)
- Protection-friendly: optional “build permission” checks for capture/release to prevent abuse in protected areas
- Persistent local logs with time-range queries (CO-like time filtering)

## Quick Start

1. Install dependencies: PacketEvents, NBTAPI (Item-NBT-API plugin)
2. Put CaptureSpawn into `plugins/` and start the server once to generate config
3. Edit `plugins/CaptureSpawn/config.yml`

Default interaction mode is THROW (can be switched back to DIRECT in config).

## Commands

- `/capturespawn reload` reload config and listeners
- `/capturespawn log <player|me> <from> [to] [release|capture|all] [limit]`
  - Time examples: `10m` `1h` `2d` `1w` `1h30m`
  - Examples:
    - `/capturespawn log me 10m all 50`
    - `/capturespawn log steve 3d 1d release 100`

## Permissions

- `capturespawn.capture` capture permission
- `capturespawn.release` release permission
- `capturespawn.bypass.blacklist` bypass capture blacklist
- `capturespawn.craft` craft permission (optional)
- `capturespawn.reload` reload permission (default: OP)
- `capturespawn.log` log query permission (default: OP)

## Config Notes

`plugins/CaptureSpawn/config.yml` (high-level)

- `interaction-mode: THROW | DIRECT`
- `throw.physics.*`: air drag / bounce restitution / friction / max bounces / min-speed threshold
- `storage.nbt-format: GZIP_BASE64` + `storage.max-bytes`: control payload size
- `protection.capture-requires-build / protection.release-requires-build`: build-based checks
- `logging.*`: local log enablement, filename pattern, flush interval, queue limit

## Local Logs

Logs are written daily by default:

- `plugins/CaptureSpawn/logs/ball-YYYY-MM-DD.log`

The log format is TSV (one record per line) and includes timestamp, player, action, entity type, world/coords, result, etc.

## Compatibility

- Dependencies:
  - PacketEvents (used for THROW mode packet-based triggering)
  - NBTAPI (Item-NBT-API plugin for reading/writing entity NBT)

## Build From Source

```bash
./gradlew build
```

Artifacts are located in `build/libs/`.
