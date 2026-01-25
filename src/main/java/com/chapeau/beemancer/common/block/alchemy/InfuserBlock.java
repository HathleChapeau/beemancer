/**
 * ============================================================
 * [InfuserBlock.java]
 * Description: Machine pour infuser du bois avec du miel
 * ============================================================
 */
package com.chapeau.beemancer.common.block.alchemy;

import com.chapeau.beemancer.common.blockentity.alchemy.InfuserBlockEntity;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public class InfuserBlock extends BaseEntityBlock {
    public static final MapCodec<InfuserBlock> CODEC = simpleCodec(InfuserBlock::new);
    public static final BooleanProperty WORKING = BooleanProperty.create("working");

    private final int tier;

    public InfuserBlock(Properties properties) {
        this(properties, 1);
    }

    public InfuserBlock(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
        this.registerDefaultState(this.stateDefinition.any().setValue(WORKING, false));
    }

    public int getTier() { return tier; }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WORKING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return switch (tier) {
            case 2 -> InfuserBlockEntity.createTier2(pos, state);
            case 3 -> InfuserBlockEntity.createTier3(pos, state);
            default -> new InfuserBlockEntity(pos, state);
        };
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        BlockEntityType<?> expectedType = switch (tier) {
            case 2 -> BeemancerBlockEntities.INFUSER_TIER2.get();
            case 3 -> BeemancerBlockEntities.INFUSER_TIER3.get();
            default -> BeemancerBlockEntities.INFUSER.get();
        };
        return createTickerHelper(type, (BlockEntityType<InfuserBlockEntity>) expectedType, InfuserBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof InfuserBlockEntity infuser && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(infuser, pos);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof InfuserBlockEntity infuser) {
                for (int i = 0; i < infuser.getInputSlot().getSlots(); i++) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), 
                        infuser.getInputSlot().getStackInSlot(i));
                }
                for (int i = 0; i < infuser.getOutputSlot().getSlots(); i++) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), 
                        infuser.getOutputSlot().getStackInSlot(i));
                }
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
