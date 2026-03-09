/**
 * ============================================================
 * [DubstepRadioBlock.java]
 * Description: Bloc Dubstep Radio — table de mix DAW ouvrant un sequenceur musical
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance               | Raison                | Utilisation                    |
 * |--------------------------|----------------------|--------------------------------|
 * | DubstepRadioBlockEntity  | BlockEntity associe  | Stockage sequence, menu        |
 * | BaseEntityBlock          | Support BlockEntity  | newBlockEntity()               |
 * | ApicaBlockEntities       | Type enregistre      | getTicker()                    |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ApicaBlocks (enregistrement)
 * - ApicaItems (BlockItem)
 *
 * ============================================================
 */
package com.chapeau.apica.common.block.radio;

import com.chapeau.apica.core.network.packets.DubstepRadioSyncPacket;
import com.chapeau.apica.core.registry.ApicaBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;

public class DubstepRadioBlock extends BaseEntityBlock {

    public static final MapCodec<DubstepRadioBlock> CODEC = simpleCodec(DubstepRadioBlock::new);
    public static final BooleanProperty PLAYING = BooleanProperty.create("playing");
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    /** Wave Mixer : base etagee + colonne + tete inclinee. */
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0, 0, 0, 16, 2, 16),
            Block.box(3, 2, 3, 13, 4, 13),
            Block.box(4, 4, 4, 12, 12, 12),
            Block.box(0, 12, 0, 16, 17, 16)
    );

    public DubstepRadioBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(PLAYING, false)
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PLAYING, FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
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
        return new DubstepRadioBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, ApicaBlockEntities.DUBSTEP_RADIO.get(),
                DubstepRadioBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof DubstepRadioBlockEntity radio) {
                serverPlayer.openMenu(radio, buf -> buf.writeBlockPos(pos));
                PacketDistributor.sendToPlayer(serverPlayer,
                        new DubstepRadioSyncPacket(pos, radio.getSequenceData().save()));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(PLAYING)) return;
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.9;
        double z = pos.getZ() + 0.5;
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.NOTE,
                    x + (random.nextDouble() - 0.5) * 0.6,
                    y,
                    z + (random.nextDouble() - 0.5) * 0.6,
                    random.nextInt(25) / 24.0, 0, 0);
        }
    }
}
