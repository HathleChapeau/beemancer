/**
 * ============================================================
 * [IMagazineHolder.java]
 * Description: Interface pour items acceptant un magazine de fluide
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | MagazineFluidData   | Lecture fluide mag   | Verification compatibilite     |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - LeafBlowerItem.java (implementation)
 * - MiningLaserItem.java (implementation)
 * - BuildingWandItem.java (implementation)
 * - ChopperHiveItem.java (implementation)
 * - ContainerScreenMagazineMixin.java (detection items)
 * - MagazineGaugeHud.java (detection items)
 * - MagazineEquipPacket.java (validation)
 *
 * ============================================================
 */
package com.chapeau.apica.common.item.magazine;

import net.minecraft.world.item.ItemStack;

import java.util.Set;

/**
 * Interface pour les items qui acceptent un magazine de fluide.
 * L'item implementant cette interface controle entierement quand et combien
 * de fluide consommer via MagazineData.consumeFluid().
 * Aucune consommation automatique n'est effectuee par le systeme.
 */
public interface IMagazineHolder {

    /**
     * Retourne les identifiants des fluides acceptes par cet item.
     * Ex: Set.of("apica:honey") pour le LeafBlower.
     */
    Set<String> getAcceptedFluids();

    /**
     * Verifie si un MagazineItem peut etre equipe sur cet item.
     * Par defaut, verifie que le fluide du magazine est dans la liste acceptee.
     */
    default boolean canAcceptMagazine(ItemStack magazineStack) {
        String fluidId = MagazineFluidData.getFluidId(magazineStack);
        return !fluidId.isEmpty() && getAcceptedFluids().contains(fluidId);
    }
}
