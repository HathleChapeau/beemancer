/**
 * ============================================================
 * [HoneyCrystalBlock.java]
 * Description: Cristal de miel - Bloc décoratif
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation           |
 * |---------------------|----------------------|-----------------------|
 * | (aucune)            |                      |                       |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ApicaBlocks.java (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.apica.common.block.altar;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Cristal de miel placeable.
 * Bloc purement décoratif avec shape custom.
 */
public class HoneyCrystalBlock extends Block {

    private final VoxelShape shape;

    public HoneyCrystalBlock(Properties properties, VoxelShape shape) {
        super(properties);
        this.shape = shape;
    }

    public HoneyCrystalBlock(Properties properties) {
        this(properties, Block.box(4, 2, 4, 12, 10, 12));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape;
    }
}
