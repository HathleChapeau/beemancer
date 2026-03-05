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
import com.chapeau.apica.common.item.tool.MiningLaserItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger LOG = LoggerFactory.getLogger("ApicaMagazinePacket");

    public static void handle(MagazineEquipPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            LOG.info("[MAG-SRV] Received packet: slotIndex={} equip={}", packet.slotIndex(), packet.equip());

            if (!(context.player() instanceof ServerPlayer player)) {
                LOG.info("[MAG-SRV] Not a ServerPlayer, aborting");
                return;
            }

            if (packet.slotIndex() < 0 || packet.slotIndex() >= player.containerMenu.slots.size()) {
                LOG.info("[MAG-SRV] Invalid slot index {} (menu has {} slots)",
                        packet.slotIndex(), player.containerMenu.slots.size());
                return;
            }

            ItemStack holderStack = player.containerMenu.slots.get(packet.slotIndex()).getItem();
            LOG.info("[MAG-SRV] holderStack item={} isEmpty={}",
                    holderStack.getItem().getClass().getSimpleName(), holderStack.isEmpty());

            if (!(holderStack.getItem() instanceof IMagazineHolder holder)) {
                LOG.info("[MAG-SRV] Not an IMagazineHolder, aborting");
                return;
            }

            if (packet.equip()) {
                handleEquip(player, holder, holderStack);
            } else {
                handleUnequip(player, holderStack);
            }

            syncChargeLevelAfterMagazineChange(holderStack);
            player.containerMenu.broadcastChanges();
        });
    }

    private static void handleEquip(ServerPlayer player, IMagazineHolder holder, ItemStack holderStack) {
        ItemStack cursorStack = player.containerMenu.getCarried();
        LOG.info("[MAG-SRV] handleEquip: cursor={} isEmpty={} isMagItem={}",
                cursorStack.getItem().getClass().getSimpleName(),
                cursorStack.isEmpty(),
                cursorStack.getItem() instanceof MagazineItem);

        if (cursorStack.isEmpty() || !(cursorStack.getItem() instanceof MagazineItem)) {
            LOG.info("[MAG-SRV] handleEquip: cursor not a magazine, aborting");
            return;
        }

        boolean canAccept = holder.canAcceptMagazine(cursorStack);
        String fluidId = MagazineFluidData.getFluidId(cursorStack);
        LOG.info("[MAG-SRV] handleEquip: canAccept={} fluidId='{}'", canAccept, fluidId);

        if (!canAccept) {
            LOG.info("[MAG-SRV] handleEquip: holder rejects magazine, aborting");
            return;
        }

        String newFluidId = MagazineFluidData.getFluidId(cursorStack);
        int newAmount = MagazineFluidData.getFluidAmount(cursorStack);

        if (MagazineData.hasMagazine(holderStack)) {
            ItemStack oldMag = MagazineData.removeMagazine(holderStack);
            MagazineData.setMagazine(holderStack, newFluidId, newAmount);
            player.containerMenu.setCarried(oldMag);
            LOG.info("[MAG-SRV] handleEquip: SWAP done");
        } else {
            MagazineData.setMagazine(holderStack, newFluidId, newAmount);
            cursorStack.shrink(1);
            player.containerMenu.setCarried(cursorStack);
            LOG.info("[MAG-SRV] handleEquip: EQUIP done, newFluid={} newAmount={}", newFluidId, newAmount);
        }
    }

    /**
     * Synchronise le chargeLevel du MiningLaser apres un changement de magazine.
     * Equip/swap → force minimum niveau 1. Unequip → force niveau 0.
     */
    private static void syncChargeLevelAfterMagazineChange(ItemStack holderStack) {
        if (!(holderStack.getItem() instanceof MiningLaserItem)) return;

        boolean hasMag = MagazineData.hasMagazine(holderStack)
                && MagazineData.getFluidAmount(holderStack) > 0;
        if (hasMag) {
            if (MiningLaserItem.getChargeLevel(holderStack) < 1) {
                MiningLaserItem.setChargeLevel(holderStack, 1);
            }
        } else {
            MiningLaserItem.setChargeLevel(holderStack, 0);
        }
    }

    private static void handleUnequip(ServerPlayer player, ItemStack holderStack) {
        if (!MagazineData.hasMagazine(holderStack)) {
            LOG.info("[MAG-SRV] handleUnequip: no magazine on holder");
            return;
        }
        if (!player.containerMenu.getCarried().isEmpty()) {
            LOG.info("[MAG-SRV] handleUnequip: cursor not empty");
            return;
        }

        ItemStack magazineStack = MagazineData.removeMagazine(holderStack);
        player.containerMenu.setCarried(magazineStack);
        LOG.info("[MAG-SRV] handleUnequip: UNEQUIP done");
    }
}
