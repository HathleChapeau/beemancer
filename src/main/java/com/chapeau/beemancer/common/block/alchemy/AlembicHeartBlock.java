/**
 * ============================================================
 * [AlembicHeartBlock.java]
 * Description: Coeur de l'Alembic - Controleur du multibloc Alembic
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                | Raison                | Utilisation           |
 * |---------------------------|----------------------|-----------------------|
 * | AlembicHeartBlockEntity   | Stockage etat        | Multibloc forme       |
 * | MultiblockProperty        | Etat multibloc       | Blockstate            |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - BeemancerBlocks.java (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.alchemy;

import com.chapeau.beemancer.common.blockentity.alchemy.AlembicHeartBlockEntity;
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
 * Coeur de l'Alembic multibloc.
 * Pattern a definir ulterieurement.
 */
public class AlembicHeartBlock extends Block implements EntityBlock {

    public static final EnumProperty<MultiblockProperty> MULTIBLOCK = MultiblockProperty.create("alembic");

    public AlembicHeartBlock(Properties properties) {
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
        return new AlembicHeartBlockEntity(pos, state);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (!state.getValue(MULTIBLOCK).equals(MultiblockProperty.NONE)) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof AlembicHeartBlockEntity heartBE) {
                    heartBE.onMultiblockBroken();
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
