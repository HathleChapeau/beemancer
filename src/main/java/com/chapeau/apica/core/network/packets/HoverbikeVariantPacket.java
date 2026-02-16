/**
 * ============================================================
 * [HoverbikeVariantPacket.java]
 * Description: Packet C2S pour changer la variante d'une partie du Hoverbike
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | HoverbikeEntity     | Entite cible         | Application du changement      |
 * | HoverbikePart       | Enum parties         | Identification de la partie    |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - HoverbikePartScreen.java: Envoi sur clic fleche
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
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Packet envoye par le client quand le joueur selectionne une variante
 * de modele dans le menu de personnalisation du hoverbike.
 * Le nombre de variantes est dynamique (charge depuis JSON cote client).
 * Le serveur valide uniquement que l'index est dans un range raisonnable.
 */
public record HoverbikeVariantPacket(int entityId, int partOrdinal, int variantIndex)
        implements CustomPacketPayload {

    private static final double MAX_INTERACTION_DISTANCE = 10.0;
    private static final int MAX_VARIANT_INDEX = 99;

    public static final Type<HoverbikeVariantPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "hoverbike_variant"));

    public static final StreamCodec<FriendlyByteBuf, HoverbikeVariantPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, HoverbikeVariantPacket::entityId,
                    ByteBufCodecs.INT, HoverbikeVariantPacket::partOrdinal,
                    ByteBufCodecs.INT, HoverbikeVariantPacket::variantIndex,
                    HoverbikeVariantPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(HoverbikeVariantPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            HoverbikePart[] parts = HoverbikePart.values();
            if (packet.partOrdinal() < 0 || packet.partOrdinal() >= parts.length) return;
            if (packet.variantIndex() < 0 || packet.variantIndex() > MAX_VARIANT_INDEX) return;

            Entity entity = player.level().getEntity(packet.entityId());
            if (!(entity instanceof HoverbikeEntity hoverbike)) return;
            if (!hoverbike.isEditMode()) return;
            if (hoverbike.distanceTo(player) > MAX_INTERACTION_DISTANCE) return;

            HoverbikePart part = parts[packet.partOrdinal()];
            hoverbike.setPartVariant(part, packet.variantIndex());
        });
    }
}
