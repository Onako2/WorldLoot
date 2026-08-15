# WorldLoot

WorldLoot allows you to generate structures post-generation in your world.  
This can be used for stuff such as random loot drops inside your world that spawn inside generated and new chunks.  

## How to set up

1. Download latest release from the [releases page](<https://github.com/Onako2/WorldLoot/releases>)
2. Put the mod inside a Minecraft 26.2 instance (useful guide https://docs.fabricmc.net/players/installing-fabric/)
3. Launch Minecraft with the mod
4. Configure inside `<minecraft_dir>/config/worldloot.json`

## Config

Default config
```json
{
  "discord": {
    "enabled": false,
    "webhookUrl": "WEBHOOK_URL",
    "lootCache": "New loot cache \"{name}\" at {x}, {y}, {z}. Time: {formatted_time}."
  },
  "structures": {
    "minecraft:overworld": [
      {
        "name": "Example",
        "structureLocation": "worldloot:example",
        "centerSpawn": {
          "x": 0,
          "z": 0
        },
        "radius": 100,
        "intervalTicks": 24000,
        "verticalBoundary": {
          "minY": 50,
          "maxY": 100
        },
        "retries": 5,
        "chatMessage": "New loot: {x}, {y}, {z}",
        "minPlayers": 0,
        "offset": {
          "x": 1,
          "y": 2,
          "z": 1
        }
      }
    ]
  }
}
```

### Explanation

#### Discord

* **enabled**: `boolean`
  * should this feature be enabled?
* **webhookUrl**: `String`, url
  * url of the webhook to use (e.g. from Discord), blank ("", " ", etc.) disables sending messages
* **lootCache**: `String`
  * message to send via webhook

#### Structures/Dimension

* dimension id (visible in F3 screen) `String`, identifier → structures `Structure[]`

#### Structure

* **name**: `String`
  * the name of your structure (should be unique)
* **structureLocation**: `String`, identifier
  * Minecraft datapack location of your structure, example: minecraft:village/plains/houses/plains_stable_1
* **centerSpawn**: `{x, y}`
  * coordinates of your center of the radius in which a structure should spawn in
* **radius**: `int`
  * the radius of the square area
* **intervalTicks**: `int`
  * how many ticks to wait between spawns
* **verticalBoundary**: `{minY, minX}`
  * in which height your structure may only spawn in (offset is applied after that)
* **retries**: `int`
  * how many times to retry at max after structure spawn couldn't meet criteria inside verticalBoundary
* **chatMessage**: `String`
  * message to send in chat, blank ("", " ", etc.) disables sending messages
* **minPlayers**: `int`
  * minimum players online in order to advance the timer
* **offset**: `{x, y, z}`
  * where you want to move the structure (if you want the structure to be just buried a bit then you can configure it here or want to center the structure)

### Placeholders

Messages inside the config support following placeholders:
* `{x}`: structure spawn x pos
* `{y}`: structure spawn y pos
* `{z}`: structure spawn z pos
* `{name}`: structure name
* `{formatted_time}`: Time formatted e.g. **2026-08-12 13:40:09**
* `{dc_relative_time}`: Shows **2 minutes ago**, etc. inside Discord (adapted to time zone)
* `{dc_short_time}`: Shows **13:42**, etc. inside Discord (adapted to time zone)
* `{dc_long_time}`: Shows **13:42:00**, etc. inside Discord (adapted to time zone)
* `{dc_short_date_time}`: Shows **12/08/26**, etc. inside Discord (adapted to time zone)
* `{dc_long_date_time}`: Shows **12 August 2026**, etc. inside Discord (adapted to time zone)
* `{dc_long_date_short_time}`: Shows **12 August 2026 13:42**, etc. inside Discord (adapted to time zone)
* `{dc_long_date_day_of_week_short_time}`: Shows **Wednesday, 12 August 2026 13:42**, etc. inside Discord (adapted to time zone)
* `{unix_millis}`: Milliseconds since Jan 01 1970. (UTC): e.g. 1786535327509
* `{unix_secs}`: Seconds since Jan 01 1970. (UTC): e.g. 1786535327
