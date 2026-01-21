/**
 * ============================================================
 * [BeeWandItem.java]
 * Description: Baguette pour contrôler les abeilles magiques
 * ============================================================
 */
package com.chapeau.beemancer.common.item.bee;

import com.chapeau.beemancer.common.entity.bee.MagicBeeEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

public class BeeWandItem extends Item {
    public BeeWandItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockPos clickedPos = context.getClickedPos();
        Vec3 clickLocation = context.getClickLocation();

        if (player == null || player.isShiftKeyDown()) return InteractionResult.PASS;

        if (!level.isClientSide()) {
            // Check if clicking on a MagicBee
            AABB searchBox = new AABB(clickLocation.subtract(0.5, 0.5, 0.5), clickLocation.add(0.5, 0.5, 0.5));
            List<MagicBeeEntity> bees = level.getEntitiesOfClass(MagicBeeEntity.class, searchBox);
            
            MagicBeeEntity clickedBee = null;
            double closestDist = Double.MAX_VALUE;
            for (MagicBeeEntity bee : bees) {
                double dist = bee.position().distanceTo(clickLocation);
                if (dist < closestDist) {
                    closestDist = dist;
                    clickedBee = bee;
                }
            }

            if (clickedBee != null) {
                // Select the bee - Ding!
                setSelectedBee(stack, clickedBee.getUUID());
                level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), 
                        SoundSource.PLAYERS, 1.0F, 2.0F);
                player.displayClientMessage(
                        Component.translatable("item.beemancer.bee_wand.selected")
                                .withStyle(ChatFormatting.GREEN), true);
            } else {
                // Click on block - set destination
                UUID selectedId = getSelectedBee(stack);
                if (selectedId != null) {
                    MagicBeeEntity selectedBee = findBeeByUUID(level, selectedId);
                    if (selectedBee != null) {
                        BlockPos targetPos = clickedPos.above();
                        selectedBee.setTargetPos(targetPos);
                        
                        // Dong!
                        level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), 
                                SoundSource.PLAYERS, 1.0F, 0.5F);
                        player.displayClientMessage(
                                Component.translatable("item.beemancer.bee_wand.destination_set")
                                        .withStyle(ChatFormatting.AQUA), true);
                    } else {
                        clearSelection(stack);
                        player.displayClientMessage(
                                Component.translatable("item.beemancer.bee_wand.bee_lost")
                                        .withStyle(ChatFormatting.RED), true);
                    }
                } else {
                    player.displayClientMessage(
                            Component.translatable("item.beemancer.bee_wand.no_selection")
                                    .withStyle(ChatFormatting.YELLOW), true);
                }
            }
        }
        
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                if (getSelectedBee(stack) != null) {
                    clearSelection(stack);
                    player.displayClientMessage(
                            Component.translatable("item.beemancer.bee_wand.deselected")
                                    .withStyle(ChatFormatting.GRAY), true);
                }
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        
        UUID selectedId = getSelectedBee(stack);
        if (selectedId != null) {
            tooltip.add(Component.translatable("item.beemancer.bee_wand.has_selection")
                    .withStyle(ChatFormatting.GREEN));
            String shortId = selectedId.toString().substring(0, 8);
            tooltip.add(Component.literal("  ID: " + shortId + "...")
                    .withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.translatable("item.beemancer.bee_wand.no_selection_hint")
                    .withStyle(ChatFormatting.GRAY));
        }
        
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("item.beemancer.bee_wand.usage1")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.beemancer.bee_wand.usage2")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("item.beemancer.bee_wand.usage3")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    private static void setSelectedBee(ItemStack stack, UUID beeId) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("SelectedBee", beeId);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static UUID getSelectedBee(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.hasUUID("SelectedBee")) {
                return tag.getUUID("SelectedBee");
            }
        }
        return null;
    }

    private static void clearSelection(ItemStack stack) {
        stack.remove(DataComponents.CUSTOM_DATA);
    }

    private static MagicBeeEntity findBeeByUUID(Level level, UUID id) {
        if (level instanceof ServerLevel serverLevel) {
            Entity entity = serverLevel.getEntity(id);
            if (entity instanceof MagicBeeEntity bee) return bee;
        }
        return null;
    }
}
