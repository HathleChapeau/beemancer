/**
 * ============================================================
 * [HoneyReservoirBlock.java]
 * Description: Bloc réservoir pour l'Honey Altar, stocke miel/royal jelly/nectar
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation           |
 * |-------------------------|----------------------|-----------------------|
 * | HoneyReservoirBlockEntity | Stockage fluide    | BlockEntity           |
 * | BeemancerBlockEntities  | Registration        | newBlockEntity        |
 * | BeemancerFluids         | Fluides acceptés    | Validation bucket     |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - BeemancerBlocks.java (registration)
 * - MultiblockPatterns.java (Honey Altar Y+2)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.altar;

import com.chapeau.beemancer.common.blockentity.altar.HoneyReservoirBlockEntity;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;

/**
 * Bloc réservoir horizontal pour l'Honey Altar.
 * Stocke miel, royal jelly ou nectar (un seul type à la fois).
 * Affiche le niveau de fluide via blockstate FLUID_LEVEL.
 */
public class HoneyReservoirBlock extends BaseEntityBlock {
    public static final MapCodec<HoneyReservoirBlock> CODEC = simpleCodec(HoneyReservoirBlock::new);

    public static final IntegerProperty FLUID_LEVEL = IntegerProperty.create("fluid_level", 0, 4);
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    // Forme horizontale: réservoir allongé
    private static final VoxelShape SHAPE = Shapes.or(
        // Capuchon fer gauche
        Block.box(0, 4, 4, 3, 12, 12),
        // Corps verre central
        Block.box(3, 4, 4, 13, 12, 12),
        // Capuchon fer droit
        Block.box(13, 4, 4, 16, 12, 12)
    );

    public HoneyReservoirBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(FLUID_LEVEL, 0)
            .setValue(FORMED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FLUID_LEVEL, FORMED);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /**
     * Quand FORMED=true, le modèle est caché - rendu par AltarHeartRenderer.
     */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return state.getValue(FORMED) ? RenderShape.INVISIBLE : RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HoneyReservoirBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, BeemancerBlockEntities.HONEY_RESERVOIR.get(),
            (lvl, pos, st, be) -> be.serverTick());
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof HoneyReservoirBlockEntity reservoir)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // Try bucket interaction via FluidUtil
        boolean success = FluidUtil.interactWithFluidHandler(player, hand, level, pos, null);
        if (success) {
            reservoir.updateFluidLevel();
            return ItemInteractionResult.SUCCESS;
        }

        // Shift+clic droit avec main vide pour vider (si multiblock formé)
        if (player.isShiftKeyDown() && stack.isEmpty()) {
            if (reservoir.isPartOfFormedMultiblock()) {
                FluidStack drained = reservoir.drain(FluidType.BUCKET_VOLUME, IFluidHandler.FluidAction.EXECUTE);
                if (!drained.isEmpty()) {
                    reservoir.updateFluidLevel();
                    return ItemInteractionResult.SUCCESS;
                }
            }
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            // Le fluide reste dans le BlockEntity pour être droppé si nécessaire
            super.onRemove(state, level, pos, newState, movedByPiston);
        }
    }
}
