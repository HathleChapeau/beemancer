/**
 * ============================================================
 * [StorageItemsSyncPacket.java]
 * Description: Packet pour synchroniser les items agrégés vers le client
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance                      | Raison                  | Utilisation           |
 * |---------------------------------|------------------------|-----------------------|
 * | Beemancer                       | MOD_ID                 | Type packet           |
 * | StorageTerminalMenu             | Menu                   | Mise à jour cache     |
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
import com.chapeau.beemancer.common.menu.storage.StorageTerminalMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Packet envoyé du serveur au client pour synchroniser les items agrégés.
 */
public record StorageItemsSyncPacket(
    BlockPos terminalPos,
    List<ItemStack> items
) implements CustomPacketPayload {

    public static final Type<StorageItemsSyncPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, "storage_items_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StorageItemsSyncPacket> STREAM_CODEC =
        StreamCodec.composite(
            BlockPos.STREAM_CODEC, StorageItemsSyncPacket::terminalPos,
            ItemStack.LIST_STREAM_CODEC, StorageItemsSyncPacket::items,
            StorageItemsSyncPacket::new
        );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(StorageItemsSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Côté client uniquement
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            // Vérifier que le joueur a le bon menu ouvert
            if (mc.player.containerMenu instanceof StorageTerminalMenu menu) {
                if (menu.getBlockPos().equals(packet.terminalPos)) {
                    menu.setAggregatedItems(new ArrayList<>(packet.items));
                }
            }
        });
    }
}
