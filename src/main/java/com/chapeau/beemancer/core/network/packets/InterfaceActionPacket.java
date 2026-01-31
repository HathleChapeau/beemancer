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
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
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

    public static void handle(InterfaceActionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
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
                case ACTION_SET_SELECTED_SLOTS -> {
                    java.util.Set<Integer> slots = parseSlotSet(packet.textValue());
                    be.setFilterSelectedSlots(packet.slot(), slots);
                }
                case ACTION_SET_GLOBAL_SELECTED_SLOTS -> {
                    java.util.Set<Integer> slots = parseSlotSet(packet.textValue());
                    be.setGlobalSelectedSlots(slots);
                }
                case ACTION_OPEN_ADJACENT_GUI -> {
                    com.chapeau.beemancer.Beemancer.LOGGER.info("[DEBUG BTN SERVER] Received ACTION_OPEN_ADJACENT_GUI from {}", player.getName().getString());
                    if (be.getLevel() == null) {
                        com.chapeau.beemancer.Beemancer.LOGGER.info("[DEBUG BTN SERVER] be.getLevel() == null, aborting");
                        return;
                    }
                    BlockPos adjPos = be.getAdjacentPos();
                    Direction facing = be.getFacing();
                    com.chapeau.beemancer.Beemancer.LOGGER.info("[DEBUG BTN SERVER] adjPos={}, facing={}", adjPos, facing);
                    if (!be.getLevel().isLoaded(adjPos)) {
                        com.chapeau.beemancer.Beemancer.LOGGER.info("[DEBUG BTN SERVER] adjPos not loaded, aborting");
                        return;
                    }
                    net.minecraft.world.level.block.state.BlockState adjState =
                            be.getLevel().getBlockState(adjPos);
                    com.chapeau.beemancer.Beemancer.LOGGER.info("[DEBUG BTN SERVER] adjState={}", adjState);
                    net.minecraft.world.level.block.entity.BlockEntity adjBe = be.getLevel().getBlockEntity(adjPos);
                    com.chapeau.beemancer.Beemancer.LOGGER.info("[DEBUG BTN SERVER] adjBE={}, isMenuProvider={}",
                            adjBe != null ? adjBe.getClass().getSimpleName() : "null",
                            adjBe instanceof net.minecraft.world.MenuProvider);
                    BlockHitResult hitResult = new BlockHitResult(
                            Vec3.atCenterOf(adjPos), facing.getOpposite(), adjPos, false);
                    var result = adjState.useWithoutItem(be.getLevel(), player, hitResult);
                    com.chapeau.beemancer.Beemancer.LOGGER.info("[DEBUG BTN SERVER] useWithoutItem result={}", result);
                }
            }
        });
    }
}
