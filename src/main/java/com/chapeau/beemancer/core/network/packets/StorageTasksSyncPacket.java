/**
 * ============================================================
 * [StorageTasksSyncPacket.java]
 * Description: Packet pour synchroniser la liste des tâches vers le client
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance                      | Raison                  | Utilisation           |
 * |---------------------------------|------------------------|-----------------------|
 * | Beemancer                       | MOD_ID                 | Type packet           |
 * | StorageTerminalMenu             | Menu                   | Mise à jour cache     |
 * | TaskDisplayData                 | Données tâche          | Contenu du packet     |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - StorageControllerBlockEntity.java (envoi sync)
 * - StorageTerminalScreen.java (réception)
 * - BeemancerNetwork.java (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.beemancer.core.network.packets;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.block.storage.TaskDisplayData;
import com.chapeau.beemancer.common.menu.storage.StorageTerminalMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Packet envoyé du serveur au client pour synchroniser les tâches de livraison.
 * Envoyé périodiquement (toutes les 20 ticks) aux viewers du terminal.
 */
public record StorageTasksSyncPacket(
    BlockPos terminalPos,
    List<TaskDisplayData> tasks
) implements CustomPacketPayload {

    public static final Type<StorageTasksSyncPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, "storage_tasks_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StorageTasksSyncPacket> STREAM_CODEC =
        new StreamCodec<>() {
            @Override
            public StorageTasksSyncPacket decode(RegistryFriendlyByteBuf buf) {
                BlockPos pos = buf.readBlockPos();
                int count = buf.readInt();
                List<TaskDisplayData> tasks = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    tasks.add(TaskDisplayData.fromNetwork(buf));
                }
                return new StorageTasksSyncPacket(pos, tasks);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, StorageTasksSyncPacket packet) {
                buf.writeBlockPos(packet.terminalPos);
                buf.writeInt(packet.tasks.size());
                for (TaskDisplayData task : packet.tasks) {
                    task.toNetwork(buf);
                }
            }
        };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(StorageTasksSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            if (mc.player.containerMenu instanceof StorageTerminalMenu menu) {
                if (menu.getBlockPos().equals(packet.terminalPos)) {
                    menu.setTaskDisplayData(new ArrayList<>(packet.tasks));
                }
            }
        });
    }
}
