/**
 * ============================================================
 * [BeeCreatorActionPacket.java]
 * Description: Packet pour les actions du BeeCreator
 * ============================================================
 */
package com.chapeau.beemancer.core.network.packets;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.menu.BeeCreatorMenu;
import com.chapeau.beemancer.core.gene.GeneCategory;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BeeCreatorActionPacket(int containerId, String categoryId, boolean next) implements CustomPacketPayload {
    
    public static final Type<BeeCreatorActionPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, "bee_creator_action"));

    public static final StreamCodec<FriendlyByteBuf, BeeCreatorActionPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, BeeCreatorActionPacket::containerId,
            ByteBufCodecs.STRING_UTF8, BeeCreatorActionPacket::categoryId,
            ByteBufCodecs.BOOL, BeeCreatorActionPacket::next,
            BeeCreatorActionPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BeeCreatorActionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                AbstractContainerMenu menu = player.containerMenu;
                if (menu instanceof BeeCreatorMenu beeMenu && menu.containerId == packet.containerId) {
                    if ("APPLY".equals(packet.categoryId)) {
                        beeMenu.applyChanges();
                    } else {
                        GeneCategory category = GeneCategory.byId(packet.categoryId);
                        if (category != null) {
                            if (packet.next) {
                                beeMenu.cycleGeneNext(category);
                            } else {
                                beeMenu.cycleGenePrevious(category);
                            }
                        }
                    }
                }
            }
        });
    }
}
