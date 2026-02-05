/**
 * ============================================================
 * [CrankBlock.java]
 * Description: Manivelle placee au-dessus de la centrifugeuse manuelle
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | ManualCentrifugeBlock         | Support requis       | Placement conditionnel         |
 * | ManualCentrifugeBlockEntity   | Processing           | onPlayerSpin() delegation      |
 * | CrankBlockEntity              | BER support          | Rendu rotation                 |
 * | BeemancerBlockEntities        | Registration BE      | Type du BlockEntity            |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - BeemancerBlocks (enregistrement)
 * - BeemancerItems (BlockItem)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.alchemy;

import com.chapeau.beemancer.common.blockentity.alchemy.CrankBlockEntity;
import com.chapeau.beemancer.common.blockentity.alchemy.ManualCentrifugeBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class CrankBlock extends BaseEntityBlock {
    public static final MapCodec<CrankBlock> CODEC = simpleCodec(CrankBlock::new);

    private static final VoxelShape SHAPE = Shapes.or(
        Block.box(7, 0, 7, 9, 5, 9),     // Handle Left
        Block.box(7, 5, 7, 14, 7, 9),    // Arm Horizontal
        Block.box(12, 7, 7, 14, 12, 9)   // Handle Right
    );

    public CrankBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrankBlockEntity(pos, state);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).getBlock() instanceof ManualCentrifugeBlock;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (!canSurvive(defaultBlockState(), context.getLevel(), context.getClickedPos())) {
            return null;
        }
        return defaultBlockState();
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (!canSurvive(state, level, pos)) {
            dropResources(state, level, pos);
            level.removeBlock(pos, false);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        BlockPos belowPos = pos.below();
        if (level.getBlockEntity(belowPos) instanceof ManualCentrifugeBlockEntity centrifuge) {
            if (centrifuge.hasCombsToProcess()) {
                boolean spun = centrifuge.onPlayerSpin(level);
                if (spun) {
                    // Update spinning state on centrifuge
                    BlockState belowState = level.getBlockState(belowPos);
                    if (belowState.hasProperty(ManualCentrifugeBlock.SPINNING)
                            && !belowState.getValue(ManualCentrifugeBlock.SPINNING)) {
                        level.setBlock(belowPos, belowState.setValue(ManualCentrifugeBlock.SPINNING, true), 3);
                    }

                    return InteractionResult.SUCCESS;
                }
            }
        }

        return InteractionResult.PASS;
    }
}
