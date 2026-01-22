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

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
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
}
