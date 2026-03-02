/**
 * ============================================================
 * [UncraftingTableBlock.java]
 * Description: Bloc Uncrafting Table — décompose les items craftés en ingrédients
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                      | Raison                | Utilisation                    |
 * |---------------------------------|----------------------|--------------------------------|
 * | UncraftingTableBlockEntity      | BlockEntity          | Logique de décomposition       |
 * | ApicaBlockEntities              | Registre BE          | Type de block entity           |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ApicaBlocks (registration)
 * - UncraftingTableBlockEntity (ticker)
 *
 * ============================================================
 */
package com.chapeau.apica.common.block.alchemy;

import com.chapeau.apica.common.blockentity.alchemy.UncraftingTableBlockEntity;
import com.chapeau.apica.core.registry.ApicaBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public class UncraftingTableBlock extends BaseEntityBlock {
    public static final MapCodec<UncraftingTableBlock> CODEC = simpleCodec(UncraftingTableBlock::new);
    public static final BooleanProperty WORKING = BooleanProperty.create("working");
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public UncraftingTableBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(WORKING, false)
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WORKING, FACING);
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

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new UncraftingTableBlockEntity(pos, state);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, (BlockEntityType<UncraftingTableBlockEntity>) ApicaBlockEntities.UNCRAFTING_TABLE.get(),
                UncraftingTableBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof UncraftingTableBlockEntity table) {
                serverPlayer.openMenu(table, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof UncraftingTableBlockEntity table) {
                for (int i = 0; i < table.getInputSlot().getSlots(); i++) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(),
                            table.getInputSlot().getStackInSlot(i));
                }
                for (int i = 0; i < table.getOutputSlots().getSlots(); i++) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(),
                            table.getOutputSlots().getStackInSlot(i));
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
