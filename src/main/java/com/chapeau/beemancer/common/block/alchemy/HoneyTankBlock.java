/**
 * ============================================================
 * [HoneyTankBlock.java]
 * Description: Tank de stockage pour fluides Beemancer
 * ============================================================
 */
package com.chapeau.beemancer.common.block.alchemy;

import com.chapeau.beemancer.common.blockentity.alchemy.HoneyTankBlockEntity;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public class HoneyTankBlock extends BaseEntityBlock {
    public static final MapCodec<HoneyTankBlock> CODEC = simpleCodec(HoneyTankBlock::new);

    private final int tier;

    public HoneyTankBlock(Properties properties) {
        this(properties, 1);
    }

    public HoneyTankBlock(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }

    public int getTier() { return tier; }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return switch (tier) {
            case 2 -> HoneyTankBlockEntity.createTier2(pos, state);
            case 3 -> HoneyTankBlockEntity.createTier3(pos, state);
            default -> new HoneyTankBlockEntity(pos, state);
        };
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        BlockEntityType<?> expectedType = switch (tier) {
            case 2 -> BeemancerBlockEntities.HONEY_TANK_TIER2.get();
            case 3 -> BeemancerBlockEntities.HONEY_TANK_TIER3.get();
            default -> BeemancerBlockEntities.HONEY_TANK.get();
        };
        return createTickerHelper(type, (BlockEntityType<HoneyTankBlockEntity>) expectedType,
            HoneyTankBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof HoneyTankBlockEntity tank) {
                serverPlayer.openMenu(tank, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
