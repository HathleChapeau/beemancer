/**
 * ============================================================
 * [PollenPotBlock.java]
 * Description: Pot pour stocker jusqu'à 16 pollens d'un seul type
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | PollenPotBlockEntity    | Stockage données     | Logique de stockage            |
 * | BeemancerBlockEntities  | Type de BlockEntity  | Enregistrement                 |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - BeemancerBlocks.java (enregistrement)
 * - PollenPotEvents.java (clic gauche)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.pollenpot;

import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
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

public class PollenPotBlock extends BaseEntityBlock {
    public static final MapCodec<PollenPotBlock> CODEC = simpleCodec(PollenPotBlock::new);

    // Shape du pot (plus petit qu'un bloc complet)
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(3, 0, 3, 13, 1, 13),   // Base
            Block.box(2, 1, 2, 14, 10, 14),  // Corps
            Block.box(1, 10, 1, 15, 12, 15)  // Rebord
    );

    public PollenPotBlock(Properties properties) {
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

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PollenPotBlockEntity(pos, state);
    }

    /**
     * Clic droit avec un item en main - ajouter du pollen.
     */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                               BlockPos pos, Player player, InteractionHand hand,
                                               BlockHitResult hit) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof PollenPotBlockEntity pot)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // Côté client: vérifier si l'action est possible pour feedback immédiat
        if (level.isClientSide()) {
            boolean isPollen = PollenPotBlockEntity.isPollen(stack);
            boolean canAccept = isPollen && pot.canAcceptPollen(stack);
            // Swing arm seulement si action valide
            return (canAccept || !isPollen) ? ItemInteractionResult.SUCCESS : ItemInteractionResult.CONSUME;
        }

        // Côté serveur
        // Vérifier si c'est du pollen
        if (!PollenPotBlockEntity.isPollen(stack)) {
            // Afficher le contenu du pot
            showPotContents(player, pot);
            return ItemInteractionResult.CONSUME;
        }

        // Essayer d'ajouter le pollen (addPollen vérifie déjà canAccept)
        if (pot.addPollen(stack)) {
            if (!player.isCreative()) {
                stack.shrink(1);
            }
            level.playSound(null, pos, SoundEvents.SAND_PLACE, SoundSource.BLOCKS, 0.5f, 1.2f);
            return ItemInteractionResult.SUCCESS;
        }

        // Échec - afficher la raison
        if (pot.getPollenCount() >= PollenPotBlockEntity.MAX_POLLEN) {
            player.displayClientMessage(
                    Component.translatable("message.beemancer.pollen_pot.full"),
                    true
            );
        } else if (!pot.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("message.beemancer.pollen_pot.wrong_type"),
                    true
            );
        }

        return ItemInteractionResult.CONSUME;
    }

    private void showPotContents(Player player, PollenPotBlockEntity pot) {
        if (pot.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable("message.beemancer.pollen_pot.empty"),
                    true
            );
        } else {
            player.displayClientMessage(
                    Component.translatable("message.beemancer.pollen_pot.contents",
                            pot.getPollenCount(),
                            PollenPotBlockEntity.MAX_POLLEN,
                            pot.getStoredPollenType().getHoverName()),
                    true
            );
        }
    }

    /**
     * Drop le contenu quand le bloc est cassé.
     */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PollenPotBlockEntity pot && !pot.isEmpty()) {
                ItemStack toDrop = pot.getStoredPollenType().copyWithCount(pot.getPollenCount());
                popResource(level, pos, toDrop);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    // =========================================================================
    // COMPARATOR SUPPORT
    // =========================================================================

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof PollenPotBlockEntity pot) {
            return pot.getComparatorOutput();
        }
        return 0;
    }
}
