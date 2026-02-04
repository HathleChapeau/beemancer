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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Mur en honeyed stone.
 * Participe aux multiblocs Infuser et Centrifuge.
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
}
