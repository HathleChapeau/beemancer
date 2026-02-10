/**
 * ============================================================
 * [CrafterGhostSlotPacket.java]
 * Description: Packet C2S pour placer/retirer un ghost item dans la grille craft
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | CrafterBlockEntity      | BE cible             | setGhostItem                   |
 * | CrafterMenu             | Menu validation      | containerId check              |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - CrafterScreen.java (envoi au clic ghost)
 * - BeemancerNetwork.java (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.beemancer.core.network.packets;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.blockentity.storage.CrafterBlockEntity;
import com.chapeau.beemancer.common.menu.storage.CrafterMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CrafterGhostSlotPacket(BlockPos crafterPos, int slotIndex, ItemStack ghostItem)
        implements CustomPacketPayload {

    public static final Type<CrafterGhostSlotPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, "crafter_ghost_slot"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CrafterGhostSlotPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, CrafterGhostSlotPacket::crafterPos,
                    ByteBufCodecs.INT, CrafterGhostSlotPacket::slotIndex,
                    ItemStack.STREAM_CODEC, CrafterGhostSlotPacket::ghostItem,
                    CrafterGhostSlotPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CrafterGhostSlotPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            // Validate slot index
            if (packet.slotIndex < 0 || packet.slotIndex >= CrafterBlockEntity.GHOST_GRID_SIZE) return;

            // Validate player has crafter menu open
            if (!(player.containerMenu instanceof CrafterMenu menu)) return;

            // Validate crafter exists and matches
            BlockEntity be = player.level().getBlockEntity(packet.crafterPos);
            if (!(be instanceof CrafterBlockEntity crafter)) return;

            // Validate distance
            if (player.distanceToSqr(
                    packet.crafterPos.getX() + 0.5,
                    packet.crafterPos.getY() + 0.5,
                    packet.crafterPos.getZ() + 0.5) > 64.0) return;

            crafter.setGhostItem(packet.slotIndex, packet.ghostItem);
        });
    }
}
