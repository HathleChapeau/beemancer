/**
 * ============================================================
 * [MagazineEquipPacket.java]
 * Description: Packet C2S pour equiper/desequiper un magazine sur un item holder
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | MagazineData        | Data holder          | setMagazine/removeMagazine     |
 * | MagazineFluidData   | Data magazine        | Lecture fluide                 |
 * | IMagazineHolder     | Interface            | Validation compatibilite       |
 * | MagazineItem        | Type check           | Verification curseur           |
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
import com.chapeau.apica.common.item.magazine.IMagazineHolder;
import com.chapeau.apica.common.item.magazine.MagazineData;
import com.chapeau.apica.common.item.magazine.MagazineFluidData;
import com.chapeau.apica.common.item.magazine.MagazineItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Packet C2S envoye quand le joueur clique sur le slot magazine bonus dans l'inventaire.
 * equip=true: attache le magazine du curseur sur l'item du slot.
 * equip=false: detache le magazine et le place dans le curseur.
 */
public record MagazineEquipPacket(int slotIndex, boolean equip)
        implements CustomPacketPayload {

    public static final Type<MagazineEquipPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Apica.MOD_ID, "magazine_equip"));

    public static final StreamCodec<FriendlyByteBuf, MagazineEquipPacket> STREAM_CODEC =
            StreamCodec.composite(
                ByteBufCodecs.INT, MagazineEquipPacket::slotIndex,
                ByteBufCodecs.BOOL, MagazineEquipPacket::equip,
                MagazineEquipPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MagazineEquipPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            // Verifier que le slot est valide
            if (packet.slotIndex() < 0 || packet.slotIndex() >= player.containerMenu.slots.size()) return;

            ItemStack holderStack = player.containerMenu.slots.get(packet.slotIndex()).getItem();
            if (!(holderStack.getItem() instanceof IMagazineHolder holder)) return;

            if (packet.equip()) {
                handleEquip(player, holder, holderStack);
            } else {
                handleUnequip(player, holderStack);
            }

            player.containerMenu.broadcastChanges();
        });
    }

    private static void handleEquip(ServerPlayer player, IMagazineHolder holder, ItemStack holderStack) {
        ItemStack cursorStack = player.containerMenu.getCarried();
        if (cursorStack.isEmpty() || !(cursorStack.getItem() instanceof MagazineItem)) return;

        // Verifier compatibilite fluide
        if (!holder.canAcceptMagazine(cursorStack)) return;

        // Si le holder a deja un magazine, ne pas ecraser
        if (MagazineData.hasMagazine(holderStack)) return;

        // Transferer les donnees du magazine vers le holder
        String fluidId = MagazineFluidData.getFluidId(cursorStack);
        int amount = MagazineFluidData.getFluidAmount(cursorStack);
        MagazineData.setMagazine(holderStack, fluidId, amount);

        // Consommer le magazine du curseur
        cursorStack.shrink(1);
        player.containerMenu.setCarried(cursorStack);
    }

    private static void handleUnequip(ServerPlayer player, ItemStack holderStack) {
        // Le curseur doit etre vide pour recuperer le magazine
        if (!player.containerMenu.getCarried().isEmpty()) return;
        if (!MagazineData.hasMagazine(holderStack)) return;

        ItemStack magazineStack = MagazineData.removeMagazine(holderStack);
        player.containerMenu.setCarried(magazineStack);
    }
}
