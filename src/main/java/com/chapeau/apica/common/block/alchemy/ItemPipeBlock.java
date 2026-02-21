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
 * | ApicaBlockEntities  | Registre             | Types tiered                   |
 * | ItemPipeNetworkManager | Réseau pipes      | Notifier ajout/retrait/toggle  |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ApicaBlocks.java (enregistrement)
 * - ClientSetup.java (block colors)
 *
 * ============================================================
 */
package com.chapeau.apica.common.block.alchemy;

import com.chapeau.apica.common.blockentity.alchemy.ItemPipeBlockEntity;
import com.chapeau.apica.common.menu.ItemFilterMenu;
import com.chapeau.apica.core.network.pipe.ItemPipeNetworkManager;
import com.chapeau.apica.core.registry.ApicaBlockEntities;
import com.chapeau.apica.core.registry.ApicaItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;

import javax.annotation.Nullable;

/**
 * Pipe d'items. Se connecte aux ItemHandler.
 * Notifie le ItemPipeNetworkManager lors des changements topologiques.
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

    // --- Filter interactions ---

    @Override
    @Nullable
    protected InteractionResult handleSpecialInteraction(BlockState state, Level level, BlockPos pos,
                                                          Player player, BlockHitResult hit) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ItemPipeBlockEntity pipe)) return null;
        if (!pipe.hasFilter()) return null;

        // Verifier que le clic est dans la zone centrale (core = pas de direction dominante)
        Direction clickedDir = getClickedDirection(pos, hit);
        if (clickedDir != null) return null; // Clic sur une face, pas le core

        if (player.isShiftKeyDown()) {
            // Shift+click: retirer le filtre
            pipe.removeFilter();
            level.setBlock(pos, state.setValue(FILTERED, false), 3);
            level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0f, 1.0f);
            if (!player.getAbilities().instabuild) {
                ItemStack filterItem = new ItemStack(ApicaItems.ITEM_FILTER.get());
                if (!player.getInventory().add(filterItem)) {
                    Block.popResource(level, pos, filterItem);
                }
            }
            player.displayClientMessage(Component.literal("Filter removed"), true);
            return InteractionResult.SUCCESS;
        } else {
            // Click normal: ouvrir le menu du filtre
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(
                    new SimpleMenuProvider(
                        (containerId, playerInv, p) -> new ItemFilterMenu(containerId, playerInv, be),
                        Component.translatable("container.apica.item_filter")
                    ),
                    buf -> buf.writeBlockPos(pos)
                );
            }
            return InteractionResult.SUCCESS;
        }
    }

    // --- Network hooks ---

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            ItemPipeNetworkManager.get(serverLevel).onPipeAdded(pos, serverLevel);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            if (!state.is(newState.getBlock())) {
                // Drop le filtre si installe
                if (level.getBlockEntity(pos) instanceof ItemPipeBlockEntity pipe && pipe.hasFilter()) {
                    Block.popResource(level, pos, new ItemStack(ApicaItems.ITEM_FILTER.get()));
                }
                // Notifier avant que le BlockEntity soit retiré
                ItemPipeNetworkManager.get(serverLevel).onPipeRemoved(pos, serverLevel);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected void onConnectionToggled(Level level, BlockPos pos) {
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            ItemPipeNetworkManager.get(serverLevel).onConnectionChanged(pos, serverLevel);
        }
    }

    // --- BlockEntity ---

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return switch (tier) {
            case 2 -> ItemPipeBlockEntity.createMk2(pos, state);
            case 3 -> ItemPipeBlockEntity.createMk3(pos, state);
            case 4 -> ItemPipeBlockEntity.createMk4(pos, state);
            default -> new ItemPipeBlockEntity(pos, state);
        };
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        BlockEntityType<?> expectedType = switch (tier) {
            case 2 -> ApicaBlockEntities.ITEM_PIPE_MK2.get();
            case 3 -> ApicaBlockEntities.ITEM_PIPE_MK3.get();
            case 4 -> ApicaBlockEntities.ITEM_PIPE_MK4.get();
            default -> ApicaBlockEntities.ITEM_PIPE.get();
        };
        return createTickerHelper(type, (BlockEntityType<ItemPipeBlockEntity>) expectedType,
            ItemPipeBlockEntity::serverTick);
    }
}
