/**
 * ============================================================
 * [StorageControllerBlock.java]
 * Description: Bloc unité centrale du réseau de stockage (multibloc)
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance                      | Raison                  | Utilisation           |
 * |---------------------------------|------------------------|-----------------------|
 * | StorageControllerBlockEntity    | BlockEntity associé    | Logique stockage      |
 * | BeemancerBlockEntities          | Type du BlockEntity    | Création et ticker    |
 * | MultiblockController            | Interface multibloc    | Formation/destruction |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - BeemancerBlocks.java (enregistrement)
 * - MultiblockPatterns.java (centre du pattern storage)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.storage;

import com.chapeau.beemancer.common.blockentity.storage.StorageControllerBlockEntity;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import com.chapeau.beemancer.core.util.StorageHelper;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Bloc unité centrale du réseau de stockage.
 * Contrôleur du multibloc Storage Controller.
 *
 * Interactions:
 * - Clic droit (non formé): Tenter la formation du multibloc
 * - Shift+clic droit: Toggle mode édition (coffres)
 * - Clic droit (formé): Afficher statut
 */
public class StorageControllerBlock extends BaseEntityBlock {
    public static final MapCodec<StorageControllerBlock> CODEC = simpleCodec(StorageControllerBlock::new);

    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    private static final VoxelShape SHAPE = Block.box(3, 3, 3, 13, 13, 13);

    public StorageControllerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
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

        // Clic normal: tenter formation si pas formé, sinon afficher statut
        if (!state.getValue(FORMED)) {
            boolean success = controller.tryFormStorage();
            if (success) {
                player.displayClientMessage(
                    Component.translatable("message.beemancer.storage_controller.formed"),
                    true
                );
            } else {
                player.displayClientMessage(
                    Component.translatable("message.beemancer.storage_controller.invalid_structure"),
                    true
                );
            }
        } else {
            int chestCount = controller.getRegisteredChests().size();
            int terminalCount = controller.getLinkedTerminals().size();
            player.displayClientMessage(
                Component.translatable("message.beemancer.storage_controller.status",
                    chestCount, terminalCount),
                true
            );
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (state.getValue(FORMED)) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof StorageControllerBlockEntity controller) {
                    controller.onMultiblockBroken();
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
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

        BlockState clickedState = level.getBlockState(clickedPos);
        if (!StorageHelper.isStorageContainer(clickedState)) {
            return false;
        }

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
