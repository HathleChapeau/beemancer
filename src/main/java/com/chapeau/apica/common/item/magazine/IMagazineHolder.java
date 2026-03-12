/**
 * ============================================================
 * [IMagazineHolder.java]
 * Description: Interface pour items acceptant un magazine de fluide
 * ============================================================
 */
package com.chapeau.apica.common.item.magazine;

import com.chapeau.apica.Apica;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface IMagazineHolder {

    String TAG_ON_RIGHT_CLICK = "OnRightClick";
    String TAG_WAS_HELD = "WasHeld";

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
    // OnRightClick (stocké sur l'item)
    // =========================================================================

    default boolean isOnRightClick(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        Apica.LOGGER.info("custom data {}", customData.copyTag().getBoolean(TAG_ON_RIGHT_CLICK));
        if (customData == null) return false;
        return customData.copyTag().getBoolean(TAG_ON_RIGHT_CLICK);
    }

    default void setOnRightClick(ItemStack stack, boolean value) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = customData != null ? customData.copyTag() : new CompoundTag();
        tag.putBoolean(TAG_ON_RIGHT_CLICK, value);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    // =========================================================================
    // WasHeld tracking (pour reset onRightClick quand pris en main)
    // =========================================================================

    default boolean wasHeld(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return false;
        return customData.copyTag().getBoolean(TAG_WAS_HELD);
    }

    default void setWasHeld(ItemStack stack, boolean value) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = customData != null ? customData.copyTag() : new CompoundTag();
        tag.putBoolean(TAG_WAS_HELD, value);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /** Appeler dans inventoryTick. Reset onRightClick quand l'item est pris en main. */
    default void trackHeldState(ItemStack stack, boolean isHeld) {
        if (isHeld && !wasHeld(stack)) {
            // Vient d'être pris en main -> reset
            Apica.LOGGER.info("RESET RIGHT CLICK");
            setOnRightClick(stack, false);
            setWasHeld(stack, true);
        } else if (!isHeld && wasHeld(stack)) {
            // Vient d'être posé
            setWasHeld(stack, false);
        }
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

    /** True si magazine absent ou vide. */
    default boolean needsReload(ItemStack holder) {
        if (!MagazineData.hasMagazine(holder)) return true;
        return MagazineData.getFluidAmount(holder) <= 0;
    }

    /** True si peut reloader (magazine vide, pas en reload). */
    default boolean canReload(Player player, ItemStack holder) {
        if (isReloading(player)) return false;
        return needsReload(holder);
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
