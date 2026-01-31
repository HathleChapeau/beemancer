/**
 * ============================================================
 * [StorageRelayBlock.java]
 * Description: Bloc relais du reseau de stockage
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                  | Utilisation           |
 * |-------------------------------|------------------------|-----------------------|
 * | StorageRelayBlockEntity      | BlockEntity associe    | Logique relais        |
 * | BeemancerBlockEntities       | Type du BlockEntity    | Creation et ticker    |
 * | StorageEditModeHandler       | Mode edition           | Toggle mode           |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - BeemancerBlocks.java (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.storage;

import com.chapeau.beemancer.common.blockentity.storage.StorageRelayBlockEntity;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Bloc relais du reseau de stockage.
 *
 * Interactions:
 * - Shift+clic droit: Toggle mode edition (coffres et connexions)
 */
public class StorageRelayBlock extends BaseEntityBlock {
    public static final MapCodec<StorageRelayBlock> CODEC = simpleCodec(StorageRelayBlock::new);

    public StorageRelayBlock(Properties properties) {
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
        return new StorageRelayBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                   BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(blockEntityType, BeemancerBlockEntities.STORAGE_RELAY.get(),
                (lvl, pos, st, be) -> StorageRelayBlockEntity.serverTick(be));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof StorageRelayBlockEntity relay)) {
            return InteractionResult.PASS;
        }

        // Shift+clic droit: toggle mode edition
        if (player.isShiftKeyDown()) {
            boolean nowEditing = relay.toggleEditMode(player.getUUID());
            if (nowEditing) {
                StorageEditModeHandler.startEditing(player.getUUID(), pos);
                player.displayClientMessage(
                    Component.translatable("message.beemancer.storage_relay.edit_mode_on"),
                    true
                );
            } else {
                StorageEditModeHandler.stopEditing(player.getUUID());
                player.displayClientMessage(
                    Component.translatable("message.beemancer.storage_relay.edit_mode_off"),
                    true
                );
            }
            return InteractionResult.SUCCESS;
        }

        // Clic normal: afficher info
        int chests = relay.getRegisteredChests().size();
        int nodes = relay.getConnectedNodes().size();
        player.displayClientMessage(
            Component.translatable("message.beemancer.storage_relay.info", chests, nodes),
            true
        );

        return InteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof StorageRelayBlockEntity relay) {
                relay.exitEditMode();
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
