/**
 * ============================================================
 * [PollenPotEvents.java]
 * Description: Gestionnaire d'événements pour le Pollen Pot (clic gauche)
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | PollenPotBlockEntity    | Accès au stockage    | Retirer pollen                 |
 * | PollenPotBlock          | Vérification bloc    | Type de bloc                   |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - Beemancer.java (enregistrement événements)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.pollenpot;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Gère le clic gauche sur le Pollen Pot pour retirer du pollen.
 */
public class PollenPotEvents {

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        // Éviter double exécution (event appelé pour chaque main)
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        Player player = event.getEntity();

        BlockState state = level.getBlockState(pos);

        // Vérifier si c'est un Pollen Pot
        if (!(state.getBlock() instanceof PollenPotBlock)) return;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof PollenPotBlockEntity pot)) return;

        // Si le pot est vide, laisser le comportement normal (casser le bloc)
        if (pot.isEmpty()) return;

        // Annuler immédiatement pour empêcher de casser le bloc (côté client et serveur)
        event.setCanceled(true);

        // Côté client: juste annuler, pas de logique
        if (level.isClientSide()) return;

        // Côté serveur: retirer un pollen
        ItemStack removed = pot.removePollen();
        if (!removed.isEmpty()) {
            // Donner au joueur ou drop
            if (!player.getInventory().add(removed)) {
                player.drop(removed, false);
            }

            level.playSound(null, pos, SoundEvents.SAND_BREAK, SoundSource.BLOCKS, 0.5f, 1.0f);
        }
    }
}
