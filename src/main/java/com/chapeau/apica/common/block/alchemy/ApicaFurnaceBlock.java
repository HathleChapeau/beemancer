/**
 * ============================================================
 * [ApicaFurnaceBlock.java]
 * Description: Four alimenté par fluide (honey, royal jelly, nectar)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                  | Raison                | Utilisation                    |
 * |-----------------------------|----------------------|--------------------------------|
 * | ApicaFurnaceBlockEntity     | BlockEntity          | Logique de cuisson             |
 * | ApicaBlockEntities          | Registre BE          | Type de block entity           |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ApicaBlocks (registration)
 * - ApicaFurnaceBlockEntity (ticker)
 *
 * ============================================================
 */
package com.chapeau.apica.common.block.alchemy;

import com.chapeau.apica.common.blockentity.alchemy.ApicaFurnaceBlockEntity;
import com.chapeau.apica.core.registry.ApicaBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public class ApicaFurnaceBlock extends BaseEntityBlock {
    public static final MapCodec<ApicaFurnaceBlock> CODEC = simpleCodec(ApicaFurnaceBlock::new);
    public static final BooleanProperty WORKING = BooleanProperty.create("working");
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private final int tier;

    public ApicaFurnaceBlock(Properties properties) {
        this(properties, 1);
    }

    public ApicaFurnaceBlock(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(WORKING, false)
                .setValue(FACING, Direction.NORTH));
    }

    public int getTier() { return tier; }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WORKING, FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return switch (tier) {
            case 2 -> ApicaFurnaceBlockEntity.createRoyal(pos, state);
            case 3 -> ApicaFurnaceBlockEntity.createNectar(pos, state);
            default -> new ApicaFurnaceBlockEntity(pos, state);
        };
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        BlockEntityType<?> expectedType = switch (tier) {
            case 2 -> ApicaBlockEntities.ROYAL_FURNACE.get();
            case 3 -> ApicaBlockEntities.NECTAR_FURNACE.get();
            default -> ApicaBlockEntities.HONEY_FURNACE.get();
        };
        return createTickerHelper(type, (BlockEntityType<ApicaFurnaceBlockEntity>) expectedType,
            ApicaFurnaceBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ApicaFurnaceBlockEntity furnace) {
                serverPlayer.openMenu(furnace, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
