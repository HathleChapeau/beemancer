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
import com.chapeau.apica.core.registry.ApicaParticles;
import com.chapeau.apica.core.util.BeeInjectionHelper;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class InjectorBlock extends BaseEntityBlock {

    public static final MapCodec<InjectorBlock> CODEC = simpleCodec(InjectorBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    // NORTH (base): projecteurs sur axe Z
    private static final VoxelShape SHAPE_NORTH = Shapes.or(
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

    // EAST (90 CW): projecteurs sur axe X
    private static final VoxelShape SHAPE_EAST = Shapes.or(
        Block.box(0, 0, 0, 16, 6, 3),      // Mur ouest (was Z)
        Block.box(13, 0, 0, 16, 6, 16),    // Mur nord (was X)
        Block.box(0, 0, 13, 16, 6, 16),    // Mur est (was Z)
        Block.box(0, 0, 0, 3, 6, 16),      // Mur sud (was X)
        Block.box(3, 5, 3, 13, 6, 13),     // Plaque centrale
        Block.box(13, 6, 5, 16, 14, 11),   // Pilier est
        Block.box(12, 9, 6, 13, 13, 10),   // Face projecteur est
        Block.box(0, 6, 5, 3, 14, 11),     // Pilier ouest
        Block.box(3, 9, 6, 4, 13, 10)      // Face projecteur ouest
    );

    // SOUTH (180): projecteurs sur axe Z inverse
    private static final VoxelShape SHAPE_SOUTH = Shapes.or(
        Block.box(13, 0, 0, 16, 6, 16),    // Mur ouest (was est)
        Block.box(0, 0, 13, 16, 6, 16),    // Mur nord (was sud)
        Block.box(0, 0, 0, 3, 6, 16),      // Mur est (was ouest)
        Block.box(0, 0, 0, 16, 6, 3),      // Mur sud (was nord)
        Block.box(3, 5, 3, 13, 6, 13),     // Plaque centrale
        Block.box(5, 6, 13, 11, 14, 16),   // Pilier sud (was nord)
        Block.box(6, 9, 12, 10, 13, 13),   // Face projecteur sud
        Block.box(5, 6, 0, 11, 14, 3),     // Pilier nord (was sud)
        Block.box(6, 9, 3, 10, 13, 4)      // Face projecteur nord
    );

    // WEST (270 CW): projecteurs sur axe X inverse
    private static final VoxelShape SHAPE_WEST = Shapes.or(
        Block.box(0, 0, 13, 16, 6, 16),    // Mur ouest (was Z)
        Block.box(0, 0, 0, 3, 6, 16),      // Mur nord (was X)
        Block.box(0, 0, 0, 16, 6, 3),      // Mur est (was Z)
        Block.box(13, 0, 0, 16, 6, 16),    // Mur sud (was X)
        Block.box(3, 5, 3, 13, 6, 13),     // Plaque centrale
        Block.box(0, 6, 5, 3, 14, 11),     // Pilier ouest
        Block.box(3, 9, 6, 4, 13, 10),     // Face projecteur ouest
        Block.box(13, 6, 5, 16, 14, 11),   // Pilier est
        Block.box(12, 9, 6, 13, 13, 10)    // Face projecteur est
    );

    public InjectorBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case EAST -> SHAPE_EAST;
            case SOUTH -> SHAPE_SOUTH;
            case WEST -> SHAPE_WEST;
            default -> SHAPE_NORTH;
        };
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

        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 11.0 / 16.0;
        double cz = pos.getZ() + 0.5;

        Direction facing = state.getValue(FACING);
        boolean zAxis = (facing == Direction.NORTH || facing == Direction.SOUTH);

        // Projecteur 1 (near side): runes vers le centre
        double x1 = zAxis ? cx : (cx - 0.5 + 3.5 / 16.0);
        double z1 = zAxis ? (pos.getZ() + 3.5 / 16.0) : cz;
        double sx1 = zAxis ? 0 : 0.025;
        double sz1 = zAxis ? 0.025 : 0;

        new ParticleEmitter(ApicaParticles.RUNE.get())
            .at(x1, cy, z1)
            .speed(sx1, 0, sz1)
            .spread(0.1, 0.05, 0.1)
            .speedVariance(0.005, 0.005, 0.005)
            .gravity(0f)
            .scale(0.03f)
            .count(1)
            .lifetime(12)
            .fadeOut()
            .fullBright()
            .spawn(level);

        // Projecteur 2 (far side): runes vers le centre
        double x2 = zAxis ? cx : (cx - 0.5 + 12.5 / 16.0);
        double z2 = zAxis ? (pos.getZ() + 12.5 / 16.0) : cz;
        double sx2 = zAxis ? 0 : -0.025;
        double sz2 = zAxis ? -0.025 : 0;

        new ParticleEmitter(ApicaParticles.RUNE.get())
            .at(x2, cy, z2)
            .speed(sx2, 0, sz2)
            .spread(0.1, 0.05, 0.1)
            .speedVariance(0.005, 0.005, 0.005)
            .gravity(0f)
            .scale(0.03f)
            .count(1)
            .lifetime(12)
            .fadeOut()
            .fullBright()
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
