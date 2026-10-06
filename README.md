# MechaniX

[![Paper](https://img.shields.io/badge/Paper-1.21.1+-blue.svg)](https://papermc.io)
[![Folia](https://img.shields.io/badge/Folia-supported-brightgreen.svg)](https://papermc.io/software/folia)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net)

A Paper/Folia plugin that adds **mechanical gates** and **teleportation portals** built from vanilla Minecraft blocks — no resource pack, no client mods, no NMS.

Build a frame out of fences, walls, or iron bars, click a button, and watch the wall slide away. Or link two frames together with a portal and walk straight through.

**🌐 [Русская версия](README.ru.md)**

---

## Features

### Gates
- Detect gates automatically by right-clicking the frame with the binding tool.
- Trigger by buttons, levers, pressure plates, or redstone.
- Configurable opening direction: `BOTTOM_UP`, `TOP_DOWN`, `X_AXIS`, `Z_AXIS`.
- Blocks are moved step-by-step with a configurable delay (default 4 ticks), so even large gates open smoothly.
- Per-player click cooldown to prevent spam.
- Optional sounds on open/close.
- Opened gates are automatically restored on server restart.
- Zone limits per player (with `mechanix.admin` bypass).

### Portals
- Two linked frames = instant teleport between them.
- Works **across worlds** (Overworld ↔ Nether ↔ End, etc.).
- Vertical, horizontal, and ceiling-mounted frames supported.
- Optional particle visualization — animated border with "breathing" fill.
- Per-portal color (hex) for `DUST` particles.
- Teleport cooldown to prevent instant-bounce loops.
- Ownership system — players manage their own portals.
- Full GUI (`/portal`) for creating, listing and deleting portals.

### General
- GUI-driven (`/mx`, `/portal`) — no commands to memorize.
- Actionbar or chat feedback (configurable).
- Fine-grained permissions (LuckPerms-compatible).
- Customizable messages in `lang/ru.yml` and `lang/en.yml`.
- **Folia-ready**: all schedulers use region-aware dispatch when running on Folia.

---

## Requirements

| Component | Version |
|---|---|
| Server | **Paper 1.21.1+** or **Folia 1.21.1+** |
| Java | **21** |

> **Spigot/CraftBukkit are NOT supported.** The plugin uses Paper-only APIs such as `Player#teleportAsync` and Adventure components.

---

## Installation

1. Download the latest `MechaniX-x.y.z.jar` from [Releases](../../releases).
2. Drop it into your server's `plugins/` folder.
3. Restart the server.
4. Edit `plugins/MechaniX/config.yml` to taste.
5. Run `/mx reload` in-game to apply changes.

---

## Quick start

### Creating a gate

1. Build a frame out of fences, walls, or iron bars — anything listed in `gates.allowed-blocks`.
2. Place a button, lever, or pressure plate nearby.
3. Run `/mx`, pick **Create gate**, then follow the prompts to select the region.
4. Bind the gate to the trigger — click the trigger when prompted.
5. Done. Right-click the trigger to open/close.

### Creating a portal

1. Build **two** frames from any solid, occluding blocks (stone, wood, etc.).
   - Both frames must be **completely closed** (a solid ring around an empty interior).
   - Minimum inner size: `portals.min-side` (default 2×2).
   - Maximum inner size: `portals.max-side` (default 21×21).
2. Run `/portal` and pick **Create portal**.
3. Click inside frame A when prompted, then inside frame B.
4. Enter a name and a hex color (`#RRGGBB`) in chat.
5. Walk through either frame to teleport to the other.

> Set `portals.allow-horizontal: false` in `config.yml` to disable lying-flat portals and only allow vertical ones.

---

## Commands

| Command | Aliases | Permission | Description |
|---|---|---|---|
| `/mx` | `/mechanix` | `mechanix.menu` | Open the main menu |
| `/mx reload` | | `mechanix.admin` | Reload the configuration |
| `/portal` | `/portals` | `mechanix.portal.use` | Open the portal menu |

---

## Permissions

| Permission | Default | Description |
|---|---|---|
| `mechanix.menu` | `true` | Access the main menu |
| `mechanix.use` | `true` | Use gates |
| `mechanix.create` | `op` | Create gates |
| `mechanix.bind` | `op` | Bind gates to triggers |
| `mechanix.delete` | `op` | Delete gates |
| `mechanix.portal.use` | `true` | Use portals |
| `mechanix.portal.create` | `op` | Create portals |
| `mechanix.portal.delete` | `op` | Delete portals |
| `mechanix.admin` | `op` | Bypass all checks and limits |

Permission checks can be disabled entirely via `permissions.enabled: false` in `config.yml`.

---

## Configuration

The full configuration is documented inline in [`config.yml`](src/main/resources/config.yml). Highlights:

```yaml
gates:
  step-delay-ticks: 4          # Delay between opening steps (ticks)
  open-direction: BOTTOM_UP    # BOTTOM_UP, TOP_DOWN, X_AXIS, Z_AXIS
  click-cooldown-ms: 500       # Button spam protection
  max-zone-volume: 20000       # Crash protection

portals:
  allow-horizontal: false      # Disable lying-flat portals
  cooldown-ms: 2000            # Teleport cooldown per player
  min-side: 2
  max-side: 21
  show-active-borders: true
  active-border-period: 2      # Lower = smoother particle stream
  particles:
    fill-enabled: true
    fill-step: 1.0
    border-density: 1
    fill-density: 1
    drift: 0.25
    spread: 0.1
```

---

## Localization

Messages live in `src/main/resources/lang/`. Two locales ship by default:

- `ru.yml` — Russian
- `en.yml` — English

To add a language, copy `en.yml`, translate the values, and set `general.locale` to the new file's base name.

---

## Building from source

```bash
git clone https://github.com/goblinthug/MechaniX.git
cd MechaniX
mvn clean package
```

The shaded jar will be at `target/MechaniX-x.y.z.jar`.

---

## Compatibility notes

- **Paper 1.20.6 and below**: `Registry.SOUNDS` doesn't exist yet; the plugin only targets **1.21+**.
- **Spigot / CraftBukkit**: **not supported** — the code uses Paper-only APIs.
- **Folia**: supported. All region-sensitive work goes through `MechaniX#runTaskAt` / `runTaskAtLater`, which dispatch to the region scheduler when Folia is detected.
- **Hybrids (Mohist, Arclight)**: untested. Paper API is partially emulated; expect rough edges.

---

## Known limitations

- Portals require a **fully closed frame**. Open arches are not supported — the interior-detection algorithm looks for an enclosed air pocket.
- A portal cannot overlap another portal's interior region.
- There is no in-game editor for existing portals — delete and recreate if you want to change a frame.

---

## Contributing

Issues and pull requests are welcome. If you find a bug, please include:

- Server type and version (Paper/Folia, exact build)
- Plugin version
- Full stack trace (if applicable)
- Steps to reproduce

---

## License

Released under the [MIT License](LICENSE).
