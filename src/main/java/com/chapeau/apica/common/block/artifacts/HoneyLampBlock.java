/**
 * ============================================================
 * [HoneyLampBlock.java]
 * Description: Lampe décorative alimentée par fluides Apica
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | HoneyLampBlockEntity    | BlockEntity associé  | Luminosité dynamique + tank    |
 * | ApicaBlockEntities      | Registre BE          | Ticker                         |
 * ------------------------------------------------------------
 *
 * PATTERN: Create FluidTankBlock
 * - getLightEmission() override lisant be.luminosity
 * - Pas de lightLevel() dans les properties
 *
 * UTILISÉ PAR:
 * - ApicaBlocks (registre)
 * - ApicaItems (block item)
 *
 * ============================================================
 */
package com.chapeau.apica.common.block.artifacts;

import com.chapeau.apica.common.blockentity.artifacts.HoneyLampBlockEntity;
import com.chapeau.apica.core.registry.ApicaBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;

public class HoneyLampBlock extends BaseEntityBlock {
    public static final MapCodec<HoneyLampBlock> CODEC = simpleCodec(HoneyLampBlock::new);

    public static final BooleanProperty PIPE_UP = BooleanProperty.create("pipe_up");

    private static final VoxelShape SHAPE = Shapes.or(
        Block.box(3, 0, 3, 13, 13, 13),
        Block.box(5, 13, 5, 11, 15, 11)
    );
    private static final VoxelShape SHAPE_RAISED = Shapes.or(
        Block.box(3, 1, 3, 13, 14, 13),
        Block.box(5, 14, 5, 11, 16, 11)
    );

    public HoneyLampBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(PIPE_UP, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(PIPE_UP) ? SHAPE_RAISED : SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PIPE_UP);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        boolean pipeAbove = context.getLevel().getBlockState(context.getClickedPos().above()).getBlock()
                instanceof com.chapeau.apica.common.block.alchemy.AbstractPipeBlock;
        return this.defaultBlockState().setValue(PIPE_UP, pipeAbove);
    }

    /**
     * Pattern Create FluidTankBlock: luminosité lue depuis le BlockEntity.
     */
    @Override
    public int getLightEmission(BlockState state, BlockGetter world, BlockPos pos) {
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof HoneyLampBlockEntity lamp) {
            return lamp.getLuminosity();
        }
        return 0;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HoneyLampBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, ApicaBlockEntities.HONEY_LAMP.get(),
                HoneyLampBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof HoneyLampBlockEntity lamp) {
                if (player.isShiftKeyDown() && stack.isEmpty()) {
                    com.chapeau.apica.core.util.IDrainable.tryDrain(level, pos, player, lamp);
                    return ItemInteractionResult.sidedSuccess(level.isClientSide());
                }

                if (FluidUtil.interactWithFluidHandler(player, hand, lamp.getFluidTank())) {
                    return ItemInteractionResult.sidedSuccess(level.isClientSide());
                }
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof HoneyLampBlockEntity lamp) {
                lamp.removeAllHelpers();
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /**
     * Etats visuels de la lampe correspondant au fluide contenu.
     */
    public enum LampState implements StringRepresentable {
        OFF("off"),
        HONEY("honey"),
        ROYAL_JELLY("royal_jelly"),
        NECTAR("nectar");

        private final String name;

        LampState(String name) { this.name = name; }

        @Override
        public String getSerializedName() { return this.name; }
    }
}
