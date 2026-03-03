/**
 * ============================================================
 * [ApicaChunkGenerator.java]
 * Description: Générateur de chunks pour la dimension Apica —
 *              terrain plat avec couches probabilistes et
 *              plateformes de concrete sur une grille
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | ChunkGenerator      | Classe de base       | Génération de terrain          |
 * | BiomeSource         | Source de biomes      | Codec sérialisation            |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ApicaChunkGenerators.java (registre du codec)
 * - data/apica/dimension/apica.json (référence JSON)
 *
 * ============================================================
 */
package com.chapeau.apica.content.dimension;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ApicaChunkGenerator extends ChunkGenerator {

    public static final MapCodec<ApicaChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource)
            ).apply(instance, instance.stable(ApicaChunkGenerator::new))
    );

    private static final int TERRAIN_HEIGHT = 50;
    private static final int GRID_SPACING = 32;
    private static final int MAX_STRUCTURE_SIZE = 7;

    private static final BlockState BEDROCK = Blocks.BEDROCK.defaultBlockState();
    private static final BlockState STONE = Blocks.STONE.defaultBlockState();
    private static final BlockState DIRT = Blocks.DIRT.defaultBlockState();
    private static final BlockState GRASS = Blocks.GRASS_BLOCK.defaultBlockState();

    // 4 couleurs de concrete pour les plateformes
    private static final BlockState[] CONCRETE_COLORS = {
            Blocks.WHITE_CONCRETE.defaultBlockState(),
            Blocks.ORANGE_CONCRETE.defaultBlockState(),
            Blocks.MAGENTA_CONCRETE.defaultBlockState(),
            Blocks.LIGHT_BLUE_CONCRETE.defaultBlockState()
    };

    // 3 types de plateformes : [largeur X, largeur Z]
    private static final int[][] PLATFORM_SIZES = {
            {5, 5},
            {6, 6},
            {4, 7}
    };

    public ApicaChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    // =========================================================================
    // TERRAIN + STRUCTURE GENERATION
    // =========================================================================

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(
            Blender blender, RandomState randomState,
            StructureManager structureManager, ChunkAccess chunk) {

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);

        int startX = chunk.getPos().getMinBlockX();
        int startZ = chunk.getPos().getMinBlockZ();

        // Remplissage du terrain couche par couche
        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                int worldX = startX + localX;
                int worldZ = startZ + localZ;

                for (int y = 0; y <= TERRAIN_HEIGHT; y++) {
                    BlockState state = getTerrainBlock(worldX, y, worldZ);
                    pos.set(localX, y, localZ);
                    chunk.setBlockState(pos, state, false);
                    oceanFloor.update(localX, y, localZ, state);
                    worldSurface.update(localX, y, localZ, state);
                }
            }
        }

        // Placement des plateformes de concrete
        placeStructures(chunk, pos, oceanFloor, worldSurface, startX, startZ);

        return CompletableFuture.completedFuture(chunk);
    }

    /**
     * Détermine le bloc à placer pour une position de terrain donnée.
     * Utilise un hash déterministe pour les couches probabilistes.
     */
    private BlockState getTerrainBlock(int x, int y, int z) {
        if (y == TERRAIN_HEIGHT) return GRASS;
        if (y >= 48) return DIRT;
        if (y == 47) return positionHash(x, y, z) < 0.5f ? DIRT : STONE;
        if (y == 46) return positionHash(x, y, z) < 0.25f ? DIRT : STONE;
        if (y >= 3) return STONE;
        if (y == 2) return positionHash(x, y, z) < 0.25f ? BEDROCK : STONE;
        if (y == 1) return positionHash(x, y, z) < 0.5f ? BEDROCK : STONE;
        return BEDROCK; // y == 0
    }

    /**
     * Place les plateformes de concrete sur une grille régulière.
     * Gère le cross-chunk : itère les cellules de grille proches et ne place
     * que les blocs qui tombent dans le chunk courant.
     */
    private void placeStructures(ChunkAccess chunk, BlockPos.MutableBlockPos pos,
                                  Heightmap oceanFloor, Heightmap worldSurface,
                                  int startX, int startZ) {
        int structureY = TERRAIN_HEIGHT + 1;

        // Chercher toutes les cellules de grille qui pourraient toucher ce chunk
        int searchRadius = MAX_STRUCTURE_SIZE + GRID_SPACING;
        int gridMinX = Math.floorDiv(startX - searchRadius, GRID_SPACING);
        int gridMaxX = Math.floorDiv(startX + 15 + searchRadius, GRID_SPACING);
        int gridMinZ = Math.floorDiv(startZ - searchRadius, GRID_SPACING);
        int gridMaxZ = Math.floorDiv(startZ + 15 + searchRadius, GRID_SPACING);

        for (int gridX = gridMinX; gridX <= gridMaxX; gridX++) {
            for (int gridZ = gridMinZ; gridZ <= gridMaxZ; gridZ++) {
                // Centre de la plateforme (décalé dans la cellule pour éviter les bords)
                long cellHash = gridHash(gridX, gridZ);
                int centerX = gridX * GRID_SPACING + 8 + (int) ((cellHash & 0xFL) % 12);
                int centerZ = gridZ * GRID_SPACING + 8 + (int) (((cellHash >>> 4) & 0xFL) % 12);

                // Type et couleur basés sur le hash
                int typeIndex = (int) (((cellHash >>> 8) & 0x7FFFFFFFL) % 3);
                int colorIndex = (int) (((cellHash >>> 16) & 0x7FFFFFFFL) % 4);
                int sizeX = PLATFORM_SIZES[typeIndex][0];
                int sizeZ = PLATFORM_SIZES[typeIndex][1];
                BlockState concrete = CONCRETE_COLORS[colorIndex];

                // Bornes de la plateforme
                int platMinX = centerX - sizeX / 2;
                int platMaxX = platMinX + sizeX - 1;
                int platMinZ = centerZ - sizeZ / 2;
                int platMaxZ = platMinZ + sizeZ - 1;

                // Intersection avec le chunk courant
                int fromX = Math.max(platMinX, startX);
                int toX = Math.min(platMaxX, startX + 15);
                int fromZ = Math.max(platMinZ, startZ);
                int toZ = Math.min(platMaxZ, startZ + 15);

                if (fromX > toX || fromZ > toZ) continue;

                for (int wx = fromX; wx <= toX; wx++) {
                    for (int wz = fromZ; wz <= toZ; wz++) {
                        int lx = wx - startX;
                        int lz = wz - startZ;
                        pos.set(lx, structureY, lz);
                        chunk.setBlockState(pos, concrete, false);
                        oceanFloor.update(lx, structureY, lz, concrete);
                        worldSurface.update(lx, structureY, lz, concrete);
                    }
                }
            }
        }
    }

    // =========================================================================
    // HASH FUNCTIONS
    // =========================================================================

    /**
     * Hash déterministe par position de bloc — retourne un float [0, 1).
     * Utilisé pour les couches probabilistes du terrain.
     */
    private static float positionHash(int x, int y, int z) {
        long hash = (long) x * 3129871L ^ (long) z * 116129781L ^ (long) y * 982451653L;
        hash = hash * hash * 42317861L + hash * 11L;
        return (float) ((hash >>> 33) & 0x7FFFFFFFL) / (float) 0x7FFFFFFFL;
    }

    /**
     * Hash déterministe par cellule de grille — retourne un long.
     * Utilisé pour déterminer le type, la couleur et le décalage des plateformes.
     */
    private static long gridHash(int gridX, int gridZ) {
        long hash = (long) gridX * 341873128712L + (long) gridZ * 132897987541L;
        hash ^= hash >>> 16;
        hash *= 0x85ebca6bL;
        hash ^= hash >>> 13;
        return hash;
    }

    // =========================================================================
    // NO-OP OVERRIDES
    // =========================================================================

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structureManager,
                             RandomState randomState, ChunkAccess chunk) {
        // Terrain plat — pas de surface building nécessaire
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState randomState,
                             BiomeManager biomeManager, StructureManager structureManager,
                             ChunkAccess chunk, GenerationStep.Carving step) {
        // Pas de grottes dans la dimension Apica
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
        // Pas de mobs natifs pour l'instant
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState randomState, BlockPos pos) {
        info.add("Apica Dimension");
    }

    // =========================================================================
    // HEIGHT & COLUMN QUERIES
    // =========================================================================

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type,
                             LevelHeightAccessor accessor, RandomState randomState) {
        return TERRAIN_HEIGHT + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor accessor,
                                     RandomState randomState) {
        BlockState[] column = new BlockState[accessor.getHeight()];
        for (int i = 0; i < column.length; i++) {
            int y = accessor.getMinBuildHeight() + i;
            if (y >= 0 && y <= TERRAIN_HEIGHT) {
                column[i] = getTerrainBlock(x, y, z);
            } else {
                column[i] = Blocks.AIR.defaultBlockState();
            }
        }
        return new NoiseColumn(accessor.getMinBuildHeight(), column);
    }

    @Override
    public int getGenDepth() {
        return 384;
    }

    @Override
    public int getSeaLevel() {
        return -1;
    }

    @Override
    public int getMinY() {
        return 0;
    }
}
