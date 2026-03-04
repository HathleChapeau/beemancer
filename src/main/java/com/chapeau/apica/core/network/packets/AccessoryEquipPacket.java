/**
 * ============================================================
 * [AccessoryEquipPacket.java]
 * Description: Packet C2S pour equiper/desequiper un accessoire
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | IAccessory          | Validation type      | Verification item curseur      |
 * | AccessoryPlayerData | Stockage             | Mise a jour slot               |
 * | ApicaAttachments    | Acces attachment     | getData/setData                |
 * | AccessorySyncPacket | Sync client          | Envoi apres modification       |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - InventoryScreenAccessoryMixin.java (envoi depuis client)
 * - ApicaNetwork.java (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.apica.core.network.packets;

import com.chapeau.apica.Apica;
import com.chapeau.apica.common.data.AccessoryPlayerData;
import com.chapeau.apica.common.item.accessory.IAccessory;
import com.chapeau.apica.core.registry.ApicaAttachments;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Envoye par le client quand le joueur clique sur un slot accessoire.
 * equip=true: place l'item du curseur dans le slot.
 * equip=false: retire l'item du slot vers le curseur.
 */
public record AccessoryEquipPacket(int slot, boolean equip) implements CustomPacketPayload {

    public static final Type<AccessoryEquipPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "accessory_equip"));

    public static final StreamCodec<FriendlyByteBuf, AccessoryEquipPacket> STREAM_CODEC =
            StreamCodec.composite(
                ByteBufCodecs.INT, AccessoryEquipPacket::slot,
                ByteBufCodecs.BOOL, AccessoryEquipPacket::equip,
                AccessoryEquipPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AccessoryEquipPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (packet.slot() < 0 || packet.slot() >= AccessoryPlayerData.SLOT_COUNT) return;

            AccessoryPlayerData data = player.getData(ApicaAttachments.ACCESSORY_DATA);
            ItemStack carried = player.containerMenu.getCarried();

            if (packet.equip()) {
                // Equip: curseur doit porter un IAccessory
                if (carried.isEmpty() || !(carried.getItem() instanceof IAccessory accessory)) return;

                // Si le slot est deja occupe, swap
                ItemStack existing = data.getAccessory(packet.slot());
                if (!existing.isEmpty() && existing.getItem() instanceof IAccessory oldAccessory) {
                    oldAccessory.onUnequip(player, existing);
                }

                data.setAccessory(packet.slot(), carried.copy());
                accessory.onEquip(player, carried);
                player.containerMenu.setCarried(existing);
            } else {
                // Unequip: curseur doit etre vide, slot doit etre occupe
                ItemStack existing = data.getAccessory(packet.slot());
                if (existing.isEmpty() || !carried.isEmpty()) return;

                if (existing.getItem() instanceof IAccessory accessory) {
                    accessory.onUnequip(player, existing);
                }

                player.containerMenu.setCarried(existing.copy());
                data.setAccessory(packet.slot(), ItemStack.EMPTY);
            }

            // Sync curseur et accessoires vers le client
            player.containerMenu.broadcastChanges();
            PacketDistributor.sendToPlayer(player, new AccessorySyncPacket(data));
        });
    }
}
