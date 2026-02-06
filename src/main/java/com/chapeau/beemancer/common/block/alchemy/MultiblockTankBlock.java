/**
 * ============================================================
 * [MultiblockTankBlock.java]
 * Description: Bloc tank multibloc cube dynamique (2x2x2 minimum)
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance                    | Raison                  | Utilisation                    |
 * |-------------------------------|------------------------|--------------------------------|
 * | MultiblockTankBlockEntity     | BlockEntity associé    | Logique multibloc              |
 * | MultiblockProperty            | État multibloc         | Blockstate formed/not formed   |
 * | BeemancerBlockEntities        | Registre               | Ticker                         |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - MultiblockTankBlockEntity.java (référence bloc)
 * - MultiblockTankRenderer.java (lecture blockstate)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.alchemy;

import com.chapeau.beemancer.common.blockentity.alchemy.MultiblockTankBlockEntity;
import com.chapeau.beemancer.core.multiblock.MultiblockProperty;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public class MultiblockTankBlock extends BaseEntityBlock {
    public static final MapCodec<MultiblockTankBlock> CODEC = simpleCodec(MultiblockTankBlock::new);

    // Propriété blockstate: NONE = non formé, TANK = formé
    public static final EnumProperty<MultiblockProperty> MULTIBLOCK = MultiblockProperty.create(MultiblockProperty.TANK);

    public MultiblockTankBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(MULTIBLOCK, MultiblockProperty.NONE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MULTIBLOCK);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // ENTITYBLOCK_ANIMATED: le renderer gère tout (modèle scalé + fluide)
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MultiblockTankBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, BeemancerBlockEntities.MULTIBLOCK_TANK.get(),
            MultiblockTankBlockEntity::serverTick);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MultiblockTankBlockEntity tank) {
                tank.onPlaced();
            }
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MultiblockTankBlockEntity tank) {
                tank.onBroken();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof MultiblockTankBlockEntity tank) {
                // Chercher le master
                MultiblockTankBlockEntity master = tank.getMaster();
                if (master != null && master.isFormed()) {
                    serverPlayer.openMenu(master, buf -> buf.writeBlockPos(master.getBlockPos()));
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
