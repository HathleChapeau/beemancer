/**
 * ============================================================
 * [HoneyCrystalBlock.java]
 * Description: Cristal de miel - Contrôleur du Honey Altar multibloc
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation           |
 * |---------------------|----------------------|-----------------------|
 * | HoneyCrystalBlockEntity | Stockage état    | Multibloc formé       |
 * | HoneyAltarMultiblock | Validation pattern  | Vérification structure |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - BeemancerBlocks.java (enregistrement)
 * - HoneyAltarMultiblock.java (centre du pattern)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.altar;

import com.chapeau.beemancer.common.blockentity.altar.HoneyCrystalBlockEntity;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * Cristal de miel placeable.
 * Sert de contrôleur pour le Honey Altar multibloc.
 * Clic droit pour tenter de former l'altar.
 */
public class HoneyCrystalBlock extends Block implements EntityBlock {

    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    // Forme: cristal pointu
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 14, 13);

    public HoneyCrystalBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HoneyCrystalBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof HoneyCrystalBlockEntity crystalBE)) {
            return InteractionResult.PASS;
        }

        // Tenter de former/vérifier l'altar
        if (state.getValue(FORMED)) {
            // Déjà formé - afficher statut
            player.displayClientMessage(
                Component.translatable("message.beemancer.honey_altar.already_formed"),
                true
            );
        } else {
            // Tenter la formation
            boolean success = crystalBE.tryFormAltar();
            if (success) {
                player.displayClientMessage(
                    Component.translatable("message.beemancer.honey_altar.formed"),
                    true
                );
            } else {
                player.displayClientMessage(
                    Component.translatable("message.beemancer.honey_altar.invalid_structure"),
                    true
                );
            }
        }

        return InteractionResult.CONSUME;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                               BlockPos pos, Player player, InteractionHand hand,
                                               BlockHitResult hitResult) {
        // Déléguer à useWithoutItem si main vide ou item non pertinent
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            // Le bloc est détruit - désactiver le multibloc
            if (state.getValue(FORMED)) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof HoneyCrystalBlockEntity crystalBE) {
                    crystalBE.onAltarBroken();
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
