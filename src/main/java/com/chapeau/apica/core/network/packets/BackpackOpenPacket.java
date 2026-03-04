/**
 * ============================================================
 * [BackpackOpenPacket.java]
 * Description: Packet C2S pour ouvrir un backpack depuis l'inventaire
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | BackpackItem        | Type check           | Validation slot                |
 * | BackpackMenu        | Menu                 | Ouverture GUI                  |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ContainerScreenMagazineMixin.java (envoi)
 * - ApicaNetwork.java (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.apica.core.network.packets;

import com.chapeau.apica.Apica;
import com.chapeau.apica.common.item.BackpackItem;
import com.chapeau.apica.common.menu.BackpackMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Envoye par le client quand le joueur fait clic droit sur un BackpackItem dans l'inventaire.
 * Le serveur valide puis ouvre le BackpackMenu.
 */
public record BackpackOpenPacket(int slotIndex) implements CustomPacketPayload {

    public static final Type<BackpackOpenPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "backpack_open"));

    public static final StreamCodec<FriendlyByteBuf, BackpackOpenPacket> STREAM_CODEC =
            StreamCodec.composite(
                ByteBufCodecs.INT, BackpackOpenPacket::slotIndex,
                BackpackOpenPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BackpackOpenPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            int menuIdx = packet.slotIndex();
            if (menuIdx < 0 || menuIdx >= player.containerMenu.slots.size()) return;

            Slot slot = player.containerMenu.slots.get(menuIdx);
            ItemStack backpackStack = slot.getItem();
            if (!(backpackStack.getItem() instanceof BackpackItem)) return;

            // Resoudre l'index reel dans l'inventaire du joueur
            int invSlot = slot.getContainerSlot();

            player.openMenu(
                new SimpleMenuProvider(
                    (containerId, playerInv, p) -> new BackpackMenu(containerId, playerInv, backpackStack, invSlot),
                    Component.translatable("container.apica.backpack")
                ),
                buf -> buf.writeInt(invSlot)
            );
        });
    }
}
