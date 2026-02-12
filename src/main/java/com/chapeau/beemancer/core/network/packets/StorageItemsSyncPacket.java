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
 *   Peut être fragmenté: lastFragment=false signifie que d'autres chunks suivent
 * - fullSync=false: delta incrémental (items modifiés/ajoutés avec nouveau count, count=0 = supprimé)
 *
 * Tous les items utilisent le format template(count=1) + VarInt(totalCount) pour économiser
 * la bande passante sur les stacks de grande quantité.
 */
public record StorageItemsSyncPacket(
    BlockPos terminalPos,
    boolean fullSync,
    boolean lastFragment,
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
                boolean last = buf.readBoolean();
                int count = buf.readVarInt();
                List<ItemStack> items = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    ItemStack template = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
                    int totalCount = buf.readVarInt();
                    if (!template.isEmpty()) {
                        template.setCount(totalCount);
                        items.add(template);
                    }
                }
                return new StorageItemsSyncPacket(pos, full, last, items);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, StorageItemsSyncPacket packet) {
                buf.writeBlockPos(packet.terminalPos);
                buf.writeBoolean(packet.fullSync);
                buf.writeBoolean(packet.lastFragment);
                buf.writeVarInt(packet.items.size());
                for (ItemStack stack : packet.items) {
                    int totalCount = stack.getCount();
                    ItemStack template = stack.copyWithCount(1);
                    ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, template);
                    buf.writeVarInt(totalCount);
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
                        menu.receiveFullSyncFragment(packet.items, packet.lastFragment);
                    } else {
                        menu.applyDeltaItems(packet.items);
                    }
                }
            }
        });
    }
}
