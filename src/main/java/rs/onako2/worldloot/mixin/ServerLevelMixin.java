package rs.onako2.worldloot.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.WritableLevelData;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rs.onako2.worldloot.ConfigException;
import rs.onako2.worldloot.DiscordHook;
import rs.onako2.worldloot.WorldLoot;
import rs.onako2.worldloot.config.Config;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin extends Level {

    @Unique
    private static final Random random = new Random();

    @Unique
    private Map<Config.Configuration.Structure, Integer> map = null;

    protected ServerLevelMixin(WritableLevelData levelData, ResourceKey<Level> dimension, RegistryAccess registryAccess, Holder<DimensionType> dimensionTypeRegistration, boolean isClientSide, boolean isDebug, long biomeZoomSeed, int maxChainedNeighborUpdates) {
        super(levelData, dimension, registryAccess, dimensionTypeRegistration, isClientSide, isDebug, biomeZoomSeed, maxChainedNeighborUpdates);
    }

    @Unique
    private static @NonNull String placeHolderPls(@NonNull String input, int x, int y, int z, @NonNull String name) {
        return input
                .replace("{x}", "" + x) // in case you are wondering why. This is so I can convert an int to a string aeap
                .replace("{y}", "" + y)
                .replace("{z}", "" + z)
                .replace("{name}", name)
                .replace("{formatted_time}", LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString().replace('T', ' '))
                .replace("{dc_relative_time}", "<t:{unix_secs}:R>")
                .replace("{dc_short_time}", "<t:{unix_secs}:t>")
                .replace("{dc_long_time}", "<t:{unix_secs}:T>")
                .replace("{dc_short_date_time}", "<t:{unix_secs}:d>")
                .replace("{dc_long_date_time}", "<t:{unix_secs}:D>")
                .replace("{dc_long_date_short_time}", "<t:{unix_secs}:f>")
                .replace("{dc_long_date_day_of_week_short_time}", "<t:{unix_secs}:F>")
                .replace("{unix_millis}", "" + System.currentTimeMillis())
                .replace("{unix_secs}", "" + ((int) (System.currentTimeMillis() / 1000)));
    }

    @Unique
    private static void placeStructure(ServerLevel level, Identifier id, BlockPos pos) {
        StructureTemplateManager manager = level.getStructureManager();

        Optional<StructureTemplate> templateOpt = manager.get(id);
        templateOpt.ifPresentOrElse(
                template -> {
                    StructurePlaceSettings settings = new StructurePlaceSettings();

                    template.placeInWorld(
                            level,
                            pos,
                            pos,
                            settings,
                            level.getRandom(),
                            2
                    );
                }, () -> WorldLoot.LOGGER.error("Structure template not found: {}", id));
    }

    @Inject(at = @At("TAIL"), method = "tick")
    private void tick(CallbackInfo info) {
        try {
            if (map == null) {
                map = WorldLoot.getTimer().get(dimension().identifier().toString());
                if (map == null) {
                    WorldLoot.LOGGER.info("{} couldn't be found", dimension().identifier().toString());
                }
            }
        } catch (Exception e) {
            WorldLoot.LOGGER.error("Failed loading for: {}", dimension().identifier().toString(), e);
        }
        if (map == null) {
            map = new ConcurrentHashMap<>();
        }
        map.forEach((structure, ticksRemaining) -> {
            if (structure.minPlayers > this.players().size()) return;
            map.put(structure, ticksRemaining - 1);
            if (ticksRemaining <= 0) {
                map.put(structure, structure.intervalTicks);
                spawnStructure(structure, 5);
            }
        });
    }

    @Unique
    private void spawnStructure(Config.Configuration.Structure structure, int retriesLeft) {
        int x = random.nextInt(-structure.radius, structure.radius) + structure.centerSpawn.x;
        int z = random.nextInt(-structure.radius, structure.radius) + structure.centerSpawn.z;
        LevelChunk chunk = getChunkAt(new BlockPos(x, 0, z));
        ServerChunkCache chunkSource = (ServerChunkCache) this.getChunkSource();

        chunkSource.getChunkFuture(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z), ChunkStatus.FULL, true)
                .thenAccept(chunkResult -> chunkResult.ifSuccess(chunkAccess -> {
                    if (chunkAccess instanceof LevelChunk) {
                        assert structure.verticalBoundary != null;
                        BlockPos placementPos = chunk.getPos().getWorldPosition().offset(0, chunk.getHeight(), 0);
                        for (int i = chunk.getHeight(); i >= structure.verticalBoundary.minY - 1; i--) {
                            if (this.getBlockState(placementPos).isSolid()) break; // I know it's deprecated, but I couldn't find an alternative, you're free to PR it <3
                            placementPos = placementPos.offset(0, -1, 0);
                        }
                        BlockState blockOver = this.getBlockState(placementPos.offset(0, 1, 0));
                        if (placementPos.getY() < structure.verticalBoundary.minY || blockOver.isSolid() || blockOver.getBlock() instanceof LiquidBlock) {
                            if (retriesLeft > 0) {
                                WorldLoot.LOGGER.info("Failed spawning structure: {}, trying again: {}", placementPos.toShortString(), structure.name);
                                spawnStructure(structure, retriesLeft - 1);
                            } else {
                                WorldLoot.LOGGER.info("Failed spawning structure: {}, won't try again: {}", placementPos.toShortString(), structure.name);
                            }
                            return;
                        }
                        placementPos = placementPos.offset(structure.offset.x, structure.offset.y, structure.offset.z);
                        placeStructure((ServerLevel) ((Object) this), Identifier.parse(structure.structureLocation), placementPos);
                        final BlockPos finalPlacementPos = placementPos;
                        if (!structure.chatMessage.isBlank()) {
                            this.players().forEach(player ->
                                    player.sendSystemMessage(
                                            Component.literal(
                                                    placeHolderPls(
                                                            structure.chatMessage,
                                                            finalPlacementPos.getX(),
                                                            finalPlacementPos.getY(),
                                                            finalPlacementPos.getZ(),
                                                            structure.name
                                                    )
                                            )
                                    )
                            );
                        }
                        if (Config.getConfig() == null)
                            WorldLoot.LOGGER.error("Can't get the config..", new ConfigException());
                        if (Config.getConfig().discord.enabled && !Config.getConfig().discord.lootCache.isBlank()) {
                            DiscordHook.sendMessage(placeHolderPls(Config.getConfig().discord.lootCache, placementPos.getX(), placementPos.getY(), placementPos.getZ(), structure.name), Config.getConfig().discord.webhookUrl);
                        }
                    }
                }));
    }
}
