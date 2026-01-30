/**
 * ============================================================
 * [BeemancerMenu.java]
 * Description: Classe de base pour les menus Beemancer avec inventaire joueur
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
 *   IncubatorMenu, MagicHiveMenu, StorageControllerMenu
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;

import javax.annotation.Nullable;

/**
 * Base class pour les menus Beemancer.
 * Fournit les méthodes utilitaires pour l'inventaire et la hotbar du joueur.
 */
public abstract class BeemancerMenu extends AbstractContainerMenu {

    protected BeemancerMenu(@Nullable MenuType<?> menuType, int containerId) {
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
}
