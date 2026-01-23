/**
 * ============================================================
 * [AlembicBlock.java]
 * Description: Alambic pour distiller le Nectar
 * ============================================================
 */
package com.chapeau.beemancer.common.block.alchemy;

import com.chapeau.beemancer.common.blockentity.alchemy.AlembicBlockEntity;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
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

import javax.annotation.Nullable;

public class AlembicBlock extends BaseEntityBlock {
    public static final MapCodec<AlembicBlock> CODEC = simpleCodec(AlembicBlock::new);
    public static final BooleanProperty DISTILLING = BooleanProperty.create("distilling");

    public AlembicBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(DISTILLING, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DISTILLING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AlembicBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, BeemancerBlockEntities.ALEMBIC.get(), AlembicBlockEntity::serverTick);
    }
}
