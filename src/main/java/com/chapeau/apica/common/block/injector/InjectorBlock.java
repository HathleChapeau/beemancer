/**
 * ============================================================
 * [InjectorBlock.java]
 * Description: Bloc injecteur d'essence pour ameliorer les stats des abeilles
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | ApicaBlockEntities      | Type BlockEntity     | Ticker, creation               |
 * | InjectorBlockEntity     | Logic metier         | BlockEntity associe            |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ApicaBlocks.java (enregistrement)
 * - ApicaItems.java (block item)
 *
 * ============================================================
 */
package com.chapeau.apica.common.block.injector;

import com.chapeau.apica.common.blockentity.injector.InjectorBlockEntity;
import com.chapeau.apica.core.registry.ApicaBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class InjectorBlock extends BaseEntityBlock {

    public static final MapCodec<InjectorBlock> CODEC = simpleCodec(InjectorBlock::new);

    public InjectorBlock(Properties properties) {
        super(properties);
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
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new InjectorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, ApicaBlockEntities.INJECTOR.get(), InjectorBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof InjectorBlockEntity injector) {
                serverPlayer.openMenu(injector, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof InjectorBlockEntity injector) {
                injector.dropContents();
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
