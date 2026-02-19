/**
 * ============================================================
 * [HoverbikePartSwapPacket.java]
 * Description: Packet C2S pour echanger une piece de moto avec une piece de l'inventaire
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | HoverbikeEntity     | Entite cible         | getPartStack/setPartStack      |
 * | HoverbikePart       | Enum parties         | Identification de la partie    |
 * | HoverbikePartItem   | Verification type    | getCategory()                  |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - HoverbikePartScreen.java: Envoi sur clic fleche swap
 * - ApicaNetwork.java: Enregistrement
 *
 * ============================================================
 */
package com.chapeau.apica.core.network.packets;

import com.chapeau.apica.Apica;
import com.chapeau.apica.common.entity.mount.HoverbikeEntity;
import com.chapeau.apica.common.entity.mount.HoverbikePart;
import com.chapeau.apica.common.item.mount.HoverbikePartItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Echange une piece equipee sur le hoverbike avec une piece de l'inventaire du joueur.
 * Le serveur valide que le slot contient une piece compatible (meme categorie).
 */
public record HoverbikePartSwapPacket(int entityId, int partOrdinal, int inventorySlot)
        implements CustomPacketPayload {

    private static final double MAX_INTERACTION_DISTANCE = 10.0;

    public static final Type<HoverbikePartSwapPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "hoverbike_part_swap"));

    public static final StreamCodec<FriendlyByteBuf, HoverbikePartSwapPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, HoverbikePartSwapPacket::entityId,
                    ByteBufCodecs.INT, HoverbikePartSwapPacket::partOrdinal,
                    ByteBufCodecs.INT, HoverbikePartSwapPacket::inventorySlot,
                    HoverbikePartSwapPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(HoverbikePartSwapPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            HoverbikePart[] parts = HoverbikePart.values();
            if (packet.partOrdinal() < 0 || packet.partOrdinal() >= parts.length) return;
            if (packet.inventorySlot() < 0 || packet.inventorySlot() >= player.getInventory().items.size()) return;

            Entity entity = player.level().getEntity(packet.entityId());
            if (!(entity instanceof HoverbikeEntity hoverbike)) return;
            if (!hoverbike.isEditMode()) return;
            if (hoverbike.distanceTo(player) > MAX_INTERACTION_DISTANCE) return;

            HoverbikePart part = parts[packet.partOrdinal()];
            ItemStack inventoryStack = player.getInventory().getItem(packet.inventorySlot());

            if (inventoryStack.isEmpty()) return;
            if (!(inventoryStack.getItem() instanceof HoverbikePartItem invPart)) return;
            if (invPart.getCategory() != part) return;

            ItemStack currentOnBike = hoverbike.getPartStack(part).copy();
            ItemStack toEquip = inventoryStack.copy();

            hoverbike.setPartStack(part, toEquip);
            player.getInventory().setItem(packet.inventorySlot(), currentOnBike);
        });
    }
}
