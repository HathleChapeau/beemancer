/**
 * ============================================================
 * [StorageTerminalBlock.java]
 * Description: Bloc terminal pour accéder au réseau de stockage
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance                      | Raison                  | Utilisation           |
 * |---------------------------------|------------------------|-----------------------|
 * | StorageTerminalBlockEntity      | BlockEntity associé    | Interface réseau      |
 * | StorageControllerBlockEntity    | Controller lié         | Liaison manuelle      |
 * | BeemancerBlockEntities          | Type du BlockEntity    | Création              |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - BeemancerBlocks.java (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.storage;

import com.chapeau.beemancer.common.blockentity.storage.StorageControllerBlockEntity;
import com.chapeau.beemancer.common.blockentity.storage.StorageTerminalBlockEntity;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
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
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Bloc terminal pour accéder au réseau de stockage.
 *
 * Interactions:
 * - Clic droit: Ouvre l'interface du terminal
 * - Le terminal doit être lié à un controller pour fonctionner
 *
 * Liaison:
 * - Quand placé par un joueur en mode "linking" (depuis le controller),
 *   se lie automatiquement au controller
 */
public class StorageTerminalBlock extends BaseEntityBlock {
    public static final MapCodec<StorageTerminalBlock> CODEC = simpleCodec(StorageTerminalBlock::new);

    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    public static final IntegerProperty FORMED_ROTATION = IntegerProperty.create("formed_rotation", 0, 3);

    public StorageTerminalBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(FORMED, false)
            .setValue(FORMED_ROTATION, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED, FORMED_ROTATION);
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
        return new StorageTerminalBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                   BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) return null;
        return createTickerHelper(blockEntityType, BeemancerBlockEntities.STORAGE_TERMINAL.get(),
            (lvl, pos, st, be) -> StorageTerminalBlockEntity.serverTick(be));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof StorageTerminalBlockEntity terminal)) {
            return InteractionResult.PASS;
        }

        // Shift+clic: afficher état de liaison
        if (player.isShiftKeyDown()) {
            if (terminal.isLinked()) {
                BlockPos controllerPos = terminal.getControllerPos();
                player.displayClientMessage(
                    Component.translatable("message.beemancer.storage_terminal.linked",
                        controllerPos.getX(), controllerPos.getY(), controllerPos.getZ()),
                    true
                );
            } else {
                player.displayClientMessage(
                    Component.translatable("message.beemancer.storage_terminal.not_linked"),
                    true
                );
            }
            return InteractionResult.SUCCESS;
        }

        // Clic normal: ouvrir l'interface
        if (!terminal.isLinked()) {
            player.displayClientMessage(
                Component.translatable("message.beemancer.storage_terminal.not_linked"),
                true
            );
            return InteractionResult.SUCCESS;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(terminal, pos);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof StorageTerminalBlockEntity terminal) {
                // Délier du controller
                terminal.unlinkController();

                // Droppy le contenu
                for (int i = 0; i < terminal.getContainerSize(); i++) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(),
                        terminal.getItem(i));
                }
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    /**
     * Appelé quand le bloc est placé.
     * Vérifie si le joueur est en mode linking pour lier automatiquement.
     */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable net.minecraft.world.entity.LivingEntity placer,
                            net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        if (level.isClientSide() || !(placer instanceof Player player)) {
            return;
        }

        // Vérifier si le joueur vient d'un controller en mode édition
        BlockPos controllerPos = StorageEditModeHandler.getEditingController(player.getUUID());
        if (controllerPos == null) return;

        // Vérifier que le controller existe et est en mode édition
        BlockEntity be = level.getBlockEntity(controllerPos);
        if (!(be instanceof StorageControllerBlockEntity controller)) {
            return;
        }

        if (!controller.canEdit(player.getUUID())) {
            return;
        }

        // Lier le terminal au controller
        BlockEntity terminalBe = level.getBlockEntity(pos);
        if (terminalBe instanceof StorageTerminalBlockEntity terminal) {
            terminal.linkToController(controllerPos);

            player.displayClientMessage(
                Component.translatable("message.beemancer.storage_terminal.auto_linked"),
                true
            );

            // Quitter le mode édition
            controller.exitEditMode();
        }
    }
}
