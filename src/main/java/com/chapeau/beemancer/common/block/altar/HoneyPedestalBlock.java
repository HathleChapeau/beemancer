/**
 * ============================================================
 * [HoneyPedestalBlock.java]
 * Description: Piédestal central du Honey Altar
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation           |
 * |---------------------|----------------------|-----------------------|
 * | Block               | Base Minecraft       | Bloc simple           |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - HoneyAltarMultiblock.java (validation pattern - centre base)
 * - BeemancerBlocks.java (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.altar;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Piédestal au centre de la base du Honey Altar.
 * Forme de colonne/piédestal avec hitbox personnalisée.
 */
public class HoneyPedestalBlock extends Block {

    // Forme: colonne centrale
    private static final VoxelShape SHAPE = Shapes.or(
        // Base (plus large)
        Block.box(2, 0, 2, 14, 3, 14),
        // Colonne centrale
        Block.box(4, 3, 4, 12, 13, 12),
        // Top (plus large)
        Block.box(2, 13, 2, 14, 16, 14)
    );

    public HoneyPedestalBlock(Properties properties) {
        super(properties);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
