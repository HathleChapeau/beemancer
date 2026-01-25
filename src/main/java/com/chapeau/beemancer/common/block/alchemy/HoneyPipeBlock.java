/**
 * ============================================================
 * [HoneyPipeBlock.java]
 * Description: Pipe pour transporter les fluides Beemancer
 * ============================================================
 * 
 * FONCTIONNEMENT:
 * - Se connecte automatiquement aux blocs avec FluidHandler
 * - Clic droit sur une face connectée = toggle mode extraction
 * - Mode extraction: tire le fluide du bloc connecté
 * - Mode normal: pousse le fluide vers les blocs connectés
 * ============================================================
 */
package com.chapeau.beemancer.common.block.alchemy;

import com.chapeau.beemancer.common.blockentity.alchemy.HoneyPipeBlockEntity;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;

import javax.annotation.Nullable;

public class HoneyPipeBlock extends BaseEntityBlock {
    public static final MapCodec<HoneyPipeBlock> CODEC = simpleCodec(HoneyPipeBlock::new);

    private final int tier;
    
    // Connection properties
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    
    // Extraction properties
    public static final BooleanProperty EXTRACT_NORTH = BooleanProperty.create("extract_north");
    public static final BooleanProperty EXTRACT_SOUTH = BooleanProperty.create("extract_south");
    public static final BooleanProperty EXTRACT_EAST = BooleanProperty.create("extract_east");
    public static final BooleanProperty EXTRACT_WEST = BooleanProperty.create("extract_west");
    public static final BooleanProperty EXTRACT_UP = BooleanProperty.create("extract_up");
    public static final BooleanProperty EXTRACT_DOWN = BooleanProperty.create("extract_down");

    private static final VoxelShape CORE = Block.box(5, 5, 5, 11, 11, 11);
    private static final VoxelShape NORTH_SHAPE = Block.box(5, 5, 0, 11, 11, 5);
    private static final VoxelShape SOUTH_SHAPE = Block.box(5, 5, 11, 11, 11, 16);
    private static final VoxelShape EAST_SHAPE = Block.box(11, 5, 5, 16, 11, 11);
    private static final VoxelShape WEST_SHAPE = Block.box(0, 5, 5, 5, 11, 11);
    private static final VoxelShape UP_SHAPE = Block.box(5, 11, 5, 11, 16, 11);
    private static final VoxelShape DOWN_SHAPE = Block.box(5, 0, 5, 11, 5, 11);

    public HoneyPipeBlock(Properties properties) {
        this(properties, 1);
    }

