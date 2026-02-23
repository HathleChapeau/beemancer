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

import com.chapeau.apica.client.particle.ParticleEmitter;
import com.chapeau.apica.common.blockentity.injector.InjectorBlockEntity;
import com.chapeau.apica.core.registry.ApicaBlockEntities;
import com.chapeau.apica.core.util.BeeInjectionHelper;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import org.joml.Vector3f;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class InjectorBlock extends BaseEntityBlock {

    public static final MapCodec<InjectorBlock> CODEC = simpleCodec(InjectorBlock::new);

    private static final VoxelShape SHAPE = Shapes.or(
        Block.box(0, 0, 0, 3, 6, 16),      // Mur ouest
        Block.box(0, 0, 0, 16, 6, 3),      // Mur nord
        Block.box(13, 0, 0, 16, 6, 16),    // Mur est
        Block.box(0, 0, 13, 16, 6, 16),    // Mur sud
        Block.box(3, 5, 3, 13, 6, 13),     // Plaque centrale
        Block.box(5, 6, 0, 11, 14, 3),     // Pilier nord
        Block.box(6, 9, 3, 10, 13, 4),     // Face projecteur nord
        Block.box(5, 6, 13, 11, 14, 16),   // Pilier sud
        Block.box(6, 9, 12, 10, 13, 13)    // Face projecteur sud
    );

    public InjectorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
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
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof InjectorBlockEntity injector)) return;

        ItemStack bee = injector.getItemHandler().getStackInSlot(InjectorBlockEntity.BEE_SLOT);
        ItemStack essence = injector.getItemHandler().getStackInSlot(InjectorBlockEntity.ESSENCE_SLOT);
        if (bee.isEmpty() || essence.isEmpty()) return;
        if (BeeInjectionHelper.isSatiated(bee)) return;

        DustParticleOptions pollen = new DustParticleOptions(new Vector3f(1.0f, 0.85f, 0.2f), 0.6f);

        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 11.0 / 16.0;

        // Particules pollen depuis le projecteur nord vers le centre
        new ParticleEmitter(pollen)
            .at(cx, cy, pos.getZ() + 3.5 / 16.0)
            .speed(0, 0, 0.025)
            .spread(0.1, 0.1, 0)
            .speedVariance(0.005, 0.005, 0.005)
            .count(1)
            .lifetime(12)
            .spawn(level);

        // Particules pollen depuis le projecteur sud vers le centre
        new ParticleEmitter(pollen)
            .at(cx, cy, pos.getZ() + 12.5 / 16.0)
            .speed(0, 0, -0.025)
            .spread(0.1, 0.1, 0)
            .speedVariance(0.005, 0.005, 0.005)
            .count(1)
            .lifetime(12)
            .spawn(level);
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
