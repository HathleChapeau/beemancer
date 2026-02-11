/**
 * ============================================================
 * [StorageCraftablesSyncPacket.java]
 * Description: Packet S2C pour synchroniser les recettes craftables vers le client
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | CraftableRecipe     | Donnees recette      | Contenu du packet              |
 * | StorageTerminalMenu | Menu cible           | Mise a jour cache client       |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - StorageItemAggregator.java (envoi sync)
 * - BeemancerNetwork.java (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.beemancer.core.network.packets;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.data.CraftableRecipe;
import com.chapeau.beemancer.common.menu.storage.StorageTerminalMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Synchronise les recettes craftables depuis le crafter vers les terminaux ouverts.
 */
public record StorageCraftablesSyncPacket(
        BlockPos terminalPos,
        List<CraftableRecipe> recipes
) implements CustomPacketPayload {

    public static final Type<StorageCraftablesSyncPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, "storage_craftables_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StorageCraftablesSyncPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, StorageCraftablesSyncPacket::terminalPos,
                    CraftableRecipe.STREAM_CODEC.apply(ByteBufCodecs.list()), StorageCraftablesSyncPacket::recipes,
                    StorageCraftablesSyncPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(StorageCraftablesSyncPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            if (mc.player.containerMenu instanceof StorageTerminalMenu menu) {
                if (menu.getBlockPos().equals(packet.terminalPos)) {
                    menu.setCraftableRecipes(new ArrayList<>(packet.recipes));
                }
            }
        });
    }
}
