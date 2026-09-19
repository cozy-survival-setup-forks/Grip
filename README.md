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
| `/grip reload` | Reload `config.yml` and `lang.yml` | `grip.admin` (ops) |

`grip.bypass` lets someone drop anything with a single press.

## Building

Needs Java 21.

```
./gradlew build
```

The jar ends up in `build/libs`. To try it on a local server:

```
./gradlew runServer
```
