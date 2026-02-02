/**
 * ============================================================
 * [AbstractPipeBlock.java]
 * Description: Base class pour les blocs pipe (fluide et item)
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | BaseEntityBlock     | Bloc avec BlockEntity| Lifecycle et ticking           |
 * | BeemancerTags       | Tags items           | Détection combs pour tint      |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - HoneyPipeBlock.java
 * - ItemPipeBlock.java
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.alchemy;

import com.chapeau.beemancer.core.registry.BeemancerTags;
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
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * Classe abstraite pour les pipes (fluide et item).
 * Gère la connexion automatique, les shapes, le tinting, et les interactions de base.
 * Les sous-classes implémentent la logique spécifique au type de contenu transporté.
 */
public abstract class AbstractPipeBlock extends BaseEntityBlock {

    protected final int tier;

    // Connection properties (partagées)
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");

    // Extraction properties (partagées)
    public static final BooleanProperty EXTRACT_NORTH = BooleanProperty.create("extract_north");
    public static final BooleanProperty EXTRACT_SOUTH = BooleanProperty.create("extract_south");
    public static final BooleanProperty EXTRACT_EAST = BooleanProperty.create("extract_east");
    public static final BooleanProperty EXTRACT_WEST = BooleanProperty.create("extract_west");
    public static final BooleanProperty EXTRACT_UP = BooleanProperty.create("extract_up");
    public static final BooleanProperty EXTRACT_DOWN = BooleanProperty.create("extract_down");

    // Tint state — contrôle la texture du core (base vs white tintable)
    public static final BooleanProperty TINTED = BooleanProperty.create("tinted");

    // Pre-computed VoxelShapes for all 64 direction combinations (6 bits)
    private static final VoxelShape CORE = Block.box(4, 4, 4, 12, 12, 12);
    private static final VoxelShape[] DIR_SHAPES = {
        Block.box(5, 5, 0, 11, 11, 4),   // NORTH (bit 0)
        Block.box(5, 5, 12, 11, 11, 16),  // SOUTH (bit 1)
        Block.box(12, 5, 5, 16, 11, 11),  // EAST (bit 2)
        Block.box(0, 5, 5, 4, 11, 11),    // WEST (bit 3)
        Block.box(5, 12, 5, 11, 16, 11),  // UP (bit 4)
        Block.box(5, 0, 5, 11, 4, 11)     // DOWN (bit 5)
    };
    private static final VoxelShape[] SHAPE_CACHE = buildShapeCache();

    private static final double CLICK_THRESHOLD = 0.1;

    private static VoxelShape[] buildShapeCache() {
        VoxelShape[] cache = new VoxelShape[64];
        for (int i = 0; i < 64; i++) {
            VoxelShape shape = CORE;
            for (int bit = 0; bit < 6; bit++) {
                if ((i & (1 << bit)) != 0) {
                    shape = Shapes.or(shape, DIR_SHAPES[bit]);
                }
            }
            cache[i] = shape;
        }
        return cache;
    }

