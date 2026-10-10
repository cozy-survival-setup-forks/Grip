# Grip

Asks for a second Q press before something valuable leaves your inventory, so a slip of the
finger doesn't cost you your tools, gear or valuables. Made for Paper 1.21.11.

## How it works

The first press on an item that needs care is held back and the player sees a short prompt with
the item's name. A second press on the same item within a few seconds drops it. Pressing Q on
anything else, or waiting too long, starts the prompt again.

Which items ask, in this order:

1. Anything on `always_confirm` (materials or item tags)
2. Items with a protected trait: renamed, custom model, enchanted, or a non-empty shulker box
   or bundle
3. Anything on the free list, or food and blocks if those are switched on, drops straight away
4. Everything else asks, which covers tools, weapons and armor

Throwing an item by clicking outside the inventory window asks the same way.

## Features

- Per-player switch with `/grip toggle`
- Prompt in the action bar with a sound, or in chat, or both
- Item tags such as `#minecraft:swords` in every list
- Creative players skip the prompt
- Short `config.yml` and `lang.yml`, reloadable in game

## Commands

| Command | What it does | Permission |
| --- | --- | --- |
| `/grip toggle` | Turn the prompt on or off for yourself | `grip.toggle` (everyone) |
| `/grip reload` | Reload `config.yml` and `lang.yml`. A value with a mistake cancels the reload and the old settings stay | `grip.admin` (ops) |
| `/grip doctor` | Health of the files, versions, last backup and recent save failures (no player data) | `grip.admin` |
| `/grip backup now` | A checked copy of the settings and data files in `backups/` | `grip.admin` |

`grip.bypass` lets someone drop anything with a single press.

## Keeping your files safe

- `config.yml` and `lang.yml` start with a `config-version` / `lang-version` number. After an update, new settings are added to your files with their comments, and nothing you changed is touched. The old file is kept next to it as `config.yml.<date>.bak` (the newest 5). A setting is only removed when the changelog says so.
- A value with a mistake (a negative time, an item that does not exist, text where a number belongs) is named in the console by file and key. On a reload the settings in use stay as they were.
- Files are written to a temporary file and moved into place, with the previous version kept as `.bak`. A file that cannot be read is restored from its `.bak`, and the unreadable one is kept as `.broken-<time>`.
- A file with a version newer than the plugin knows is left alone and a warning is logged.

## Telemetry

On startup Grip sends a small anonymous beacon (plugin name/version, server software/version,
online/max player counts, and a random ID with no player data) so we know which versions are in
use. Turn it off with `metrics.enabled: false` in `config.yml`. The random ID is kept as `server-id`
in `data.yml` (older versions kept it in a `.server-id` file, which is moved over unchanged).
The address and the interval are fixed in the plugin and are not settings.

## Building

Needs Java 21.

```
./gradlew build
```

The jar ends up in `build/libs`. To try it on a local server:

```
./gradlew runServer
```

See `LICENSE`: free to run on your own servers, not for redistribution or resale.
