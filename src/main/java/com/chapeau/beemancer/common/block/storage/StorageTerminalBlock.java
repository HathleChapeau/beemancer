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
 * | StorageControllerBlockEntity    | Controller lié         | Cleanup on remove     |
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
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import com.chapeau.beemancer.core.multiblock.MultiblockProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Bloc terminal pour accéder au réseau de stockage.
 * Peut être placé librement sur n'importe quelle surface horizontale.
 * Modèle plat 12×12×3 orienté selon la face de placement.
 *
 * formed_rotation:
 * - 0: plate face nord (z=0-3)
 * - 1: plate face est (après y=90)
 * - 2: plate face sud (après y=180)
 * - 3: plate face ouest (après y=270)
 */
public class StorageTerminalBlock extends BaseEntityBlock {
    public static final MapCodec<StorageTerminalBlock> CODEC = simpleCodec(StorageTerminalBlock::new);

    public static final EnumProperty<MultiblockProperty> MULTIBLOCK = MultiblockProperty.create("storage");
    public static final IntegerProperty FORMED_ROTATION = IntegerProperty.create("formed_rotation", 0, 3);

    // VoxelShapes pour chaque rotation (plaque 12×12×3)
    private static final VoxelShape SHAPE_ROT0 = Block.box(2, 2, 13, 14, 14, 16);
    private static final VoxelShape SHAPE_ROT1 = Block.box(0, 2, 2, 3, 14, 14);
    private static final VoxelShape SHAPE_ROT2 = Block.box(2, 2, 0, 14, 14, 3);
    private static final VoxelShape SHAPE_ROT3 = Block.box(13, 2, 2, 16, 14, 14);

    public StorageTerminalBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(MULTIBLOCK, MultiblockProperty.NONE)
            .setValue(FORMED_ROTATION, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MULTIBLOCK, FORMED_ROTATION);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FORMED_ROTATION)) {
            case 1 -> SHAPE_ROT1;
            case 2 -> SHAPE_ROT2;
            case 3 -> SHAPE_ROT3;
            default -> SHAPE_ROT0;
        };
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
        return null;
    }

    /**
     * Détermine la rotation lors du placement.
     * Le terminal peut être placé sur n'importe quelle face horizontale.
     * La rotation est déterminée par la face cliquée.
     */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();

        // Seulement les faces horizontales (N/S/E/W)
        if (clickedFace.getAxis().isVertical()) {
            return null;
        }

        int rotation = clickedFaceToRotation(clickedFace);
        return this.defaultBlockState().setValue(FORMED_ROTATION, rotation);
    }

    /**
     * Convertit la face cliquée en rotation du terminal.
     */
    private static int clickedFaceToRotation(Direction clickedFace) {
        return switch (clickedFace) {
            case NORTH -> 0;
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
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
                BlockPos ctrlPos = terminal.getControllerPos();
                if (ctrlPos != null && level.isLoaded(ctrlPos)) {
                    BlockEntity ctrlBe = level.getBlockEntity(ctrlPos);
                    if (ctrlBe instanceof StorageControllerBlockEntity controller) {
                        controller.getNetworkRegistry().unregisterBlock(pos);
                        controller.setChanged();
                        controller.syncNodeToClient();
                    }
                }
                terminal.unlinkController();

                for (int i = 0; i < terminal.getContainerSize(); i++) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(),
                        terminal.getItem(i));
                }
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
