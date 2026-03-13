/**
 * ============================================================
 * [MagazineReloadStartPacket.java]
 * Description: Packet S2C pour démarrer l'animation de reload côté client
 * ============================================================
 */
package com.chapeau.apica.core.network.packets;

import com.chapeau.apica.Apica;
import com.chapeau.apica.client.animation.AnimationTimer;
import com.chapeau.apica.common.item.magazine.IMagazineHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Packet S2C envoyé par le serveur pour démarrer l'animation de reload côté client.
 */
public record MagazineReloadStartPacket(boolean mainHand) implements CustomPacketPayload {

    public static final Type<MagazineReloadStartPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "magazine_reload_start"));

    public static final StreamCodec<FriendlyByteBuf, MagazineReloadStartPacket> STREAM_CODEC =
            StreamCodec.composite(
                ByteBufCodecs.BOOL, MagazineReloadStartPacket::mainHand,
                MagazineReloadStartPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MagazineReloadStartPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = Minecraft.getInstance().player;
            if (player == null) return;

            InteractionHand hand = packet.mainHand() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
            ItemStack stack = player.getItemInHand(hand);

            if (!(stack.getItem() instanceof IMagazineHolder holder)) return;

            float currentTime = AnimationTimer.getRenderTime(
                    Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true));
            holder.startReloadAnimation(player, stack, currentTime);
        });
    }
}
