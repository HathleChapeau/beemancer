/**
 * ============================================================
 * [NectarFilterBlock.java]
 * Description: Filtre pour purifier le Royal Jelly
 * ============================================================
 */
package com.chapeau.beemancer.common.block.alchemy;

import com.chapeau.beemancer.common.blockentity.alchemy.NectarFilterBlockEntity;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class NectarFilterBlock extends BaseEntityBlock {
    public static final MapCodec<NectarFilterBlock> CODEC = simpleCodec(NectarFilterBlock::new);

    public NectarFilterBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NectarFilterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, BeemancerBlockEntities.NECTAR_FILTER.get(), NectarFilterBlockEntity::serverTick);
    }
}
