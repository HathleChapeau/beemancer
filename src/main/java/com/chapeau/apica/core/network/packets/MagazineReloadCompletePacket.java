/**
 * ============================================================
 * [MagazineReloadCompletePacket.java]
 * Description: Packet C2S envoyé quand l'animation de reload est terminée
 * ============================================================
 */
package com.chapeau.apica.core.network.packets;

import com.chapeau.apica.Apica;
import com.chapeau.apica.common.item.magazine.IMagazineHolder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Packet C2S envoyé par le client quand l'animation de reload est terminée.
 * Le serveur effectue le swap de magazine et reset le reload state.
 */
public record MagazineReloadCompletePacket(boolean mainHand) implements CustomPacketPayload {

    public static final Type<MagazineReloadCompletePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "magazine_reload_complete"));

    public static final StreamCodec<FriendlyByteBuf, MagazineReloadCompletePacket> STREAM_CODEC =
            StreamCodec.composite(
                ByteBufCodecs.BOOL, MagazineReloadCompletePacket::mainHand,
                MagazineReloadCompletePacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MagazineReloadCompletePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            InteractionHand hand = packet.mainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            ItemStack stack = player.getItemInHand(hand);

            if (!(stack.getItem() instanceof IMagazineHolder holder)) return;

            // Effectuer le reload côté serveur
            holder.doReload(player, stack);
            holder.setReloading(player, false);
        });
    }
}
