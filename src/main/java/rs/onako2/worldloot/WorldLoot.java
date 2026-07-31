package rs.onako2.worldloot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.onako2.worldloot.config.Config;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WorldLoot implements ModInitializer {
	public static final String MOD_ID = "worldloot";

	public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final Map<Config.Configuration.Structure, Integer> timer = new ConcurrentHashMap<>();

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing WorldLoot...");
        try {
            Config.Configuration config = Config.getConfig();
			for (Config.Configuration.Structure structure : config.structures) {
				timer.put(structure, structure.intervalTicks);
			}
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
