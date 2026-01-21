/**
 * ============================================================
 * [StorageCrateBlock.java]
 * Description: Bloc de stockage avec inventaire de double coffre
 * ============================================================
 * 
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance               | Raison                  | Utilisation           |
 * |--------------------------|------------------------|-----------------------|
 * | BeemancerBlockEntities   | Type du BlockEntity    | Création de l'entity  |
 * | StorageCrateBlockEntity  | BlockEntity associé    | Liaison bloc-entity   |
 * ------------------------------------------------------------
 * 
 * UTILISÉ PAR:
 * - BeemancerBlocks.java (enregistrement)
 * 
 * ============================================================
 */
package com.chapeau.beemancer.common.block.storage;

import com.chapeau.beemancer.common.blockentity.storage.StorageCrateBlockEntity;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class StorageCrateBlock extends BaseEntityBlock {
    public static final MapCodec<StorageCrateBlock> CODEC = simpleCodec(StorageCrateBlock::new);

    public StorageCrateBlock(Properties properties) {
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
        return new StorageCrateBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, 
                                                Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof StorageCrateBlockEntity storageEntity) {
                serverPlayer.openMenu(storageEntity, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof StorageCrateBlockEntity storageEntity) {
                storageEntity.dropContents();
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
