/**
 * ============================================================
 * [BackpackTooltip.java]
 * Description: Donnees tooltip pour la preview du contenu d'un backpack
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | ItemStack           | Items a afficher     | Liste des stacks               |
 * | TooltipComponent    | Systeme vanilla      | Interface tooltip custom       |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - BackpackItem.java (getTooltipImage)
 * - ClientBackpackTooltip.java (rendu client)
 *
 * ============================================================
 */
package com.chapeau.apica.common.item;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Contient la liste des items a afficher dans le tooltip visuel du backpack.
 * Maximum 12 items (grille 6x2).
 */
public record BackpackTooltip(List<ItemStack> items) implements TooltipComponent {

    public static final int MAX_DISPLAY = 12;
    public static final int COLUMNS = 6;
}
