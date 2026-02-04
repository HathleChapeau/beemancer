/**
 * ============================================================
 * [CentrifugeHeartBlock.java]
 * Description: Coeur de la Centrifugeuse - Controleur du multibloc Centrifuge
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                  | Raison                | Utilisation           |
 * |-----------------------------|----------------------|-----------------------|
 * | CentrifugeHeartBlockEntity  | Stockage etat        | Multibloc forme       |
 * | MultiblockProperty          | Etat multibloc       | Blockstate            |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - BeemancerBlocks.java (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.alchemy;

import com.chapeau.beemancer.common.blockentity.alchemy.CentrifugeHeartBlockEntity;
import com.chapeau.beemancer.core.multiblock.MultiblockProperty;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import javax.annotation.Nullable;

/**
 * Coeur de la Centrifugeuse multibloc.
 * Pattern a definir ulterieurement.
 */
public class CentrifugeHeartBlock extends Block implements EntityBlock {

    public static final EnumProperty<MultiblockProperty> MULTIBLOCK = MultiblockProperty.create("centrifuge");

    public CentrifugeHeartBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(MULTIBLOCK, MultiblockProperty.NONE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MULTIBLOCK);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CentrifugeHeartBlockEntity(pos, state);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (!state.getValue(MULTIBLOCK).equals(MultiblockProperty.NONE)) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof CentrifugeHeartBlockEntity heartBE) {
                    heartBE.onMultiblockBroken();
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
