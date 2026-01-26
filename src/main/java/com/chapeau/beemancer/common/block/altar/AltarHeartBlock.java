/**
 * ============================================================
 * [AltarHeartBlock.java]
 * Description: Coeur de l'Autel - Contrôleur du Honey Altar multibloc
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation           |
 * |-------------------------|----------------------|-----------------------|
 * | AltarHeartBlockEntity   | Stockage état        | Multibloc formé       |
 * | MultiblockValidator     | Validation pattern   | Vérification structure|
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - BeemancerBlocks.java (enregistrement)
 * - MultiblockPatterns.java (contrôleur du pattern)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.altar;

import com.chapeau.beemancer.common.blockentity.altar.AltarHeartBlockEntity;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * Coeur de l'Autel de Miel.
 * Sert de contrôleur pour le Honey Altar multibloc.
 * Clic droit pour tenter de former l'altar.
 */
public class AltarHeartBlock extends Block implements EntityBlock {

    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    // Forme: petit cube centré
    private static final VoxelShape SHAPE = Block.box(4, 4, 4, 12, 12, 12);

    public AltarHeartBlock(Properties properties) {
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

    /**
     * Quand FORMED=true, utilise ENTITYBLOCK_ANIMATED pour que le BlockEntityRenderer soit appelé.
     * Le modèle par défaut est caché et remplacé par le rendu custom de tout le multibloc.
     */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return state.getValue(FORMED) ? RenderShape.ENTITYBLOCK_ANIMATED : RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AltarHeartBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof AltarHeartBlockEntity heartBE)) {
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
            boolean success = heartBE.tryFormAltar();
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
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            // Le bloc est détruit - désactiver le multibloc
            if (state.getValue(FORMED)) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof AltarHeartBlockEntity heartBE) {
                    heartBE.onMultiblockBroken();
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
