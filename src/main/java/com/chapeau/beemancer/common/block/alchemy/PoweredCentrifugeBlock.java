/**
 * ============================================================
 * [PoweredCentrifugeBlock.java]
 * Description: Centrifugeuse automatique alimentée par Honey
 * ============================================================
 */
package com.chapeau.beemancer.common.block.alchemy;

import com.chapeau.beemancer.common.blockentity.alchemy.PoweredCentrifugeBlockEntity;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
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

public class PoweredCentrifugeBlock extends BaseEntityBlock {
    public static final MapCodec<PoweredCentrifugeBlock> CODEC = simpleCodec(PoweredCentrifugeBlock::new);
    public static final BooleanProperty WORKING = BooleanProperty.create("working");

    private final int tier;

    public PoweredCentrifugeBlock(Properties properties) {
        this(properties, 1);
    }

    public PoweredCentrifugeBlock(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
        this.registerDefaultState(this.stateDefinition.any().setValue(WORKING, false));
    }

    public int getTier() { return tier; }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WORKING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return switch (tier) {
            case 2 -> PoweredCentrifugeBlockEntity.createTier2(pos, state);
            default -> new PoweredCentrifugeBlockEntity(pos, state);
        };
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        BlockEntityType<?> expectedType = switch (tier) {
            case 2 -> BeemancerBlockEntities.POWERED_CENTRIFUGE_TIER2.get();
            default -> BeemancerBlockEntities.POWERED_CENTRIFUGE.get();
        };
        return createTickerHelper(type, (BlockEntityType<PoweredCentrifugeBlockEntity>) expectedType,
            PoweredCentrifugeBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PoweredCentrifugeBlockEntity centrifuge) {
                serverPlayer.openMenu(centrifuge, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
