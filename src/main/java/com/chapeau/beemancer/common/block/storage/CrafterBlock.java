/**
 * ============================================================
 * [CrafterBlock.java]
 * Description: Bloc Crafter pour le crafting automatique du reseau de stockage
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                | Raison                | Utilisation                    |
 * |---------------------------|----------------------|--------------------------------|
 * | CrafterBlockEntity        | BlockEntity associe  | Inventaire et logique          |
 * | StorageControllerBlockEntity | Controller link   | Notification unlink on remove  |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - BeemancerBlocks.java (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.storage;

import com.chapeau.beemancer.common.blockentity.storage.CrafterBlockEntity;
import com.chapeau.beemancer.common.blockentity.storage.StorageControllerBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class CrafterBlock extends BaseEntityBlock {

    public static final MapCodec<CrafterBlock> CODEC = simpleCodec(CrafterBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public CrafterBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
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
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrafterBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof CrafterBlockEntity crafter)) return InteractionResult.PASS;

        if (crafter.getControllerPos() == null) {
            player.displayClientMessage(
                    Component.translatable("message.beemancer.crafter.not_linked"), true);
            return InteractionResult.SUCCESS;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(crafter, crafter.getBlockPos());
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CrafterBlockEntity crafter) {
                // Drop inventory contents
                for (int i = 0; i < crafter.getInventory().getSlots(); i++) {
                    Containers.dropItemStack(level,
                            pos.getX(), pos.getY(), pos.getZ(),
                            crafter.getInventory().getStackInSlot(i));
                }

                // Drop craft buffer items
                for (net.minecraft.world.item.ItemStack buffered : crafter.getCraftBuffer()) {
                    Containers.dropItemStack(level,
                            pos.getX(), pos.getY(), pos.getZ(), buffered);
                }

                // Notify controller to unlink
                BlockPos ctrlPos = crafter.getControllerPos();
                if (ctrlPos != null && level.isLoaded(ctrlPos)) {
                    BlockEntity ctrlBe = level.getBlockEntity(ctrlPos);
                    if (ctrlBe instanceof StorageControllerBlockEntity controller) {
                        controller.unlinkCrafter();
                    }
                }
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