    public HoneyPipeBlock(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(NORTH, false).setValue(SOUTH, false)
            .setValue(EAST, false).setValue(WEST, false)
            .setValue(UP, false).setValue(DOWN, false)
            .setValue(EXTRACT_NORTH, false).setValue(EXTRACT_SOUTH, false)
            .setValue(EXTRACT_EAST, false).setValue(EXTRACT_WEST, false)
            .setValue(EXTRACT_UP, false).setValue(EXTRACT_DOWN, false));
    }

    public int getTier() { return tier; }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
        builder.add(EXTRACT_NORTH, EXTRACT_SOUTH, EXTRACT_EAST, EXTRACT_WEST, EXTRACT_UP, EXTRACT_DOWN);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CORE;
        if (state.getValue(NORTH)) shape = Shapes.or(shape, NORTH_SHAPE);
        if (state.getValue(SOUTH)) shape = Shapes.or(shape, SOUTH_SHAPE);
        if (state.getValue(EAST)) shape = Shapes.or(shape, EAST_SHAPE);
        if (state.getValue(WEST)) shape = Shapes.or(shape, WEST_SHAPE);
        if (state.getValue(UP)) shape = Shapes.or(shape, UP_SHAPE);
        if (state.getValue(DOWN)) shape = Shapes.or(shape, DOWN_SHAPE);
        return shape;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        // Determine which face was clicked based on hit location
        Direction clickedFace = getClickedConnectionFace(state, pos, hit);
        if (clickedFace == null) {
            return InteractionResult.PASS;
        }

        // Toggle extraction mode for that direction
        BooleanProperty extractProp = getExtractProperty(clickedFace);
        boolean newValue = !state.getValue(extractProp);
        level.setBlock(pos, state.setValue(extractProp, newValue), 3);

        // Feedback
        level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.5f, newValue ? 1.2f : 0.8f);
        player.displayClientMessage(
            Component.literal(clickedFace.getName() + ": " + (newValue ? "Extraction" : "Insertion")), 
            true);

        return InteractionResult.SUCCESS;
    }

    @Nullable
    private Direction getClickedConnectionFace(BlockState state, BlockPos pos, BlockHitResult hit) {
        Vec3 hitVec = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
        
        // Check each connected direction
        for (Direction dir : Direction.values()) {
            if (!isConnected(state, dir)) continue;
            
            AABB connectionBox = getConnectionBounds(dir);
            if (connectionBox.contains(hitVec)) {
                return dir;
            }
        }
        return null;
    }

    private AABB getConnectionBounds(Direction dir) {
        return switch (dir) {
            case NORTH -> new AABB(5/16.0, 5/16.0, 0, 11/16.0, 11/16.0, 5/16.0);
            case SOUTH -> new AABB(5/16.0, 5/16.0, 11/16.0, 11/16.0, 11/16.0, 1);
            case EAST -> new AABB(11/16.0, 5/16.0, 5/16.0, 1, 11/16.0, 11/16.0);
            case WEST -> new AABB(0, 5/16.0, 5/16.0, 5/16.0, 11/16.0, 11/16.0);
            case UP -> new AABB(5/16.0, 11/16.0, 5/16.0, 11/16.0, 1, 11/16.0);
            case DOWN -> new AABB(5/16.0, 0, 5/16.0, 11/16.0, 5/16.0, 11/16.0);
        };
    }

    public static BooleanProperty getConnectionProperty(Direction dir) {
        return switch (dir) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
            case UP -> UP;
            case DOWN -> DOWN;
        };
    }

    public static BooleanProperty getExtractProperty(Direction dir) {
        return switch (dir) {
            case NORTH -> EXTRACT_NORTH;
            case SOUTH -> EXTRACT_SOUTH;
            case EAST -> EXTRACT_EAST;
            case WEST -> EXTRACT_WEST;
            case UP -> EXTRACT_UP;
            case DOWN -> EXTRACT_DOWN;
        };
    }

    public static boolean isConnected(BlockState state, Direction dir) {
        return state.getValue(getConnectionProperty(dir));
    }

    public static boolean isExtracting(BlockState state, Direction dir) {
        return state.getValue(getExtractProperty(dir));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return getConnectionState(context.getLevel(), context.getClickedPos(), defaultBlockState());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                   LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return getConnectionState(level, pos, state);
    }

    private BlockState getConnectionState(LevelAccessor level, BlockPos pos, BlockState currentState) {
        BlockState newState = currentState;
        for (Direction dir : Direction.values()) {
            boolean connected = canConnect(level, pos, dir);
            newState = newState.setValue(getConnectionProperty(dir), connected);
            // Clear extraction flag if no longer connected
            if (!connected) {
                newState = newState.setValue(getExtractProperty(dir), false);
            }
        }
        return newState;
    }

    private boolean canConnect(LevelAccessor level, BlockPos pos, Direction direction) {
        BlockPos neighborPos = pos.relative(direction);
        BlockState neighborState = level.getBlockState(neighborPos);
        
        // Connect to other pipes
        if (neighborState.getBlock() instanceof HoneyPipeBlock) {
            return true;
        }
        
        // Connect to fluid handlers
        if (level instanceof Level realLevel) {
            var cap = realLevel.getCapability(Capabilities.FluidHandler.BLOCK, neighborPos, direction.getOpposite());
            return cap != null;
        }
        
        return false;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return switch (tier) {
            case 2 -> HoneyPipeBlockEntity.createTier2(pos, state);
            case 3 -> HoneyPipeBlockEntity.createTier3(pos, state);
            case 4 -> HoneyPipeBlockEntity.createTier4(pos, state);
            default -> new HoneyPipeBlockEntity(pos, state);
        };
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        BlockEntityType<?> expectedType = switch (tier) {
            case 2 -> BeemancerBlockEntities.HONEY_PIPE_TIER2.get();
            case 3 -> BeemancerBlockEntities.HONEY_PIPE_TIER3.get();
            case 4 -> BeemancerBlockEntities.HONEY_PIPE_TIER4.get();
            default -> BeemancerBlockEntities.HONEY_PIPE.get();
        };
        return createTickerHelper(type, (BlockEntityType<HoneyPipeBlockEntity>) expectedType,
            HoneyPipeBlockEntity::serverTick);
    }
}
