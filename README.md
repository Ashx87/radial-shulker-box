# Radial Shulker Box

A Fabric mod that lets you open any shulker box in your inventory through a radial ("pie") menu — hold a hotkey, point, release. No more digging through your inventory and placing boxes on the ground.

<!-- TODO(Ashx87): 在这里嵌入演示 GIF（按住 G 打开轮盘 → 悬停预览 → 松开打开潜影盒） -->
> 📹 *Demo GIF coming soon.*

## Features

- **Radial menu** — hold the hotkey (default **G**) to fan out every shulker box in your inventory as a segmented ring
- **Live preview** — hovering a segment shows the box's name and a 3×9 grid of its contents in the centre of the ring
- **One-motion open** — release the hotkey over a segment to open that box directly, from anywhere; release over the centre (or press ESC) to cancel
- **Any binding** — works bound to keyboard keys or mouse buttons
- **Crisp rendering** — the ring is anti-aliased at native screen resolution on every GUI scale, and grows automatically when you carry many boxes
- **Vanilla feel** — shulker open/close sounds, action-bar hint when you have no boxes, spectator-safe
- **Dupe-proof** — box contents are written back to the item on every change server-side; moving or dropping the box closes the menu instantly
- **Localized** — English, 简体中文, 繁體中文

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft **26.1**
2. Drop [Fabric API](https://modrinth.com/mod/fabric-api) and this mod's jar into your `mods` folder
3. The mod must be present on **both client and server** (a singleplayer world counts as both)

## Usage

| Action | Result |
| --- | --- |
| Hold **G** (rebindable in Controls) | Opens the radial menu |
| Move cursor over a segment | Highlights it and previews the box contents |
| Release over a segment | Opens that shulker box |
| Release over the centre / press ESC | Closes the menu without opening anything |

## Building from source

Requires JDK 25.

```bash
./gradlew build
```

The jar lands in `build/libs/`. Run `./gradlew runClient` to test in a dev environment, and `./gradlew test` for the unit tests.

## License

[CC0-1.0](LICENSE) — public domain. Do whatever you like with it.
