/**
 * ============================================================
 * [HoneyedStoneStairBlock.java]
 * Description: Escalier en pierre miellée pour le Honey Altar
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation           |
 * |---------------------|----------------------|-----------------------|
 * | StairBlock          | Base escalier        | Comportement standard |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - HoneyAltarMultiblock.java (validation pattern avec orientation)
 * - BeemancerBlocks.java (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.altar;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import com.chapeau.beemancer.core.multiblock.MultiblockProperty;

/**
 * Escalier en pierre miellée.
 * L'orientation est importante pour la validation du Honey Altar.
 * Devient invisible quand le multibloc est formé.
 */
public class HoneyedStoneStairBlock extends StairBlock {

    public static final EnumProperty<MultiblockProperty> MULTIBLOCK = MultiblockProperty.create("altar");

    public HoneyedStoneStairBlock(BlockState baseState, Properties properties) {
        super(baseState, properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(FACING, net.minecraft.core.Direction.NORTH)
            .setValue(HALF, net.minecraft.world.level.block.state.properties.Half.BOTTOM)
            .setValue(SHAPE, net.minecraft.world.level.block.state.properties.StairsShape.STRAIGHT)
            .setValue(WATERLOGGED, false)
            .setValue(MULTIBLOCK, MultiblockProperty.NONE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(MULTIBLOCK);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
