/**
 * ============================================================
 * [HoneyPedestalBlock.java]
 * Description: Piedestal central du Honey Altar - stocke un item
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                  | Raison                | Utilisation           |
 * |-----------------------------|----------------------|-----------------------|
 * | HoneyPedestalBlockEntity    | Stockage item        | Clic droit            |
 * | BeemancerBlockEntities      | Type BlockEntity     | Enregistrement        |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - HoneyAltarMultiblock.java (validation pattern - centre base)
 * - BeemancerBlocks.java (enregistrement)
 * - AltarHeartBlockEntity.java (lecture item pour craft)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.altar;

import com.chapeau.beemancer.common.blockentity.altar.HoneyPedestalBlockEntity;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.chapeau.beemancer.core.multiblock.MultiblockProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import javax.annotation.Nullable;

/**
 * Piedestal au centre de la base du Honey Altar.
 * Forme de colonne/piedestal avec hitbox personnalisee.
 * Clic droit avec item = place l'item sur le piedestal.
 * Clic droit sans item = retire l'item.
 * Participe aux multiblocs Altar et Extractor.
 */
public class HoneyPedestalBlock extends Block implements EntityBlock {

    public static final EnumProperty<MultiblockProperty> MULTIBLOCK =
        MultiblockProperty.create("altar", "extractor");

    // Forme: colonne centrale
    private static final VoxelShape SHAPE = Shapes.or(
        // Base (plus large)
        Block.box(2, 0, 2, 14, 3, 14),
        // Colonne centrale
        Block.box(4, 3, 4, 12, 13, 12),
        // Top (plus large)
        Block.box(2, 13, 2, 14, 16, 14)
    );

    public HoneyPedestalBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(MULTIBLOCK, MultiblockProperty.NONE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MULTIBLOCK);
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
        return new HoneyPedestalBlockEntity(pos, state);
    }

    // Clic droit avec item en main
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                               BlockPos pos, Player player, InteractionHand hand,
                                               BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof HoneyPedestalBlockEntity pedestal)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // Si le pedestal est vide et on a un item, le placer
        if (pedestal.isEmpty() && !stack.isEmpty()) {
            if (pedestal.placeItem(stack)) {
                if (!player.isCreative()) {
                    stack.shrink(1);
                }
                level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0f, 1.0f);
                return ItemInteractionResult.CONSUME;
            }
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    // Clic droit sans item en main
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof HoneyPedestalBlockEntity pedestal)) {
            return InteractionResult.PASS;
        }

        // Si le pedestal a un item, le retirer
        if (!pedestal.isEmpty()) {
            ItemStack removed = pedestal.removeItem();
            if (!removed.isEmpty()) {
                // Donner l'item au joueur ou le drop
                if (!player.getInventory().add(removed)) {
                    player.drop(removed, false);
                }
                level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0f, 1.0f);
                return InteractionResult.CONSUME;
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            // Drop l'item stocke si le bloc est casse
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof HoneyPedestalBlockEntity pedestal && !pedestal.isEmpty()) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    pedestal.getStoredItem());
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
