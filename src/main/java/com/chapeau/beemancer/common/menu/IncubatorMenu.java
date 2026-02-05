/**
 * ============================================================
 * [IncubatorMenu.java]
 * Description: Menu de l'incubateur avec slot larve unique
 * ============================================================
 */
package com.chapeau.beemancer.common.menu;

import com.chapeau.beemancer.client.gui.widget.BeemancerSlot;
import com.chapeau.beemancer.common.block.incubator.IncubatorBlockEntity;
import com.chapeau.beemancer.common.item.bee.MagicBeeItem;
import com.chapeau.beemancer.common.quest.QuestEvents;
import com.chapeau.beemancer.content.gene.species.DataDrivenSpeciesGene;
import com.chapeau.beemancer.core.gene.BeeGeneData;
import com.chapeau.beemancer.core.gene.Gene;
import com.chapeau.beemancer.core.gene.GeneCategory;
import com.chapeau.beemancer.core.registry.BeemancerBlocks;
import com.chapeau.beemancer.core.registry.BeemancerItems;
import com.chapeau.beemancer.core.registry.BeemancerMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.ItemStackHandler;

public class IncubatorMenu extends BeemancerMenu {
    private final IncubatorBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    private final ContainerData data;

    private static final int INCUBATOR_SLOT = 0;
    private static final int PLAYER_INV_START = 1;
    private static final int PLAYER_INV_END = 28;
    private static final int HOTBAR_START = 28;
    private static final int HOTBAR_END = 37;

    // Client constructor
    public IncubatorMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory,
                playerInventory.player.level().getBlockEntity(buf.readBlockPos()),
                new SimpleContainerData(2));
    }

    // Server constructor
    public IncubatorMenu(int containerId, Inventory playerInventory, BlockEntity be, ContainerData data) {
        super(BeemancerMenus.INCUBATOR.get(), containerId);
        this.blockEntity = (IncubatorBlockEntity) be;
        this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());
        this.data = data;

        addDataSlots(data);

        // Single slot - accepts larva, allows extracting bee
        addSlot(new BeemancerSlot(blockEntity.getItemHandler(), 0, 80, 35)
                .withFilter(stack -> stack.is(BeemancerItems.BEE_LARVA.get()))
                .withOnExtract(this::onBeeExtracted));

        // Player inventory
        addPlayerInventory(playerInventory, 8, 88);
        addPlayerHotbar(playerInventory, 8, 146);
    }

    public IncubatorBlockEntity getBlockEntity() { return blockEntity; }

    public int getIncubationProgress() {
        return data.get(0);
    }

    public int getIncubationTime() {
        return data.get(1);
    }

    public float getProgressRatio() {
        int time = getIncubationTime();
        if (time <= 0) return 0;
        return (float) getIncubationProgress() / time;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);

        if (slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            result = stackInSlot.copy();

            if (index == INCUBATOR_SLOT) {
                // From incubator to player
                if (!moveItemStackTo(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // From player to incubator
                if (!moveItemStackTo(stackInSlot, INCUBATOR_SLOT, INCUBATOR_SLOT + 1, false)) {
                    // Si ca ne rentre pas, deplacer entre inventaire et hotbar
                    if (index >= PLAYER_INV_START && index < PLAYER_INV_END) {
                        if (!moveItemStackTo(stackInSlot, HOTBAR_START, HOTBAR_END, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (index >= HOTBAR_START && index < HOTBAR_END) {
                        if (!moveItemStackTo(stackInSlot, PLAYER_INV_START, PLAYER_INV_END, false)) {
                            return ItemStack.EMPTY;
                        }
                    }
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, BeemancerBlocks.INCUBATOR.get());
    }

    /**
     * Callback quand un joueur extrait une abeille du slot.
     * Detecte l'espece et complete la quete BEE_INCUBATOR correspondante.
     */
    private void onBeeExtracted(Player player, ItemStack stack) {
        if (stack.is(BeemancerItems.MAGIC_BEE.get())) {
            BeeGeneData geneData = MagicBeeItem.getGeneData(stack);
            Gene speciesGene = geneData.getGene(GeneCategory.SPECIES);
            if (speciesGene instanceof DataDrivenSpeciesGene ddGene) {
                String speciesId = ddGene.getId();
                QuestEvents.onBeeIncubatorExtract(player, speciesId);
            }
        }
    }
}
