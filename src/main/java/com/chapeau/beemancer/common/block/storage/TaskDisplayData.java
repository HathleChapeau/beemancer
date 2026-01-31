/**
 * ============================================================
 * [TaskDisplayData.java]
 * Description: Données d'affichage d'une tâche de livraison pour sync réseau
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance     | Raison                | Utilisation       |
 * |----------------|----------------------|-------------------|
 * | ItemStack      | Template de l'item   | Affichage icône   |
 * | FriendlyByteBuf| Sérialisation réseau | Sync S->C         |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - StorageDeliveryManager.java (création des données)
 * - StorageTasksSyncPacket.java (transport réseau)
 * - StorageTerminalScreen.java (affichage onglet Tasks)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.block.storage;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Représentation légère d'une DeliveryTask pour affichage côté client.
 * Contient uniquement les données nécessaires au rendu et à l'annulation.
 */
public record TaskDisplayData(
    UUID taskId,
    ItemStack template,
    int count,
    String state,
    String type,
    List<UUID> dependencyIds
) {

    /**
     * Écrit les données dans un buffer réseau.
     */
    public void toNetwork(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(taskId);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, template);
        buf.writeInt(count);
        buf.writeUtf(state, 32);
        buf.writeUtf(type, 16);
        buf.writeInt(dependencyIds.size());
        for (UUID depId : dependencyIds) {
            buf.writeUUID(depId);
        }
    }

    /**
     * Lit les données depuis un buffer réseau.
     */
    public static TaskDisplayData fromNetwork(RegistryFriendlyByteBuf buf) {
        UUID taskId = buf.readUUID();
        ItemStack template = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        int count = buf.readInt();
        String state = buf.readUtf(32);
        String type = buf.readUtf(16);
        int depCount = buf.readInt();
        List<UUID> deps = new ArrayList<>(depCount);
        for (int i = 0; i < depCount; i++) {
            deps.add(buf.readUUID());
        }
        return new TaskDisplayData(taskId, template, count, state, type, deps);
    }
}
