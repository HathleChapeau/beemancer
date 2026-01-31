/**
 * ============================================================
 * [InterfaceActionPacket.java]
 * Description: Packet C2S multi-action pour les Import/Export Interface
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | NetworkInterfaceMenu          | Menu cible           | Verification container         |
 * | NetworkInterfaceBlockEntity   | BE cible             | Application des changements    |
 * | ImportInterfaceBlockEntity    | Import specifique    | setMaxCount                    |
 * | ExportInterfaceBlockEntity    | Export specifique    | setMinKeep                     |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - NetworkInterfaceScreen.java (envoi des actions)
 * - BeemancerNetwork.java (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.beemancer.core.network.packets;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.blockentity.storage.ExportInterfaceBlockEntity;
import com.chapeau.beemancer.common.blockentity.storage.ImportInterfaceBlockEntity;
import com.chapeau.beemancer.common.blockentity.storage.NetworkInterfaceBlockEntity;
import com.chapeau.beemancer.common.menu.storage.NetworkInterfaceMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Packet C2S pour les actions de l'interface Import/Export.
 *
 * Actions:
 * - 0: SET_FILTER_MODE (textValue = "ITEM" ou "TEXT")
 * - 1: SET_TEXT_FILTER (slot = index 0-8, textValue = texte du filtre)
 * - 2: SET_MAX_COUNT (slot = delta: +1, -1, +16, -16)
 * - 3: SET_MIN_KEEP (slot = delta: +1, -1, +16, -16)
 */
public record InterfaceActionPacket(int containerId, int action, int slot, String textValue)
        implements CustomPacketPayload {

    public static final int ACTION_SET_FILTER_MODE = 0;
    public static final int ACTION_SET_TEXT_FILTER = 1;
    public static final int ACTION_SET_MAX_COUNT = 2;
    public static final int ACTION_SET_MIN_KEEP = 3;

    public static final Type<InterfaceActionPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, "interface_action"));

    public static final StreamCodec<FriendlyByteBuf, InterfaceActionPacket> STREAM_CODEC =
            StreamCodec.composite(
                ByteBufCodecs.INT, InterfaceActionPacket::containerId,
                ByteBufCodecs.INT, InterfaceActionPacket::action,
                ByteBufCodecs.INT, InterfaceActionPacket::slot,
                ByteBufCodecs.STRING_UTF8, InterfaceActionPacket::textValue,
                InterfaceActionPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(InterfaceActionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!(player.containerMenu instanceof NetworkInterfaceMenu menu)) return;
            if (menu.containerId != packet.containerId()) return;

            NetworkInterfaceBlockEntity be = menu.getBlockEntity();
            if (be == null) return;

            switch (packet.action()) {
                case ACTION_SET_FILTER_MODE -> {
                    try {
                        NetworkInterfaceBlockEntity.FilterMode mode =
                                NetworkInterfaceBlockEntity.FilterMode.valueOf(packet.textValue());
                        be.setFilterMode(mode);
                    } catch (IllegalArgumentException ignored) { }
                }
                case ACTION_SET_TEXT_FILTER -> {
                    int idx = packet.slot();
                    if (idx >= 0 && idx < 9) {
                        be.setTextFilter(idx, packet.textValue());
                    }
                }
                case ACTION_SET_MAX_COUNT -> {
                    if (be instanceof ImportInterfaceBlockEntity imp) {
                        imp.setMaxCount(imp.getMaxCount() + packet.slot());
                    }
                }
                case ACTION_SET_MIN_KEEP -> {
                    if (be instanceof ExportInterfaceBlockEntity exp) {
                        exp.setMinKeep(exp.getMinKeep() + packet.slot());
                    }
                }
            }
        });
    }
}
