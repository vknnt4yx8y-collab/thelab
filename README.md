# TheLab MiniGame Plugin

A complete, production-ready Minecraft Spigot/Paper plugin that implements the **TheLab MiniGame** — a multi-arena, round-based party minigame inspired by Hypixel's "The Lab" arcade game.

## Features

- **14 fully implemented experiments (microgames)**
- **Multi-arena support** with full isolation
- **Dr. Zuk narrator system** with themed messages
- **Non-flickering team-based scoreboards**
- **Player stat tracking** (SQLite / MySQL)
- **Join signs** with auto-update
- **Interactive admin setup wizard**
- **Arena region save/restore** after each game
- **Player inventory protection** (save/restore on join/leave)
- **Tab-completed commands**

## Requirements

- Minecraft server: Spigot or Paper 1.21.1
- Java 21+

### Optional Dependencies
- PlaceholderAPI
- ProtocolLib

## Building

```bash
gradle build
```

The compiled jar will be at `build/libs/TheLab.jar`.

## Installation

1. Copy `TheLab.jar` to your server's `plugins/` folder
2. Start/restart your server
3. Configure the plugin in `plugins/TheLab/config.yml`
4. Create arenas with `/thelab create <id> <minPlayers> <maxPlayers>`
5. Use `/thelab setup <id>` to run the interactive setup wizard

## Quick Start

```
/thelab create arena1 2 16
/thelab add arena1 dodge-ball
/thelab add arena1 fight
/thelab add arena1 gold-rush
/thelab setlobby arena1
/thelab enable arena1
```

## Commands

| Command | Permission | Description |
|---------|-----------|-------------|
| `/thelab join <id>` | `thelab.join` | Join an arena |
| `/thelab quickjoin` | `thelab.join` | Join the best available arena |
| `/thelab leave` | `thelab.use` | Leave current arena |
| `/thelab spectate <id>` | `thelab.spectate` | Spectate an arena |
| `/thelab stats [player]` | `thelab.stats` | View player stats |
| `/thelab leaderboard [category]` | `thelab.leaderboard` | View leaderboard |
| `/thelab list` | `thelab.use` | List all arenas |
| `/thelab create <id> <min> <max>` | `thelab.admin` | Create a new arena |
| `/thelab delete <id>` | `thelab.admin` | Delete an arena |
| `/thelab setup <id>` | `thelab.admin` | Run setup wizard |
| `/thelab add <id> <experiment>` | `thelab.admin` | Enable experiment for arena |
| `/thelab remove <id> <experiment>` | `thelab.admin` | Disable experiment for arena |
| `/thelab setlobby <id>` | `thelab.admin` | Set arena lobby spawn |
| `/thelab setspawn <id> <experiment>` | `thelab.admin` | Add experiment spawn point |
| `/thelab setmainlobby` | `thelab.admin` | Set main server lobby |
| `/thelab enable <id>` | `thelab.admin` | Enable arena |
| `/thelab disable <id>` | `thelab.admin` | Disable arena |
| `/thelab forcestart <id>` | `thelab.admin` | Force start game |
| `/thelab reload` | `thelab.admin` | Reload plugin configs |

Aliases: `/tl`, `/lab`

## Permissions

| Permission | Default | Description |
|-----------|---------|-------------|
| `thelab.use` | true | Basic command access |
| `thelab.join` | true | Join/quickjoin arenas |
| `thelab.spectate` | true | Spectate arenas |
| `thelab.stats` | true | View stats |
| `thelab.leaderboard` | true | View leaderboard |
| `thelab.admin` | op | All admin commands |

## Experiments (14 Total)

1. **Dodge Ball** - Throw snowballs to eliminate opponents (120s)
2. **Electric Floor** - Stand on the announced color or fall! (180s)
3. **Gold Rush** - Collect the most gold (90s)
4. **Crazy Paints** - Paint the most floor tiles your color (90s)
5. **Balloon Pop** - Shoot balloons for points (60s)
6. **Snowman** - Freeze opponents with snowballs (120s)
7. **Splegg** - Shoot eggs to break floor blocks (180s)
8. **Fight** - Standard deathmatch PvP (120s)
9. **Whack-A-Mob** - Hit the right mobs for points (60s)
10. **Boat Wars** - Destroy enemy boats with arrows (150s)
11. **Pig Racing** - Race your pig around the track (180s)
12. **Rocket Race** - Fly through checkpoints with Elytra (120s)
13. **Breaking Blocks** - Mine blocks for points (90s)
14. **Catastrophic** - Survive escalating disasters (180s)

## Configuration Files

All settings configurable in `plugins/TheLab/`:
- `config.yml` - Main settings (scoring, timing, database, etc.)
- `messages.yml` - All player-facing strings + Dr. Zuk narrator lines
- `scoreboard.yml` - Scoreboard layouts for each phase
- `sounds.yml` - Sound effects configuration
- `experiments.yml` - Per-experiment duration and scoring
- `arenas.yml` - Arena data (auto-managed)

## Arena Setup Wizard

Run `/thelab setup <id>` and follow the interactive prompts:
1. Stand at lobby spawn → type `set`
2. Stand at spectator spawn → type `set`
3. Left-click corner 1, right-click corner 2 → type `set`
4. Add spawn points for experiments → `addspawn` per point, `done` when finished

Type `cancel` at any time to exit.

## Database

Default: SQLite. To use MySQL, set in `config.yml`:

```yaml
database:
  type: MYSQL
  mysql:
    host: "localhost"
    port: 3306
    database: "thelab"
    username: "root"
    password: "yourpassword"
```
