/**
 * ============================================================
 * [BuildingWandItem.java]
 * Description: Baguette de construction avec previsualisation et placement
 * ============================================================
 */
package com.chapeau.apica.common.item.tool;

import com.chapeau.apica.common.item.magazine.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class BuildingWandItem extends Item implements IMagazineHolder {

    private static final int COST_PER_BLOCK = 1;

    public BuildingWandItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public Set<String> getAcceptedFluids() {
        return MagazineConstants.STANDARD_FLUIDS;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return MagazineData.hasMagazine(stack);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int amount = MagazineData.getFluidAmount(stack);
        return Math.round((float) amount / MagazineFluidData.MAX_CAPACITY * 13f);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return MagazineConstants.getBarColorForFluid(MagazineData.getFluidId(stack));
    }

    public static int getMaxBlocksForFluid(ItemStack stack) {
        String fluidId = MagazineData.getFluidId(stack);
        if (fluidId.contains("nectar")) return 20;
        if (fluidId.contains("royal_jelly")) return 15;
        if (fluidId.contains("honey")) return 10;
        return 10;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        ItemStack wandStack = context.getItemInHand();

        if (level.isClientSide()) {
            if (MagazineInputHelper.isMouseDown()) {
                setOnRightClick(player, true);
                return InteractionResult.CONSUME;
            }
        } else {
            if (isOnRightClick(player)) {
                if (canReload(player, wandStack)) {
                    doReload(player, wandStack);
                }
                setOnRightClick(player, false);
                return InteractionResult.CONSUME;
            }
        }

        if (!canUse(player, wandStack)) {
            return InteractionResult.FAIL;
        }

        BlockPos clickedPos = context.getClickedPos();
        Direction face = context.getClickedFace();
        BlockState sourceState = level.getBlockState(clickedPos);

        if (sourceState.isAir()) return InteractionResult.PASS;

        List<BlockPos> previewPositions = calculatePreviewPositions(
            level, player, wandStack, clickedPos, face, sourceState
        );

        if (previewPositions.isEmpty()) return InteractionResult.PASS;

        if (!level.isClientSide()) {
            int placed = 0;
            Block sourceBlock = sourceState.getBlock();

            for (BlockPos pos : previewPositions) {
                if (!level.getBlockState(pos).canBeReplaced()) continue;
                if (!MagazineData.consumeFluid(wandStack, COST_PER_BLOCK)) break;

                BlockPos behindPos = pos.relative(face.getOpposite());
                BlockState behindState = level.getBlockState(behindPos);

                level.setBlock(pos, behindState, 3);
                placed++;

                if (!player.isCreative()) {
                    consumeItem(player, sourceBlock);
                }
            }

            if (placed > 0) {
                SoundType sound = sourceState.getSoundType();
                level.playSound(null, clickedPos, sound.getPlaceSound(),
                    SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    public static List<BlockPos> calculatePreviewPositions(Level level, Player player,
                                                            ItemStack wandStack,
                                                            BlockPos sourcePos, Direction face,
                                                            BlockState sourceState) {
        List<BlockPos> result = new ArrayList<>();
        Block sourceBlock = sourceState.getBlock();

        BlockPos startPos = sourcePos.relative(face);

        if (!level.getBlockState(startPos).canBeReplaced()) {
            return result;
        }

        int maxByFluid = getMaxBlocksForFluid(wandStack);
        int maxByAmount = MagazineData.getFluidAmount(wandStack) / COST_PER_BLOCK;
        int limit = maxByFluid;
        if (!player.isCreative()) {
            limit = Math.min(limit, countItemsInInventory(player, sourceBlock));
        }
        limit = Math.min(limit, maxByAmount);

        if (limit <= 0) return result;

        Direction[] spreadDirs = getSpreadDirections(face);

        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(startPos);
        visited.add(startPos);

        while (!queue.isEmpty() && result.size() < limit) {
            BlockPos current = queue.poll();

            if (!level.getBlockState(current).canBeReplaced()) {
                continue;
            }

            BlockPos behindPos = current.relative(face.getOpposite());
            BlockState behindState = level.getBlockState(behindPos);
            if (!behindState.is(sourceBlock)) {
                continue;
            }

            result.add(current);

            for (BlockPos neighbor : getNeighbors2D(current, spreadDirs)) {
                if (visited.contains(neighbor)) continue;
                visited.add(neighbor);

                if (getDistance2D(startPos, neighbor, spreadDirs) <= maxByFluid) {
                    queue.add(neighbor);
                }
            }
        }

        return result;
    }

    public static List<BlockPos> calculatePreviewPositions(Level level, Player player,
                                                            BlockPos sourcePos, Direction face,
                                                            BlockState sourceState) {
        ItemStack wandStack = player.getMainHandItem();
        if (!(wandStack.getItem() instanceof BuildingWandItem)) {
            wandStack = player.getOffhandItem();
        }
        return calculatePreviewPositions(level, player, wandStack, sourcePos, face, sourceState);
    }

    private static Direction[] getSpreadDirections(Direction face) {
        return switch (face.getAxis()) {
            case X -> new Direction[]{Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH};
            case Y -> new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
            case Z -> new Direction[]{Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST};
        };
    }

    private static List<BlockPos> getNeighbors2D(BlockPos pos, Direction[] spreadDirs) {
        List<BlockPos> neighbors = new ArrayList<>();

        for (Direction dir : spreadDirs) {
            neighbors.add(pos.relative(dir));
        }

        if (spreadDirs.length >= 4) {
            neighbors.add(pos.relative(spreadDirs[0]).relative(spreadDirs[2]));
            neighbors.add(pos.relative(spreadDirs[0]).relative(spreadDirs[3]));
            neighbors.add(pos.relative(spreadDirs[1]).relative(spreadDirs[2]));
            neighbors.add(pos.relative(spreadDirs[1]).relative(spreadDirs[3]));
        }

        return neighbors;
    }

    private static int getDistance2D(BlockPos start, BlockPos pos, Direction[] spreadDirs) {
        int dx = 0, dy = 0;

        for (Direction dir : spreadDirs) {
            int diff = switch (dir.getAxis()) {
                case X -> pos.getX() - start.getX();
                case Y -> pos.getY() - start.getY();
                case Z -> pos.getZ() - start.getZ();
            };
            diff = Math.abs(diff);

            if (dir == spreadDirs[0] || dir == spreadDirs[1]) {
                dx = Math.max(dx, diff);
            } else {
                dy = Math.max(dy, diff);
            }
        }

        return Math.max(dx, dy);
    }

    private static int countItemsInInventory(Player player, Block block) {
        int count = 0;
        Item blockItem = block.asItem();

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == blockItem) {
                count += stack.getCount();
            }
        }

        return count;
    }

    private static void consumeItem(Player player, Block block) {
        Item blockItem = block.asItem();

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() == blockItem) {
                stack.shrink(1);
                return;
            }
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide()) {
            if (MagazineInputHelper.isMouseDown()) {
                setOnRightClick(player, true);
                return InteractionResultHolder.consume(stack);
            }
        } else {
            if (isOnRightClick(player)) {
                if (canReload(player, stack)) {
                    doReload(player, stack);
                }
                setOnRightClick(player, false);
                return InteractionResultHolder.consume(stack);
            }
        }

        return InteractionResultHolder.fail(stack);
    }
}
