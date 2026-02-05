/**
 * ============================================================
 * [MagicBreedingCrystalBlock.java]
 * Description: Cristal magique activant le mode breeding des ruches
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | MagicHiveBlockEntity| Détection ruche      | Active breeding quand au-dessus|
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - MagicHiveBlockEntity (détection mode breeding)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.crystal;

import com.chapeau.beemancer.core.registry.BeemancerParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MagicBreedingCrystalBlock extends Block {
    // Crystal shape (centered, smaller than full block)
    private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 14, 12);

    public MagicBreedingCrystalBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) != 0) return;

        double x = pos.getX() + 0.25 + random.nextDouble() * 0.5;
        double y = pos.getY() + 0.1;
        double z = pos.getZ() + 0.25 + random.nextDouble() * 0.5;
        double xSpeed = (random.nextDouble() - 0.5) * 0.02;
        double ySpeed = 0.02 + random.nextDouble() * 0.03;
        double zSpeed = (random.nextDouble() - 0.5) * 0.02;

        level.addParticle(BeemancerParticles.RUNE.get(), x, y, z, xSpeed, ySpeed, zSpeed);
    }
}
