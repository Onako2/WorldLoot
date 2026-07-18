package rs.onako2.worldloot.config;

import org.jspecify.annotations.NonNull;
import rs.onako2.worldloot.ConfigException;
import rs.onako2.worldloot.WorldLoot;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class Config {

    public static final File configFile = new File("config/" + WorldLoot.MOD_ID + ".json");
    private static Configuration config = null;

    public static void initialize() throws IOException {
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            config = defaultConfig();
            Files.writeString(configFile.toPath(), WorldLoot.GSON.toJson(config));
        } else {
            try {
                config = WorldLoot.GSON.fromJson(Files.readString(configFile.toPath()), Configuration.class);
                config.check();
            } catch (Exception e) {
                WorldLoot.LOGGER.error("Failed loading the config", e);
                if (
                        configFile.renameTo(
                                new File(configFile.getAbsolutePath() + "." + System.currentTimeMillis() + ".bkp.")
                        )
                ) {
                    Files.writeString(configFile.toPath(), WorldLoot.GSON.toJson(defaultConfig()));
                }
            }
        }
    }

    public static @NonNull Configuration defaultConfig() {
        return Configuration.defaultConfig();
    }

    public static class Configuration {
        public final Discord discord;
        public final Structure[] structures;

        public Configuration(Discord discord, Structure[] structures) {
            this.discord = discord;
            this.structures = structures;
        }

        // default config
        public static Configuration defaultConfig() {
            return new Configuration(Discord.defaultConfig(), new Structure[]{Structure.defaultConfig()});
        }

        public void check() throws ConfigException {
            if (discord == null) {
                throw new ConfigException("Missing discord block");
            }
            discord.check();
            if (structures == null) {
                throw new ConfigException("Missing structure array");
            }
            if (structures.length == 0) {
                throw new ConfigException("Structure array is empty");
            }
            for (Structure structure : structures) {
                structure.check();
            }
        }

        public static class Discord {
            public final boolean enabled;
            public final String webhookUrl;
            public final String lootCache;

            public Discord(boolean enabled, String webhookUrl, String lootCache) {
                this.enabled = enabled;
                this.webhookUrl = webhookUrl;
                this.lootCache = lootCache;
            }

            // default discord config
            public static Discord defaultConfig() {
                return new Discord(false, "WEBHOOK_URL", "New loot cache at {x}, {y}, {z}. Time: {formatted_time}.");
            }

            public void check() throws ConfigException {
                if (webhookUrl == null) {
                    throw new ConfigException("Missing webhookUrl in discord block");
                }
                if (lootCache == null) {
                    throw new ConfigException("Missing loot cache in discord block, url: " + webhookUrl);
                }
            }
        }

        public static class Structure {
            public final String name;
            public final Pos3d offset;
            public final String structureLocation;
            public final Pos2d centerSpawn;
            public final int radius;
            public final int intervalTicks;

            public Structure(String name, Pos3d offset, String structureLocation, Pos2d centerSpawn, int radius, int intervalTicks) {
                this.name = name;
                this.offset = offset;
                this.structureLocation = structureLocation;
                this.centerSpawn = centerSpawn;
                this.radius = radius;
                this.intervalTicks = intervalTicks;
            }

            public static Structure defaultConfig() {
                return new Structure("Example", new Pos3d(1, 2, 1), "worldloot:example", new Pos2d(0, 0), 100, 24000);
            }

            public void check() throws ConfigException {
                if (name == null) {
                    throw new ConfigException("Missing name in structure block");
                }
                if (structureLocation == null) {
                    throw new ConfigException("Missing structureLocation in structure block, name: " + name);
                }
                if (centerSpawn == null) {
                    throw new ConfigException("Missing centerSpawn in structure block, name: " + name);
                }
                if (radius == 0) {
                    throw new ConfigException("Missing radius in structure block,  name: " + name);
                } else if (radius < 0) {
                    throw new ConfigException("radius must be positive, but is " + radius + ", name: " + name);
                }
                if (intervalTicks == 0) {
                    throw new ConfigException("Missing intervalTicks in structure block,  name: " + name);
                } else if (intervalTicks < 0) {
                    throw new ConfigException("intervalTicks must be positive, but is " + intervalTicks + ", name: " + name);
                }
            }
        }

        public static class Pos2d {
            public final int x;
            public final int z;

            public Pos2d(int x, int z) {
                this.x = x;
                this.z = z;
            }
        }

        public static class Pos3d {
            public final int x;
            public final int y;
            public final int z;

            public Pos3d(int x, int y, int z) {
                this.x = x;
                this.y = y;
                this.z = z;
            }
        }
    }
}
