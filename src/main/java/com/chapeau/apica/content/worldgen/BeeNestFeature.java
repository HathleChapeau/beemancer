/**
 * ============================================================
 * [BeeNestFeature.java]
 * Description: Feature custom pour placer des nids d'abeilles dans le monde
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance               | Raison                  | Utilisation                    |
 * |--------------------------|------------------------|--------------------------------|
 * | BeeNestFeatureConfig     | Configuration          | Species et type placement      |
 * | BeeNestBlock             | Bloc nid               | Blockstate avec species        |
 * | ApicaBlocks          | Registre blocs         | Acces au bloc BEE_NEST         |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ApicaFeatures.java (registration)
 * - Configured Feature JSON (reference)
 *
 * ============================================================
 */
package com.chapeau.apica.content.worldgen;

import com.chapeau.apica.common.block.hive.BeeNestBlock;
import com.chapeau.apica.core.registry.ApicaBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * Place un bee nest selon le type de placement configure.
 * 5 algorithmes: SURFACE, TREE, UNDERGROUND, NETHER_SURFACE, END_SURFACE.
 */
public class BeeNestFeature extends Feature<BeeNestFeatureConfig> {

    public BeeNestFeature(Codec<BeeNestFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<BeeNestFeatureConfig> context) {
        BeeNestFeatureConfig config = context.config();
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        BlockState nestState = ApicaBlocks.BEE_NEST.get().defaultBlockState()
                .setValue(BeeNestBlock.SPECIES, BeeNestBlock.NestSpecies.fromSpeciesId(config.speciesId()));

        return switch (config.placementType()) {
            case SURFACE -> placeSurface(level, origin, nestState);
            case TREE -> placeTree(level, origin, nestState, context.random());
            case UNDERGROUND -> placeUnderground(level, origin, nestState);
            case NETHER_SURFACE -> placeNetherSurface(level, origin, nestState);
            case END_SURFACE -> placeEndSurface(level, origin, nestState);
        };
    }

    private boolean placeSurface(WorldGenLevel level, BlockPos origin, BlockState nestState) {
        BlockState below = level.getBlockState(origin.below());
        if (!below.isFaceSturdy(level, origin.below(), Direction.UP)) return false;
        if (!level.getBlockState(origin).isAir()) return false;
        if (!level.getBlockState(origin.above()).isAir()) return false;
        level.setBlock(origin, nestState, 2);
        return true;
    }

    private boolean placeTree(WorldGenLevel level, BlockPos origin, BlockState nestState,
                              net.minecraft.util.RandomSource random) {
        for (int dy = 1; dy <= 6; dy++) {
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    BlockPos logPos = origin.offset(dx, dy, dz);
                    if (!level.getBlockState(logPos).is(BlockTags.LOGS)) continue;

                    Direction[] dirs = Direction.Plane.HORIZONTAL.stream().toArray(Direction[]::new);
                    Direction start = dirs[random.nextInt(dirs.length)];
                    for (int i = 0; i < 4; i++) {
                        Direction dir = Direction.from2DDataValue((start.get2DDataValue() + i) % 4);
                        BlockPos nestPos = logPos.relative(dir);
                        if (level.getBlockState(nestPos).isAir()) {
                            level.setBlock(nestPos, nestState, 2);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean placeUnderground(WorldGenLevel level, BlockPos origin, BlockState nestState) {
        int stoneCount = 0;
        for (Direction dir : Direction.values()) {
            BlockState neighbor = level.getBlockState(origin.relative(dir));
            if (neighbor.is(BlockTags.BASE_STONE_OVERWORLD)) {
                stoneCount++;
            }
        }
        if (stoneCount < 5) return false;
        if (!level.getBlockState(origin).is(BlockTags.BASE_STONE_OVERWORLD)) return false;
        level.setBlock(origin, nestState, 2);
        return true;
    }

    private boolean placeNetherSurface(WorldGenLevel level, BlockPos origin, BlockState nestState) {
        BlockState below = level.getBlockState(origin.below());
        boolean validGround = below.is(BlockTags.BASE_STONE_NETHER)
                || below.is(Blocks.SOUL_SAND) || below.is(Blocks.SOUL_SOIL)
                || below.is(BlockTags.NYLIUM);
        if (!validGround) return false;
        if (!level.getBlockState(origin).isAir()) return false;
        level.setBlock(origin, nestState, 2);
        return true;
    }

    private boolean placeEndSurface(WorldGenLevel level, BlockPos origin, BlockState nestState) {
        BlockState below = level.getBlockState(origin.below());
        if (!below.is(Blocks.END_STONE)) return false;
        if (!level.getBlockState(origin).isAir()) return false;
        level.setBlock(origin, nestState, 2);
        return true;
    }
}
