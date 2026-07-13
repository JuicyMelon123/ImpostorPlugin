# ImpostorGame

An Among-Us style impostor minigame plugin for Paper/Spigot servers (1.20.x), inspired by
the format used in YusufTe's "aramızda bir HAİN var" videos.

## Features

- **`/impstart`** — picks a random impostor from everyone online, RTPs all players
  to random spots, and reveals each player's role as a **title on screen** (never in chat).
- **Changeable RTP range** — set `rtp.range` in `config.yml`.
- **Impostor ability item** — a gray dye placed *only* in the impostor's inventory.
  Right-click to turn it green and gain **Resistance II + Strength II** (no particles).
  Right-click again to turn it back off.
- **Impostor-only kill message** — configurable text (`messages.impostor-kill`) sent
  only to the impostor when they get a kill.
- **Kick-and-spectate ruleset** — any player who dies is **kicked** (not banned) and
  told to rejoin. On rejoin, they're automatically set to **spectator mode** so they
  can watch the rest of the round. The impostor gets **one respawn** first; only once
  that's used up does dying kick them too.
- **Win conditions** — the round ends automatically and broadcasts a win message:
  - **Crewmates win** if the impostor is eliminated (out of respawns and kicked)
  - **Crewmates win** if the **Ender Dragon is killed**
  - **Impostor wins** if every crewmate has been eliminated
- **Tracker compass** — given to every player. Right-click opens a GUI with:
  - a **red dye** "En yakındaki oyuncu" → tracks the nearest other player
  - a **green dye** "En uzaktaki oyuncu" → tracks the furthest other player
  - the compass needle points at the target continuously, no coordinates shown.
- **Chat lockdown** — all normal chat, death messages, join/leave messages, and
  private message commands (`/msg`, `/tell`, `/w`, `/r`, etc.) are disabled, meant
  to be used alongside a voice chat plugin.
- **Tab list hidden** — every player is hidden from everyone's tab list (Tab key
  overlay). Players stay fully visible and interactable in the world; this only
  affects the tab overlay, so no one can tell who's online, who left, or who got
  banned just by checking tab.
- **Nametags hidden** — the floating name label above every player's head is hidden
  via a scoreboard team. Players stay fully visible and interactable in the world;
  this only removes the name text above their head.

## Commands & Permissions

| Command | Description | Permission |
|---|---|---|
| `/impstart` | Starts a new round | `impostorgame.admin` (default: op) |
| `/impostorgame reload` | Reloads `config.yml` | `impostorgame.admin` |
| `/impostorgame stop` | Force-stops the current round without banning anyone | `impostorgame.admin` |

## Configuration (`config.yml`)

```yaml
rtp:
  range: 300              # RTP radius in blocks

game:
  clear-inventory: true   # clear inventories at round start

respawn:
  impostor-respawns: 1    # how many times the impostor may respawn before being banned

messages:
  impostor-role: "&c&lYOU ARE THE IMPOSTOR"
  impostor-subtitle: "&7Kill everyone without getting caught!"
  crewmate-role: "&a&lYOU ARE A CREWMATE"
  crewmate-subtitle: "&7Find the impostor before it's too late!"
  impostor-kill: "&cYou killed someone!"

ban:
  message: "&cYou were eliminated from the Impostor Game."

rules:
  block-chat: true
  block-msg-commands: true
  blocked-commands: [msg, tell, w, whisper, r, reply, t, emsg, em, message, mail]
  block-death-messages: true
  block-join-leave-messages: true
  hide-tab-list: true
  hide-nametags: true
```

Every value here can be changed and reloaded with `/impostorgame reload` — no
recompiling needed for text/number tweaks.

## How to build

This is a normal Maven project targeting **Paper 26.1.x** (Minecraft 26.1.1). You'll need
**Java 25** and **Maven** installed locally (this sandbox has no internet access to
PaperMC's Maven repo, so the jar could not be pre-built for you here — build it on your
own machine or with your IDE, same as your TPAPlugin/SpawnPlugin projects).

```bash
cd ImpostorGame
mvn clean package
```

The finished jar will be at `target/ImpostorGame.jar`. Drop it into your server's
`plugins/` folder (the same way you'd upload TPAPlugin/SpawnPlugin via FTP) and restart.
Make sure the server itself is a **Paper 26.1.x build** — Spigot's old artifact format
was retired as of 26.1, so this won't compile against old-style Spigot dependencies
anymore.

If you use IntelliJ/Eclipse instead: import as a Maven project, let it download
`paper-api` from the PaperMC repository declared in `pom.xml`, then build the artifact
from your IDE's build menu.

## Notes / things worth testing before using on a real server

- **Kicking eliminated players is not destructive** — no bans happen anymore, so
  there's nothing to `/pardon` afterward. Eliminated players just rejoin as spectators.
  Still worth testing a full round once to confirm the win-condition broadcasts and
  spectator mode behave the way you expect.
- The plugin targets **Paper 26.1.x**. The `pom.xml` uses a version range
  (`[26.1.2.build,)`) that always grabs the latest available build — pin it to a
  specific build like `26.1.2.build.74-stable` if you want reproducible builds instead.
- This will **not** run on old-style Spigot/CraftBukkit servers or on 1.21.11 and
  earlier — Paper changed its distribution format at 26.1. If you're running an older
  server version, let me know and I'll adjust the `pom.xml`/`plugin.yml` back to the
  old format.
- The compass tracker updates once per second — fine for gameplay, but the needle
  will lag slightly behind fast-moving targets by design (avoids spamming updates).
