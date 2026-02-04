/**
 * ============================================================
 * [HoneyedStoneWallBlock.java]
 * Description: Mur en honeyed stone avec propriete multibloc
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
 * - BeemancerBlocks.java (enregistrement)
 * - MultiblockPatterns.java (pattern infuser/centrifuge)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.building;

import com.chapeau.beemancer.core.multiblock.MultiblockProperty;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Mur en honeyed stone.
 * Participe aux multiblocs Infuser et Centrifuge.
 *
 * WallBlock stocke ses VoxelShapes dans un Map<BlockState, VoxelShape> construit
 * uniquement pour les etats avec MULTIBLOCK=NONE. On normalise la propriete
 * MULTIBLOCK avant le lookup pour eviter un NPE.
 */
public class HoneyedStoneWallBlock extends WallBlock {

    public static final EnumProperty<MultiblockProperty> MULTIBLOCK =
        MultiblockProperty.create("infuser", "centrifuge");

    public HoneyedStoneWallBlock(Properties properties) {
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
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return super.getShape(state.setValue(MULTIBLOCK, MultiblockProperty.NONE), level, pos, context);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return super.getCollisionShape(state.setValue(MULTIBLOCK, MultiblockProperty.NONE), level, pos, context);
    }
}
