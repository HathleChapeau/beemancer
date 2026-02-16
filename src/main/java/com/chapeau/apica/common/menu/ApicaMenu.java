/**
 * ============================================================
 * [ApicaMenu.java]
 * Description: Classe de base pour les menus Apica avec inventaire joueur
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | AbstractContainerMenu   | Base Minecraft menu  | Slots et interactions          |
 * | Slot                    | Slots inventaire     | Ajout player inventory/hotbar  |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ManualCentrifugeMenu, PoweredCentrifugeMenu, AlembicMenu,
 *   InfuserMenu, CrystallizerMenu, HoneyTankMenu, MultiblockTankMenu,
 *   IncubatorMenu, MagicHiveMenu
 *
 * ============================================================
 */
package com.chapeau.apica.common.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.function.Predicate;

/**
 * Base class pour les menus Apica.
 * Fournit les méthodes utilitaires pour l'inventaire et la hotbar du joueur.
 */
public abstract class ApicaMenu extends AbstractContainerMenu {

    protected ApicaMenu(@Nullable MenuType<?> menuType, int containerId) {
        super(menuType, containerId);
    }

    /**
     * Ajoute les 27 slots de l'inventaire principal du joueur (3 rangées de 9).
     *
     * @param playerInv l'inventaire du joueur
     * @param x         coordonnée X de départ (coin supérieur gauche)
     * @param y         coordonnée Y de départ (coin supérieur gauche)
     */
    protected final void addPlayerInventory(Inventory playerInv, int x, int y) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, x + col * 18, y + row * 18));
            }
        }
    }

    /**
     * Ajoute les 9 slots de la hotbar du joueur.
     *
     * @param playerInv l'inventaire du joueur
     * @param x         coordonnée X de départ
     * @param y         coordonnée Y de départ
     */
    protected final void addPlayerHotbar(Inventory playerInv, int x, int y) {
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, x + col * 18, y));
        }
    }

    /**
     * Implementation generique de quickMoveStack pour les menus avec slots container + inventaire joueur.
     * Gere: container→joueur, joueur→input (filtre optionnel), inv↔hotbar.
     *
     * @param index        index du slot clique
     * @param containerEnd premier index de l'inventaire joueur (= nombre de slots container)
     * @param inputStart   premier slot d'input pour le deplacement joueur→container (-1 pour desactiver)
     * @param inputEnd     un apres le dernier slot d'input (-1 pour desactiver)
     * @param inputFilter  filtre pour tester si un item peut aller dans l'input (null = essayer sans filtre)
     * @return l'ItemStack resultat du deplacement
     */
    protected ItemStack doQuickMove(int index, int containerEnd,
                                     int inputStart, int inputEnd,
                                     @Nullable Predicate<ItemStack> inputFilter) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();

        int playerInvStart = containerEnd;
        int playerInvEnd = containerEnd + 27;
        int hotbarStart = playerInvEnd;
        int hotbarEnd = hotbarStart + 9;

        if (index < containerEnd) {
            // Container → player
            if (!moveItemStackTo(stack, playerInvStart, hotbarEnd, true))
                return ItemStack.EMPTY;
        } else {
            boolean tryInput = inputStart >= 0 && inputEnd > inputStart;
            boolean filterPasses = tryInput && (inputFilter == null || inputFilter.test(stack));

            if (filterPasses) {
                if (!moveItemStackTo(stack, inputStart, inputEnd, false))
                    return ItemStack.EMPTY;
            } else if (index >= playerInvStart && index < playerInvEnd) {
                if (!moveItemStackTo(stack, hotbarStart, hotbarEnd, false))
                    return ItemStack.EMPTY;
            } else if (index >= hotbarStart && index < hotbarEnd) {
                if (!moveItemStackTo(stack, playerInvStart, playerInvEnd, false))
                    return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return result;
    }
}
