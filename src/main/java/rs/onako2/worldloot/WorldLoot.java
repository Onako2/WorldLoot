package rs.onako2.worldloot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.onako2.worldloot.config.Config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WorldLoot implements ModInitializer {
    public static final String MOD_ID = "worldloot";

    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final Map<String, Map<Config.Configuration.Structure, Integer>> timer = new ConcurrentHashMap<>();

    public static Map<String, Map<Config.Configuration.Structure, Integer>> getTimer() {
        return timer;
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing WorldLoot...");
        Config.Configuration config = Config.getConfig();
        if (config == null) {
            LOGGER.error("Config could not be loaded");
            return;
        }
        config.structures.forEach((id, structures) -> {
            timer.put(id, new ConcurrentHashMap<>());
            for (Config.Configuration.Structure structure : structures) {
                timer.get(id).put(structure, structure.intervalTicks);
            }
        });
    }
}
