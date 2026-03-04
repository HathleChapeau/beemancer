/**
 * ============================================================
 * [BackpackOpenPacket.java]
 * Description: Packet C2S pour ouvrir un backpack depuis un slot accessoire
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | BackpackItem        | Type check           | Validation slot                |
 * | BackpackMenu        | Menu                 | Ouverture GUI                  |
 * | AccessoryPlayerData | Donnees              | Lecture backpack               |
 * | ApicaAttachments    | Acces attachment     | getData                        |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - InventoryScreenAccessoryMixin.java (envoi depuis tab click)
 * - BackpackScreen.java (tab switch)
 * - ApicaNetwork.java (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.apica.core.network.packets;

import com.chapeau.apica.Apica;
import com.chapeau.apica.common.data.AccessoryPlayerData;
import com.chapeau.apica.common.item.BackpackItem;
import com.chapeau.apica.common.menu.BackpackMenu;
import com.chapeau.apica.core.registry.ApicaAttachments;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Envoye par le client quand le joueur clique sur le tab Backpack.
 * Le serveur lit le backpack depuis AccessoryPlayerData et ouvre BackpackMenu.
 */
public record BackpackOpenPacket(int accessorySlot) implements CustomPacketPayload {

    public static final Type<BackpackOpenPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "backpack_open"));

    public static final StreamCodec<FriendlyByteBuf, BackpackOpenPacket> STREAM_CODEC =
            StreamCodec.composite(
                ByteBufCodecs.INT, BackpackOpenPacket::accessorySlot,
                BackpackOpenPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BackpackOpenPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            int slot = packet.accessorySlot();
            if (slot < 0 || slot >= AccessoryPlayerData.SLOT_COUNT) return;

            AccessoryPlayerData data = player.getData(ApicaAttachments.ACCESSORY_DATA);
            ItemStack backpackStack = data.getAccessory(slot);
            if (!(backpackStack.getItem() instanceof BackpackItem)) return;

            player.openMenu(
                new SimpleMenuProvider(
                    (containerId, playerInv, p) -> new BackpackMenu(containerId, playerInv, backpackStack, slot),
                    Component.translatable("container.apica.backpack")
                ),
                buf -> buf.writeInt(slot)
            );
        });
    }
}
