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
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Piédestal au centre de la base du Honey Altar.
 * Forme de colonne/piédestal avec hitbox personnalisée.
 * Devient invisible quand le multibloc est formé.
 */
public class HoneyPedestalBlock extends Block {

    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

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
        this.registerDefaultState(this.stateDefinition.any().setValue(FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
