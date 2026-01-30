/**
 * ============================================================
 * [ItemPipeBlock.java]
 * Description: Pipe pour transporter les items
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | AbstractPipeBlock   | Base pipe            | Connexion, shapes, interaction |
 * | ItemPipeBlockEntity | BlockEntity item     | Gestion buffer items           |
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

import com.chapeau.beemancer.common.blockentity.alchemy.ItemPipeBlockEntity;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;

import javax.annotation.Nullable;

/**
 * Pipe d'items. Se connecte aux ItemHandler.
 */
public class ItemPipeBlock extends AbstractPipeBlock {
    public static final MapCodec<ItemPipeBlock> CODEC = simpleCodec(ItemPipeBlock::new);

    public ItemPipeBlock(Properties properties) {
        this(properties, 1);
    }

    public ItemPipeBlock(Properties properties, int tier) {
        super(properties, tier);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    // --- AbstractPipeBlock implementation ---

    @Override
    protected boolean isPipeEntity(BlockEntity be) {
        return be instanceof ItemPipeBlockEntity;
    }

    @Override
    protected boolean isPipeDisconnected(BlockEntity be, Direction dir) {
        return ((ItemPipeBlockEntity) be).isDisconnected(dir);
    }

    @Override
    protected void setPipeDisconnected(BlockEntity be, Direction dir, boolean disconnected) {
        ((ItemPipeBlockEntity) be).setDisconnected(dir, disconnected);
    }

    @Override
    protected boolean isSamePipeType(BlockState neighborState) {
        return neighborState.getBlock() instanceof ItemPipeBlock;
    }

    @Override
    protected boolean hasCapabilityAt(Level level, BlockPos pos, Direction side) {
        return level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side) != null;
    }

    @Override
    protected boolean isTintablePipe(BlockEntity be) {
        return be instanceof ItemPipeBlockEntity;
    }

    @Override
    protected boolean hasTint(BlockEntity be) {
        return be instanceof ItemPipeBlockEntity pipe && pipe.hasTint();
    }

    @Override
    protected void applyTint(BlockEntity be, int color) {
        ((ItemPipeBlockEntity) be).setTintColor(color);
    }

    // --- BlockEntity ---

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return switch (tier) {
            case 2 -> ItemPipeBlockEntity.createTier2(pos, state);
            case 3 -> ItemPipeBlockEntity.createTier3(pos, state);
            case 4 -> ItemPipeBlockEntity.createTier4(pos, state);
            default -> new ItemPipeBlockEntity(pos, state);
        };
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        BlockEntityType<?> expectedType = switch (tier) {
            case 2 -> BeemancerBlockEntities.ITEM_PIPE_TIER2.get();
            case 3 -> BeemancerBlockEntities.ITEM_PIPE_TIER3.get();
            case 4 -> BeemancerBlockEntities.ITEM_PIPE_TIER4.get();
            default -> BeemancerBlockEntities.ITEM_PIPE.get();
        };
        return createTickerHelper(type, (BlockEntityType<ItemPipeBlockEntity>) expectedType,
            ItemPipeBlockEntity::serverTick);
    }
}
