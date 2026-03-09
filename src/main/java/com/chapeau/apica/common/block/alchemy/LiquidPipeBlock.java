/**
 * ============================================================
 * [LiquidPipeBlock.java]
 * Description: Pipe pour transporter les fluides Apica
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | AbstractPipeBlock   | Base pipe            | Connexion, shapes, interaction |
 * | LiquidPipeBlockEntity| BlockEntity fluide   | Gestion buffer fluide          |
 * | ApicaBlockEntities| Registre           | Types tiered                   |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ApicaBlocks.java (enregistrement)
 * - ClientSetup.java (block colors)
 *
 * ============================================================
 */
package com.chapeau.apica.common.block.alchemy;

import com.chapeau.apica.common.blockentity.alchemy.LiquidPipeBlockEntity;
import com.chapeau.apica.core.registry.ApicaBlockEntities;
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
public class LiquidPipeBlock extends AbstractPipeBlock {
    public static final MapCodec<LiquidPipeBlock> CODEC = simpleCodec(LiquidPipeBlock::new);

    public LiquidPipeBlock(Properties properties) {
        this(properties, 1);
    }

    public LiquidPipeBlock(Properties properties, int tier) {
        super(properties, tier);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    // --- AbstractPipeBlock implementation ---

    @Override
    protected boolean isPipeEntity(BlockEntity be) {
        return be instanceof LiquidPipeBlockEntity;
    }

    @Override
    protected boolean isPipeDisconnected(BlockEntity be, Direction dir) {
        return ((LiquidPipeBlockEntity) be).isDisconnected(dir);
    }

    @Override
    protected void setPipeDisconnected(BlockEntity be, Direction dir, boolean disconnected) {
        ((LiquidPipeBlockEntity) be).setDisconnected(dir, disconnected);
    }

    @Override
    protected boolean isSamePipeType(BlockState neighborState) {
        return neighborState.getBlock() instanceof LiquidPipeBlock;
    }

    @Override
    protected boolean hasCapabilityAt(Level level, BlockPos pos, Direction side) {
        return level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side) != null;
    }

    @Override
    protected boolean isPipeExtracting(BlockEntity be, Direction dir) {
        return be instanceof LiquidPipeBlockEntity pipe && pipe.isExtracting(dir);
    }

    @Override
    protected void setPipeExtracting(BlockEntity be, Direction dir, boolean extracting) {
        if (be instanceof LiquidPipeBlockEntity pipe) {
            pipe.setExtracting(dir, extracting);
        }
    }

    @Override
    protected boolean isTintablePipe(BlockEntity be) {
        return be instanceof LiquidPipeBlockEntity;
    }

    @Override
    protected boolean hasTint(BlockEntity be) {
        return be instanceof LiquidPipeBlockEntity pipe && pipe.hasTint();
    }

    @Override
    protected void applyTint(BlockEntity be, int color) {
        ((LiquidPipeBlockEntity) be).setTintColor(color);
    }

    @Override
    @Nullable
    protected InteractionResult handleSpecialInteraction(BlockState state, Level level, BlockPos pos,
                                                          Player player, BlockHitResult hit) {
        // Shift+clic droit = vider le pipe
        if (player.isShiftKeyDown()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof LiquidPipeBlockEntity pipe) {
                com.chapeau.apica.core.util.IDrainable.tryDrain(level, pos, player, pipe);
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
            case 2 -> LiquidPipeBlockEntity.createMk2(pos, state);
            case 3 -> LiquidPipeBlockEntity.createMk3(pos, state);
            case 4 -> LiquidPipeBlockEntity.createMk4(pos, state);
            default -> new LiquidPipeBlockEntity(pos, state);
        };
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        BlockEntityType<?> expectedType = switch (tier) {
            case 2 -> ApicaBlockEntities.LIQUID_PIPE_MK2.get();
            case 3 -> ApicaBlockEntities.LIQUID_PIPE_MK3.get();
            case 4 -> ApicaBlockEntities.LIQUID_PIPE_MK4.get();
            default -> ApicaBlockEntities.LIQUID_PIPE.get();
        };
        return createTickerHelper(type, (BlockEntityType<LiquidPipeBlockEntity>) expectedType,
            LiquidPipeBlockEntity::serverTick);
    }
}
