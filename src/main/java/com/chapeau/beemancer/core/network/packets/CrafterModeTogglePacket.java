/**
 * ============================================================
 * [CrafterModeTogglePacket.java]
 * Description: Packet C2S pour basculer le mode craft/machine du Crafter
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | CrafterBlockEntity      | BE cible             | setMode, clearGhostItems       |
 * | CrafterMenu             | Menu validation      | updateGhostSlotVisibility      |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - CrafterScreen.java (bouton toggle)
 * - BeemancerNetwork.java (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.beemancer.core.network.packets;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.blockentity.storage.CrafterBlockEntity;
import com.chapeau.beemancer.common.menu.storage.CrafterMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CrafterModeTogglePacket(BlockPos crafterPos)
        implements CustomPacketPayload {

    public static final Type<CrafterModeTogglePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, "crafter_mode_toggle"));

    public static final StreamCodec<FriendlyByteBuf, CrafterModeTogglePacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, CrafterModeTogglePacket::crafterPos,
                    CrafterModeTogglePacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CrafterModeTogglePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            if (!(player.containerMenu instanceof CrafterMenu menu)) return;

            BlockEntity be = player.level().getBlockEntity(packet.crafterPos);
            if (!(be instanceof CrafterBlockEntity crafter)) return;

            if (player.distanceToSqr(
                    packet.crafterPos.getX() + 0.5,
                    packet.crafterPos.getY() + 0.5,
                    packet.crafterPos.getZ() + 0.5) > 64.0) return;

            // Toggle mode: 0→1, 1→0
            int newMode = crafter.getMode() == 0 ? 1 : 0;
            crafter.setMode(newMode);
            crafter.clearGhostItems();
            menu.updateGhostSlotVisibility();
            menu.updateOutputVisibility();
        });
    }
}
