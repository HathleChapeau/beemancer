/**
 * ============================================================
 * [HiveMultiblockBlock.java]
 * Description: Bloc ruche pour multibloc 3x3x3
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation           |
 * |-------------------------|----------------------|-----------------------|
 * | HiveMultiblockBlockEntity| Contrôleur           | Logique ruche         |
 * | MultiblockValidator     | Validation           | Vérification pattern  |
 * | MultiblockPatterns      | Pattern              | Définition structure  |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - BeemancerBlocks.java (registration)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.hive;

import com.chapeau.beemancer.core.multiblock.MultiblockPatterns;
import com.chapeau.beemancer.core.multiblock.MultiblockValidator;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public class HiveMultiblockBlock extends BaseEntityBlock {
    public static final MapCodec<HiveMultiblockBlock> CODEC = simpleCodec(HiveMultiblockBlock::new);

    public HiveMultiblockBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HiveMultiblockBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, BeemancerBlockEntities.HIVE_MULTIBLOCK.get(),
            HiveMultiblockBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof HiveMultiblockBlockEntity hive) {
                // Try to find the master controller
                HiveMultiblockBlockEntity master = hive.findOrBecomeController();
                if (master != null && master.isFormed()) {
                    serverPlayer.openMenu(master, buf -> buf.writeBlockPos(master.getBlockPos()));
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof HiveMultiblockBlockEntity hive) {
                hive.onBroken();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
