/**
 * ============================================================
 * [BeemancerSlot.java]
 * Description: Slot personnalise avec support d'image de fond
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | SlotItemHandler     | Classe parente       | Fonctionnalite de base slot    |
 * | ResourceLocation    | Chemin texture       | Image de fond optionnelle      |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - Tous les menus d'alchimie
 * - Tout menu necessitant un indicateur visuel de slot
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.widget;

import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

import javax.annotation.Nullable;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class BeemancerSlot extends SlotItemHandler {
    @Nullable
    private final ResourceLocation backgroundTexture;
    @Nullable
    private final ResourceLocation backgroundAtlas;
    @Nullable
    private Predicate<ItemStack> filter;
    @Nullable
    private BiConsumer<net.minecraft.world.entity.player.Player, ItemStack> onExtractCallback;
    private boolean canInsert = true;
    private boolean canExtract = true;

    /**
     * Constructeur de base - utilise le fond de slot Minecraft par defaut
     */
    public BeemancerSlot(IItemHandler itemHandler, int index, int x, int y) {
        super(itemHandler, index, x, y);
        this.backgroundTexture = null;
        this.backgroundAtlas = null;
    }

    /**
     * Constructeur avec image de fond personnalisee
     * @param backgroundTexture Chemin vers la texture dans l'atlas des blocs
     */
    public BeemancerSlot(IItemHandler itemHandler, int index, int x, int y, ResourceLocation backgroundTexture) {
        super(itemHandler, index, x, y);
        this.backgroundTexture = backgroundTexture;
        this.backgroundAtlas = InventoryMenu.BLOCK_ATLAS;
    }

    /**
     * Constructeur avec atlas et texture personnalises
     */
    public BeemancerSlot(IItemHandler itemHandler, int index, int x, int y,
                         ResourceLocation backgroundAtlas, ResourceLocation backgroundTexture) {
        super(itemHandler, index, x, y);
        this.backgroundTexture = backgroundTexture;
        this.backgroundAtlas = backgroundAtlas;
    }

    /**
     * Definit un filtre pour les items acceptes
     */
    public BeemancerSlot withFilter(Predicate<ItemStack> filter) {
        this.filter = filter;
        return this;
    }

    /**
     * Desactive l'insertion (slot output only)
     */
    public BeemancerSlot outputOnly() {
        this.canInsert = false;
        return this;
    }

    /**
     * Desactive l'extraction (slot input only)
     */
    public BeemancerSlot inputOnly() {
        this.canExtract = false;
        return this;
    }

    /**
     * Definit un callback appele quand un joueur extrait un item du slot.
     * @param callback BiConsumer(Player, ItemStack) appele a l'extraction
     */
    public BeemancerSlot withOnExtract(BiConsumer<net.minecraft.world.entity.player.Player, ItemStack> callback) {
        this.onExtractCallback = callback;
        return this;
    }

    @Override
    public void onTake(net.minecraft.world.entity.player.Player player, ItemStack stack) {
        super.onTake(player, stack);
        if (onExtractCallback != null && !stack.isEmpty()) {
            onExtractCallback.accept(player, stack);
        }
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        if (!canInsert) return false;
        if (filter != null && !filter.test(stack)) return false;
        return super.mayPlace(stack);
    }

    @Override
    public boolean mayPickup(net.minecraft.world.entity.player.Player player) {
        if (!canExtract) return false;
        return super.mayPickup(player);
    }

    @Nullable
    @Override
    public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
        if (backgroundTexture != null && backgroundAtlas != null) {
            return Pair.of(backgroundAtlas, backgroundTexture);
        }
        return super.getNoItemIcon();
    }

    /**
     * Factory method pour creer un slot d'input avec icone de rayon (comb)
     */
    public static BeemancerSlot combInput(IItemHandler handler, int index, int x, int y) {
        return new BeemancerSlot(handler, index, x, y,
            ResourceLocation.fromNamespaceAndPath("beemancer", "gui/empty_slot_comb"));
    }

    /**
     * Factory method pour creer un slot d'output (pas d'insertion)
     */
    public static BeemancerSlot output(IItemHandler handler, int index, int x, int y) {
        return new BeemancerSlot(handler, index, x, y).outputOnly();
    }

    /**
     * Factory method pour creer un slot d'output avec icone
     */
    public static BeemancerSlot output(IItemHandler handler, int index, int x, int y, ResourceLocation icon) {
        return new BeemancerSlot(handler, index, x, y, icon).outputOnly();
    }

    /**
     * Factory method pour creer un slot de bois (logs)
     */
    public static BeemancerSlot woodInput(IItemHandler handler, int index, int x, int y) {
        return new BeemancerSlot(handler, index, x, y,
            ResourceLocation.fromNamespaceAndPath("beemancer", "item/empty_slot_wood"));
    }
}
