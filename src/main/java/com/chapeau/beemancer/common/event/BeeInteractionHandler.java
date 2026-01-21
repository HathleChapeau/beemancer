/**
 * ============================================================
 * [BeeInteractionHandler.java]
 * Description: Gère les interactions spéciales avec les abeilles
 * ============================================================
 * 
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation         |
 * |---------------------|----------------------|---------------------|
 * | DebugBeeEntity      | Entité cible         | Capture abeille     |
 * | BeeDebugItem        | Item résultant       | Création item       |
 * | BeemancerItems      | Vérification item    | Type checking       |
 * ------------------------------------------------------------
 * 
 * UTILISÉ PAR:
 * - Chargé automatiquement via @EventBusSubscriber
 * 
 * ============================================================
 */
package com.chapeau.beemancer.common.event;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.entity.bee.DebugBeeEntity;
import com.chapeau.beemancer.common.item.bee.BeeDebugItem;
import com.chapeau.beemancer.core.registry.BeemancerItems;
import net.minecraft.world.InteractionHand;
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
        
        // Vérifier si c'est un shift+clic droit sur une DebugBee
        if (player.isShiftKeyDown() && event.getTarget() instanceof DebugBeeEntity bee) {
            ItemStack heldItem = player.getItemInHand(event.getHand());
            
            // Vérifier si on tient un BeeDebugItem vide ou rien
            boolean canCapture = heldItem.isEmpty() || 
                    (heldItem.is(BeemancerItems.BEE_DEBUG.get()) && heldItem.getCount() < heldItem.getMaxStackSize());

            if (canCapture && !player.level().isClientSide()) {
                // Créer l'item avec les stats de l'abeille
                ItemStack beeItem = BeeDebugItem.captureToItem(bee);
                
                // Donner l'item au joueur
                if (!player.getInventory().add(beeItem)) {
                    player.drop(beeItem, false);
                }
                
                // Supprimer l'entité
                bee.discard();
                
                event.setCanceled(true);
            }
        }
    }
}
