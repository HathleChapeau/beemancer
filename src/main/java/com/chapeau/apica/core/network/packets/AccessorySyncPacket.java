/**
 * ============================================================
 * [AccessorySyncPacket.java]
 * Description: Packet S2C pour synchroniser les accessoires equipes au client
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | AccessoryClientCache| Cache client         | Mise a jour cote client        |
 * | AccessoryPlayerData | Donnees serveur      | Construction du packet         |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - AccessoryEquipPacket.java (apres equip/unequip)
 * - Apica.java (sync au login)
 * - ApicaNetwork.java (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.apica.core.network.packets;

import com.chapeau.apica.Apica;
import com.chapeau.apica.client.gui.AccessoryClientCache;
import com.chapeau.apica.common.data.AccessoryPlayerData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Envoye du serveur au client pour synchroniser les 2 slots accessoire.
 * Le client met a jour AccessoryClientCache.
 */
public record AccessorySyncPacket(ItemStack slot0, ItemStack slot1) implements CustomPacketPayload {

    public static final Type<AccessorySyncPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "accessory_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AccessorySyncPacket> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public AccessorySyncPacket decode(RegistryFriendlyByteBuf buf) {
                    ItemStack s0 = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
                    ItemStack s1 = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
                    return new AccessorySyncPacket(s0, s1);
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buf, AccessorySyncPacket packet) {
                    ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, packet.slot0);
                    ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, packet.slot1);
                }
            };

    public AccessorySyncPacket(AccessoryPlayerData data) {
        this(data.getAccessory(0).copy(), data.getAccessory(1).copy());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AccessorySyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            AccessoryClientCache.update(packet.slot0, packet.slot1);
        });
    }
}
