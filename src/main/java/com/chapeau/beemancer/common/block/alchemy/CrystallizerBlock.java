/**
 * ============================================================
 * [CrystallizerBlock.java]
 * Description: Machine magique qui transforme Royal Jelly en Nectar
 * ============================================================
 */
package com.chapeau.beemancer.common.block.alchemy;

import com.chapeau.beemancer.common.blockentity.alchemy.CrystallizerBlockEntity;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import com.chapeau.beemancer.core.registry.BeemancerItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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

public class CrystallizerBlock extends BaseEntityBlock {
    public static final MapCodec<CrystallizerBlock> CODEC = simpleCodec(CrystallizerBlock::new);
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public CrystallizerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrystallizerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, BeemancerBlockEntities.CRYSTALLIZER.get(), CrystallizerBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) return ItemInteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof CrystallizerBlockEntity crystallizer)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // Insert crystals
        if (stack.is(BeemancerItems.HONEY_CRYSTAL.get()) || 
            stack.is(BeemancerItems.ENRICHED_HONEY_CRYSTAL.get()) ||
            stack.is(BeemancerItems.RADIANT_HONEY_CRYSTAL.get())) {
            /*
            if (crystallizer.insertCrystal(stack.copyWithCount(1))) {
                if (!player.isCreative()) stack.shrink(1);
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_PLACE, SoundSource.BLOCKS, 1.0f, 1.2f);
                return ItemInteractionResult.SUCCESS;
            }*/
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CrystallizerBlockEntity crystallizer) {
                serverPlayer.openMenu(crystallizer, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
