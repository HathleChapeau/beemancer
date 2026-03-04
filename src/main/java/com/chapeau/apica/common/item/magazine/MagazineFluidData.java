/**
 * ============================================================
 * [MagazineFluidData.java]
 * Description: R/W data fluide stocke sur un MagazineItem via CUSTOM_DATA
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | DataComponents      | Stockage NBT         | CUSTOM_DATA read/write         |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - MagazineItem.java (barre durabilite, tooltip)
 * - MagazineData.java (equip/unequip)
 * - IMagazineHolder.java (verification fluide accepte)
 *
 * ============================================================
 */
package com.chapeau.apica.common.item.magazine;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Utilitaire statique pour lire/ecrire les donnees de fluide sur un MagazineItem.
 * Stocke le type de fluide (ResourceLocation string) et la quantite (mB) dans CUSTOM_DATA.
 * Pattern identique a HoverbikePartData.
 */
public final class MagazineFluidData {

    private static final String ROOT_KEY = "MagazineFluid";
    private static final String FLUID_ID_KEY = "FluidId";
    private static final String AMOUNT_KEY = "Amount";

    /** Capacite maximale d'un magazine en millibuckets. */
    public static final int MAX_CAPACITY = 1000;

    private MagazineFluidData() {}

    /** Retourne l'identifiant du fluide (ex: "apica:honey"), ou "" si vide. */
    public static String getFluidId(ItemStack stack) {
        CompoundTag tag = getDataTag(stack);
        if (tag == null) return "";
        return tag.getString(FLUID_ID_KEY);
    }

    /** Retourne la quantite de fluide en mB (0 si vide). */
    public static int getFluidAmount(ItemStack stack) {
        CompoundTag tag = getDataTag(stack);
        if (tag == null) return 0;
        return tag.getInt(AMOUNT_KEY);
    }

    /** Ecrit le type et la quantite de fluide sur le stack. */
    public static void setFluid(ItemStack stack, String fluidId, int amount) {
        CompoundTag tag = getOrCreateDataTag(stack);
        tag.putString(FLUID_ID_KEY, fluidId);
        tag.putInt(AMOUNT_KEY, Math.max(0, Math.min(amount, MAX_CAPACITY)));
        saveDataTag(stack, tag);
    }

    /** Retourne true si le magazine est vide (pas de fluide ou quantite 0). */
    public static boolean isEmpty(ItemStack stack) {
        String id = getFluidId(stack);
        return id.isEmpty() || getFluidAmount(stack) <= 0;
    }

    // =========================================================================
    // NBT Helpers (pattern HoverbikePartData)
    // =========================================================================

    private static CompoundTag getDataTag(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return null;
        CompoundTag tag = customData.copyTag();
        if (!tag.contains(ROOT_KEY, Tag.TAG_COMPOUND)) return null;
        return tag.getCompound(ROOT_KEY);
    }

    private static CompoundTag getOrCreateDataTag(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag outer = (customData != null) ? customData.copyTag() : new CompoundTag();
        if (!outer.contains(ROOT_KEY, Tag.TAG_COMPOUND)) {
            outer.put(ROOT_KEY, new CompoundTag());
        }
        return outer.getCompound(ROOT_KEY);
    }

    private static void saveDataTag(ItemStack stack, CompoundTag dataRoot) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag outer = (customData != null) ? customData.copyTag() : new CompoundTag();
        outer.put(ROOT_KEY, dataRoot);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(outer));
    }
}
