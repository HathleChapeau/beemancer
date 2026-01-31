/**
 * ============================================================
 * [StorageTaskCancelPacket.java]
 * Description: Packet pour annuler une tâche de livraison depuis le client
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance                      | Raison                  | Utilisation           |
 * |---------------------------------|------------------------|-----------------------|
 * | Beemancer                       | MOD_ID                 | Type packet           |
 * | StorageTerminalBlockEntity      | Terminal               | Accès au controller   |
 * | StorageControllerBlockEntity    | Controller             | Annulation tâche      |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - StorageTerminalScreen.java (envoi annulation)
 * - BeemancerNetwork.java (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.beemancer.core.network.packets;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.blockentity.storage.StorageControllerBlockEntity;
import com.chapeau.beemancer.common.blockentity.storage.StorageTerminalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/**
 * Packet envoyé du client au serveur pour annuler une tâche de livraison.
 * Annule la tâche et toutes ses dépendances.
 */
public record StorageTaskCancelPacket(
    BlockPos terminalPos,
    UUID taskId
) implements CustomPacketPayload {

    public static final Type<StorageTaskCancelPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, "storage_task_cancel"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StorageTaskCancelPacket> STREAM_CODEC =
        new StreamCodec<>() {
            @Override
            public StorageTaskCancelPacket decode(RegistryFriendlyByteBuf buf) {
                return new StorageTaskCancelPacket(buf.readBlockPos(), buf.readUUID());
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, StorageTaskCancelPacket packet) {
                buf.writeBlockPos(packet.terminalPos);
                buf.writeUUID(packet.taskId);
            }
        };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(StorageTaskCancelPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            // Vérifier la distance
            double distSqr = player.distanceToSqr(
                packet.terminalPos.getX() + 0.5,
                packet.terminalPos.getY() + 0.5,
                packet.terminalPos.getZ() + 0.5
            );
            if (distSqr > 64.0) return;

            // Récupérer le terminal
            BlockEntity be = player.level().getBlockEntity(packet.terminalPos);
            if (!(be instanceof StorageTerminalBlockEntity terminal)) return;

            // Vérifier que le terminal est lié
            StorageControllerBlockEntity controller = terminal.getController();
            if (controller == null) return;

            // Annuler la tâche
            controller.cancelTask(packet.taskId);
        });
    }
}
