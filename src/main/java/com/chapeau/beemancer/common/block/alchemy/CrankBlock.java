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
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - BeemancerBlocks (enregistrement)
 * - BeemancerItems (BlockItem)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.alchemy;

import com.chapeau.beemancer.common.blockentity.alchemy.ManualCentrifugeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class CrankBlock extends Block {

    private static final VoxelShape SHAPE = Shapes.or(
        Block.box(7, 0, 7, 9, 2, 9),     // Tige centrale
        Block.box(4, 2, 7, 6, 8, 9),     // Poignee gauche
        Block.box(5, 6, 7, 11, 8, 9),    // Bras horizontal
        Block.box(10, 8, 7, 12, 14, 9)   // Poignee droite
    );

    public CrankBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
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

                    // Sound
                    if (level.random.nextInt(10) == 0) {
                        level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.5f, 1.2f);
                    }

                    // Particles on the centrifuge
                    if (level instanceof ServerLevel serverLevel) {
                        double offsetX = level.random.nextDouble() * 0.6 - 0.3;
                        double offsetZ = level.random.nextDouble() * 0.6 - 0.3;
                        serverLevel.sendParticles(ParticleTypes.FALLING_HONEY,
                            belowPos.getX() + 0.5 + offsetX, belowPos.getY() + 0.8,
                            belowPos.getZ() + 0.5 + offsetZ,
                            1, 0, 0, 0, 0);
                    }

                    return InteractionResult.SUCCESS;
                }
            }
        }

        return InteractionResult.PASS;
    }
}
