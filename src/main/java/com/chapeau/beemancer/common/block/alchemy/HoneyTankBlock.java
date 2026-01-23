/**
 * ============================================================
 * [HoneyTankBlock.java]
 * Description: Tank de stockage pour les fluides Beemancer
 * ============================================================
 */
package com.chapeau.beemancer.common.block.alchemy;

import com.chapeau.beemancer.common.blockentity.alchemy.HoneyTankBlockEntity;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;

public class HoneyTankBlock extends BaseEntityBlock {
    public static final MapCodec<HoneyTankBlock> CODEC = simpleCodec(HoneyTankBlock::new);
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 16, 15);

    public HoneyTankBlock(Properties properties) {
        super(properties);
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
        return new HoneyTankBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, BeemancerBlockEntities.HONEY_TANK.get(), HoneyTankBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof HoneyTankBlockEntity tank)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // Handle bucket interaction
        if (stack.getItem() instanceof BucketItem) {
            boolean success = FluidUtil.interactWithFluidHandler(player, hand, tank.getFluidTank());
            if (success) {
                level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0f, 1.0f);
                return ItemInteractionResult.SUCCESS;
            }
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof HoneyTankBlockEntity tank) {
            float fillRatio = (float) tank.getFluidTank().getFluidAmount() / tank.getFluidTank().getCapacity();
            return (int) (fillRatio * 15);
        }
        return 0;
    }
}
