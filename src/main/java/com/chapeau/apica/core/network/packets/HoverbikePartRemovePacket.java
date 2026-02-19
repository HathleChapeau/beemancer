/**
 * ============================================================
 * [HoverbikePartRemovePacket.java]
 * Description: Packet C2S pour retirer une piece du hoverbike vers l'inventaire du joueur
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | HoverbikeEntity     | Entite cible         | getPartStack/setPartStack      |
 * | HoverbikePart       | Enum parties         | Identification de la partie    |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - HoverbikePartScreen.java: Envoi sur clic bouton Remove
 * - ApicaNetwork.java: Enregistrement
 *
 * ============================================================
 */
package com.chapeau.apica.core.network.packets;

import com.chapeau.apica.Apica;
import com.chapeau.apica.common.entity.mount.HoverbikeEntity;
import com.chapeau.apica.common.entity.mount.HoverbikePart;
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
 * Retire une piece du hoverbike et la donne au joueur.
 * Si l'inventaire est plein, la piece est droppee au sol.
 */
public record HoverbikePartRemovePacket(int entityId, int partOrdinal)
        implements CustomPacketPayload {

    private static final double MAX_INTERACTION_DISTANCE = 10.0;

    public static final Type<HoverbikePartRemovePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "hoverbike_part_remove"));

    public static final StreamCodec<FriendlyByteBuf, HoverbikePartRemovePacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, HoverbikePartRemovePacket::entityId,
                    ByteBufCodecs.INT, HoverbikePartRemovePacket::partOrdinal,
                    HoverbikePartRemovePacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(HoverbikePartRemovePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            HoverbikePart[] parts = HoverbikePart.values();
            if (packet.partOrdinal() < 0 || packet.partOrdinal() >= parts.length) return;

            Entity entity = player.level().getEntity(packet.entityId());
            if (!(entity instanceof HoverbikeEntity hoverbike)) return;
            if (!hoverbike.isEditMode()) return;
            if (hoverbike.distanceTo(player) > MAX_INTERACTION_DISTANCE) return;

            HoverbikePart part = parts[packet.partOrdinal()];
            ItemStack currentOnBike = hoverbike.getPartStack(part);
            if (currentOnBike.isEmpty()) return;

            ItemStack removed = currentOnBike.copy();
            hoverbike.setPartStack(part, ItemStack.EMPTY);

            if (!player.getInventory().add(removed)) {
                player.drop(removed, false);
            }
        });
    }
}
