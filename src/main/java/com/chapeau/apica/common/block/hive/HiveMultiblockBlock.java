/**
 * ============================================================
 * [HiveMultiblockBlock.java]
 * Description: Bloc ruche pour multibloc 3x3x3
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation           |
 * |-------------------------|----------------------|-----------------------|
 * | HiveMultiblockBlockEntity| Contrôleur           | Logique ruche         |
 * | MultiblockValidator     | Validation           | Vérification pattern  |
 * | MultiblockPatterns      | Pattern              | Définition structure  |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ApicaBlocks.java (registration)
 *
 * ============================================================
 */
package com.chapeau.apica.common.block.hive;

import com.chapeau.apica.core.multiblock.MultiblockProperty;
import com.chapeau.apica.core.registry.ApicaBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public class HiveMultiblockBlock extends BaseEntityBlock {
    public static final MapCodec<HiveMultiblockBlock> CODEC = simpleCodec(HiveMultiblockBlock::new);

    public static final EnumProperty<MultiblockProperty> MULTIBLOCK = MultiblockProperty.create("hive");
    public static final BooleanProperty CONTROLLER = BooleanProperty.create("controller");

    private static final VoxelShape SHAPE_NORMAL = Block.box(0, 0, 0, 16, 16, 16);
    private static final VoxelShape SHAPE_CONTROLLER = Block.box(-16, -16, -16, 32, 32, 32);

    public HiveMultiblockBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(MULTIBLOCK, MultiblockProperty.NONE)
                .setValue(CONTROLLER, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MULTIBLOCK, CONTROLLER);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        if (state.getValue(MULTIBLOCK) != MultiblockProperty.NONE && !state.getValue(CONTROLLER)) {
            return RenderShape.INVISIBLE;
        }
        return RenderShape.MODEL;
    }

    @Override
    protected int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        if (state.getValue(MULTIBLOCK) != MultiblockProperty.NONE && !state.getValue(CONTROLLER)) {
            return 0;
        }
        return super.getLightBlock(state, level, pos);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(MULTIBLOCK) != MultiblockProperty.NONE && state.getValue(CONTROLLER)) {
            return SHAPE_CONTROLLER;
        }
        return SHAPE_NORMAL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HiveMultiblockBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, ApicaBlockEntities.HIVE_MULTIBLOCK.get(),
            HiveMultiblockBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof HiveMultiblockBlockEntity hive) {
                HiveMultiblockBlockEntity master = hive.findOrBecomeController();
                if (master != null && master.isFormed()) {
                    serverPlayer.openMenu(master, buf -> buf.writeBoolean(true));
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof HiveMultiblockBlockEntity hive) {
                hive.onBroken();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
