/**
 * ============================================================
 * [ResonatorTraitMatchPacket.java]
 * Description: Packet C2S envoye quand le client detecte un match de waveform sur un trait
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | ResonatorBlockEntity    | BE cible             | startTraitAnalysis()           |
 * | ResonatorMenu           | Menu actif           | Verification container         |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - ResonatorScreen (envoi au match detecte)
 * - ApicaNetwork (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.apica.core.network.packets;

import com.chapeau.apica.Apica;
import com.chapeau.apica.common.block.resonator.ResonatorBlockEntity;
import com.chapeau.apica.common.menu.ResonatorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ResonatorTraitMatchPacket(BlockPos pos, String traitKey) implements CustomPacketPayload {

    public static final Type<ResonatorTraitMatchPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "resonator_trait_match"));

    public static final StreamCodec<FriendlyByteBuf, ResonatorTraitMatchPacket> STREAM_CODEC =
            StreamCodec.of(ResonatorTraitMatchPacket::write, ResonatorTraitMatchPacket::read);

    private static void write(FriendlyByteBuf buf, ResonatorTraitMatchPacket packet) {
        buf.writeBlockPos(packet.pos());
        buf.writeUtf(packet.traitKey());
    }

    private static ResonatorTraitMatchPacket read(FriendlyByteBuf buf) {
        return new ResonatorTraitMatchPacket(buf.readBlockPos(), buf.readUtf());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ResonatorTraitMatchPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!(player.containerMenu instanceof ResonatorMenu)) return;
            if (player.blockPosition().distSqr(packet.pos()) > 64 * 64) return;

            if (player.level().getBlockEntity(packet.pos()) instanceof ResonatorBlockEntity resonator) {
                if (!resonator.hasBee()) return;
                if (resonator.isAnalysisInProgress()) return;

                resonator.startTraitAnalysis(player.getUUID(), packet.traitKey());

                player.openMenu(resonator, buf -> {
                    buf.writeBlockPos(packet.pos());
                    ItemStack.OPTIONAL_STREAM_CODEC.encode(
                            (RegistryFriendlyByteBuf) buf, resonator.getStoredBee());
                    buf.writeBoolean(true);
                });
            }
        });
    }
}
