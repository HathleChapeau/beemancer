/**
 * ============================================================
 * [MagazineReloadPacket.java]
 * Description: Packet C2S pour demander un reload de magazine
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
 * Packet C2S envoyé quand le joueur fait un right-click (mouseDown) avec un IMagazineHolder.
 * Le serveur vérifie canReload() et effectue le reload si possible.
 */
public record MagazineReloadPacket(boolean mainHand) implements CustomPacketPayload {

    public static final Type<MagazineReloadPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "magazine_reload"));

    public static final StreamCodec<FriendlyByteBuf, MagazineReloadPacket> STREAM_CODEC =
            StreamCodec.composite(
                ByteBufCodecs.BOOL, MagazineReloadPacket::mainHand,
                MagazineReloadPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MagazineReloadPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            InteractionHand hand = packet.mainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            ItemStack stack = player.getItemInHand(hand);

            if (!(stack.getItem() instanceof IMagazineHolder holder)) return;

            if (holder.canReload(player, stack)) {
                holder.doReload(player, stack);
            }
        });
    }
}
