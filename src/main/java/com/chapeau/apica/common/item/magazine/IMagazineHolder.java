/**
 * ============================================================
 * [IMagazineHolder.java]
 * Description: Interface pour items acceptant un magazine de fluide
 * ============================================================
 */
package com.chapeau.apica.common.item.magazine;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface IMagazineHolder {

    // Track reload state par joueur (server + client)
    Map<UUID, Boolean> RELOAD_STATE = new HashMap<>();

    Set<String> getAcceptedFluids();

    // =========================================================================
    // Reload state
    // =========================================================================

    default boolean isReloading(Player player) {
        return RELOAD_STATE.getOrDefault(player.getUUID(), false);
    }

    default void setReloading(Player player, boolean reloading) {
        RELOAD_STATE.put(player.getUUID(), reloading);
    }

    // =========================================================================
    // Check methods
    // =========================================================================

    /** True si peut utiliser l'item (magazine plein ET pas en reload). */
    default boolean canUse(Player player, ItemStack holder) {
        if (isReloading(player)) return false;
        if (!MagazineData.hasMagazine(holder)) return false;
        return MagazineData.getFluidAmount(holder) > 0;
    }

    /** True si peut reloader (mouse DOWN, magazine vide, pas déjà en reload). Consomme le click. */
    default boolean canReload(Player player, ItemStack holder) {
        if (isReloading(player)) return false;
        if (!MagazineInputHelper.isMouseDown()) return false;
        if (!MagazineData.hasMagazine(holder)) {
            MagazineInputHelper.consume();
            return true;
        }
        if (MagazineData.getFluidAmount(holder) <= 0) {
            MagazineInputHelper.consume();
            return true;
        }
        return false;
    }

    // =========================================================================
    // Actions
    // =========================================================================

    /** Effectue le reload. Appeler côté server uniquement. */
    default void doReload(Player player, ItemStack holder) {
        MagazineReloadHelper.tryReload(player, holder);
        setReloading(player, true);
    }

    /** Reset le reload state. Appeler quand mouse UP. */
    default void resetReload(Player player) {
        setReloading(player, false);
    }

    // =========================================================================
    // Magazine acceptance
    // =========================================================================

    default boolean canAcceptMagazine(ItemStack magazineStack) {
        if (magazineStack.getItem() instanceof CreativeMagazineItem) {
            return getAcceptedFluids().contains(MagazineConstants.NECTAR_ID);
        }
        String fluidId = MagazineFluidData.getFluidId(magazineStack);
        return !fluidId.isEmpty() && getAcceptedFluids().contains(fluidId);
    }
}
