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
 * | AlembicHeartBlockEntity   | Stockage etat        | Multibloc + processing|
 * | MultiblockProperty        | Etat multibloc       | Blockstate            |
 * | BeemancerBlockEntities    | Type registration    | Ticker                |
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
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

/**
 * Coeur de l'Alembic multibloc.
 * Clic droit: forme le multibloc ou ouvre le menu si deja forme.
 */
public class AlembicHeartBlock extends Block implements EntityBlock {

    public static final EnumProperty<MultiblockProperty> MULTIBLOCK = MultiblockProperty.create("alembic");
    public static final BooleanProperty DISTILLING = BooleanProperty.create("distilling");

    public AlembicHeartBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(MULTIBLOCK, MultiblockProperty.NONE)
            .setValue(DISTILLING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MULTIBLOCK, DISTILLING);
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

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return type == BeemancerBlockEntities.ALEMBIC_HEART.get()
            ? (lvl, pos, st, be) -> AlembicHeartBlockEntity.serverTick(lvl, pos, st, (AlembicHeartBlockEntity) be)
            : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof AlembicHeartBlockEntity heartBE)) {
            return InteractionResult.PASS;
        }

        if (state.getValue(MULTIBLOCK) != MultiblockProperty.NONE) {
            player.openMenu(heartBE, pos);
            return InteractionResult.CONSUME;
        } else {
            boolean success = heartBE.tryFormMultiblock();
            if (success) {
                player.displayClientMessage(
                    Component.translatable("message.beemancer.alembic.formed"), true);
            } else {
                player.displayClientMessage(
                    Component.translatable("message.beemancer.alembic.invalid_structure"), true);
            }
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (state.getValue(MULTIBLOCK) != MultiblockProperty.NONE) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof AlembicHeartBlockEntity heartBE) {
                    heartBE.onMultiblockBroken();
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
