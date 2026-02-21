/**
 * ============================================================
 * [ItemFilterActionPacket.java]
 * Description: Packet C2S pour les actions du filtre d'item pipe
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | ItemFilterMenu                | Menu cible           | Verification container         |
 * | ItemPipeBlockEntity           | BE cible             | Application des changements    |
 * | ItemFilterData                | Donnees filtre       | Mode, priority, ghost slots    |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ItemFilterScreen.java (envoi des actions)
 * - ApicaNetwork.java (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.apica.core.network.packets;

import com.chapeau.apica.Apica;
import com.chapeau.apica.common.blockentity.alchemy.ItemPipeBlockEntity;
import com.chapeau.apica.common.data.ItemFilterData;
import com.chapeau.apica.common.menu.ItemFilterMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Packet C2S pour les actions du filtre d'item pipe.
 *
 * Actions:
 * - 0: TOGGLE_MODE — bascule Accept/Deny
 * - 1: CHANGE_PRIORITY — value = delta (+1 ou -1)
 * - 2: SET_GHOST_SLOT — value = slot index (0-8), itemData = l'item a placer/retirer
 */
public record ItemFilterActionPacket(int containerId, int action, int value, ItemStack itemData)
        implements CustomPacketPayload {

    public static final int ACTION_TOGGLE_MODE = 0;
    public static final int ACTION_CHANGE_PRIORITY = 1;
    public static final int ACTION_SET_GHOST_SLOT = 2;

    public static final Type<ItemFilterActionPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "item_filter_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ItemFilterActionPacket> STREAM_CODEC =
            StreamCodec.composite(
                ByteBufCodecs.INT, ItemFilterActionPacket::containerId,
                ByteBufCodecs.INT, ItemFilterActionPacket::action,
                ByteBufCodecs.INT, ItemFilterActionPacket::value,
                ItemStack.OPTIONAL_STREAM_CODEC, ItemFilterActionPacket::itemData,
                ItemFilterActionPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ItemFilterActionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!(player.containerMenu instanceof ItemFilterMenu menu)) return;
            if (menu.containerId != packet.containerId()) return;

            ItemPipeBlockEntity be = menu.getBlockEntity();
            if (be == null) return;
            ItemFilterData filter = be.getFilter();
            if (filter == null) return;

            switch (packet.action()) {
                case ACTION_TOGGLE_MODE -> {
                    filter.toggleMode();
                    be.setChanged();
                    be.syncToClient();
                }
                case ACTION_CHANGE_PRIORITY -> {
                    filter.setPriority(filter.getPriority() + packet.value());
                    be.setChanged();
                    be.syncToClient();
                }
                case ACTION_SET_GHOST_SLOT -> {
                    int slot = packet.value();
                    if (slot >= 0 && slot < ItemFilterData.SLOT_COUNT) {
                        filter.setGhostSlot(slot, packet.itemData());
                        be.setChanged();
                        be.syncToClient();
                    }
                }
            }
        });
    }
}
