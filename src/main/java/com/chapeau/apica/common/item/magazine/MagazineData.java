/**
 * ============================================================
 * [MagazineData.java]
 * Description: R/W data du magazine attache sur un item holder via CUSTOM_DATA
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | DataComponents      | Stockage NBT         | CUSTOM_DATA read/write         |
 * | MagazineFluidData   | Data magazine item   | Creation stack au retrait       |
 * | ApicaItems          | Registre items       | Creation MagazineItem stack     |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - IMagazineHolder.java (verification)
 * - LeafBlowerItem.java (consommation fluide)
 * - MiningLaserItem.java (consommation fluide)
 * - BuildingWandItem.java (consommation fluide)
 * - ChopperHiveItem.java (consommation fluide)
 * - MagazineEquipPacket.java (equip/unequip)
 * - MagazineGaugeHud.java (lecture niveau)
 * - ContainerScreenMagazineMixin.java (affichage)
 *
 * ============================================================
 */
package com.chapeau.apica.common.item.magazine;

import com.chapeau.apica.core.registry.ApicaItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Utilitaire statique pour lire/ecrire les donnees d'un magazine attache
 * sur un item holder (ex: LeafBlower). Le magazine est stocke dans CUSTOM_DATA
 * du holder, pas comme un item imbrique.
 */
public final class MagazineData {

    private static final String ROOT_KEY = "MagazineData";
    private static final String FLUID_ID_KEY = "FluidId";
    private static final String AMOUNT_KEY = "Amount";
    private static final String CREATIVE_KEY = "Creative";
    private static final String LAST_FLUID_KEY = "LastFluidId";

    private MagazineData() {}

    /** Verifie si un magazine est attache au holder. */
    public static boolean hasMagazine(ItemStack holder) {
        CompoundTag tag = getDataTag(holder);
        if (tag == null) return false;
        String fluidId = tag.getString(FLUID_ID_KEY);
        return !fluidId.isEmpty();
    }

    /** Retourne l'ID du fluide du magazine attache, ou "" si aucun. */
    public static String getFluidId(ItemStack holder) {
        CompoundTag tag = getDataTag(holder);
        if (tag == null) return "";
        return tag.getString(FLUID_ID_KEY);
    }

    /** Retourne la quantite de fluide restante dans le magazine attache. */
    public static int getFluidAmount(ItemStack holder) {
        CompoundTag tag = getDataTag(holder);
        if (tag == null) return 0;
        // Creative magazine toujours plein
        if (tag.getBoolean(CREATIVE_KEY)) return MagazineFluidData.MAX_CAPACITY;
        return tag.getInt(AMOUNT_KEY);
    }

    /** Verifie si le magazine attache est un magazine creatif. */
    public static boolean isCreative(ItemStack holder) {
        CompoundTag tag = getDataTag(holder);
        if (tag == null) return false;
        return tag.getBoolean(CREATIVE_KEY);
    }

    /** Attache un magazine au holder avec le fluide et la quantite donnes. */
    public static void setMagazine(ItemStack holder, String fluidId, int amount) {
        setMagazine(holder, fluidId, amount, false);
    }

    /** Attache un magazine au holder avec le fluide, la quantite, et le flag creatif. */
    public static void setMagazine(ItemStack holder, String fluidId, int amount, boolean creative) {
        CompoundTag tag = getOrCreateDataTag(holder);
        tag.putString(FLUID_ID_KEY, fluidId);
        tag.putInt(AMOUNT_KEY, Math.max(0, amount));
        tag.putBoolean(CREATIVE_KEY, creative);
        // Enregistrer le dernier fluide equipe pour le reload
        if (!fluidId.isEmpty()) {
            tag.putString(LAST_FLUID_KEY, fluidId);
        }
        saveDataTag(holder, tag);
    }

    /** Retourne l'ID du dernier fluide equipe, ou "" si aucun. */
    public static String getLastFluidId(ItemStack holder) {
        CompoundTag tag = getDataTag(holder);
        if (tag == null) return "";
        return tag.getString(LAST_FLUID_KEY);
    }

    /** Definit le dernier fluide equipe (pour le reload). */
    public static void setLastFluidId(ItemStack holder, String fluidId) {
        CompoundTag tag = getOrCreateDataTag(holder);
        tag.putString(LAST_FLUID_KEY, fluidId);
        saveDataTag(holder, tag);
    }

    /**
     * Retire le magazine du holder et retourne un MagazineItem avec le fluide restant.
     * Si aucun magazine n'est attache, retourne ItemStack.EMPTY.
     */
    public static ItemStack removeMagazine(ItemStack holder) {
        if (!hasMagazine(holder)) return ItemStack.EMPTY;

        String fluidId = getFluidId(holder);
        int amount = getFluidAmount(holder);
        boolean creative = isCreative(holder);

        // Supprimer les donnees du holder
        CustomData customData = holder.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag outer = customData.copyTag();
            outer.remove(ROOT_KEY);
            if (outer.isEmpty()) {
                holder.remove(DataComponents.CUSTOM_DATA);
            } else {
                holder.set(DataComponents.CUSTOM_DATA, CustomData.of(outer));
            }
        }

        // Creer le MagazineItem avec le fluide restant
        if (creative) {
            return new ItemStack(ApicaItems.CREATIVE_MAGAZINE.get());
        }
        ItemStack magazine = new ItemStack(ApicaItems.MAGAZINE.get());
        if (amount > 0 && !fluidId.isEmpty()) {
            MagazineFluidData.setFluid(magazine, fluidId, amount);
        }
        return magazine;
    }

    /**
     * Consomme du fluide du magazine attache.
     * Si le fluide restant est insuffisant mais > 0, consomme tout le reste et retourne true.
     * Retourne false uniquement si le magazine est absent ou deja vide.
     * Les magazines creatifs ne sont jamais consommes.
     */
    public static boolean consumeFluid(ItemStack holder, int cost) {
        if (cost <= 0) return true;
        CompoundTag tag = getDataTag(holder);
        if (tag == null) return false;

        // Creative magazine ne se vide jamais
        if (tag.getBoolean(CREATIVE_KEY)) return true;

        int current = tag.getInt(AMOUNT_KEY);
        if (current <= 0) return false;

        CompoundTag writeTag = getOrCreateDataTag(holder);
        writeTag.putInt(AMOUNT_KEY, Math.max(0, current - cost));
        saveDataTag(holder, writeTag);
        return true;
    }

    // =========================================================================
    // Fluid cost helpers
    // =========================================================================

    /**
     * Retourne le multiplicateur de cout base sur le fluide du magazine attache.
     * Honey = 1x, Royal Jelly = 0.9x, Nectar = 0.8x.
     */
    public static float getFluidCostMultiplier(ItemStack holder) {
        String fluidId = getFluidId(holder);
        if (fluidId.contains("nectar")) return 0.8f;
        if (fluidId.contains("royal_jelly")) return 0.9f;
        return 1f;
    }

    /**
     * Calcule le cout effectif apres application du multiplicateur fluide.
     * Retourne au minimum 1 mB.
     */
    public static int computeEffectiveCost(ItemStack holder, int baseCost) {
        return Math.max(1, Math.round(baseCost * getFluidCostMultiplier(holder)));
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
