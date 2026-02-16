/**
 * ============================================================
 * [HoverbikePartItem.java]
 * Description: Item representant une piece modulaire du hoverbike
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | HoverbikePart       | Enum categories      | Identifie la categorie         |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ApicaItems.java: Enregistrement des 12 items de pieces
 * - AssemblyTableBlock.java: Validation du type d'item
 * - ApicaCreativeTabs.java: Ajout dans le tab hoverbike
 *
 * ============================================================
 */
package com.chapeau.apica.common.item.mount;

import com.chapeau.apica.common.entity.mount.HoverbikePart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Item pour une piece de hoverbike.
 * Chaque variante (3 par categorie, 4 categories = 12 items) a sa propre instance.
 */
public class HoverbikePartItem extends Item {

    private final HoverbikePart category;
    private final int variantIndex;

    public HoverbikePartItem(Properties properties, HoverbikePart category, int variantIndex) {
        super(properties);
        this.category = category;
        this.variantIndex = variantIndex;
    }

    public HoverbikePart getCategory() {
        return category;
    }

    public int getVariantIndex() {
        return variantIndex;
    }

    /**
     * Verifie si un ItemStack est une piece de hoverbike.
     */
    public static boolean isHoverbikePart(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof HoverbikePartItem;
    }
}
