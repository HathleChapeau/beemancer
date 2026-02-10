/**
 * ============================================================
 * [CrafterMenu.java]
 * Description: Menu du Crafter - slots reserve, output, library, ghost grid
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | BeemancerMenu           | Base menu            | Player inv, quick move         |
 * | BeemancerSlot           | Slot custom          | Filter, output only            |
 * | GhostSlot               | Slot fantome         | Ghost items 3x3               |
 * | CrafterBlockEntity      | BE crafter           | Inventaire, ghost items        |
 * | BeemancerMenus          | Type du menu         | Constructeur                   |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - CrafterBlockEntity.java (createMenu)
 * - CrafterScreen.java (GUI)
 * - ClientSetup.java (register)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.menu.storage;

import com.chapeau.beemancer.client.gui.widget.BeemancerSlot;
import com.chapeau.beemancer.common.blockentity.storage.CrafterBlockEntity;
import com.chapeau.beemancer.common.data.CraftingPaperData;
import com.chapeau.beemancer.common.data.PartCraftingPaperData;
import com.chapeau.beemancer.common.item.CraftingPaperItem;
import com.chapeau.beemancer.common.item.PartCraftingPaperItem;
import com.chapeau.beemancer.common.menu.BeemancerMenu;
import com.chapeau.beemancer.common.menu.slot.GhostSlot;
import com.chapeau.beemancer.core.registry.BeemancerMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class CrafterMenu extends BeemancerMenu {

    // === Slot layout ===
    public static final int SLOT_RESERVE = 0;
    public static final int SLOT_OUTPUT_A = 1;
    public static final int SLOT_OUTPUT_B = 2;
    public static final int LIBRARY_START = 3;
    public static final int LIBRARY_END = 10;
    public static final int GHOST_START = 11;
    public static final int GHOST_END = 19;
    public static final int PLAYER_START = 20;
    public static final int PLAYER_END = 55;

    // === ContainerData indices ===
    public static final int DATA_MODE = 0;
    public static final int DATA_HAS_BLANK_PAPER = 1;
    public static final int DATA_CRAFTING = 2;
    public static final int DATA_SIZE = 3;

    @Nullable
    private final CrafterBlockEntity blockEntity;
    private final ContainerData data;
    private final GhostSlot[] ghostSlots = new GhostSlot[CrafterBlockEntity.GHOST_GRID_SIZE];

    // === Client constructor (from network) ===
    public CrafterMenu(int containerId, Inventory playerInv, FriendlyByteBuf buf) {
        this(containerId, playerInv, getBlockEntity(playerInv, buf.readBlockPos()),
                new SimpleContainerData(DATA_SIZE));
    }

    // === Server constructor ===
    public CrafterMenu(int containerId, Inventory playerInv, CrafterBlockEntity be) {
        this(containerId, playerInv, be, createServerData(be));
    }

    // === Main constructor ===
    private CrafterMenu(int containerId, Inventory playerInv,
                        @Nullable CrafterBlockEntity be, ContainerData data) {
        super(BeemancerMenus.CRAFTER.get(), containerId);
        this.blockEntity = be;
        this.data = data;

        ItemStackHandler inv = be != null ? be.getInventory() : new ItemStackHandler(CrafterBlockEntity.TOTAL_SLOTS);
        ItemStackHandler ghost = be != null ? be.getGhostItems() : new ItemStackHandler(CrafterBlockEntity.GHOST_GRID_SIZE);

        // Slot 0: Reserve (blank Crafting Paper only)
        addSlot(new BeemancerSlot(inv, CrafterBlockEntity.SLOT_RESERVE, 14, 108)
                .withFilter(stack -> stack.getItem() instanceof CraftingPaperItem
                        && !CraftingPaperData.hasData(stack)));

        // Slots 1-2: Output A and B (output only)
        addSlot(BeemancerSlot.output(inv, CrafterBlockEntity.SLOT_OUTPUT_A, 68, 108));
        addSlot(BeemancerSlot.output(inv, CrafterBlockEntity.SLOT_OUTPUT_B, 90, 108));

        // Slots 3-10: Library (8 slots, accepts inscribed papers)
        for (int i = 0; i < 8; i++) {
            int slot = CrafterBlockEntity.LIBRARY_START + i;
            int x = 8 + (i % 4) * 18;
            int y = 140 + (i / 4) * 18;
            addSlot(new BeemancerSlot(inv, slot, x, y)
                    .withFilter(stack ->
                            (stack.getItem() instanceof CraftingPaperItem && CraftingPaperData.hasData(stack))
                            || (stack.getItem() instanceof PartCraftingPaperItem && PartCraftingPaperData.hasData(stack))));
        }

        // Slots 11-19: Ghost grid 3x3 (mode craft)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int idx = row * 3 + col;
                int x = 26 + col * 18;
                int y = 18 + row * 18;
                GhostSlot gs = new GhostSlot(ghost, idx, x, y);
                ghostSlots[idx] = gs;
                addSlot(gs);
            }
        }

        // Player inventory and hotbar
        addPlayerInventory(playerInv, 8, 184);
        addPlayerHotbar(playerInv, 8, 242);

        addDataSlots(data);
        updateGhostSlotVisibility();
    }

    // === Accessors ===

    @Nullable
    public CrafterBlockEntity getBlockEntity() { return blockEntity; }

    public ContainerData getData() { return data; }

    public int getMode() { return data.get(DATA_MODE); }

    public boolean hasBlankPaper() { return data.get(DATA_HAS_BLANK_PAPER) != 0; }

    public boolean isCrafting() { return data.get(DATA_CRAFTING) != 0; }

    public GhostSlot[] getGhostSlots() { return ghostSlots; }

    // === Ghost slot visibility ===

    public void updateGhostSlotVisibility() {
        boolean craftMode = getMode() == 0;
        for (GhostSlot gs : ghostSlots) {
            gs.setActive(craftMode);
        }
    }

    // === Ghost slot interaction ===

    @Override
    public void clicked(int slotId, int button, net.minecraft.world.inventory.ClickType clickType, Player player) {
        if (slotId >= GHOST_START && slotId <= GHOST_END && blockEntity != null) {
            int ghostIdx = slotId - GHOST_START;
            ItemStack carried = getCarried();
            if (!carried.isEmpty()) {
                blockEntity.setGhostItem(ghostIdx, carried);
            } else {
                blockEntity.setGhostItem(ghostIdx, ItemStack.EMPTY);
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    // === Quick move (shift-click) ===

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        // Ghost slots: no quick move
        if (slotIndex >= GHOST_START && slotIndex <= GHOST_END) {
            return ItemStack.EMPTY;
        }
        return doQuickMove(slotIndex, LIBRARY_END,
                SLOT_RESERVE, LIBRARY_END + 1, null);
    }

    // === Still valid ===

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity == null) return false;
        return player.distanceToSqr(
                blockEntity.getBlockPos().getX() + 0.5,
                blockEntity.getBlockPos().getY() + 0.5,
                blockEntity.getBlockPos().getZ() + 0.5) <= 64.0;
    }

    // === Helpers ===

    @Nullable
    private static CrafterBlockEntity getBlockEntity(Inventory playerInv, BlockPos pos) {
        BlockEntity be = playerInv.player.level().getBlockEntity(pos);
        return be instanceof CrafterBlockEntity crafter ? crafter : null;
    }

    private static ContainerData createServerData(CrafterBlockEntity be) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case DATA_MODE -> be.getMode();
                    case DATA_HAS_BLANK_PAPER -> be.getInventory()
                            .getStackInSlot(CrafterBlockEntity.SLOT_RESERVE).isEmpty() ? 0 : 1;
                    case DATA_CRAFTING -> be.isCrafting() ? 1 : 0;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) { }

            @Override
            public int getCount() { return DATA_SIZE; }
        };
    }
}
