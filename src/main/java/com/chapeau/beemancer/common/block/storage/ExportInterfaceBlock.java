/**
 * ============================================================
 * [ExportInterfaceBlock.java]
 * Description: Bloc Export Interface - exporte automatiquement vers le reseau
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | ExportInterfaceBlockEntity    | BlockEntity associe  | Logique d'export               |
 * | BeemancerBlockEntities        | Type du BlockEntity  | Ticker                         |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - BeemancerBlocks.java (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.storage;

import com.chapeau.beemancer.common.blockentity.storage.ExportInterfaceBlockEntity;
import com.chapeau.beemancer.common.blockentity.storage.NetworkInterfaceBlockEntity;
import com.chapeau.beemancer.common.blockentity.storage.StorageControllerBlockEntity;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Bloc Export Interface.
 * Se place avec FACING pointant vers l'inventaire adjacent cible (6 directions).
 * FACING = direction vers le bloc a scanner.
 */
public class ExportInterfaceBlock extends BaseEntityBlock {

    public static final MapCodec<ExportInterfaceBlock> CODEC = simpleCodec(ExportInterfaceBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    private static final VoxelShape SHAPE_DOWN  = Block.box(3, 0, 3, 13, 1, 13);
    private static final VoxelShape SHAPE_UP    = Block.box(3, 15, 3, 13, 16, 13);
    private static final VoxelShape SHAPE_NORTH = Block.box(3, 3, 0, 13, 13, 1);
    private static final VoxelShape SHAPE_SOUTH = Block.box(3, 3, 15, 13, 13, 16);
    private static final VoxelShape SHAPE_WEST  = Block.box(0, 3, 3, 1, 13, 13);
    private static final VoxelShape SHAPE_EAST  = Block.box(15, 3, 3, 16, 13, 13);

    public ExportInterfaceBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(FACING, Direction.DOWN));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> SHAPE_NORTH;
            case SOUTH -> SHAPE_SOUTH;
            case EAST  -> SHAPE_EAST;
            case WEST  -> SHAPE_WEST;
            case UP  -> SHAPE_UP;
            default    -> SHAPE_DOWN;
        };
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
            .setValue(FACING, context.getClickedFace().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ExportInterfaceBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                   BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) return null;
        return createTickerHelper(blockEntityType, BeemancerBlockEntities.EXPORT_INTERFACE.get(),
            (lvl, pos, st, be) -> be.serverTick());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ExportInterfaceBlockEntity iface)) return InteractionResult.PASS;

        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(iface, iface.getBlockPos());
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof NetworkInterfaceBlockEntity iface) {
                BlockPos ctrlPos = iface.getControllerPos();
                if (ctrlPos != null && level.isLoaded(ctrlPos)) {
                    BlockEntity ctrlBe = level.getBlockEntity(ctrlPos);
                    if (ctrlBe instanceof StorageControllerBlockEntity controller) {
                        controller.getNetworkRegistry().unregisterBlock(pos);
                        controller.setChanged();
                        controller.syncNodeToClient();
                    }
                }
                iface.unlinkController();
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
