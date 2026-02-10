/**
 * ============================================================
 * [CrafterMenu.java]
 * Description: Menu du Crafter - slots reserve, output, library paginee, ghost grid
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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class CrafterMenu extends BeemancerMenu {

    // === Slot indices ===
    public static final int SLOT_RESERVE = 0;
    public static final int SLOT_OUTPUT_A = 1;
    public static final int SLOT_OUTPUT_B = 2;
    public static final int LIBRARY_START = 3;
    public static final int LIBRARY_END = 38;
    public static final int GHOST_START = 39;
    public static final int GHOST_END = 47;
    public static final int PLAYER_START = 48;
    public static final int PLAYER_END = 83;

    // === Layout positions ===
    public static final int RESERVE_X = 80;
    public static final int RESERVE_Y = 5;
    public static final int OUTPUT_A_X = 134;
    public static final int OUTPUT_A_Y = 5;
    public static final int OUTPUT_B_X = 152;
    public static final int OUTPUT_B_Y = 5;
    public static final int LIBRARY_X = 8;
    public static final int LIBRARY_Y = 96;
    public static final int GHOST_X = 26;
    public static final int GHOST_Y = 26;
    public static final int PLAYER_INV_Y = 142;
    public static final int HOTBAR_Y = 200;

    // === Library pages ===
    public static final int LIBRARY_COLS = 9;
    public static final int LIBRARY_TOTAL = LIBRARY_END - LIBRARY_START + 1;
    public static final int LIBRARY_ROWS = (LIBRARY_TOTAL + LIBRARY_COLS - 1) / LIBRARY_COLS;
    public static final int LIBRARY_VISIBLE_ROWS = 2;
    public static final int LIBRARY_TOTAL_PAGES =
            (LIBRARY_ROWS + LIBRARY_VISIBLE_ROWS - 1) / LIBRARY_VISIBLE_ROWS;

    // === ContainerData indices ===
    public static final int DATA_MODE = 0;
    public static final int DATA_HAS_BLANK_PAPER = 1;
    public static final int DATA_CRAFTING = 2;
    public static final int DATA_SIZE = 3;

    @Nullable
    private final CrafterBlockEntity blockEntity;
    private final ContainerData data;
    private final GhostSlot[] ghostSlots = new GhostSlot[CrafterBlockEntity.GHOST_GRID_SIZE];
    private final BeemancerSlot[] librarySlots = new BeemancerSlot[LIBRARY_TOTAL];
    private BeemancerSlot outputBSlot;
    private int libraryPage = 0;

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

        ItemStackHandler inv = be != null ? be.getInventory()
                : new ItemStackHandler(CrafterBlockEntity.TOTAL_SLOTS);
        ItemStackHandler ghost = be != null ? be.getGhostItems()
                : new ItemStackHandler(CrafterBlockEntity.GHOST_GRID_SIZE);

        // Slot 0: Reserve (blank Crafting Paper only)
        addSlot(new BeemancerSlot(inv, CrafterBlockEntity.SLOT_RESERVE, RESERVE_X, RESERVE_Y)
                .withFilter(stack -> stack.getItem() instanceof CraftingPaperItem
                        && !CraftingPaperData.hasData(stack)));

        // Slots 1-2: Output A and B (output only)
        addSlot(BeemancerSlot.output(inv, CrafterBlockEntity.SLOT_OUTPUT_A, OUTPUT_A_X, OUTPUT_A_Y));
        outputBSlot = (BeemancerSlot) BeemancerSlot.output(
                inv, CrafterBlockEntity.SLOT_OUTPUT_B, OUTPUT_B_X, OUTPUT_B_Y);
        addSlot(outputBSlot);

        // Slots 3-38: Library (36 slots, 9 per row, 2 rows visible per page)
        for (int i = 0; i < LIBRARY_TOTAL; i++) {
            int slot = CrafterBlockEntity.LIBRARY_START + i;
            int col = i % LIBRARY_COLS;
            int globalRow = i / LIBRARY_COLS;
            int rowInPage = globalRow % LIBRARY_VISIBLE_ROWS;
            int page = globalRow / LIBRARY_VISIBLE_ROWS;
            BeemancerSlot libSlot = new BeemancerSlot(inv, slot,
                    LIBRARY_X + col * 18, LIBRARY_Y + rowInPage * 18);
            libSlot.withFilter(stack ->
                    (stack.getItem() instanceof CraftingPaperItem && CraftingPaperData.hasData(stack))
                    || (stack.getItem() instanceof PartCraftingPaperItem
                            && PartCraftingPaperData.hasData(stack)));
            libSlot.setActive(page == 0);
            librarySlots[i] = libSlot;
            addSlot(libSlot);
        }

        // Slots 39-47: Ghost grid 3x3 (mode craft)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int idx = row * 3 + col;
                GhostSlot gs = new GhostSlot(ghost, idx,
                        GHOST_X + col * 18, GHOST_Y + row * 18);
                ghostSlots[idx] = gs;
                addSlot(gs);
            }
        }

        // Player inventory and hotbar
        addPlayerInventory(playerInv, 8, PLAYER_INV_Y);
        addPlayerHotbar(playerInv, 8, HOTBAR_Y);

        addDataSlots(data);
        updateGhostSlotVisibility();
        updateOutputVisibility();
    }

    // === Accessors ===

    @Nullable
    public CrafterBlockEntity getBlockEntity() { return blockEntity; }
    public ContainerData getData() { return data; }
    public int getMode() { return data.get(DATA_MODE); }
    public boolean hasBlankPaper() { return data.get(DATA_HAS_BLANK_PAPER) != 0; }
    public boolean isCrafting() { return data.get(DATA_CRAFTING) != 0; }
    public GhostSlot[] getGhostSlots() { return ghostSlots; }
    public int getLibraryPage() { return libraryPage; }

    // === Ghost slot visibility ===

    public void updateGhostSlotVisibility() {
        boolean craftMode = getMode() == 0;
        for (GhostSlot gs : ghostSlots) {
            gs.setActive(craftMode);
        }
    }

    // === Output B visibility (hidden in craft mode) ===

    public void updateOutputVisibility() {
        outputBSlot.setActive(getMode() == 1);
    }

    // === Library page navigation ===

    public void setLibraryPage(int page) {
        this.libraryPage = Mth.clamp(page, 0, LIBRARY_TOTAL_PAGES - 1);
        updateLibraryVisibility();
    }

    private void updateLibraryVisibility() {
        for (int i = 0; i < LIBRARY_TOTAL; i++) {
            int globalRow = i / LIBRARY_COLS;
            int page = globalRow / LIBRARY_VISIBLE_ROWS;
            librarySlots[i].setActive(page == libraryPage);
        }
    }

    // === Ghost slot interaction ===

    @Override
    public void clicked(int slotId, int button,
                        net.minecraft.world.inventory.ClickType clickType, Player player) {
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
        if (slotIndex >= GHOST_START && slotIndex <= GHOST_END) {
            return ItemStack.EMPTY;
        }
        return doQuickMove(slotIndex, PLAYER_START,
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
