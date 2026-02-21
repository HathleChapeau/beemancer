/**
 * ============================================================
 * [PipeTransitItem.java]
 * Description: Item en transit dans le réseau de pipes avec route pré-calculée
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | ItemStack           | Item transporté      | Stockage de l'item             |
 * | BlockPos            | Positions route      | Chemin à travers le réseau     |
 * | CompoundTag         | Sérialisation        | Sauvegarde/chargement NBT      |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ItemPipeBlockEntity.java (items en transit dans le buffer)
 * - PipeNetwork.java (création lors du routage)
 *
 * ============================================================
 */
package com.chapeau.apica.core.network.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Représente un item voyageant à travers le réseau de pipes.
 * L'item suit une route pré-calculée (liste de BlockPos) et avance d'un hop par cycle.
 * Si la route est invalidée (pipe cassée), le pipe courant demande une re-route au réseau.
 */
public class PipeTransitItem {
    private final ItemStack stack;
    private final List<BlockPos> route;
    private int currentRouteIndex;
    private final BlockPos destinationPos;

    public PipeTransitItem(ItemStack stack, List<BlockPos> route, int currentRouteIndex, BlockPos destinationPos) {
        this.stack = stack;
        this.route = new ArrayList<>(route);
        this.currentRouteIndex = currentRouteIndex;
        this.destinationPos = destinationPos;
    }

    public ItemStack getStack() {
        return stack;
    }

    public List<BlockPos> getRoute() {
        return route;
    }

    public int getCurrentRouteIndex() {
        return currentRouteIndex;
    }

    public BlockPos getDestinationPos() {
        return destinationPos;
    }

    /**
     * Retourne la prochaine position sur la route (le prochain hop).
     * Retourne null si on est déjà à la fin de la route.
     */
    public BlockPos nextHop() {
        int nextIndex = currentRouteIndex + 1;
        if (nextIndex < route.size()) {
            return route.get(nextIndex);
        }
        return null;
    }

    /**
     * Retourne la position actuelle sur la route.
     */
    public BlockPos currentPos() {
        if (currentRouteIndex >= 0 && currentRouteIndex < route.size()) {
            return route.get(currentRouteIndex);
        }
        return route.isEmpty() ? BlockPos.ZERO : route.getFirst();
    }

    /**
     * Vérifie si l'item est arrivé à la dernière pipe de la route (prêt pour insertion).
     */
    public boolean isAtEndOfRoute() {
        return currentRouteIndex >= route.size() - 1;
    }

    /**
     * Avance d'un hop sur la route.
     */
    public void advance() {
        currentRouteIndex++;
    }

    /**
     * Sérialise en NBT pour sauvegarde.
     */
    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.put("Stack", stack.save(registries));
        tag.putInt("RouteIndex", currentRouteIndex);
        tag.put("Destination", NbtUtils.writeBlockPos(destinationPos));

        ListTag routeTag = new ListTag();
        for (BlockPos pos : route) {
            CompoundTag posTag = new CompoundTag();
            posTag.put("Pos", NbtUtils.writeBlockPos(pos));
            routeTag.add(posTag);
        }
        tag.put("Route", routeTag);

        return tag;
    }

    /**
     * Charge depuis NBT.
     */
    public static PipeTransitItem load(CompoundTag tag, HolderLookup.Provider registries) {
        ItemStack stack = ItemStack.parseOptional(registries, tag.getCompound("Stack"));
        int routeIndex = tag.getInt("RouteIndex");
        BlockPos destination = NbtUtils.readBlockPos(tag, "Destination").orElse(BlockPos.ZERO);

        List<BlockPos> route = new ArrayList<>();
        ListTag routeTag = tag.getList("Route", Tag.TAG_COMPOUND);
        for (int i = 0; i < routeTag.size(); i++) {
            NbtUtils.readBlockPos(routeTag.getCompound(i), "Pos").ifPresent(route::add);
        }

        return new PipeTransitItem(stack, route, routeIndex, destination);
    }
}
