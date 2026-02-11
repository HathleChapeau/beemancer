/**
 * ============================================================
 * [StorageItemsSyncPacket.java]
 * Description: Packet pour synchroniser les items agrégés vers le client (full ou delta)
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
 * - StorageItemAggregator.java (envoi sync)
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
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Packet envoyé du serveur au client pour synchroniser les items agrégés.
 * Supporte deux modes:
 * - fullSync=true: remplacement complet de la liste (premier sync, nouveau viewer)
 * - fullSync=false: delta incrémental (items modifiés/ajoutés avec nouveau count, count=0 = supprimé)
 */
public record StorageItemsSyncPacket(
    BlockPos terminalPos,
    boolean fullSync,
    List<ItemStack> items
) implements CustomPacketPayload {

    public static final Type<StorageItemsSyncPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, "storage_items_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StorageItemsSyncPacket> STREAM_CODEC =
        new StreamCodec<>() {
            @Override
            public StorageItemsSyncPacket decode(RegistryFriendlyByteBuf buf) {
                BlockPos pos = buf.readBlockPos();
                boolean full = buf.readBoolean();
                int count = buf.readVarInt();
                List<ItemStack> items = new ArrayList<>(count);
                if (full) {
                    for (int i = 0; i < count; i++) {
                        items.add(ItemStack.STREAM_CODEC.decode(buf));
                    }
                } else {
                    // Delta mode: encode template (count=1) + separate deltaCount
                    // deltaCount=0 means removal
                    for (int i = 0; i < count; i++) {
                        ItemStack template = ItemStack.STREAM_CODEC.decode(buf);
                        int deltaCount = buf.readVarInt();
                        template.setCount(deltaCount);
                        items.add(template);
                    }
                }
                return new StorageItemsSyncPacket(pos, full, items);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, StorageItemsSyncPacket packet) {
                buf.writeBlockPos(packet.terminalPos);
                buf.writeBoolean(packet.fullSync);
                buf.writeVarInt(packet.items.size());
                if (packet.fullSync) {
                    for (ItemStack stack : packet.items) {
                        ItemStack.STREAM_CODEC.encode(buf, stack);
                    }
                } else {
                    // Delta mode: encode template (count=1) + separate deltaCount
                    for (ItemStack stack : packet.items) {
                        int deltaCount = stack.getCount();
                        ItemStack template = stack.copyWithCount(1);
                        ItemStack.STREAM_CODEC.encode(buf, template);
                        buf.writeVarInt(deltaCount);
                    }
                }
            }
        };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(StorageItemsSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            if (mc.player.containerMenu instanceof StorageTerminalMenu menu) {
                if (menu.getBlockPos().equals(packet.terminalPos)) {
                    if (packet.fullSync) {
                        menu.setAggregatedItems(new ArrayList<>(packet.items));
                    } else {
                        menu.applyDeltaItems(packet.items);
                    }
                }
            }
        });
    }
}
