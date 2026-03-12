/**
 * ============================================================
 * [MagazineReloadHelper.java]
 * Description: Logique de reload automatique pour les IMagazineHolder
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | MagazineConstants   | Constantes fluides   | NECTAR_ID                      |
 * | MagazineData        | Data magazine holder | Lecture/ecriture magazine      |
 * | MagazineFluidData   | Data magazine item   | Lecture fluide magazine        |
 * | IMagazineHolder     | Interface holder     | Verification fluide accepte    |
 * | ApicaItems          | Registre items       | Detection MagazineItem         |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - LeafBlowerItem.java (reload sur click droit)
 * - MiningLaserItem.java (reload sur click droit)
 * - BuildingWandItem.java (reload sur click droit)
 * - ChopperHiveItem.java (reload sur click droit)
 * - RailgunItem.java (reload sur click droit)
 *
 * ============================================================
 */
package com.chapeau.apica.common.item.magazine;

import com.chapeau.apica.core.registry.ApicaItems;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Helper pour le systeme de reload automatique des magazines.
 * Quand le joueur fait click droit avec un holder vide:
 * 1. Cherche un magazine avec le meme fluide que le dernier equipe
 * 2. Si trouve: swap empty avec full (si possible)
 * 3. Si pas de meme fluide: prend le premier magazine compatible
 */
public final class MagazineReloadHelper {

    private MagazineReloadHelper() {}

    /**
     * Tente de recharger le holder depuis l'inventaire du joueur.
     *
     * @param player Le joueur
     * @param holder L'item holder (doit implementer IMagazineHolder)
     * @return true si un reload a ete effectue
     */
    public static boolean tryReload(Player player, ItemStack holder) {
        if (!(holder.getItem() instanceof IMagazineHolder magazineHolder)) {
            return false;
        }

        // Verifier si le magazine actuel est vide ou absent
        boolean hasEmptyMagazine = MagazineData.hasMagazine(holder) && MagazineData.getFluidAmount(holder) <= 0;
        boolean noMagazine = !MagazineData.hasMagazine(holder);

        if (!hasEmptyMagazine && !noMagazine) {
            return false; // Magazine present et non vide, pas de reload
        }

        Inventory inventory = player.getInventory();
        String lastFluidId = MagazineData.getLastFluidId(holder);

        // Chercher un magazine de remplacement
        int replacementSlot = -1;
        ItemStack replacementMag = ItemStack.EMPTY;

        // Priorite 1: meme fluide que le dernier equipe
        if (!lastFluidId.isEmpty()) {
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack invStack = inventory.getItem(i);
                if (isMagazineWithFluid(invStack, lastFluidId) && magazineHolder.canAcceptMagazine(invStack)) {
                    int amount = getMagazineAmount(invStack);
                    if (amount > 0) {
                        replacementSlot = i;
                        replacementMag = invStack;
                        break;
                    }
                }
            }
        }

        // Priorite 2: premier magazine compatible avec du fluide
        if (replacementSlot < 0) {
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack invStack = inventory.getItem(i);
                if (isMagazine(invStack) && magazineHolder.canAcceptMagazine(invStack)) {
                    int amount = getMagazineAmount(invStack);
                    if (amount > 0) {
                        replacementSlot = i;
                        replacementMag = invStack;
                        break;
                    }
                }
            }
        }

        if (replacementSlot < 0) {
            return false; // Aucun magazine de remplacement trouve
        }

        // Effectuer le swap
        ItemStack emptyMag = ItemStack.EMPTY;
        if (hasEmptyMagazine) {
            // Retirer le magazine vide du holder
            emptyMag = MagazineData.removeMagazine(holder);
        }

        // Equiper le nouveau magazine
        String newFluidId = getFluidId(replacementMag);
        int newAmount = getMagazineAmount(replacementMag);
        boolean isCreative = replacementMag.getItem() == ApicaItems.CREATIVE_MAGAZINE.get();

        if (isCreative) {
            MagazineData.setMagazine(holder, MagazineConstants.NECTAR_ID, MagazineFluidData.MAX_CAPACITY, true);
        } else {
            MagazineData.setMagazine(holder, newFluidId, newAmount, false);
        }

        // Gerer l'inventaire
        if (!emptyMag.isEmpty()) {
            // Mettre le magazine vide a la place du plein
            inventory.setItem(replacementSlot, emptyMag);
        } else {
            // Pas de magazine vide, juste retirer le plein
            inventory.setItem(replacementSlot, ItemStack.EMPTY);
        }

        // Son de reload
        player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_PICKUP,
                SoundSource.PLAYERS, 0.5f, 1.2f);

        return true;
    }

    /**
     * Verifie si l'item est un magazine (normal ou creatif).
     */
    private static boolean isMagazine(ItemStack stack) {
        return stack.getItem() == ApicaItems.MAGAZINE.get()
            || stack.getItem() == ApicaItems.CREATIVE_MAGAZINE.get();
    }

    /**
     * Verifie si l'item est un magazine avec le fluide specifie.
     */
    private static boolean isMagazineWithFluid(ItemStack stack, String fluidId) {
        if (!isMagazine(stack)) return false;
        return getFluidId(stack).equals(fluidId);
    }

    /**
     * Retourne l'ID du fluide d'un magazine.
     */
    private static String getFluidId(ItemStack magazineStack) {
        if (magazineStack.getItem() == ApicaItems.CREATIVE_MAGAZINE.get()) {
            return MagazineConstants.NECTAR_ID;
        }
        return MagazineFluidData.getFluidId(magazineStack);
    }

    /**
     * Retourne la quantite de fluide d'un magazine.
     */
    private static int getMagazineAmount(ItemStack magazineStack) {
        if (magazineStack.getItem() == ApicaItems.CREATIVE_MAGAZINE.get()) {
            return MagazineFluidData.MAX_CAPACITY;
        }
        return MagazineFluidData.getFluidAmount(magazineStack);
    }
}
