/**
 * ============================================================
 * [CrafterBlockEntity.java]
 * Description: BlockEntity du Crafter - inventaire, ghost items, craft buffer
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | IDeliveryEndpoint       | Reception items bee  | receiveDeliveredItems          |
 * | CraftManager            | Orchestration crafts | Tick, submit, cancel           |
 * | CraftTask               | Tache de craft       | Validation livraisons, extract |
 * | CraftingPaperData       | Donnees recette      | Lecture bibliotheque           |
 * | CraftingPaperItem       | Filtrage slots       | isItemValid                    |
 * | PartCraftingPaperItem   | Filtrage slots       | isItemValid                    |
 * | BeemancerBlockEntities  | Type du BlockEntity  | Constructeur                   |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - CrafterBlock.java (creation, onRemove)
 * - CrafterMenu.java (slots, ContainerData)
 * - CraftManager.java (lecture recettes, craft buffer)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.blockentity.storage;

import com.chapeau.beemancer.common.data.CraftTask;
import com.chapeau.beemancer.common.data.CraftingPaperData;
import com.chapeau.beemancer.common.data.PartCraftingPaperData;
import com.chapeau.beemancer.common.item.CraftingPaperItem;
import com.chapeau.beemancer.common.item.PartCraftingPaperItem;
import com.chapeau.beemancer.common.menu.storage.CrafterMenu;
import com.chapeau.beemancer.core.registry.BeemancerBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CrafterBlockEntity extends BlockEntity implements MenuProvider, IDeliveryEndpoint {

    public static final int SLOT_RESERVE = 0;
    public static final int SLOT_OUTPUT_A = 1;
    public static final int SLOT_OUTPUT_B = 2;
    public static final int LIBRARY_START = 3;
    public static final int LIBRARY_END = 38;
    public static final int TOTAL_SLOTS = 39;
    public static final int GHOST_GRID_SIZE = 9;
    public static final int CRAFT_BUFFER_MAX_STACKS = 54;

    // === Mode ===
    private int mode = 0; // 0=craft, 1=machine

    // === Controller link ===
    @Nullable
    private BlockPos controllerPos;

    // === Crafting state ===
    private boolean crafting = false;

    // === Inventory: 39 slots (reserve + 2 outputs + 36 library) ===
    private final ItemStackHandler inventory = new ItemStackHandler(TOTAL_SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == SLOT_RESERVE) {
                return stack.getItem() instanceof CraftingPaperItem
                        && !CraftingPaperData.hasData(stack);
            }
            if (slot == SLOT_OUTPUT_A || slot == SLOT_OUTPUT_B) {
                return false; // output only
            }
            if (slot >= LIBRARY_START && slot <= LIBRARY_END) {
                return (stack.getItem() instanceof CraftingPaperItem && CraftingPaperData.hasData(stack))
                        || (stack.getItem() instanceof PartCraftingPaperItem && PartCraftingPaperData.hasData(stack));
            }
            return false;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            syncToClient();
        }
    };

    // === Ghost items: 3x3 grid for craft mode ===
    private final ItemStackHandler ghostItems = new ItemStackHandler(GHOST_GRID_SIZE) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            syncToClient();
        }
    };

    // === Craft buffer: items received by bees for active craft ===
    private final List<ItemStack> craftBuffer = new ArrayList<>();

    // === Craft manager: orchestration des crafts automatiques ===
    private final CraftManager craftManager = new CraftManager(this);

    public CrafterBlockEntity(BlockPos pos, BlockState blockState) {
        super(BeemancerBlockEntities.CRAFTER.get(), pos, blockState);
    }

    // === Accessors ===

    public ItemStackHandler getInventory() { return inventory; }
    public ItemStackHandler getGhostItems() { return ghostItems; }
    public List<ItemStack> getCraftBuffer() { return craftBuffer; }
    public CraftManager getCraftManager() { return craftManager; }
    public int getMode() { return mode; }
    public void setMode(int mode) { this.mode = mode; setChanged(); }
    public boolean isCrafting() { return crafting; }
    public void setCrafting(boolean crafting) { this.crafting = crafting; setChanged(); }

    @Nullable
    public BlockPos getControllerPos() { return controllerPos; }

    public void linkToController(BlockPos pos) {
        this.controllerPos = pos;
        setChanged();
    }

    public void unlinkController() {
        this.controllerPos = null;
        setChanged();
    }

    /**
     * Verifie si le crafter est disponible pour lancer un nouveau craft.
     * Conditions: pas de craft en cours, controller lie, buffer vide.
     */
    public boolean isAvailableForCraft() {
        return !crafting && controllerPos != null && craftBuffer.isEmpty();
    }

    // === Ghost slot manipulation ===

    public void setGhostItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= GHOST_GRID_SIZE) return;
        ghostItems.setStackInSlot(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
        setChanged();
    }

    public void clearGhostItems() {
        for (int i = 0; i < GHOST_GRID_SIZE; i++) {
            ghostItems.setStackInSlot(i, ItemStack.EMPTY);
        }
        setChanged();
    }

    // === Output slot helpers ===

    public boolean setOutputA(ItemStack stack) {
        if (!inventory.getStackInSlot(SLOT_OUTPUT_A).isEmpty()) return false;
        inventory.setStackInSlot(SLOT_OUTPUT_A, stack);
        return true;
    }

    public boolean setOutputB(ItemStack stack) {
        if (!inventory.getStackInSlot(SLOT_OUTPUT_B).isEmpty()) return false;
        inventory.setStackInSlot(SLOT_OUTPUT_B, stack);
        return true;
    }

    // === Craft buffer ===

    public void addToCraftBuffer(ItemStack stack) {
        if (stack.isEmpty()) return;
        // Try to merge with existing
        for (ItemStack existing : craftBuffer) {
            if (ItemStack.isSameItemSameComponents(existing, stack)
                    && existing.getCount() < existing.getMaxStackSize()) {
                int space = existing.getMaxStackSize() - existing.getCount();
                int toAdd = Math.min(space, stack.getCount());
                existing.grow(toAdd);
                stack.shrink(toAdd);
                if (stack.isEmpty()) {
                    setChanged();
                    return;
                }
            }
        }
        // Buffer cap: refuse if already at max stacks
        if (craftBuffer.size() >= CRAFT_BUFFER_MAX_STACKS) return;
        craftBuffer.add(stack.copy());
        setChanged();
    }

    public void clearCraftBuffer() {
        craftBuffer.clear();
        setChanged();
    }

    /**
     * Extrait des items du craft buffer correspondant au template donne.
     * Utilise par le CraftManager pour preparer les ingredients.
     *
     * @param template l'item a extraire (ignore le count)
     * @param maxCount le nombre maximum a extraire
     * @return le stack extrait, ou EMPTY si rien trouve
     */
    public ItemStack extractFromCraftBuffer(ItemStack template, int maxCount) {
        if (template.isEmpty() || maxCount <= 0) return ItemStack.EMPTY;

        int extracted = 0;
        for (int i = craftBuffer.size() - 1; i >= 0; i--) {
            ItemStack bufferStack = craftBuffer.get(i);
            if (!ItemStack.isSameItemSameComponents(template, bufferStack)) continue;

            int toTake = Math.min(maxCount - extracted, bufferStack.getCount());
            bufferStack.shrink(toTake);
            extracted += toTake;

            if (bufferStack.isEmpty()) {
                craftBuffer.remove(i);
            }
            if (extracted >= maxCount) break;
        }

        if (extracted == 0) return ItemStack.EMPTY;
        setChanged();
        return template.copyWithCount(extracted);
    }

    /**
     * Extrait le resultat du craft apres completion.
     * Consomme les ingredients du buffer et retourne le resultat.
     * Appele par le CraftManager quand l'etat passe a RETURNING_ITEMS.
     *
     * @param task la tache dont on extrait le resultat
     * @return le resultat du craft, ou EMPTY si les ingredients sont insuffisants
     */
    public ItemStack extractCraftResult(CraftTask task) {
        if (task == null) return ItemStack.EMPTY;

        // Verifier que tous les ingredients sont dans le buffer
        Map<CraftTask.ResourceKey, Integer> required = task.getRequiredResources();
        for (Map.Entry<CraftTask.ResourceKey, Integer> entry : required.entrySet()) {
            int found = 0;
            for (ItemStack bufferStack : craftBuffer) {
                if (entry.getKey().matches(bufferStack)) {
                    found += bufferStack.getCount();
                }
            }
            if (found < entry.getValue()) return ItemStack.EMPTY;
        }

        // Consommer les ingredients
        for (Map.Entry<CraftTask.ResourceKey, Integer> entry : required.entrySet()) {
            int toConsume = entry.getValue();
            for (int i = craftBuffer.size() - 1; i >= 0 && toConsume > 0; i--) {
                ItemStack bufferStack = craftBuffer.get(i);
                if (!entry.getKey().matches(bufferStack)) continue;

                int take = Math.min(toConsume, bufferStack.getCount());
                bufferStack.shrink(take);
                toConsume -= take;

                if (bufferStack.isEmpty()) {
                    craftBuffer.remove(i);
                }
            }
        }

        setChanged();
        return task.getResult();
    }

    // === IDeliveryEndpoint ===

    @Override
    public ItemStack receiveDeliveredItems(ItemStack items) {
        if (items.isEmpty()) return ItemStack.EMPTY;

        // If no craft is active, reject the delivery
        if (!crafting || !craftManager.isBusy()) {
            return items;
        }

        // Validate the item matches a resource needed by an active task
        boolean isNeeded = false;
        for (CraftTask task : craftManager.getAllTasks().values()) {
            if (task.getState() == CraftTask.TaskState.CANCELLED
                    || task.getState() == CraftTask.TaskState.COMPLETED) continue;
            for (Map.Entry<CraftTask.ResourceKey, Integer> entry : task.getRequiredResources().entrySet()) {
                if (entry.getKey().matches(items)) {
                    isNeeded = true;
                    break;
                }
            }
            if (isNeeded) break;
        }

        if (!isNeeded) return items;

        addToCraftBuffer(items);
        return ItemStack.EMPTY;
    }

    // === MenuProvider ===

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.beemancer.crafter");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player player) {
        return new CrafterMenu(containerId, playerInv, this);
    }

    // === NBT ===

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        tag.put("Inventory", inventory.serializeNBT(registries));
        tag.put("GhostItems", ghostItems.serializeNBT(registries));
        tag.putInt("Mode", mode);
        tag.putBoolean("Crafting", crafting);

        if (controllerPos != null) {
            tag.putInt("ControllerX", controllerPos.getX());
            tag.putInt("ControllerY", controllerPos.getY());
            tag.putInt("ControllerZ", controllerPos.getZ());
        }

        // Craft buffer
        if (!craftBuffer.isEmpty()) {
            ListTag bufferTag = new ListTag();
            for (ItemStack stack : craftBuffer) {
                if (!stack.isEmpty()) {
                    bufferTag.add(stack.save(registries));
                }
            }
            tag.put("CraftBuffer", bufferTag);
        }

        // Craft manager
        craftManager.save(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
            // Migration: si l'ancien NBT avait un Size different (ex: 11 ou 30),
            // deserializeNBT resize le handler. On le remet a TOTAL_SLOTS.
            if (inventory.getSlots() != TOTAL_SLOTS) {
                List<ItemStack> saved = new ArrayList<>();
                for (int i = 0; i < inventory.getSlots(); i++) {
                    saved.add(inventory.getStackInSlot(i));
                }
                inventory.setSize(TOTAL_SLOTS);
                for (int i = 0; i < Math.min(saved.size(), TOTAL_SLOTS); i++) {
                    inventory.setStackInSlot(i, saved.get(i));
                }
            }
        }
        if (tag.contains("GhostItems")) {
            ghostItems.deserializeNBT(registries, tag.getCompound("GhostItems"));
        }

        mode = tag.getInt("Mode");
        crafting = tag.getBoolean("Crafting");

        if (tag.contains("ControllerX")) {
            controllerPos = new BlockPos(
                    tag.getInt("ControllerX"),
                    tag.getInt("ControllerY"),
                    tag.getInt("ControllerZ")
            );
        } else {
            controllerPos = null;
        }

        craftBuffer.clear();
        if (tag.contains("CraftBuffer", Tag.TAG_LIST)) {
            ListTag bufferTag = tag.getList("CraftBuffer", Tag.TAG_COMPOUND);
            for (int i = 0; i < bufferTag.size(); i++) {
                ItemStack stack = ItemStack.parseOptional(registries, bufferTag.getCompound(i));
                if (!stack.isEmpty()) {
                    craftBuffer.add(stack);
                }
            }
        }

        // Craft manager
        craftManager.load(tag, registries);
    }

    // === Client sync ===

    public void syncToClient() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }
}
