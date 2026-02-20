/**
 * ============================================================
 * [StorageBarrelBlock.java]
 * Description: Barrel de stockage mono-item avec support void upgrade
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | StorageBarrelBlockEntity      | BlockEntity associe  | Logique de stockage            |
 * | ApicaBlockEntities            | Type du BlockEntity  | Registration                   |
 * | ApicaItems                    | Void upgrade item    | Detection clic droit           |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ApicaBlocks.java (enregistrement)
 * - Apica.java (event LeftClickBlock)
 *
 * ============================================================
 */
package com.chapeau.apica.common.block.storage;

import com.chapeau.apica.common.blockentity.storage.StorageBarrelBlockEntity;
import com.chapeau.apica.core.registry.ApicaItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StorageBarrelBlock extends BaseEntityBlock {

    public static final MapCodec<StorageBarrelBlock> CODEC = simpleCodec(StorageBarrelBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    private static final Map<Direction, VoxelShape> SHAPES = new EnumMap<>(Direction.class);
    static {
        VoxelShape full = Shapes.block();
        SHAPES.put(Direction.NORTH, Shapes.join(full, Block.box(0.5, 0.5, 0, 15.5, 15.5, 1), BooleanOp.ONLY_FIRST));
        SHAPES.put(Direction.SOUTH, Shapes.join(full, Block.box(0.5, 0.5, 15, 15.5, 15.5, 16), BooleanOp.ONLY_FIRST));
        SHAPES.put(Direction.EAST,  Shapes.join(full, Block.box(15, 0.5, 0.5, 16, 15.5, 15.5), BooleanOp.ONLY_FIRST));
        SHAPES.put(Direction.WEST,  Shapes.join(full, Block.box(0, 0.5, 0.5, 1, 15.5, 15.5), BooleanOp.ONLY_FIRST));
        SHAPES.put(Direction.UP,    Shapes.join(full, Block.box(0.5, 15, 0.5, 15.5, 16, 15.5), BooleanOp.ONLY_FIRST));
        SHAPES.put(Direction.DOWN,  Shapes.join(full, Block.box(0.5, 0, 0.5, 15.5, 1, 15.5), BooleanOp.ONLY_FIRST));
    }

    private static final int EXTRACT_COOLDOWN_TICKS = 20;
    private static final Map<UUID, Long> lastExtractTime = new HashMap<>();

    private final int tier;

    public StorageBarrelBlock(Properties properties) {
        this(properties, 1);
    }

    public StorageBarrelBlock(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH));
    }

    public int getTier() {
        return tier;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.getOrDefault(state.getValue(FACING), Shapes.block());
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getClickedFace());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StorageBarrelBlockEntity(pos, state);
    }

    // --- Right-click with item: insert on front face only ---

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                               BlockPos pos, Player player, InteractionHand hand,
                                               BlockHitResult hit) {
        Direction facing = state.getValue(FACING);
        if (hit.getDirection() != facing) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        if (level.isClientSide()) return ItemInteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof StorageBarrelBlockEntity barrel)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // Apply void upgrade
        if (stack.is(ApicaItems.VOID_UPGRADE.get())) {
            if (!barrel.hasVoidUpgrade()) {
                barrel.setVoidUpgrade(true);
                if (!player.isCreative()) stack.shrink(1);
                level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.5f, 1.2f);
                return ItemInteractionResult.SUCCESS;
            }
            return ItemInteractionResult.CONSUME;
        }

        if (player.isShiftKeyDown()) {
            // Shift+right-click: insert all matching items from inventory
            int totalInserted = insertAllMatching(barrel, player, stack);
            if (totalInserted > 0) {
                level.playSound(null, pos, SoundEvents.BUNDLE_INSERT, SoundSource.BLOCKS, 0.8f, 1.0f);
            }
        } else {
            // Right-click: insert 1 stack
            int inserted = barrel.insertItem(stack, true);
            if (inserted > 0) {
                if (!player.isCreative()) stack.shrink(inserted);
                level.playSound(null, pos, SoundEvents.BUNDLE_INSERT, SoundSource.BLOCKS, 0.8f, 1.0f);
            }
        }

        return ItemInteractionResult.SUCCESS;
    }

    // --- Right-click empty hand: void icon removal on front face ---

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hit) {
        Direction facing = state.getValue(FACING);
        if (hit.getDirection() != facing) return InteractionResult.PASS;

        if (level.isClientSide()) return InteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof StorageBarrelBlockEntity barrel)) return InteractionResult.PASS;

        // Check if clicking the void icon area
        if (barrel.hasVoidUpgrade() && isClickOnVoidIcon(hit, facing, pos)) {
            barrel.setVoidUpgrade(false);
            ItemStack voidItem = new ItemStack(ApicaItems.VOID_UPGRADE.get());
            if (!player.getInventory().add(voidItem)) {
                player.drop(voidItem, false);
            }
            level.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.3f, 1.5f);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    // --- Left-click on front face: extract items ---

    /**
     * Event handler pour le clic gauche sur le barrel.
     * Clic gauche sur la face avant avec items = extraire.
     * Clic gauche sur autre face ou barrel vide = casser normalement.
     */
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);

        if (!(state.getBlock() instanceof StorageBarrelBlock)) return;

        Direction facing = state.getValue(FACING);
        if (event.getFace() != facing) return;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof StorageBarrelBlockEntity barrel)) return;

        if (barrel.getStoredCount() <= 0) return;

        // Cancel block breaking
        event.setCanceled(true);

        if (!level.isClientSide()) {
            Player player = event.getEntity();
            UUID playerId = player.getUUID();
            long gameTime = level.getGameTime();

            // Cooldown: 1 extraction per second (20 ticks)
            Long lastTime = lastExtractTime.get(playerId);
            if (lastTime != null && gameTime - lastTime < EXTRACT_COOLDOWN_TICKS) return;
            lastExtractTime.put(playerId, gameTime);

            // Shift+left = 1 stack, left = 1 item
            ItemStack extracted = barrel.extractItem(!player.isShiftKeyDown());
            if (!extracted.isEmpty()) {
                if (!player.getInventory().add(extracted)) {
                    player.drop(extracted, false);
                }
                level.playSound(null, pos, SoundEvents.BUNDLE_REMOVE_ONE, SoundSource.BLOCKS, 0.8f, 1.0f);
            }
        }
    }

    // --- Helpers ---

    /**
     * Insere tous les items du meme type depuis l'inventaire du joueur.
     */
    private int insertAllMatching(StorageBarrelBlockEntity barrel, Player player, ItemStack reference) {
        Inventory inv = player.getInventory();
        int totalInserted = 0;

        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack slot = inv.getItem(i);
            if (slot.isEmpty()) continue;
            if (!ItemStack.isSameItemSameComponents(reference, slot)) continue;

            int inserted = barrel.insertItem(slot, true);
            if (inserted > 0) {
                if (!player.isCreative()) slot.shrink(inserted);
                totalInserted += inserted;
            }
        }

        return totalInserted;
    }

    /**
     * Verifie si le clic est sur l'icone void (bas-droite de la face avant).
     * Zone: 4x4 pixels en bas-droite de la face FACING.
     */
    private boolean isClickOnVoidIcon(BlockHitResult hit, Direction facing, BlockPos pos) {
        if (hit.getDirection() != facing) return false;

        Vec3 hitLoc = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
        double u, v;

        switch (facing) {
            case NORTH -> { u = 1.0 - hitLoc.x; v = 1.0 - hitLoc.y; }
            case SOUTH -> { u = hitLoc.x; v = 1.0 - hitLoc.y; }
            case WEST  -> { u = hitLoc.z; v = 1.0 - hitLoc.y; }
            case EAST  -> { u = 1.0 - hitLoc.z; v = 1.0 - hitLoc.y; }
            case UP    -> { u = hitLoc.x; v = hitLoc.z; }
            case DOWN  -> { u = hitLoc.x; v = 1.0 - hitLoc.z; }
            default    -> { return false; }
        }

        return u >= 0.75 && u <= 1.0 && v >= 0.75 && v <= 1.0;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof StorageBarrelBlockEntity barrel) {
                barrel.dropContents(level, pos);
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
