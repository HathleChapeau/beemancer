/**
 * ============================================================
 * [HoneyPipeBlock.java]
 * Description: Pipe pour transporter les fluides Beemancer
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | AbstractPipeBlock   | Base pipe            | Connexion, shapes, interaction |
 * | HoneyPipeBlockEntity| BlockEntity fluide   | Gestion buffer fluide          |
 * | BeemancerBlockEntities| Registre           | Types tiered                   |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - BeemancerBlocks.java (enregistrement)
 * - ClientSetup.java (block colors)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.alchemy;

import com.chapeau.beemancer.common.blockentity.alchemy.HoneyPipeBlockEntity;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;

import javax.annotation.Nullable;

/**
 * Pipe de fluide. Se connecte aux FluidHandler.
 * Supporte le shift+clic pour vider le buffer interne.
 */
public class HoneyPipeBlock extends AbstractPipeBlock {
    public static final MapCodec<HoneyPipeBlock> CODEC = simpleCodec(HoneyPipeBlock::new);

    public HoneyPipeBlock(Properties properties) {
        this(properties, 1);
    }

    public HoneyPipeBlock(Properties properties, int tier) {
        super(properties, tier);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    // --- AbstractPipeBlock implementation ---

    @Override
    protected boolean isPipeEntity(BlockEntity be) {
        return be instanceof HoneyPipeBlockEntity;
    }

    @Override
    protected boolean isPipeDisconnected(BlockEntity be, Direction dir) {
        return ((HoneyPipeBlockEntity) be).isDisconnected(dir);
    }

    @Override
    protected void setPipeDisconnected(BlockEntity be, Direction dir, boolean disconnected) {
        ((HoneyPipeBlockEntity) be).setDisconnected(dir, disconnected);
    }

    @Override
    protected boolean isSamePipeType(BlockState neighborState) {
        return neighborState.getBlock() instanceof HoneyPipeBlock;
    }

    @Override
    protected boolean hasCapabilityAt(Level level, BlockPos pos, Direction side) {
        return level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side) != null;
    }

    @Override
    protected boolean isTintablePipe(BlockEntity be) {
        return be instanceof HoneyPipeBlockEntity;
    }

    @Override
    protected boolean hasTint(BlockEntity be) {
        return be instanceof HoneyPipeBlockEntity pipe && pipe.hasTint();
    }

    @Override
    protected void applyTint(BlockEntity be, int color) {
        ((HoneyPipeBlockEntity) be).setTintColor(color);
    }

    @Override
    @Nullable
    protected InteractionResult handleSpecialInteraction(BlockState state, Level level, BlockPos pos,
                                                          Player player, BlockHitResult hit) {
        // Shift+clic droit = vider le pipe
        if (player.isShiftKeyDown()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof HoneyPipeBlockEntity pipe) {
                int drained = pipe.getBuffer().drain(Integer.MAX_VALUE,
                    net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE).getAmount();
                if (drained > 0) {
                    level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.5f, 1.0f);
                    player.displayClientMessage(Component.literal("Drained " + drained + " mB"), true);
                } else {
                    player.displayClientMessage(Component.literal("Pipe is empty"), true);
                }
                return InteractionResult.SUCCESS;
            }
        }
        return null;
    }

    // --- BlockEntity ---

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return switch (tier) {
            case 2 -> HoneyPipeBlockEntity.createTier2(pos, state);
            case 3 -> HoneyPipeBlockEntity.createTier3(pos, state);
            case 4 -> HoneyPipeBlockEntity.createTier4(pos, state);
            default -> new HoneyPipeBlockEntity(pos, state);
        };
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        BlockEntityType<?> expectedType = switch (tier) {
            case 2 -> BeemancerBlockEntities.HONEY_PIPE_TIER2.get();
            case 3 -> BeemancerBlockEntities.HONEY_PIPE_TIER3.get();
            case 4 -> BeemancerBlockEntities.HONEY_PIPE_TIER4.get();
            default -> BeemancerBlockEntities.HONEY_PIPE.get();
        };
        return createTickerHelper(type, (BlockEntityType<HoneyPipeBlockEntity>) expectedType,
            HoneyPipeBlockEntity::serverTick);
    }
}
