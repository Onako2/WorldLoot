package rs.onako2.worldloot.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rs.onako2.worldloot.DiscordHook;
import rs.onako2.worldloot.WorldLoot;
import rs.onako2.worldloot.config.Config;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin extends Level {

    @Unique
    private static final Random random = new Random();

    protected ServerLevelMixin(WritableLevelData levelData, ResourceKey<Level> dimension, RegistryAccess registryAccess, Holder<DimensionType> dimensionTypeRegistration, boolean isClientSide, boolean isDebug, long biomeZoomSeed, int maxChainedNeighborUpdates) {
        super(levelData, dimension, registryAccess, dimensionTypeRegistration, isClientSide, isDebug, biomeZoomSeed, maxChainedNeighborUpdates);
    }

    @Inject(at = @At("TAIL"), method = "tick")
    private void tick(CallbackInfo info) {
        WorldLoot.timer.forEach((structure, ticksRemaining) -> {
            WorldLoot.timer.put(structure, ticksRemaining - 1);
            if (ticksRemaining <= 0) {
                WorldLoot.timer.put(structure, structure.intervalTicks);
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
                    if (chunkAccess instanceof LevelChunk levelChunk) {
                        int minAdd = chunk.getMinY() < 0 ? chunk.getMinY() * -1 : 0;
                        assert structure.verticalBoundary != null;
                        BlockPos placementPos = chunk.getPos().getWorldPosition().offset(0, chunk.getHeight(), 0);
                        for (int i = chunk.getHeight(); i >= structure.verticalBoundary.minY - 1; i--) {
                            if (this.getBlockState(placementPos).isSolid()) break;
                            placementPos = placementPos.offset(0, -1, 0);
                        }
                        if (placementPos.getY() < structure.verticalBoundary.minY || this.getBlockState(placementPos.offset(0, 1, 0)).isSolid()) {
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
                        if (Objects.requireNonNull(Config.getConfig()).discord.enabled) {
                            DiscordHook.sendMessage(Config.getConfig().discord.lootCache.replace("{x}", "" + placementPos.getX()).replace("{y}", "" + placementPos.getY()).replace("{z}", "" + placementPos.getZ()).replace("{name}", structure.name).replace("{formatted_time}", LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString()), Config.getConfig().discord.webhookUrl);
                        }
                    }
                }));
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
}
