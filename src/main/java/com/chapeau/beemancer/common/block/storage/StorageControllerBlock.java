/**
 * ============================================================
 * [StorageControllerBlock.java]
 * Description: Bloc unité centrale du réseau de stockage
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance                      | Raison                  | Utilisation           |
 * |---------------------------------|------------------------|-----------------------|
 * | StorageControllerBlockEntity    | BlockEntity associé    | Logique stockage      |
 * | BeemancerBlockEntities          | Type du BlockEntity    | Création et ticker    |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - BeemancerBlocks.java (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.storage;

import com.chapeau.beemancer.common.blockentity.storage.StorageControllerBlockEntity;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Bloc unité centrale du réseau de stockage.
 *
 * Interactions:
 * - Shift+clic droit: Toggle mode édition
 * - En mode édition, clic droit sur coffre: Enregistrer/retirer
 *
 * Le bloc ne possède pas de GUI propre - seul le terminal permet
 * d'accéder aux items du réseau.
 */
public class StorageControllerBlock extends BaseEntityBlock {
    public static final MapCodec<StorageControllerBlock> CODEC = simpleCodec(StorageControllerBlock::new);

    public StorageControllerBlock(Properties properties) {
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
        return new StorageControllerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                   BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(blockEntityType, BeemancerBlockEntities.STORAGE_CONTROLLER.get(),
                (lvl, pos, st, be) -> StorageControllerBlockEntity.serverTick(be));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof StorageControllerBlockEntity controller)) {
            return InteractionResult.PASS;
        }

        // Shift+clic droit: toggle mode édition
        if (player.isShiftKeyDown()) {
            boolean nowEditing = controller.toggleEditMode(player.getUUID());
            if (nowEditing) {
                StorageEditModeHandler.startEditing(player.getUUID(), pos);
                player.displayClientMessage(
                    Component.translatable("message.beemancer.storage_controller.edit_mode_on"),
                    true
                );
            } else {
                StorageEditModeHandler.stopEditing(player.getUUID());
                player.displayClientMessage(
                    Component.translatable("message.beemancer.storage_controller.edit_mode_off"),
                    true
                );
            }
            return InteractionResult.SUCCESS;
        }

        // Clic normal: afficher statut
        int chestCount = controller.getRegisteredChests().size();
        int terminalCount = controller.getLinkedTerminals().size();
        player.displayClientMessage(
            Component.translatable("message.beemancer.storage_controller.status",
                chestCount, terminalCount),
            true
        );

        return InteractionResult.SUCCESS;
    }

    /**
     * Appelé par d'autres blocs quand le joueur clique dessus en mode édition.
     */
    public static boolean handleChestClick(Level level, BlockPos controllerPos,
                                            BlockPos clickedPos, Player player) {
        if (level.isClientSide()) return false;

        BlockEntity be = level.getBlockEntity(controllerPos);
        if (!(be instanceof StorageControllerBlockEntity controller)) {
            return false;
        }

        if (!controller.canEdit(player.getUUID())) {
            return false;
        }

        // Vérifier si c'est un coffre
        BlockState clickedState = level.getBlockState(clickedPos);
        if (!(clickedState.getBlock() instanceof ChestBlock) &&
            !clickedState.is(Blocks.CHEST) &&
            !clickedState.is(Blocks.TRAPPED_CHEST) &&
            !clickedState.is(Blocks.BARREL)) {
            return false;
        }

        // Toggle ce coffre
        boolean wasRegistered = controller.getRegisteredChests().contains(clickedPos);
        controller.toggleChest(clickedPos);

        if (wasRegistered) {
            player.displayClientMessage(
                Component.translatable("message.beemancer.storage_controller.chest_removed"),
                true
            );
        } else {
            int count = controller.getRegisteredChests().size();
            player.displayClientMessage(
                Component.translatable("message.beemancer.storage_controller.chests_registered", count),
                true
            );
        }

        return true;
    }
}
