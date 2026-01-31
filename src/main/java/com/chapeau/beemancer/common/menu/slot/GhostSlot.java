/**
 * ============================================================
 * [GhostSlot.java]
 * Description: Slot fantome pour les filtres - n'accepte/extrait rien physiquement
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | SlotItemHandler               | Base NeoForge slot   | Affichage ghost item           |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - NetworkInterfaceMenu.java (filter slots)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.menu.slot;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * Slot fantome: affiche un item comme filtre visuel mais ne permet
 * ni insertion ni extraction physique. Le set/clear est gere par
 * le menu via clicked() override.
 *
 * Supporte un etat actif/inactif: quand inactif, le slot est
 * place hors-ecran (-999, -999) et n'est pas rendu.
 */
public class GhostSlot extends SlotItemHandler {

    private boolean active = true;

    public GhostSlot(IItemHandler handler, int index, int x, int y) {
        super(handler, index, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public boolean mayPickup(net.minecraft.world.entity.player.Player player) {
        return false;
    }

    @Override
    public int getMaxStackSize() {
        return 0;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
