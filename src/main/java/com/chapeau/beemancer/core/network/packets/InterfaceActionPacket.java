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
 * | InterfaceFilter               | Filtre individuel    | Mode, texte, quantite          |
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
import com.chapeau.beemancer.common.blockentity.storage.InterfaceFilter;
import com.chapeau.beemancer.common.blockentity.storage.NetworkInterfaceBlockEntity;
import com.chapeau.beemancer.common.menu.storage.NetworkInterfaceMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Packet C2S pour les actions de l'interface Import/Export.
 *
 * Actions:
 * - 0: ADD_FILTER — ajoute un filtre (max 3)
 * - 1: REMOVE_FILTER — supprime un filtre (slot = filterIndex)
 * - 2: SET_FILTER_MODE — change le mode d'un filtre (slot = filterIndex, textValue = "ITEM"/"TEXT")
 * - 3: SET_FILTER_TEXT — change le texte d'un filtre (slot = filterIndex, textValue = texte)
 * - 4: SET_FILTER_QUANTITY — change la quantite d'un filtre (slot = filterIndex, textValue = qty string)
 */
public record InterfaceActionPacket(int containerId, int action, int slot, String textValue)
        implements CustomPacketPayload {

    public static final int ACTION_ADD_FILTER = 0;
    public static final int ACTION_REMOVE_FILTER = 1;
    public static final int ACTION_SET_FILTER_MODE = 2;
    public static final int ACTION_SET_FILTER_TEXT = 3;
    public static final int ACTION_SET_FILTER_QUANTITY = 4;
    public static final int ACTION_SET_SELECTED_SLOTS = 5;
    public static final int ACTION_SET_GLOBAL_SELECTED_SLOTS = 6;
    public static final int ACTION_OPEN_ADJACENT_GUI = 7;

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

    private static java.util.Set<Integer> parseSlotSet(String text) {
        java.util.Set<Integer> slots = new java.util.HashSet<>();
        if (text == null || text.isEmpty()) return slots;
        for (String part : text.split(",")) {
            try {
                slots.add(Integer.parseInt(part.trim()));
            } catch (NumberFormatException ignored) { }
        }
        return slots;
    }

    /**
     * Parse le textValue encode avec BlockPos: "x,y,z;slot1,slot2,slot3".
     * Retourne la partie apres le ';' (les slots), ou le texte entier si pas de ';'.
     */
    private static String extractSlotsFromText(String text) {
        int sepIdx = text.indexOf(';');
        return sepIdx >= 0 ? text.substring(sepIdx + 1) : text;
    }

    /**
     * Cherche le NetworkInterfaceBlockEntity soit via le menu actif du joueur,
     * soit via le BlockPos encode dans le textValue (fallback pour l'overlay adjacent).
     */
    private static NetworkInterfaceBlockEntity findBlockEntity(
            ServerPlayer player, int containerId, String textValue) {
        // Chemin normal: le joueur a le menu ouvert
        if (player.containerMenu instanceof NetworkInterfaceMenu menu
                && menu.containerId == containerId) {
            return menu.getBlockEntity();
        }
        // Fallback: BlockPos encode dans le textValue (format "x,y,z;...")
        int sepIdx = textValue.indexOf(';');
        if (sepIdx < 0) return null;
        String posStr = textValue.substring(0, sepIdx);
        String[] parts = posStr.split(",");
        if (parts.length != 3) return null;
        try {
            BlockPos pos = new BlockPos(
                Integer.parseInt(parts[0].trim()),
                Integer.parseInt(parts[1].trim()),
                Integer.parseInt(parts[2].trim())
            );
            if (player.blockPosition().distSqr(pos) > 64 * 64) return null;
            if (player.level().getBlockEntity(pos)
                    instanceof NetworkInterfaceBlockEntity foundBe) {
                return foundBe;
            }
        } catch (NumberFormatException ignored) { }
        return null;
    }

    public static void handle(InterfaceActionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            // Pour les actions de selection de slots, on accepte le fallback par BlockPos
            if (packet.action() == ACTION_SET_SELECTED_SLOTS
                    || packet.action() == ACTION_SET_GLOBAL_SELECTED_SLOTS) {
                NetworkInterfaceBlockEntity be = findBlockEntity(
                    player, packet.containerId(), packet.textValue());
                if (be == null) return;
                String slotsText = extractSlotsFromText(packet.textValue());
                java.util.Set<Integer> slots = parseSlotSet(slotsText);
                if (packet.action() == ACTION_SET_GLOBAL_SELECTED_SLOTS) {
                    be.setGlobalSelectedSlots(slots);
                } else {
                    be.setFilterSelectedSlots(packet.slot(), slots);
                }
                return;
            }

            // Toutes les autres actions requierent le menu actif
            if (!(player.containerMenu instanceof NetworkInterfaceMenu menu)) return;
            if (menu.containerId != packet.containerId()) return;

            NetworkInterfaceBlockEntity be = menu.getBlockEntity();
            if (be == null) return;

            switch (packet.action()) {
                case ACTION_ADD_FILTER -> {
                    be.addFilter();
                    menu.updateFilterSlots();
                }
                case ACTION_REMOVE_FILTER -> {
                    be.removeFilter(packet.slot());
                    menu.updateFilterSlots();
                }
                case ACTION_SET_FILTER_MODE -> {
                    try {
                        InterfaceFilter.FilterMode mode =
                                InterfaceFilter.FilterMode.valueOf(packet.textValue());
                        be.setFilterMode(packet.slot(), mode);
                    } catch (IllegalArgumentException ignored) { }
                }
                case ACTION_SET_FILTER_TEXT -> {
                    be.setFilterText(packet.slot(), packet.textValue());
                }
                case ACTION_SET_FILTER_QUANTITY -> {
                    try {
                        int qty = Integer.parseInt(packet.textValue());
                        be.setFilterQuantity(packet.slot(), qty);
                    } catch (NumberFormatException ignored) { }
                }
                case ACTION_OPEN_ADJACENT_GUI -> {
                    if (be.getLevel() == null) return;
                    BlockPos adjPos = be.getAdjacentPos();
                    if (!be.getLevel().isLoaded(adjPos)) return;
                    net.minecraft.world.level.block.entity.BlockEntity adjBe =
                            be.getLevel().getBlockEntity(adjPos);
                    if (adjBe instanceof net.minecraft.world.MenuProvider menuProvider) {
                        player.openMenu(menuProvider, adjPos);
                    }
                }
            }
        });
    }
}