    protected AbstractPipeBlock(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(NORTH, false).setValue(SOUTH, false)
            .setValue(EAST, false).setValue(WEST, false)
            .setValue(UP, false).setValue(DOWN, false)
            .setValue(EXTRACT_NORTH, false).setValue(EXTRACT_SOUTH, false)
            .setValue(EXTRACT_EAST, false).setValue(EXTRACT_WEST, false)
            .setValue(EXTRACT_UP, false).setValue(EXTRACT_DOWN, false)
            .setValue(TINTED, false));
    }

    public int getTier() { return tier; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
        builder.add(EXTRACT_NORTH, EXTRACT_SOUTH, EXTRACT_EAST, EXTRACT_WEST, EXTRACT_UP, EXTRACT_DOWN);
        builder.add(TINTED);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int index = 0;
        if (state.getValue(NORTH)) index |= 1;
        if (state.getValue(SOUTH)) index |= 2;
        if (state.getValue(EAST))  index |= 4;
        if (state.getValue(WEST))  index |= 8;
        if (state.getValue(UP))    index |= 16;
        if (state.getValue(DOWN))  index |= 32;
        return SHAPE_CACHE[index];
    }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    // --- Interaction: Dye / Comb tinting ---

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof DyeItem dyeItem) {
            if (level.isClientSide()) return ItemInteractionResult.SUCCESS;
            BlockEntity be = level.getBlockEntity(pos);
            if (isTintablePipe(be)) {
                DyeColor color = dyeItem.getDyeColor();
                applyTint(be, color.getTextureDiffuseColor());
                level.setBlock(pos, state.setValue(TINTED, true), 3);
                level.playSound(null, pos, SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
                player.displayClientMessage(Component.literal("Tinted: " + color.getName()), true);
                return ItemInteractionResult.SUCCESS;
            }
        }

        if (stack.is(BeemancerTags.Items.COMBS)) {
            if (level.isClientSide()) return ItemInteractionResult.SUCCESS;
            BlockEntity be = level.getBlockEntity(pos);
            if (hasTint(be)) {
                applyTint(be, -1);
                level.setBlock(pos, state.setValue(TINTED, false), 3);
                level.playSound(null, pos, SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.BLOCKS, 0.7f, 1.0f);
                player.displayClientMessage(Component.literal("Tint removed"), true);
                return ItemInteractionResult.SUCCESS;
            }
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    // --- Interaction: Connection / Extraction toggle ---

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        // Hook pour logique spécifique (ex: shift+drain pour fluid pipes)
        InteractionResult specialResult = handleSpecialInteraction(state, level, pos, player, hit);
        if (specialResult != null) return specialResult;

        Direction clickedDir = getClickedDirection(pos, hit);
        if (clickedDir == null) return InteractionResult.PASS;

        BlockEntity be = level.getBlockEntity(pos);
        boolean isDisconnected = isPipeEntity(be) && isPipeDisconnected(be, clickedDir);

        if (isDisconnected) {
            setPipeDisconnected(be, clickedDir, false);
            BlockState newState = getConnectionState(level, pos, state);
            level.setBlock(pos, newState, 3);
            level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.5f, 1.2f);
            player.displayClientMessage(Component.literal(clickedDir.getName() + ": Connected"), true);
            return InteractionResult.SUCCESS;
        }

        if (!isConnected(state, clickedDir)) {
            BlockPos neighborPos = pos.relative(clickedDir);
            BlockEntity neighborBe = level.getBlockEntity(neighborPos);
            if (isPipeEntity(neighborBe) && isPipeDisconnected(neighborBe, clickedDir.getOpposite())) {
                setPipeDisconnected(neighborBe, clickedDir.getOpposite(), false);
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
        boolean neighborIsPipe = isSamePipeType(level.getBlockState(neighborPos));

        if (neighborIsPipe) {
            if (isPipeEntity(be)) {
                setPipeDisconnected(be, clickedDir, true);
            }
            BlockState newState = state
                .setValue(getConnectionProperty(clickedDir), false)
                .setValue(getExtractProperty(clickedDir), false);
            level.setBlock(pos, newState, 3);
            level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.5f, 0.6f);
            player.displayClientMessage(Component.literal(clickedDir.getName() + ": Disconnected"), true);
        } else {
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

    // --- Connection state ---

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

    protected BlockState getConnectionState(LevelAccessor level, BlockPos pos, BlockState currentState) {
        BlockState newState = currentState;
        for (Direction dir : Direction.values()) {
            boolean connected = canConnect(level, pos, dir);
            newState = newState.setValue(getConnectionProperty(dir), connected);
            if (!connected) {
                newState = newState.setValue(getExtractProperty(dir), false);
            }
        }
        return newState;
    }

    private boolean canConnect(LevelAccessor level, BlockPos pos, Direction direction) {
        BlockEntity be = level.getBlockEntity(pos);
        if (isPipeEntity(be) && isPipeDisconnected(be, direction)) {
            return false;
        }

        BlockPos neighborPos = pos.relative(direction);
        BlockState neighborState = level.getBlockState(neighborPos);

        if (isSamePipeType(neighborState)) {
            BlockEntity neighborBe = level.getBlockEntity(neighborPos);
            if (isPipeEntity(neighborBe) && isPipeDisconnected(neighborBe, direction.getOpposite())) {
                return false;
            }
            return true;
        }

        if (level instanceof Level realLevel) {
            return hasCapabilityAt(realLevel, neighborPos, direction.getOpposite());
        }

        return false;
    }

    // --- Static utility methods ---

    @Nullable
    protected static Direction getClickedDirection(BlockPos pos, BlockHitResult hit) {
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

    // --- Abstract methods for subclass-specific behavior ---

    /** Vérifie si le BlockEntity est un pipe de ce type. */
    protected abstract boolean isPipeEntity(BlockEntity be);

    /** Vérifie si le pipe est déconnecté dans la direction donnée. */
    protected abstract boolean isPipeDisconnected(BlockEntity be, Direction dir);

    /** Définit l'état de déconnexion du pipe. */
    protected abstract void setPipeDisconnected(BlockEntity be, Direction dir, boolean disconnected);

    /** Vérifie si le BlockState voisin est un pipe du même type. */
    protected abstract boolean isSamePipeType(BlockState neighborState);

    /** Vérifie si le bloc voisin a la capability appropriée. */
    protected abstract boolean hasCapabilityAt(Level level, BlockPos pos, Direction side);

    /** Vérifie si le BlockEntity supporte le tinting. */
    protected abstract boolean isTintablePipe(BlockEntity be);

    /** Vérifie si le pipe a une teinte. */
    protected abstract boolean hasTint(BlockEntity be);

    /** Applique une teinte au pipe. */
    protected abstract void applyTint(BlockEntity be, int color);

    /**
     * Hook pour logique d'interaction spécifique avant le traitement connexion/extraction.
     * Retourne null pour continuer le traitement normal, ou un InteractionResult pour court-circuiter.
     */
    @Nullable
    protected InteractionResult handleSpecialInteraction(BlockState state, Level level, BlockPos pos,
                                                          Player player, BlockHitResult hit) {
        return null;
    }
}
