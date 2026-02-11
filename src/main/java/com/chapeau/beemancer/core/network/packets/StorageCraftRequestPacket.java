/**
 * ============================================================
 * [StorageCraftRequestPacket.java]
 * Description: Packet C2S pour requete hybride stock+craft depuis le terminal
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | StorageTerminalBlockEntity    | Terminal cible       | requestItem, getController     |
 * | CrafterBlockEntity            | Crafter lie          | Library, CraftManager          |
 * | CraftManager                  | Queue de crafts      | queueCrafts                    |
 * | CraftingPaperData             | Donnees recette      | Recherche dans la library      |
 * | CraftingPaperItem             | Filtre items         | Identification papers          |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - TerminalRequestPopup.java (envoi requete)
 * - BeemancerNetwork.java (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.beemancer.core.network.packets;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.blockentity.storage.CraftManager;
import com.chapeau.beemancer.common.blockentity.storage.CrafterBlockEntity;
import com.chapeau.beemancer.common.blockentity.storage.StorageControllerBlockEntity;
import com.chapeau.beemancer.common.blockentity.storage.StorageTerminalBlockEntity;
import com.chapeau.beemancer.common.data.CraftingPaperData;
import com.chapeau.beemancer.common.item.CraftingPaperItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Requete hybride: stockCount items depuis le reseau + craftCount items via auto-craft.
 */
public record StorageCraftRequestPacket(
        BlockPos terminalPos,
        ItemStack requestedItem,
        int stockCount,
        int craftCount
) implements CustomPacketPayload {

    public static final Type<StorageCraftRequestPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, "storage_craft_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StorageCraftRequestPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, StorageCraftRequestPacket::terminalPos,
                    ItemStack.STREAM_CODEC, StorageCraftRequestPacket::requestedItem,
                    ByteBufCodecs.INT, StorageCraftRequestPacket::stockCount,
                    ByteBufCodecs.INT, StorageCraftRequestPacket::craftCount,
                    StorageCraftRequestPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(StorageCraftRequestPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;

            double distSqr = player.distanceToSqr(
                    packet.terminalPos.getX() + 0.5,
                    packet.terminalPos.getY() + 0.5,
                    packet.terminalPos.getZ() + 0.5);
            if (distSqr > 64.0) return;

            BlockEntity be = player.level().getBlockEntity(packet.terminalPos);
            if (!(be instanceof StorageTerminalBlockEntity terminal)) return;
            if (!terminal.isLinked()) return;

            StorageControllerBlockEntity controller = terminal.getController();
            if (controller == null || controller.isHoneyDepleted()) return;

            // Fulfill stock portion via existing request system
            int safeStock = Mth.clamp(packet.stockCount, 0, 64 * 54);
            if (safeStock > 0) {
                terminal.requestItem(packet.requestedItem, safeStock);
            }

            // Fulfill craft portion via CraftManager
            int safeCraft = Mth.clamp(packet.craftCount, 0, 64 * 27);
            if (safeCraft > 0) {
                CrafterBlockEntity crafter = controller.getCrafter();
                if (crafter == null) return;

                CraftingPaperData recipe = findRecipeFor(
                        crafter, packet.requestedItem, player.level().registryAccess());
                if (recipe == null) return;

                int resultPerCraft = recipe.result().getCount();
                int craftsNeeded = (safeCraft + resultPerCraft - 1) / resultPerCraft;
                long gameTick = player.level().getGameTime();

                CraftManager craftManager = crafter.getCraftManager();
                craftManager.queueCrafts(recipe, craftsNeeded, gameTick);
            }
        });
    }

    /**
     * Cherche un CraftingPaper dans la library du crafter dont le resultat correspond.
     */
    private static CraftingPaperData findRecipeFor(CrafterBlockEntity crafter,
                                                     ItemStack target,
                                                     HolderLookup.Provider registries) {
        ItemStackHandler inv = crafter.getInventory();
        for (int slot = CrafterBlockEntity.LIBRARY_START; slot <= CrafterBlockEntity.LIBRARY_END; slot++) {
            ItemStack paper = inv.getStackInSlot(slot);
            if (paper.isEmpty() || !(paper.getItem() instanceof CraftingPaperItem)) continue;
            CraftingPaperData data = CraftingPaperData.readFromStack(paper, registries);
            if (data == null || data.result().isEmpty()) continue;
            if (ItemStack.isSameItemSameComponents(data.result(), target)) {
                return data;
            }
        }
        return null;
    }
}
