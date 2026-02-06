/**
 * ============================================================
 * [InfuserHeartBlock.java]
 * Description: Coeur de l'Infuser - Controleur du multibloc Infuser
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                | Raison                | Utilisation           |
 * |---------------------------|----------------------|-----------------------|
 * | InfuserHeartBlockEntity   | Stockage etat        | Multibloc + processing|
 * | MultiblockProperty        | Etat multibloc       | Blockstate            |
 * | BeemancerBlockEntities    | Type registration    | Ticker                |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - BeemancerBlocks.java (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.alchemy;

import com.chapeau.beemancer.common.blockentity.alchemy.InfuserHeartBlockEntity;
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
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * Coeur de l'Infuser multibloc.
 * Clic droit: forme le multibloc ou ouvre le menu si deja forme.
 *
 * Multibloc 3x3x3 (meme layout que la centrifuge):
 * Etage Y-1: Reservoirs aux cardinaux (output) + Honeyed Stone coins/centre
 * Etage Y+0: Coeur au centre, air autour
 * Etage Y+1: Reservoirs aux cardinaux (input) + Honeyed Stone coins/centre
 */
public class InfuserHeartBlock extends Block implements EntityBlock {

    public static final EnumProperty<MultiblockProperty> MULTIBLOCK = MultiblockProperty.create("infuser");
    public static final BooleanProperty WORKING = BooleanProperty.create("working");

    // VoxelShape pour le coeur seul (non-forme): petit cube 8x8x8
    private static final VoxelShape SHAPE_CORE = Block.box(4, 4, 4, 12, 12, 12);

    // VoxelShape pour les 2 cubes centraux du multibloc forme: 24x24x24 centre
    // De -4 a 20 sur chaque axe (24 pixels = 1.5 bloc)
    private static final VoxelShape SHAPE_CUBES = Block.box(-4, -4, -4, 20, 20, 20);

    public InfuserHeartBlock(Properties properties) {
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
        // Quand forme: le BER rend le core, le blockstate model rend le frame
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(MULTIBLOCK) != MultiblockProperty.NONE) {
            return SHAPE_CUBES;
        }
        return SHAPE_CORE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(MULTIBLOCK) != MultiblockProperty.NONE) {
            return SHAPE_CUBES;
        }
        return SHAPE_CORE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new InfuserHeartBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return type == BeemancerBlockEntities.INFUSER_HEART.get()
            ? (lvl, pos, st, be) -> InfuserHeartBlockEntity.serverTick(lvl, pos, st, (InfuserHeartBlockEntity) be)
            : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof InfuserHeartBlockEntity heartBE)) {
            return InteractionResult.PASS;
        }

        if (state.getValue(MULTIBLOCK) != MultiblockProperty.NONE) {
            player.openMenu(heartBE, pos);
            return InteractionResult.CONSUME;
        } else {
            boolean success = heartBE.tryFormMultiblock();
            if (success) {
                player.displayClientMessage(
                    Component.translatable("message.beemancer.infuser.formed"), true);
            } else {
                player.displayClientMessage(
                    Component.translatable("message.beemancer.infuser.invalid_structure"), true);
            }
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof InfuserHeartBlockEntity heartBE) {
                if (state.getValue(MULTIBLOCK) != MultiblockProperty.NONE) {
                    heartBE.onMultiblockBroken();
                }
                for (int i = 0; i < heartBE.getInputSlot().getSlots(); i++) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(),
                        heartBE.getInputSlot().getStackInSlot(i));
                }
                for (int i = 0; i < heartBE.getOutputSlot().getSlots(); i++) {
                    Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(),
                        heartBE.getOutputSlot().getStackInSlot(i));
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
