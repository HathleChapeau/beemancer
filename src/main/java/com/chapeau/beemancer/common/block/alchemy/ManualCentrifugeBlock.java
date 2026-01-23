/**
 * ============================================================
 * [ManualCentrifugeBlock.java]
 * Description: Centrifugeuse manuelle pour extraire le miel des rayons
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | ManualCentrifugeBE  | BlockEntity          | Logique d'extraction           |
 * | BeemancerBlockEntities| Enregistrement     | Type de BlockEntity            |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - BeemancerBlocks (enregistrement)
 * - Joueur (interaction manuelle)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.alchemy;

import com.chapeau.beemancer.common.blockentity.alchemy.ManualCentrifugeBlockEntity;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class ManualCentrifugeBlock extends BaseEntityBlock {
    public static final MapCodec<ManualCentrifugeBlock> CODEC = simpleCodec(ManualCentrifugeBlock::new);
    public static final BooleanProperty SPINNING = BooleanProperty.create("spinning");

    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 14, 15);

    public ManualCentrifugeBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(SPINNING, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SPINNING);
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
        return new ManualCentrifugeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, BeemancerBlockEntities.MANUAL_CENTRIFUGE.get(),
            ManualCentrifugeBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ManualCentrifugeBlockEntity centrifuge)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // Insert honeycomb
        if (stack.is(Items.HONEYCOMB) || stack.is(net.minecraft.world.item.Items.HONEYCOMB)) {
            if (centrifuge.canInsertComb()) {
                centrifuge.insertComb(stack.copyWithCount(1));
                if (!player.isCreative()) {
                    stack.shrink(1);
                }
                level.playSound(null, pos, SoundEvents.BEEHIVE_ENTER, SoundSource.BLOCKS, 1.0f, 1.0f);
                return ItemInteractionResult.SUCCESS;
            }
        }

        // Insert royal comb
        if (stack.getItem() == com.chapeau.beemancer.core.registry.BeemancerItems.ROYAL_COMB.get()) {
            if (centrifuge.canInsertComb()) {
                centrifuge.insertComb(stack.copyWithCount(1));
                if (!player.isCreative()) {
                    stack.shrink(1);
                }
                level.playSound(null, pos, SoundEvents.BEEHIVE_ENTER, SoundSource.BLOCKS, 1.0f, 1.0f);
                return ItemInteractionResult.SUCCESS;
            }
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ManualCentrifugeBlockEntity centrifuge)) {
            return InteractionResult.PASS;
        }

        // Manual spin - right click without item
        if (centrifuge.canSpin()) {
            centrifuge.spin();
            level.setBlock(pos, state.setValue(SPINNING, true), 3);
            level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.8f, 1.2f);

            // Spawn honey particles
            if (level instanceof ServerLevel serverLevel) {
                for (int i = 0; i < 5; i++) {
                    double offsetX = level.random.nextDouble() * 0.6 - 0.3;
                    double offsetZ = level.random.nextDouble() * 0.6 - 0.3;
                    serverLevel.sendParticles(ParticleTypes.FALLING_HONEY,
                        pos.getX() + 0.5 + offsetX, pos.getY() + 0.8, pos.getZ() + 0.5 + offsetZ,
                        1, 0, 0, 0, 0);
                }
            }
            return InteractionResult.SUCCESS;
        }

        // Extract output if no combs to process
        ItemStack output = centrifuge.extractOutput();
        if (!output.isEmpty()) {
            player.addItem(output);
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.5f, 1.0f);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ManualCentrifugeBlockEntity centrifuge) {
                Containers.dropContents(level, pos, centrifuge.getDrops());
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(SPINNING)) {
            // Particle effects while spinning
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.7;
            double z = pos.getZ() + 0.5;
            level.addParticle(ParticleTypes.FALLING_HONEY,
                x + random.nextDouble() * 0.4 - 0.2,
                y,
                z + random.nextDouble() * 0.4 - 0.2,
                0, 0, 0);
        }
    }
}
