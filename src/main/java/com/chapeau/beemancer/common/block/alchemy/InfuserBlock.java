/**
 * ============================================================
 * [InfuserBlock.java]
 * Description: Infuse du Nectar avec Pollen pour créer des Nectars élémentaires
 * ============================================================
 */
package com.chapeau.beemancer.common.block.alchemy;

import com.chapeau.beemancer.common.blockentity.alchemy.InfuserBlockEntity;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import com.chapeau.beemancer.core.registry.BeemancerItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
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

public class InfuserBlock extends BaseEntityBlock {
    public static final MapCodec<InfuserBlock> CODEC = simpleCodec(InfuserBlock::new);
    public static final BooleanProperty INFUSING = BooleanProperty.create("infusing");

    public InfuserBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(INFUSING, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(INFUSING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new InfuserBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, BeemancerBlockEntities.INFUSER.get(), InfuserBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) return ItemInteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof InfuserBlockEntity infuser)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // Insert elemental pollen
        if (stack.is(BeemancerItems.FIRE_POLLEN.get()) ||
            stack.is(BeemancerItems.FROST_POLLEN.get()) ||
            stack.is(BeemancerItems.STORM_POLLEN.get())) {
            if (infuser.insertPollen(stack.copyWithCount(1))) {
                if (!player.isCreative()) stack.shrink(1);
                level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0f, 1.4f);
                return ItemInteractionResult.SUCCESS;
            }
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
