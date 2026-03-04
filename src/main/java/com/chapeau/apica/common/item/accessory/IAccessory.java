/**
 * ============================================================
 * [IAccessory.java]
 * Description: Interface pour les items equipables dans les slots accessoire
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | Player              | Contexte joueur      | Callbacks equip/unequip        |
 * | ItemStack           | Stack equipe         | Reference item                 |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - BackpackItem.java (implementation)
 * - AccessoryEquipPacket.java (validation type)
 * - InventoryScreenAccessoryMixin.java (detection slots)
 *
 * ============================================================
 */
package com.chapeau.apica.common.item.accessory;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Interface marqueur pour les items qui peuvent etre places dans les slots accessoire.
 * Les callbacks onEquip/onUnequip sont appeles cote serveur lors de l'equipement.
 */
public interface IAccessory {

    /**
     * Appele cote serveur quand l'accessoire est equipe dans un slot.
     * @param player le joueur qui equipe
     * @param stack le stack equipe
     */
    default void onEquip(Player player, ItemStack stack) {
    }

    /**
     * Appele cote serveur quand l'accessoire est retire d'un slot.
     * @param player le joueur qui retire
     * @param stack le stack retire
     */
    default void onUnequip(Player player, ItemStack stack) {
    }

    /**
     * Indique si cet accessoire ajoute un onglet en haut de l'inventaire.
     * @return true si un tab doit etre affiche
     */
    default boolean hasInventoryTab() {
        return false;
    }

    /**
     * Appele cote client quand le joueur clique sur le tab de cet accessoire.
     * @param accessorySlot l'index du slot accessoire (0 ou 1)
     */
    default void onInventoryTabClicked(int accessorySlot) {
    }

    /**
     * Retourne l'icone a afficher dans le tab de l'inventaire pour cet accessoire.
     * Si null, utilise l'item lui-meme (comportement par defaut).
     * @return l'ItemStack icone du tab, ou null pour le comportement par defaut
     */
    @Nullable
    default ItemStack getTabIcon() {
        return null;
    }
}
