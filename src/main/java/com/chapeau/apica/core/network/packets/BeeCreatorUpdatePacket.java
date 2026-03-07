/**
 * ============================================================
 * [BeeCreatorUpdatePacket.java]
 * Description: Packet C2S pour mettre a jour la couleur d'une partie dans le Bee Creator
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance               | Raison                | Utilisation                    |
 * |--------------------------|----------------------|--------------------------------|
 * | BeeCreatorBlockEntity    | BE cible             | Application de la couleur      |
 * | BeeCreatorMenu           | Menu actif           | Verification container         |
 * | BeePart                  | Enum parties         | Index → partie                 |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - BeeCreatorScreen (envoi couleur)
 * - ApicaNetwork (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.apica.core.network.packets;

import com.chapeau.apica.Apica;
import com.chapeau.apica.common.block.beecreator.BeeCreatorBlockEntity;
import com.chapeau.apica.common.block.beecreator.BeePart;
import com.chapeau.apica.common.menu.BeeCreatorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BeeCreatorUpdatePacket(BlockPos pos, int partIndex, int color)
        implements CustomPacketPayload {

    public static final Type<BeeCreatorUpdatePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "bee_creator_update"));

    public static final StreamCodec<FriendlyByteBuf, BeeCreatorUpdatePacket> STREAM_CODEC =
            StreamCodec.of(BeeCreatorUpdatePacket::write, BeeCreatorUpdatePacket::read);

    private static void write(FriendlyByteBuf buf, BeeCreatorUpdatePacket packet) {
        buf.writeBlockPos(packet.pos());
        buf.writeInt(packet.partIndex());
        buf.writeInt(packet.color());
    }

    private static BeeCreatorUpdatePacket read(FriendlyByteBuf buf) {
        return new BeeCreatorUpdatePacket(buf.readBlockPos(), buf.readInt(), buf.readInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BeeCreatorUpdatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!(player.containerMenu instanceof BeeCreatorMenu)) return;
            if (player.blockPosition().distSqr(packet.pos()) > 64 * 64) return;

            if (player.level().getBlockEntity(packet.pos()) instanceof BeeCreatorBlockEntity be) {
                int idx = packet.partIndex();
                if (idx == BeeCreatorBlockEntity.BODY_TYPE_SLOT) {
                    be.setBodyType(packet.color());
                } else if (idx == BeeCreatorBlockEntity.WING_TYPE_SLOT) {
                    be.setWingType(packet.color());
                } else if (idx == BeeCreatorBlockEntity.STINGER_TYPE_SLOT) {
                    be.setStingerType(packet.color());
                } else if (idx == BeeCreatorBlockEntity.ANTENNA_TYPE_SLOT) {
                    be.setAntennaType(packet.color());
                } else {
                    BeePart part = BeePart.byIndex(idx);
                    be.setPartColor(part, packet.color());
                }
            }
        });
    }
}
