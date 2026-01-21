/**
 * ============================================================
 * [BeeWandItem.java]
 * Description: Baguette pour contrôler les abeilles debug
 * ============================================================
 * * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation         |
 * |---------------------|----------------------|---------------------|
 * | DebugBeeEntity      | Entité cible         | Sélection/navigation|
 * ------------------------------------------------------------
 * * UTILISÉ PAR:
 * - BeemancerItems.java (enregistrement)
 * * ============================================================
 */
package com.chapeau.beemancer.common.item.bee;

import com.chapeau.beemancer.common.entity.bee.DebugBeeEntity;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.UUID;

public class BeeWandItem extends Item {
    public BeeWandItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    /**
     * SÉLECTION : Se déclenche lors d'un CLIC DROIT DIRECT sur l'entité.
     */
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof DebugBeeEntity clickedBee) {
            if (!player.level().isClientSide()) {
                // Log console local
                System.out.println("[BeeWand] Clic direct sur l'abeille : " + clickedBee.getUUID());

                ItemStack wand = player.getItemInHand(hand);
                setSelectedBee(wand, clickedBee.getUUID());

                player.level().playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(),
                        SoundSource.PLAYERS, 1.0F, 2.0F);

                player.displayClientMessage(
                        Component.translatable("item.beemancer.bee_wand.selected")
                                .withStyle(ChatFormatting.GREEN), true);
            }
            return InteractionResult.sidedSuccess(player.level().isClientSide());
        }
        return InteractionResult.PASS;
    }

    /**
     * MOUVEMENT : Se déclenche lors d'un clic sur un BLOC (le sol).
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockPos clickedPos = context.getClickedPos();

        if (player == null || player.isShiftKeyDown()) return InteractionResult.PASS;

        if (!level.isClientSide()) {
            UUID selectedId = getSelectedBee(stack);

            if (selectedId != null) {
                DebugBeeEntity selectedBee = findBeeByUUID(level, selectedId);
                if (selectedBee != null) {
                    // On définit la destination au-dessus du bloc cliqué
                    selectedBee.setTargetPos(clickedPos.above());

                    level.playSound(null, player.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(),
                            SoundSource.PLAYERS, 1.0F, 0.5F);
                    player.displayClientMessage(
                            Component.translatable("item.beemancer.bee_wand.destination_set")
                                    .withStyle(ChatFormatting.AQUA), true);
                    return InteractionResult.SUCCESS;
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
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Shift + clic dans le vide = désélectionner
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
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        UUID selectedId = getSelectedBee(stack);
        if (selectedId != null) {
            tooltipComponents.add(Component.translatable("item.beemancer.bee_wand.has_selection")
                    .withStyle(ChatFormatting.GREEN));
            String shortId = selectedId.toString().substring(0, 8);
            tooltipComponents.add(Component.literal("  ID: " + shortId + "...")
                    .withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltipComponents.add(Component.translatable("item.beemancer.bee_wand.no_selection_hint")
                    .withStyle(ChatFormatting.GRAY));
        }

        tooltipComponents.add(Component.empty());
        tooltipComponents.add(Component.translatable("item.beemancer.bee_wand.usage1")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltipComponents.add(Component.translatable("item.beemancer.bee_wand.usage2")
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltipComponents.add(Component.translatable("item.beemancer.bee_wand.usage3")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    // --- Méthodes utilitaires ---

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

    private static DebugBeeEntity findBeeByUUID(Level level, UUID id) {
        if (level instanceof ServerLevel serverLevel) {
            Entity entity = serverLevel.getEntity(id);
            if (entity instanceof DebugBeeEntity bee) {
                return bee;
            }
        }
        return null;
    }
}