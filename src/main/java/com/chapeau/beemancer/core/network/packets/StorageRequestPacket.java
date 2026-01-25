/**
 * ============================================================
 * [StorageRequestPacket.java]
 * Description: Packet pour demander des items au réseau de stockage
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance                      | Raison                  | Utilisation           |
 * |---------------------------------|------------------------|-----------------------|
 * | Beemancer                       | MOD_ID                 | Type packet           |
 * | StorageTerminalMenu             | Menu                   | Exécution requête     |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - StorageTerminalScreen.java (envoi requête)
 * - BeemancerNetwork.java (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.beemancer.core.network.packets;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.blockentity.storage.StorageTerminalBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Packet envoyé du client au serveur pour demander des items.
 */
public record StorageRequestPacket(
    BlockPos terminalPos,
    ItemStack requestedItem,
    int count
) implements CustomPacketPayload {

    public static final Type<StorageRequestPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, "storage_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StorageRequestPacket> STREAM_CODEC =
        StreamCodec.composite(
            BlockPos.STREAM_CODEC, StorageRequestPacket::terminalPos,
            ItemStack.STREAM_CODEC, StorageRequestPacket::requestedItem,
            ByteBufCodecs.INT, StorageRequestPacket::count,
            StorageRequestPacket::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(StorageRequestPacket packet, IPayloadContext context) {
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
            if (!terminal.isLinked()) return;

            // Exécuter la requête
            terminal.requestItem(packet.requestedItem, packet.count);
        });
    }
}
