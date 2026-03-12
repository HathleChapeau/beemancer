/**
 * ============================================================
 * [ChopperHiveItem.java]
 * Description: Cube qui detecte, surligne et abat les buches connectees
 * ============================================================
 */
package com.chapeau.apica.common.item.tool;

import com.chapeau.apica.common.item.magazine.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class ChopperHiveItem extends Item implements IMagazineHolder {

    public static final int MAX_SCAN = 256;

    public ChopperHiveItem(Properties properties) {
        super(properties);
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

    public static int getTicksPerBlockForFluid(ItemStack stack) {
        String fluidId = MagazineData.getFluidId(stack);
        if (fluidId.contains("nectar")) return 6;
        if (fluidId.contains("royal_jelly")) return 8;
        return 12;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        ItemStack stack = context.getItemInHand();

        if (canReload(player, stack)) {
            if (!level.isClientSide()) {
                doReload(player, stack);
            }
            setReloading(player, true);
            return InteractionResult.SUCCESS;
        }

        if (!canUse(player, stack)) {
            return InteractionResult.FAIL;
        }

        BlockPos clickedPos = context.getClickedPos();
        BlockState clickedState = level.getBlockState(clickedPos);

        if (!clickedState.is(BlockTags.LOGS)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            if (ChopperHiveChoppingState.isActive(player.getUUID())) {
                return InteractionResult.CONSUME;
            }

            List<BlockPos> logs = findConnectedLogs(level, clickedPos);
            logs.sort(Comparator.<BlockPos>comparingInt(BlockPos::getY).reversed()
                    .thenComparingInt(BlockPos::getX)
                    .thenComparingInt(BlockPos::getZ));
            ChopperHiveChoppingState.start(player.getUUID(), logs);
        } else {
            List<BlockPos> logs = findConnectedLogs(level, clickedPos);
            ChopperHiveLockHelper.lockWith(logs, level.getGameTime());
        }

        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide() && ChopperHiveChoppingState.isActive(player.getUUID())) {
            return InteractionResultHolder.success(stack);
        }

        if (canReload(player, stack)) {
            if (!level.isClientSide()) {
                doReload(player, stack);
            }
            setReloading(player, true);
            return InteractionResultHolder.success(stack);
        }

        return InteractionResultHolder.fail(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide() || !(entity instanceof Player player)) return;

        boolean holding = selected || player.getOffhandItem() == stack;
        if (!holding) {
            ChopperHiveChoppingState.clear(player.getUUID());
            return;
        }

        ChopperHiveChoppingState.tick(player, (ServerLevel) level, stack);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged;
    }

    public static List<BlockPos> findConnectedLogs(Level level, BlockPos startPos) {
        List<BlockPos> result = new ArrayList<>();
        BlockState startState = level.getBlockState(startPos);

        if (!startState.is(BlockTags.LOGS)) {
            return result;
        }

        Block targetBlock = startState.getBlock();
        int startY = startPos.getY();

        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(startPos);
        visited.add(startPos);

        while (!queue.isEmpty() && result.size() < MAX_SCAN) {
            BlockPos current = queue.poll();
            result.add(current);

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;

                        BlockPos neighbor = current.offset(dx, dy, dz);

                        if (visited.contains(neighbor)) continue;
                        if (neighbor.getY() < startY) continue;

                        BlockState neighborState = level.getBlockState(neighbor);
                        if (neighborState.is(targetBlock)) {
                            visited.add(neighbor);
                            queue.add(neighbor);
                        }
                    }
                }
            }
        }

        return result;
    }
}
