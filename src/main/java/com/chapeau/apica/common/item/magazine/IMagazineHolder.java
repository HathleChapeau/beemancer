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
 * | MagazineConstants   | Constantes fluides   | NECTAR_ID                      |
 * | MagazineFluidData   | Lecture fluide mag   | Verification compatibilite     |
 * | CreativeMagazineItem| Magazine creatif     | Detection instanceof           |
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

import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Optional;
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
     * Les magazines creatifs sont traites comme du nectar.
     */
    default boolean canAcceptMagazine(ItemStack magazineStack) {
        // Creative magazine = nectar
        if (magazineStack.getItem() instanceof CreativeMagazineItem) {
            return getAcceptedFluids().contains(MagazineConstants.NECTAR_ID);
        }
        String fluidId = MagazineFluidData.getFluidId(magazineStack);
        return !fluidId.isEmpty() && getAcceptedFluids().contains(fluidId);
    }

    /**
     * Gere le clic droit quand le magazine est vide/absent.
     * Tente un reload et retourne SUCCESS pour interrompre l'action.
     * Le joueur devra refaire un clic pour utiliser l'item.
     *
     * Le reload ne se declenche que sur la transition mouse DOWN (pas maintained).
     *
     * @return Optional.empty() si le magazine est valide (continuer l'action),
     *         Optional.of(SUCCESS) si reload tente (interrompre l'action)
     */
    default Optional<InteractionResultHolder<ItemStack>> tryReloadOnUse(Level level, Player player, ItemStack holder) {
        if (!MagazineData.hasMagazine(holder) || MagazineData.getFluidAmount(holder) <= 0) {
            // Client: verifier que c'est un vrai mouse DOWN (pas maintenu)
            if (level.isClientSide() && !MagazineInputHelper.shouldAllowReload()) {
                return Optional.of(InteractionResultHolder.pass(holder));
            }
            if (!level.isClientSide()) {
                MagazineReloadHelper.tryReload(player, holder);
            }
            return Optional.of(InteractionResultHolder.success(holder));
        }
        return Optional.empty();
    }

    /**
     * Version pour useOn() (clic sur bloc).
     * Meme logique que tryReloadOnUse mais retourne InteractionResult.
     *
     * @return Optional.empty() si le magazine est valide (continuer l'action),
     *         Optional.of(SUCCESS) si reload tente (interrompre l'action)
     */
    default Optional<InteractionResult> tryReloadOnUseOn(Level level, Player player, ItemStack holder) {
        if (!MagazineData.hasMagazine(holder) || MagazineData.getFluidAmount(holder) <= 0) {
            // Client: verifier que c'est un vrai mouse DOWN (pas maintenu)
            if (level.isClientSide() && !MagazineInputHelper.shouldAllowReload()) {
                return Optional.of(InteractionResult.PASS);
            }
            if (!level.isClientSide()) {
                MagazineReloadHelper.tryReload(player, holder);
            }
            return Optional.of(InteractionResult.SUCCESS);
        }
        return Optional.empty();
    }
}
