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
 * | StorageControllerBlock          | Placement validation   | canSurvive            |
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
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Bloc terminal pour accéder au réseau de stockage.
 * Se pose uniquement sur un Storage Controller (comme un item frame).
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

    public static final IntegerProperty FORMED_ROTATION = IntegerProperty.create("formed_rotation", 0, 3);

    // VoxelShapes pour chaque rotation (plaque 12×12×3 flush contre le controller)
    // rotation=0 (y=0): plate au sud (z=13-16)
    // rotation=1 (y=90): plate à l'ouest (x=0-3)
    // rotation=2 (y=180): plate au nord (z=0-3)
    // rotation=3 (y=270): plate à l'est (x=13-16)
    private static final VoxelShape SHAPE_ROT0 = Block.box(2, 2, 13, 14, 14, 16);
    private static final VoxelShape SHAPE_ROT1 = Block.box(0, 2, 2, 3, 14, 14);
    private static final VoxelShape SHAPE_ROT2 = Block.box(2, 2, 0, 14, 14, 3);
    private static final VoxelShape SHAPE_ROT3 = Block.box(13, 2, 2, 16, 14, 14);

    public StorageTerminalBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(FORMED_ROTATION, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED_ROTATION);
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
        if (level.isClientSide()) return null;
        return createTickerHelper(blockEntityType, BeemancerBlockEntities.STORAGE_TERMINAL.get(),
            (lvl, pos, st, be) -> StorageTerminalBlockEntity.serverTick(be));
    }

    /**
     * Détermine la rotation lors du placement.
     * Le terminal se pose sur la face horizontale d'un controller.
     * La plaque est collée contre le controller (face arrière = vers le controller).
     */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();

        // Seulement les faces horizontales (N/S/E/W)
        if (clickedFace.getAxis().isVertical()) {
            return null;
        }

        // Vérifier que le bloc derrière (là où on clique) est un controller
        BlockPos behind = context.getClickedPos().relative(clickedFace.getOpposite());
        BlockState behindState = context.getLevel().getBlockState(behind);
        if (!(behindState.getBlock() instanceof StorageControllerBlock)) {
            return null;
        }

        // La plaque face vers la direction cliquée (away from controller)
        int rotation = clickedFaceToRotation(clickedFace);

        return this.defaultBlockState().setValue(FORMED_ROTATION, rotation);
    }

    /**
     * Vérifie que le terminal est toujours attaché à un controller.
     */
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        int rotation = state.getValue(FORMED_ROTATION);
        Direction toController = rotationToControllerDirection(rotation);
        BlockPos controllerPos = pos.relative(toController);
        return level.getBlockState(controllerPos).getBlock() instanceof StorageControllerBlock;
    }

    /**
     * Quand un bloc voisin change, vérifier que le controller est toujours là.
     */
    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                   LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        int rotation = state.getValue(FORMED_ROTATION);
        Direction toController = rotationToControllerDirection(rotation);
        if (direction == toController) {
            if (!canSurvive(state, (LevelReader) level, pos)) {
                return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
            }
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    /**
     * Convertit la face cliquée du controller en rotation du terminal.
     * Le terminal est flush contre la face cliquée.
     * Click north face → terminal au nord → plate côté sud (contre le controller) → rotation=0
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

    /**
     * Retourne la direction vers le controller (là où la plate est flush).
     * rotation=0: plate au sud → controller au sud
     * rotation=1: plate à l'ouest → controller à l'ouest
     * rotation=2: plate au nord → controller au nord
     * rotation=3: plate à l'est → controller à l'est
     */
    private static Direction rotationToControllerDirection(int rotation) {
        return switch (rotation) {
            case 0 -> Direction.SOUTH;
            case 1 -> Direction.WEST;
            case 2 -> Direction.NORTH;
            case 3 -> Direction.EAST;
            default -> Direction.SOUTH;
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
                terminal.unlinkController();

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
     * Lie automatiquement au controller adjacent.
     */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable net.minecraft.world.entity.LivingEntity placer,
                            net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        if (level.isClientSide() || !(placer instanceof Player player)) {
            return;
        }

        // Le controller est dans la direction du flush (basé sur la rotation)
        int rotation = state.getValue(FORMED_ROTATION);
        Direction toController = rotationToControllerDirection(rotation);
        BlockPos controllerPos = pos.relative(toController);

        BlockEntity be = level.getBlockEntity(controllerPos);
        if (be instanceof StorageControllerBlockEntity controller) {
            BlockEntity terminalBe = level.getBlockEntity(pos);
            if (terminalBe instanceof StorageTerminalBlockEntity terminal) {
                terminal.linkToController(controllerPos);

                player.displayClientMessage(
                    Component.translatable("message.beemancer.storage_terminal.auto_linked"),
                    true
                );
            }
        }
    }
}
