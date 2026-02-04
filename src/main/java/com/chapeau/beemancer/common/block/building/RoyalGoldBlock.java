/**
 * ============================================================
 * [RoyalGoldBlock.java]
 * Description: Bloc d'or royal avec propriete multibloc
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | Block               | Base Minecraft       | Bloc simple                    |
 * | MultiblockProperty  | Etat multibloc       | Blockstate multiblock          |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - BeemancerBlocks.java (enregistrement)
 * - MultiblockPatterns.java (pattern alembic)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.building;

import com.chapeau.beemancer.core.multiblock.MultiblockProperty;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Bloc d'or royal.
 * Participe au multibloc Alembic.
 */
public class RoyalGoldBlock extends Block {

    public static final EnumProperty<MultiblockProperty> MULTIBLOCK = MultiblockProperty.create("alembic");

    public RoyalGoldBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(MULTIBLOCK, MultiblockProperty.NONE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MULTIBLOCK);
    }
}
