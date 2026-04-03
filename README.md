# ![MUTE-Title (4)p2](https://github.com/user-attachments/assets/31babf2b-ce7e-47ac-a0bd-9095367aa010)

## PlaytimeLink Fabric
PlaytimeLink is a server-side Fabric mod designed to bridge playtime data from a Velocity proxy to your Minecraft server. It allows you to display player playtime and rankings using placeholders on any Fabric-based server.

## Installation
1. Download the latest `PlaytimeLink-Fabric` JAR.
2. Place the JAR into your server's `mods/` directory.
3. Ensure all required dependencies are also installed.
4. Restart your server.

## Dependencies
- **Fabric API**: Standard library for Fabric mods.
- **Patbox Text Placeholder API**: Required for placeholder registration and parsing.
- **Velocity-PlayTime**: Must be installed and configured on your Velocity proxy.
- **PAPIProxyBridge**: Required for cross-server communication between the proxy and the Fabric server.

## Placeholders
The mod provides the following placeholders via the `vptlink` namespace:

| Placeholder | Description |
| :--- | :--- |
| `%vptlink:totalhours%` | Total hours played by the player. |
| `%vptlink:minutes%` | Remaining minutes of playtime (0-59). |
| `%vptlink:formatted%` | Compact playtime like `8w 6d 12h`. |
| `%vptlink:place%` | The player's current rank in the playtime top list. |
| `%vptlink:topname:<rank>%` | The name of the player at the specified rank (e.g., `%vptlink:topname:1%`). |
| `%vptlink:toptime_totalhours:<rank>%` | Total hours played by the player at the specified rank. |

### Fallback Behavior
- **Loading...**: Displayed when data is still being fetched from the proxy.
- **Not in toplist**: Displayed for `%vptlink:place%` if the player isn't ranked.

## TAB Configuration Example
To display playtime in your header using the [TAB](https://modrinth.com/plugin/tab) mod, use the following configuration:

```yaml
header:
  - '&7Your playtime: &e%vptlink:formatted%'
  - '&7Rank: &e#%vptlink:place%'
```

## Compatibility
- **Minecraft Version**: 1.21.7 (Primary)
- **Java Version**: 21 or higher
- **Environment**: Server-side only

## Repository Notes
- This repository publishes the Fabric mod source from the root project.
- Local reference copies such as `PlaytimeLink/` and `Velocity-PlayTime/` are ignored in this workspace and are not part of the published root project.
- Before publishing your own fork, review any config templates and generated files to ensure no local credentials were added.
- Local-only workspace files such as IDE metadata, `bin/`, and nested Git metadata are ignored and should not be included in commits.

## Automated Releases
- GitHub Actions now rebuilds the mod on every push to `master` and on manual dispatch.
- The workflow force-updates the `latest` release tag to the current commit and uploads a single stable asset named `playtimelink-fabric-latest.jar`.
- After enabling Actions in your fork, download the current artifact from the repository's **Releases** page.
