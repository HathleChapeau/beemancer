/**
 * ============================================================
 * [ResonatorBlock.java]
 * Description: Bloc resonateur avec GUI d'onde interactive et support abeille
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | ResonatorBlockEntity    | Stockage parametres  | Persistance freq/knobs/bee     |
 * | BaseEntityBlock         | Support BlockEntity  | newBlockEntity()               |
 * | ApicaItems              | MagicBee check       | Filtre item abeille            |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ApicaBlocks (enregistrement)
 * - ApicaItems (BlockItem)
 *
 * ============================================================
 */
package com.chapeau.apica.common.block.resonator;

import com.chapeau.apica.common.codex.CodexPlayerData;
import com.chapeau.apica.common.item.bee.MagicBeeItem;
import com.chapeau.apica.core.registry.ApicaAttachments;
import com.chapeau.apica.core.registry.ApicaBlockEntities;
import com.chapeau.apica.core.registry.ApicaItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public class ResonatorBlock extends BaseEntityBlock {

    public static final MapCodec<ResonatorBlock> CODEC = simpleCodec(ResonatorBlock::new);

    public ResonatorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ResonatorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, ApicaBlockEntities.RESONATOR.get(),
                ResonatorBlockEntity::serverTick);
    }

    // Clic droit avec item en main (abeille)
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                               BlockPos pos, Player player, InteractionHand hand,
                                               BlockHitResult hitResult) {
        if (level.isClientSide()) return ItemInteractionResult.SUCCESS;

        if (!stack.is(ApicaItems.MAGIC_BEE.get())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ResonatorBlockEntity resonator)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (resonator.hasBee()) {
            // Swap: retirer l'ancienne abeille et placer la nouvelle
            ItemStack oldBee = resonator.removeBee();
            resonator.placeBee(stack);
            if (!player.isCreative()) {
                stack.shrink(1);
            }
            // Donner l'ancienne abeille au joueur
            if (!player.getInventory().add(oldBee)) {
                player.drop(oldBee, false);
            }
        } else {
            // Placer l'abeille
            resonator.placeBee(stack);
            if (!player.isCreative()) {
                stack.shrink(1);
            }
        }

        level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0f, 1.0f);
        return ItemInteractionResult.CONSUME;
    }

    // Clic droit sans item en main
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ResonatorBlockEntity resonator) {
                // Shift+clic droit: reprendre l'abeille (interdit pendant analyse)
                if (player.isShiftKeyDown() && resonator.hasBee() && !resonator.isAnalysisInProgress()) {
                    ItemStack removed = resonator.removeBee();
                    if (!removed.isEmpty()) {
                        if (!player.getInventory().add(removed)) {
                            player.drop(removed, false);
                        }
                        level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM,
                                SoundSource.BLOCKS, 1.0f, 1.0f);
                        return InteractionResult.CONSUME;
                    }
                }
                // Ouvrir le menu (analyse ou normal)
                if (player instanceof ServerPlayer serverPlayer) {
                    boolean analysisMode = false;
                    if (resonator.hasBee()) {
                        String speciesId = MagicBeeItem.getSpeciesId(resonator.getStoredBee());
                        if (speciesId != null) {
                            CodexPlayerData codex = serverPlayer.getData(ApicaAttachments.CODEX_DATA);
                            if (!codex.isSpeciesKnown(speciesId)) {
                                analysisMode = true;
                                if (!resonator.isAnalysisInProgress()) {
                                    resonator.startAnalysis(serverPlayer.getUUID());
                                }
                            }
                        }
                    }
                    final boolean sendAnalysisMode = analysisMode;
                    serverPlayer.openMenu(resonator, buf -> {
                        buf.writeBlockPos(pos);
                        ItemStack.OPTIONAL_STREAM_CODEC.encode(
                                (RegistryFriendlyByteBuf) buf, resonator.getStoredBee());
                        buf.writeBoolean(sendAnalysisMode);
                    });
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    // Drop l'abeille quand le bloc est casse
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ResonatorBlockEntity resonator && resonator.hasBee()) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        resonator.getStoredBee());
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
