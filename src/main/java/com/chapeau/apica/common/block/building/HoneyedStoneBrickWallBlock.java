/**
 * ============================================================
 * [HoneyedStoneBrickWallBlock.java]
 * Description: Mur en honeyed stone brick avec propriete multibloc
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | WallBlock           | Base mur             | Comportement standard          |
 * | MultiblockProperty  | Etat multibloc       | Blockstate multiblock          |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ApicaBlocks.java (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.apica.common.block.building;

import com.chapeau.apica.core.multiblock.MultiblockProperty;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Mur en honeyed stone brick.
 * Participe aux multiblocs Infuser et Centrifuge.
 *
 * WallBlock stocke ses VoxelShapes dans un Map construit
 * uniquement pour les etats avec MULTIBLOCK=NONE. On normalise la propriete
 * MULTIBLOCK avant le lookup pour eviter un NPE.
 */
public class HoneyedStoneBrickWallBlock extends WallBlock {

    public static final EnumProperty<MultiblockProperty> MULTIBLOCK =
        MultiblockProperty.create("infuser", "centrifuge");

    public HoneyedStoneBrickWallBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(MULTIBLOCK, MultiblockProperty.NONE)
            .setValue(UP, true)
            .setValue(NORTH_WALL, net.minecraft.world.level.block.state.properties.WallSide.NONE)
            .setValue(EAST_WALL, net.minecraft.world.level.block.state.properties.WallSide.NONE)
            .setValue(SOUTH_WALL, net.minecraft.world.level.block.state.properties.WallSide.NONE)
            .setValue(WEST_WALL, net.minecraft.world.level.block.state.properties.WallSide.NONE)
            .setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(MULTIBLOCK);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state != null) {
            state = state.setValue(MULTIBLOCK, MultiblockProperty.NONE);
        }
        return state;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                   LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        MultiblockProperty mb = state.getValue(MULTIBLOCK);
        BlockState updated = super.updateShape(state, direction, neighborState, level, pos, neighborPos);
        if (updated.getBlock() == this) {
            updated = updated.setValue(MULTIBLOCK, mb);
        }
        return updated;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return super.getShape(state.setValue(MULTIBLOCK, MultiblockProperty.NONE), level, pos, context);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return super.getCollisionShape(state.setValue(MULTIBLOCK, MultiblockProperty.NONE), level, pos, context);
    }
}
