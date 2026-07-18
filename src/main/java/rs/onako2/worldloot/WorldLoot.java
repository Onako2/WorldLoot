package rs.onako2.worldloot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rs.onako2.worldloot.config.Config;

import java.io.IOException;

public class WorldLoot implements ModInitializer {
	public static final String MOD_ID = "worldloot";

	public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing WorldLoot...");
        try {
            Config.initialize();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
