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
 * | BlockPos       | Position requester   | Affichage origine |
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

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
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
    List<UUID> dependencyIds,
    String origin,
    String blockedReason,
    @Nullable BlockPos requesterPos,
    String requesterType,
    @Nullable UUID parentTaskId
) {

    /**
     * Écrit les données dans un buffer réseau.
     */
    public void toNetwork(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(taskId);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, template);
        buf.writeInt(count);
        buf.writeUtf(state, 32);
        buf.writeInt(dependencyIds.size());
        for (UUID depId : dependencyIds) {
            buf.writeUUID(depId);
        }
        buf.writeUtf(origin, 16);
        buf.writeUtf(blockedReason, 64);
        buf.writeBoolean(requesterPos != null);
        if (requesterPos != null) {
            buf.writeLong(requesterPos.asLong());
        }
        buf.writeUtf(requesterType, 32);
        buf.writeBoolean(parentTaskId != null);
        if (parentTaskId != null) {
            buf.writeUUID(parentTaskId);
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
        int depCount = buf.readInt();
        List<UUID> deps = new ArrayList<>(depCount);
        for (int i = 0; i < depCount; i++) {
            deps.add(buf.readUUID());
        }
        String origin = buf.readUtf(16);
        String blockedReason = buf.readUtf(64);
        BlockPos reqPos = buf.readBoolean() ? BlockPos.of(buf.readLong()) : null;
        String reqType = buf.readUtf(32);
        UUID parentId = buf.readBoolean() ? buf.readUUID() : null;
        return new TaskDisplayData(taskId, template, count, state, deps, origin,
            blockedReason, reqPos, reqType, parentId);
    }
}
