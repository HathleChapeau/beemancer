/**
 * ============================================================
 * [RepairToolStationBlock.java]
 * Description: Bloc Repair Tool Station — slab qui repare les outils endommages
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                      | Raison                | Utilisation                    |
 * |---------------------------------|----------------------|--------------------------------|
 * | RepairToolStationBlockEntity    | BlockEntity          | Logique de reparation          |
 * | ApicaBlockEntities              | Registre BE          | Type de block entity           |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ApicaBlocks (registration)
 * - RepairToolStationBlockEntity (ticker)
 *
 * ============================================================
 */
package com.chapeau.apica.common.block.artifacts;

import com.chapeau.apica.common.blockentity.artifacts.RepairToolStationBlockEntity;
import com.chapeau.apica.core.registry.ApicaBlockEntities;
import com.mojang.serialization.MapCodec;
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
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidUtil;

import javax.annotation.Nullable;

public class RepairToolStationBlock extends BaseEntityBlock {
    public static final MapCodec<RepairToolStationBlock> CODEC = simpleCodec(RepairToolStationBlock::new);
    public static final BooleanProperty REPAIRING = BooleanProperty.create("repairing");

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 6, 16);

    public RepairToolStationBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(REPAIRING, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(REPAIRING);
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
        return new RepairToolStationBlockEntity(pos, state);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type,
                (BlockEntityType<RepairToolStationBlockEntity>) ApicaBlockEntities.REPAIR_TOOL_STATION.get(),
                RepairToolStationBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) return ItemInteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof RepairToolStationBlockEntity station)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // Try placing a damaged tool
        if (station.isEmpty() && stack.isDamageableItem() && stack.isDamaged()) {
            if (station.placeItem(stack)) {
                if (!player.isCreative()) {
                    stack.shrink(1);
                }
                level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0f, 1.0f);
                return ItemInteractionResult.CONSUME;
            }
        }

        // Try fluid interaction (bucket)
        if (FluidUtil.interactWithFluidHandler(player, hand, station.getFluidTank())) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof RepairToolStationBlockEntity station)) {
            return InteractionResult.PASS;
        }

        if (!station.isEmpty()) {
            ItemStack removed = station.removeItem();
            if (!removed.isEmpty()) {
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
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof RepairToolStationBlockEntity station && !station.isEmpty()) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        station.getStoredItem());
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
