/**
 * ============================================================
 * [BeeInteractionHandler.java]
 * Description: Gère les interactions spéciales avec les abeilles
 * ============================================================
 */
package com.chapeau.beemancer.common.event;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.entity.bee.MagicBeeEntity;
import com.chapeau.beemancer.common.item.bee.MagicBeeItem;
import com.chapeau.beemancer.core.registry.BeemancerItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = Beemancer.MOD_ID)
public class BeeInteractionHandler {

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        
        // Shift+right-click on MagicBee to capture
        if (player.isShiftKeyDown() && event.getTarget() instanceof MagicBeeEntity bee) {
            ItemStack heldItem = player.getItemInHand(event.getHand());
            
            boolean canCapture = heldItem.isEmpty() || 
                    (heldItem.is(BeemancerItems.MAGIC_BEE.get()) && heldItem.getCount() < heldItem.getMaxStackSize());

            if (canCapture && !player.level().isClientSide()) {
                ItemStack beeItem = MagicBeeItem.captureFromEntity(bee);
                
                if (!player.getInventory().add(beeItem)) {
                    player.drop(beeItem, false);
                }
                
                bee.discard();
                event.setCanceled(true);
            }
        }
    }
}
