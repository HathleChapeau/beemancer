/**
 * ============================================================
 * [HoneyedSlabBlock.java]
 * Description: Slab en honeyed stone pour structures multibloc
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | MultiblockProperty  | Etat multibloc       | Blockstate multiblock          |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - BeemancerBlocks.java (enregistrement)
 * - MultiblockPatterns.java (pattern hive_multiblock)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.altar;

import com.chapeau.beemancer.core.multiblock.MultiblockProperty;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Slab en honeyed stone.
 * Participe au multibloc Hive.
 */
public class HoneyedSlabBlock extends SlabBlock {

    public static final EnumProperty<MultiblockProperty> MULTIBLOCK = MultiblockProperty.create("hive");

    public HoneyedSlabBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(TYPE, net.minecraft.world.level.block.state.properties.SlabType.BOTTOM)
            .setValue(WATERLOGGED, false)
            .setValue(MULTIBLOCK, MultiblockProperty.NONE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(MULTIBLOCK);
    }
}
