/**
 * ============================================================
 * [ExtractorHeartBlock.java]
 * Description: Cœur de l'Extracteur d'Essence - Contrôleur multibloc
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance                  | Raison                | Utilisation           |
 * |-----------------------------|----------------------|-----------------------|
 * | ExtractorHeartBlockEntity   | Stockage état        | Multibloc formé       |
 * | BeemancerBlockEntities      | Type BlockEntity     | Création              |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - BeemancerBlocks.java (enregistrement)
 * - MultiblockPatterns.java (centre du pattern)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.extractor;

import com.chapeau.beemancer.common.blockentity.extractor.ExtractorHeartBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * Cœur de l'Extracteur d'Essence.
 * Sert de contrôleur pour le multibloc Essence Extractor.
 * Clic droit pour tenter de former l'extracteur.
 */
public class ExtractorHeartBlock extends Block implements EntityBlock {

    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    private static final VoxelShape SHAPE = Block.box(2, 2, 2, 14, 14, 14);

    public ExtractorHeartBlock(Properties properties) {
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

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ExtractorHeartBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof ExtractorHeartBlockEntity extractor) {
                ExtractorHeartBlockEntity.serverTick(lvl, pos, st, extractor);
            }
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ExtractorHeartBlockEntity extractorBE)) {
            return InteractionResult.PASS;
        }

        if (state.getValue(FORMED)) {
            player.displayClientMessage(
                Component.translatable("message.beemancer.essence_extractor.already_formed"),
                true
            );
        } else {
            boolean success = extractorBE.tryFormExtractor();
            if (success) {
                player.displayClientMessage(
                    Component.translatable("message.beemancer.essence_extractor.formed"),
                    true
                );
            } else {
                player.displayClientMessage(
                    Component.translatable("message.beemancer.essence_extractor.invalid_structure"),
                    true
                );
            }
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (state.getValue(FORMED)) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof ExtractorHeartBlockEntity extractorBE) {
                    extractorBE.onMultiblockBroken();
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
