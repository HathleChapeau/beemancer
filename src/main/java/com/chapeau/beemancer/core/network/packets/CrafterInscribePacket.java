/**
 * ============================================================
 * [CrafterInscribePacket.java]
 * Description: Packet C2S pour inscrire une recette sur un Crafting Paper
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | CrafterBlockEntity      | BE cible             | Inventaire, ghost items        |
 * | CraftingPaperData       | Donnees recette      | Ecriture sur le paper          |
 * | PartCraftingPaperData   | Donnees machine      | Ecriture sur les parts         |
 * | CraftingPaperItem       | Item recette         | Verification slot              |
 * | PartCraftingPaperItem   | Item part            | Creation des parts             |
 * | BeemancerItems          | Registre items       | Creation des stacks            |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - CrafterScreen.java (bouton Inscribe)
 * - BeemancerNetwork.java (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.beemancer.core.network.packets;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.blockentity.storage.CrafterBlockEntity;
import com.chapeau.beemancer.common.data.CraftingPaperData;
import com.chapeau.beemancer.common.data.PartCraftingPaperData;
import com.chapeau.beemancer.common.item.CraftingPaperItem;
import com.chapeau.beemancer.common.menu.storage.CrafterMenu;
import com.chapeau.beemancer.core.registry.BeemancerItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record CrafterInscribePacket(
        BlockPos crafterPos,
        int mode,
        List<ItemStack> machineInputs,
        List<ItemStack> machineOutputs
) implements CustomPacketPayload {

    public static final Type<CrafterInscribePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, "crafter_inscribe"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CrafterInscribePacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, CrafterInscribePacket::crafterPos,
                    ByteBufCodecs.INT, CrafterInscribePacket::mode,
                    ItemStack.LIST_STREAM_CODEC, CrafterInscribePacket::machineInputs,
                    ItemStack.LIST_STREAM_CODEC, CrafterInscribePacket::machineOutputs,
                    CrafterInscribePacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CrafterInscribePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            if (!(player.containerMenu instanceof CrafterMenu)) return;
            if (!(player.level() instanceof ServerLevel serverLevel)) return;

            BlockEntity be = serverLevel.getBlockEntity(packet.crafterPos);
            if (!(be instanceof CrafterBlockEntity crafter)) return;

            if (player.distanceToSqr(
                    packet.crafterPos.getX() + 0.5,
                    packet.crafterPos.getY() + 0.5,
                    packet.crafterPos.getZ() + 0.5) > 64.0) return;

            // Check blank paper available in slot 0
            ItemStack paperStack = crafter.getInventory().getStackInSlot(CrafterBlockEntity.SLOT_RESERVE);
            if (paperStack.isEmpty() || !(paperStack.getItem() instanceof CraftingPaperItem)) return;
            if (CraftingPaperData.hasData(paperStack)) return;

            if (packet.mode == 0) {
                handleCraftInscribe(crafter, serverLevel);
            } else {
                handleMachineInscribe(crafter, serverLevel, packet.machineInputs, packet.machineOutputs);
            }
        });
    }

    private static void handleCraftInscribe(CrafterBlockEntity crafter, ServerLevel level) {
        // Read 9 ghost items
        List<ItemStack> ingredients = new ArrayList<>(9);
        boolean hasAny = false;
        for (int i = 0; i < CrafterBlockEntity.GHOST_GRID_SIZE; i++) {
            ItemStack ghost = crafter.getGhostItems().getStackInSlot(i);
            ingredients.add(ghost.isEmpty() ? ItemStack.EMPTY : ghost.copy());
            if (!ghost.isEmpty()) hasAny = true;
        }
        if (!hasAny) return;

        // Build CraftingInput and find recipe
        CraftingInput craftInput = CraftingInput.of(3, 3, ingredients);
        var recipeHolder = level.getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, craftInput, level);
        if (recipeHolder.isEmpty()) return;

        ItemStack result = recipeHolder.get().value().assemble(craftInput, level.registryAccess());
        if (result.isEmpty()) return;

        // Check output slot A is empty
        if (!crafter.getInventory().getStackInSlot(CrafterBlockEntity.SLOT_OUTPUT_A).isEmpty()) return;

        // Consume 1 blank paper
        crafter.getInventory().extractItem(CrafterBlockEntity.SLOT_RESERVE, 1, false);

        // Create inscribed Crafting Paper
        ItemStack inscribed = new ItemStack(BeemancerItems.CRAFTING_PAPER.get());
        CraftingPaperData data = new CraftingPaperData(ingredients, result);
        CraftingPaperData.applyToStack(inscribed, data, level.registryAccess());

        // Place in output A
        crafter.setOutputA(inscribed);
    }

    private static void handleMachineInscribe(CrafterBlockEntity crafter,
                                               ServerLevel level,
                                               List<ItemStack> inputs,
                                               List<ItemStack> outputs) {
        // Validate: at least 1 input and 1 output
        List<ItemStack> validInputs = inputs.stream()
                .filter(s -> !s.isEmpty()).toList();
        List<ItemStack> validOutputs = outputs.stream()
                .filter(s -> !s.isEmpty()).toList();
        if (validInputs.isEmpty() || validOutputs.isEmpty()) return;

        // Check both output slots are empty
        if (!crafter.getInventory().getStackInSlot(CrafterBlockEntity.SLOT_OUTPUT_A).isEmpty()) return;
        if (!crafter.getInventory().getStackInSlot(CrafterBlockEntity.SLOT_OUTPUT_B).isEmpty()) return;

        // Consume 1 blank paper
        crafter.getInventory().extractItem(CrafterBlockEntity.SLOT_RESERVE, 1, false);

        // Generate shared craftId
        UUID craftId = UUID.randomUUID();

        // Create INPUT Part Crafting Paper
        ItemStack inputPart = new ItemStack(BeemancerItems.PART_CRAFTING_PAPER.get());
        PartCraftingPaperData inputData = new PartCraftingPaperData(
                PartCraftingPaperData.PartMode.INPUT, craftId, new ArrayList<>(validInputs));
        PartCraftingPaperData.applyToStack(inputPart, inputData, level.registryAccess());

        // Create OUTPUT Part Crafting Paper
        ItemStack outputPart = new ItemStack(BeemancerItems.PART_CRAFTING_PAPER.get());
        PartCraftingPaperData outputData = new PartCraftingPaperData(
                PartCraftingPaperData.PartMode.OUTPUT, craftId, new ArrayList<>(validOutputs));
        PartCraftingPaperData.applyToStack(outputPart, outputData, level.registryAccess());

        // Place in output slots
        crafter.setOutputA(inputPart);
        crafter.setOutputB(outputPart);
    }
}
