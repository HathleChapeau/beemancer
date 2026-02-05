/**
 * ============================================================
 * [CentrifugeHeartBlock.java]
 * Description: Coeur de la Centrifugeuse - Controleur du multibloc Centrifuge
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                  | Raison                | Utilisation           |
 * |-----------------------------|----------------------|-----------------------|
 * | CentrifugeHeartBlockEntity  | Stockage etat        | Multibloc + processing|
 * | MultiblockProperty          | Etat multibloc       | Blockstate            |
 * | BeemancerBlockEntities      | Type registration    | Ticker                |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - BeemancerBlocks.java (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.alchemy;

import com.chapeau.beemancer.common.blockentity.alchemy.CentrifugeHeartBlockEntity;
import com.chapeau.beemancer.core.multiblock.MultiblockProperty;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * Coeur de la Centrifugeuse multibloc.
 * Clic droit: forme le multibloc ou ouvre le menu si deja forme.
 *
 * Multibloc 3x3x3: Le coeur est au centre (Y+0), avec Honeyed Stone autour.
 * Etage Y-1: 3x3 Honeyed Stone
 * Etage Y+0: Coeur au centre, air autour
 * Etage Y+1: 3x3 Honeyed Stone
 */
public class CentrifugeHeartBlock extends Block implements EntityBlock {

    public static final EnumProperty<MultiblockProperty> MULTIBLOCK = MultiblockProperty.create("centrifuge");
    public static final BooleanProperty WORKING = BooleanProperty.create("working");

    // VoxelShape pour le coeur seul (non-forme)
    private static final VoxelShape SHAPE_CORE = Block.box(4, 4, 4, 12, 12, 12);

    // VoxelShape complete du multibloc forme (3x3x3 blocs)
    // Le coeur est au centre, donc la shape va de -1 bloc a +2 blocs sur chaque axe
    // En pixels: -16 a 32 sur X/Z, -16 a 32 sur Y
    private static final VoxelShape SHAPE_FORMED = Shapes.or(
        // Etage Y-1 (sol): 3x3 blocs pleins, Y de -16 a 0
        Block.box(-16, -16, -16, 32, 0, 32),
        // Etage Y+0 (coeur): seulement le cube central visible, rendu par BER
        SHAPE_CORE,
        // Etage Y+1 (toit): 3x3 blocs pleins, Y de 16 a 32
        Block.box(-16, 16, -16, 32, 32, 32)
    );

    public CentrifugeHeartBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(MULTIBLOCK, MultiblockProperty.NONE)
            .setValue(WORKING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MULTIBLOCK, WORKING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(MULTIBLOCK) != MultiblockProperty.NONE) {
            return SHAPE_FORMED;
        }
        return SHAPE_CORE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(MULTIBLOCK) != MultiblockProperty.NONE) {
            return SHAPE_FORMED;
        }
        return SHAPE_CORE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CentrifugeHeartBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != BeemancerBlockEntities.CENTRIFUGE_HEART.get()) {
            return null;
        }

        if (level.isClientSide()) {
            return (lvl, pos, st, be) -> CentrifugeHeartBlockEntity.clientTick(lvl, pos, st, (CentrifugeHeartBlockEntity) be);
        } else {
            return (lvl, pos, st, be) -> CentrifugeHeartBlockEntity.serverTick(lvl, pos, st, (CentrifugeHeartBlockEntity) be);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof CentrifugeHeartBlockEntity heartBE)) {
            return InteractionResult.PASS;
        }

        if (state.getValue(MULTIBLOCK) != MultiblockProperty.NONE) {
            player.openMenu(heartBE, pos);
            return InteractionResult.CONSUME;
        } else {
            boolean success = heartBE.tryFormMultiblock();
            if (success) {
                player.displayClientMessage(
                    Component.translatable("message.beemancer.centrifuge.formed"), true);
            } else {
                player.displayClientMessage(
                    Component.translatable("message.beemancer.centrifuge.invalid_structure"), true);
            }
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CentrifugeHeartBlockEntity heartBE) {
                if (state.getValue(MULTIBLOCK) != MultiblockProperty.NONE) {
                    heartBE.onMultiblockBroken();
                }
                for (int i = 0; i < heartBE.getInputSlot().getSlots(); i++) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(),
                        heartBE.getInputSlot().getStackInSlot(i));
                }
                for (int i = 0; i < heartBE.getOutputSlots().getSlots(); i++) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(),
                        heartBE.getOutputSlots().getStackInSlot(i));
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
