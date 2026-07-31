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
import rs.onako2.worldloot.WorldLoot;
import rs.onako2.worldloot.config.Config;

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
                spawnStructure(structure);
            }
        });
    }

    @Unique
    private void spawnStructure(Config.Configuration.Structure structure) {
        // TODO: Implement
        int x = random.nextInt(-structure.radius, structure.radius) + structure.centerSpawn.x;
        int z = random.nextInt(-structure.radius, structure.radius) + structure.centerSpawn.z;
        LevelChunk chunk = getChunkAt(new BlockPos(x, 0, z));
        ServerChunkCache chunkSource = (ServerChunkCache) this.getChunkSource();

        chunkSource.getChunkFuture(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z), ChunkStatus.FULL, true)
                .thenAccept(chunkResult -> chunkResult.ifSuccess(chunkAccess -> {
                    if (chunkAccess instanceof LevelChunk levelChunk) {
                        this.players().forEach(player -> {
                            player.sendSystemMessage(Component.literal("Loaded chunk at: " + levelChunk.getPos()));
                            BlockPos placementPos = chunk.getPos().getWorldPosition();
                            placeStructure((ServerLevel) ((Object) this), Identifier.parse(structure.structureLocation), placementPos);
                        });
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
