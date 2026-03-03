/**
 * ============================================================
 * [LaunchpadBlock.java]
 * Description: Bloc Launchpad — projette les entites dans une direction configurable
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                  | Raison                | Utilisation                    |
 * |-----------------------------|----------------------|--------------------------------|
 * | LaunchpadBlockEntity        | BlockEntity          | Logique de projection          |
 * | ApicaBlockEntities          | Registre BE          | Type de block entity           |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ApicaBlocks (registration)
 * - LaunchpadBlockEntity (ticker)
 *
 * ============================================================
 */
package com.chapeau.apica.common.block.alchemy;

import com.chapeau.apica.common.blockentity.alchemy.LaunchpadBlockEntity;
import com.chapeau.apica.core.registry.ApicaBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidUtil;

import javax.annotation.Nullable;

public class LaunchpadBlock extends BaseEntityBlock {
    public static final MapCodec<LaunchpadBlock> CODEC = simpleCodec(LaunchpadBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty ANGLE = IntegerProperty.create("angle", 0, 6);

    private static final VoxelShape SHAPE_NORTH = Shapes.or(
            Block.box(1, 0, 1, 15, 8, 15),
            Block.box(3, 0, 14, 13, 10, 16)
    );
    private static final VoxelShape SHAPE_SOUTH = Shapes.or(
            Block.box(1, 0, 1, 15, 8, 15),
            Block.box(3, 0, 0, 13, 10, 2)
    );
    private static final VoxelShape SHAPE_EAST = Shapes.or(
            Block.box(1, 0, 1, 15, 8, 15),
            Block.box(0, 0, 3, 2, 10, 13)
    );
    private static final VoxelShape SHAPE_WEST = Shapes.or(
            Block.box(1, 0, 1, 15, 8, 15),
            Block.box(14, 0, 3, 16, 10, 13)
    );

    public LaunchpadBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(ANGLE, 0));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ANGLE);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> SHAPE_SOUTH;
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
            default -> SHAPE_NORTH;
        };
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LaunchpadBlockEntity(pos, state);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, (BlockEntityType<LaunchpadBlockEntity>) ApicaBlockEntities.LAUNCHPAD.get(),
                LaunchpadBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof LaunchpadBlockEntity launchpad) {
                if (FluidUtil.interactWithFluidHandler(player, hand, launchpad.getFluidTank())) {
                    return ItemInteractionResult.sidedSuccess(level.isClientSide());
                }
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            int currentAngle = state.getValue(ANGLE);
            int newAngle = (currentAngle + 1) % 7;
            level.setBlock(pos, state.setValue(ANGLE, newAngle), 3);
            player.displayClientMessage(
                    Component.translatable("message.apica.launchpad.angle", newAngle * 10),
                    true
            );
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
