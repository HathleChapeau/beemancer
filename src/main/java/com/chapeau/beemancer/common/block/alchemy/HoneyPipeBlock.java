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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import com.chapeau.beemancer.core.registry.BeemancerTags;
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

    // Multibloc formed state
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    // Core: 6x6x6 center + 8x8x8 frame outline
    private static final VoxelShape CORE = Block.box(4, 4, 4, 12, 12, 12);
    // Connections: 6x6 tubes (same width as core)
    private static final VoxelShape NORTH_SHAPE = Block.box(5, 5, 0, 11, 11, 4);
    private static final VoxelShape SOUTH_SHAPE = Block.box(5, 5, 12, 11, 11, 16);
    private static final VoxelShape EAST_SHAPE = Block.box(12, 5, 5, 16, 11, 11);
    private static final VoxelShape WEST_SHAPE = Block.box(0, 5, 5, 4, 11, 11);
    private static final VoxelShape UP_SHAPE = Block.box(5, 12, 5, 11, 16, 11);
    private static final VoxelShape DOWN_SHAPE = Block.box(5, 0, 5, 11, 4, 11);

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
            .setValue(EXTRACT_UP, false).setValue(EXTRACT_DOWN, false)
            .setValue(FORMED, false));
    }

    public int getTier() { return tier; }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
        builder.add(EXTRACT_NORTH, EXTRACT_SOUTH, EXTRACT_EAST, EXTRACT_WEST, EXTRACT_UP, EXTRACT_DOWN);
        builder.add(FORMED);
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

    private static final double CLICK_THRESHOLD = 0.1;

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        // Colorant: applique une teinte au core (sans consommer)
        if (stack.getItem() instanceof DyeItem dyeItem) {
            if (level.isClientSide()) return ItemInteractionResult.SUCCESS;

            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof HoneyPipeBlockEntity pipe) {
                DyeColor color = dyeItem.getDyeColor();
                int rgb = color.getTextureDiffuseColor();
                pipe.setTintColor(rgb);

                // Force visual update
                level.setBlock(pos, state, 3);

                level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
                player.displayClientMessage(Component.literal("Tinted: " + color.getName()), true);
                return ItemInteractionResult.SUCCESS;
            }
        }

        // Comb: retire la teinte (sans consommer)
        if (stack.is(BeemancerTags.Items.COMBS)) {
            if (level.isClientSide()) return ItemInteractionResult.SUCCESS;

            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof HoneyPipeBlockEntity pipe && pipe.hasTint()) {
                pipe.setTintColor(-1);

                // Force visual update
                level.setBlock(pos, state, 3);

                level.playSound(null, pos, SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.BLOCKS, 0.7f, 1.0f);
                player.displayClientMessage(Component.literal("Tint removed"), true);
                return ItemInteractionResult.SUCCESS;
            }
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        // Shift+clic droit = vider le pipe
        if (player.isShiftKeyDown()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof HoneyPipeBlockEntity pipe) {
                int drained = pipe.getBuffer().drain(Integer.MAX_VALUE, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE).getAmount();
                if (drained > 0) {
                    level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.5f, 1.0f);
                    player.displayClientMessage(Component.literal("Drained " + drained + " mB"), true);
                } else {
                    player.displayClientMessage(Component.literal("Pipe is empty"), true);
                }
                return InteractionResult.SUCCESS;
            }
        }

        Direction clickedDir = getClickedDirection(pos, hit);
        if (clickedDir == null) return InteractionResult.PASS;

        BlockEntity be = level.getBlockEntity(pos);
        boolean isDisconnected = be instanceof HoneyPipeBlockEntity pipe && pipe.isDisconnected(clickedDir);

        if (isDisconnected) {
            // Reconnect
            ((HoneyPipeBlockEntity) be).setDisconnected(clickedDir, false);
            BlockState newState = getConnectionState(level, pos, state);
            level.setBlock(pos, newState, 3);

            level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.5f, 1.2f);
            player.displayClientMessage(Component.literal(clickedDir.getName() + ": Connected"), true);
            return InteractionResult.SUCCESS;
        }

        if (!isConnected(state, clickedDir)) {
            // Check si le voisin a deconnecte de notre cote -> reconnect depuis l'autre pipe
            BlockPos neighborPos = pos.relative(clickedDir);
            BlockEntity neighborBe = level.getBlockEntity(neighborPos);
            if (neighborBe instanceof HoneyPipeBlockEntity neighborPipe
                && neighborPipe.isDisconnected(clickedDir.getOpposite())) {
                neighborPipe.setDisconnected(clickedDir.getOpposite(), false);
                // Recalculer les deux cotes
                BlockState newState = getConnectionState(level, pos, state);
                level.setBlock(pos, newState, 3);
                BlockState neighborState = level.getBlockState(neighborPos);
                level.setBlock(neighborPos, getConnectionState(level, neighborPos, neighborState), 3);

                level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.5f, 1.2f);
                player.displayClientMessage(Component.literal(clickedDir.getName() + ": Connected"), true);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }

        BlockPos neighborPos = pos.relative(clickedDir);
        boolean neighborIsPipe = level.getBlockState(neighborPos).getBlock() instanceof HoneyPipeBlock;

        if (neighborIsPipe) {
            // Disconnect pipe-to-pipe
            if (be instanceof HoneyPipeBlockEntity pipe) {
                pipe.setDisconnected(clickedDir, true);
            }
            BlockState newState = state
                .setValue(getConnectionProperty(clickedDir), false)
                .setValue(getExtractProperty(clickedDir), false);
            level.setBlock(pos, newState, 3);

            level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.literal(clickedDir.getName() + ": Disconnected"), true);
        } else {
            // Toggle extraction (non-pipe neighbor)
            BooleanProperty extractProp = getExtractProperty(clickedDir);
            boolean newValue = !state.getValue(extractProp);
            level.setBlock(pos, state.setValue(extractProp, newValue), 3);

            level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.5f, newValue ? 1.2f : 0.8f);
            player.displayClientMessage(
                Component.literal(clickedDir.getName() + ": " + (newValue ? "Extraction" : "Insertion")),
                true);
        }

        return InteractionResult.SUCCESS;
    }

    @Nullable
    private Direction getClickedDirection(BlockPos pos, BlockHitResult hit) {
        Vec3 hitVec = hit.getLocation().subtract(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);

        Direction best = null;
        double bestDot = CLICK_THRESHOLD;

        for (Direction dir : Direction.values()) {
            Vec3 normal = Vec3.atLowerCornerOf(dir.getNormal());
            double dot = hitVec.dot(normal);
            if (dot > bestDot) {
                bestDot = dot;
                best = dir;
            }
        }
        return best;
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
        // Check si manuellement deconnecte
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof HoneyPipeBlockEntity pipe && pipe.isDisconnected(direction)) {
            return false;
        }

        BlockPos neighborPos = pos.relative(direction);
        BlockState neighborState = level.getBlockState(neighborPos);

        // Connect to other pipes
        if (neighborState.getBlock() instanceof HoneyPipeBlock) {
            // Check si le voisin a deconnecte de notre cote
            BlockEntity neighborBe = level.getBlockEntity(neighborPos);
            if (neighborBe instanceof HoneyPipeBlockEntity neighborPipe
                && neighborPipe.isDisconnected(direction.getOpposite())) {
                return false;
            }
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
