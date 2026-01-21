/**
 * ============================================================
 * [BeeCreatorMenu.java]
 * Description: Menu pour le BeeCreator avec sélection de gènes
 * ============================================================
 */
package com.chapeau.beemancer.common.menu;

import com.chapeau.beemancer.common.item.bee.MagicBeeItem;
import com.chapeau.beemancer.core.gene.BeeGeneData;
import com.chapeau.beemancer.core.gene.Gene;
import com.chapeau.beemancer.core.gene.GeneCategory;
import com.chapeau.beemancer.core.gene.GeneRegistry;
import com.chapeau.beemancer.core.registry.BeemancerItems;
import com.chapeau.beemancer.core.registry.BeemancerMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BeeCreatorMenu extends AbstractContainerMenu {
    private final Container container;
    private final BlockPos blockPos;
    private final ContainerLevelAccess access;
    
    // Current gene selections (for editing before applying)
    private final BeeGeneData editingGenes = new BeeGeneData();
    private final List<GeneCategory> categories;

    // Client constructor
    public BeeCreatorMenu(int containerId, Inventory playerInv, FriendlyByteBuf data) {
        this(containerId, playerInv, new SimpleContainer(1), data.readBlockPos());
    }

    // Server constructor
    public BeeCreatorMenu(int containerId, Inventory playerInv, Container container, BlockPos pos) {
        super(BeemancerMenus.BEE_CREATOR.get(), containerId);
        this.container = container;
        this.blockPos = pos;
        this.access = ContainerLevelAccess.create(playerInv.player.level(), pos);
        
        checkContainerSize(container, 1);
        container.startOpen(playerInv.player);

        // Get sorted categories
        this.categories = new ArrayList<>(GeneRegistry.getAllCategories());
        this.categories.sort(Comparator.comparingInt(GeneCategory::getDisplayOrder));

        // Bee slot (centered at top)
        this.addSlot(new BeeSlot(container, 0, 80, 20));

        // Player inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 102 + row * 18));
            }
        }

        // Player hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 160));
        }

        // Initialize editing genes from bee if present
        loadGenesFromBee();
    }

    public List<GeneCategory> getCategories() {
        return categories;
    }

    public Gene getSelectedGene(GeneCategory category) {
        return editingGenes.getGene(category);
    }

    public void loadGenesFromBee() {
        ItemStack beeStack = container.getItem(0);
        if (!beeStack.isEmpty() && beeStack.is(BeemancerItems.MAGIC_BEE.get())) {
            BeeGeneData beeData = MagicBeeItem.getGeneData(beeStack);
            editingGenes.copyFrom(beeData);
        }
    }

    public void cycleGeneNext(GeneCategory category) {
        Gene current = editingGenes.getGene(category);
        if (current != null) {
            Gene next = GeneRegistry.getNextGene(current);
            editingGenes.setGene(next);
        }
    }

    public void cycleGenePrevious(GeneCategory category) {
        Gene current = editingGenes.getGene(category);
        if (current != null) {
            Gene prev = GeneRegistry.getPreviousGene(current);
            editingGenes.setGene(prev);
        }
    }

    public boolean canApplyChanges() {
        ItemStack beeStack = container.getItem(0);
        return !beeStack.isEmpty() && beeStack.is(BeemancerItems.MAGIC_BEE.get());
    }

    public void applyChanges() {
        if (!canApplyChanges()) return;
        
        ItemStack beeStack = container.getItem(0);
        MagicBeeItem.saveGeneData(beeStack, editingGenes);
        container.setChanged();
    }

    public boolean hasBee() {
        return !container.getItem(0).isEmpty();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            result = slotStack.copy();
            
            if (index == 0) {
                // From bee slot to inventory
                if (!this.moveItemStackTo(slotStack, 1, 37, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // From inventory to bee slot
                if (slotStack.is(BeemancerItems.MAGIC_BEE.get())) {
                    if (!this.moveItemStackTo(slotStack, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }
            
            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }

    // Custom slot that only accepts MagicBee
    private static class BeeSlot extends Slot {
        public BeeSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.is(BeemancerItems.MAGIC_BEE.get());
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }
}
