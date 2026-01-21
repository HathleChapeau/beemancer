/**
 * ============================================================
 * [BeeDebugItem.java]
 * Description: Item représentant une abeille debug avec stats
 * ============================================================
 * 
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation         |
 * |---------------------|----------------------|---------------------|
 * | BeemancerEntities   | Type de l'entité     | Spawn de l'abeille  |
 * | DebugBeeEntity      | Entité abeille       | Création/récup      |
 * ------------------------------------------------------------
 * 
 * UTILISÉ PAR:
 * - BeemancerItems.java (enregistrement)
 * 
 * ============================================================
 */
package com.chapeau.beemancer.common.item.bee;

import com.chapeau.beemancer.common.entity.bee.DebugBeeEntity;
import com.chapeau.beemancer.core.registry.BeemancerEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class BeeDebugItem extends Item {
    public BeeDebugItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos();

        if (player == null) return InteractionResult.FAIL;

        // Shift + clic droit = pas de spawn (réservé au pickup)
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            // Spawn l'abeille
            BlockPos spawnPos = pos.above();
            DebugBeeEntity bee = BeemancerEntities.DEBUG_BEE.get().create(level);
            
            if (bee != null) {
                bee.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, 0, 0);
                
                // Charger les stats depuis l'item si présentes
                CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
                if (customData != null) {
                    CompoundTag tag = customData.copyTag();
                    if (tag.contains("BeeHp")) bee.setBeeHp(tag.getInt("BeeHp"));
                    if (tag.contains("BeeSpeed")) bee.setBeeSpeed(tag.getInt("BeeSpeed"));
                    if (tag.contains("BeeStrength")) bee.setBeeStrength(tag.getInt("BeeStrength"));
                }
                
                level.addFreshEntity(bee);
                
                // Consommer l'item en survie
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
        }
        
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /**
     * Méthode pour récupérer une abeille dans l'item
     */
    public static ItemStack captureToItem(DebugBeeEntity bee) {
        ItemStack stack = new ItemStack(com.chapeau.beemancer.core.registry.BeemancerItems.BEE_DEBUG.get());
        
        CompoundTag tag = new CompoundTag();
        tag.putInt("BeeHp", bee.getBeeHp());
        tag.putInt("BeeSpeed", bee.getBeeSpeed());
        tag.putInt("BeeStrength", bee.getBeeStrength());
        
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        
        return stack;
    }

    /**
     * Récupère les stats depuis l'ItemStack
     */
    public static int[] getStats(ItemStack stack) {
        int hp = 1, speed = 1, strength = 1;
        
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag tag = customData.copyTag();
            if (tag.contains("BeeHp")) hp = tag.getInt("BeeHp");
            if (tag.contains("BeeSpeed")) speed = tag.getInt("BeeSpeed");
            if (tag.contains("BeeStrength")) strength = tag.getInt("BeeStrength");
        }
        
        return new int[]{hp, speed, strength};
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        
        if (net.minecraft.client.gui.screens.Screen.hasShiftDown()) {
            // Afficher les stats détaillées
            int[] stats = getStats(stack);
            tooltipComponents.add(Component.translatable("item.beemancer.bee_debug.stat.hp", stats[0])
                    .withStyle(ChatFormatting.GREEN));
            tooltipComponents.add(Component.translatable("item.beemancer.bee_debug.stat.strength", stats[2])
                    .withStyle(ChatFormatting.RED));
            tooltipComponents.add(Component.translatable("item.beemancer.bee_debug.stat.speed", stats[1])
                    .withStyle(ChatFormatting.BLUE));
        } else {
            // Message "Press shift for more detail"
            tooltipComponents.add(Component.translatable("item.beemancer.bee_debug.shift_hint")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
    }
}
