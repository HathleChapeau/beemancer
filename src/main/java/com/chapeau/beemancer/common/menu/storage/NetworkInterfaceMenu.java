/**
 * ============================================================
 * [NetworkInterfaceMenu.java]
 * Description: Menu partage pour Import/Export Interface avec ghost slots dynamiques
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | NetworkInterfaceBlockEntity   | Source des donnees   | Filtres, controller            |
 * | InterfaceFilter               | Filtre individuel    | Ghost slots per-filter         |
 * | GhostSlot                    | Slots fantomes       | Filter slots 0-14              |
 * | BeemancerMenus               | Type du menu         | Constructeur                   |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - NetworkInterfaceBlockEntity.java (creation du menu via MenuProvider)
 * - NetworkInterfaceScreen.java (affichage)
 * - BeemancerMenus.java (enregistrement)
 *
 * ============================================================
 */
package com.chapeau.beemancer.common.menu.storage;

import com.chapeau.beemancer.common.blockentity.storage.InterfaceFilter;
import com.chapeau.beemancer.common.blockentity.storage.NetworkInterfaceBlockEntity;
import com.chapeau.beemancer.common.menu.BeemancerMenu;
import com.chapeau.beemancer.common.menu.slot.GhostSlot;
import com.chapeau.beemancer.core.registry.BeemancerMenus;
import net.minecraft.core.BlockPos;
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
import org.jetbrains.annotations.Nullable;

/**
 * Menu partage pour Import et Export Interface.
 *
 * Slots Layout:
 * - 0-4:   Ghost filter slots pour filtre 0 (5 slots)
 * - 5-9:   Ghost filter slots pour filtre 1 (5 slots)
 * - 10-14: Ghost filter slots pour filtre 2 (5 slots)
 * - 15-41: Player inventory (27)
 * - 42-50: Player hotbar (9)
 *
 * Les ghost slots de filtres inactifs ont isActive=false.
 */
public class NetworkInterfaceMenu extends BeemancerMenu {

    public static final int SLOTS_PER_FILTER = InterfaceFilter.SLOTS_PER_FILTER;
    public static final int MAX_FILTERS = InterfaceFilter.MAX_FILTERS;
    public static final int TOTAL_GHOST_SLOTS = SLOTS_PER_FILTER * MAX_FILTERS;

    public static final int FILTER_START = 0;
    public static final int FILTER_END = TOTAL_GHOST_SLOTS;
    public static final int PLAYER_START = TOTAL_GHOST_SLOTS;
    public static final int PLAYER_END = PLAYER_START + 36;

    // ContainerData indices
    public static final int DATA_IS_IMPORT = 0;
    public static final int DATA_IS_LINKED = 1;
    public static final int DATA_FILTER_COUNT = 2;
    public static final int DATA_SIZE = 3;

    // Ghost slot positions (matching screen layout)
    private static final int GHOST_SLOTS_X = 23;
    private static final int GHOST_SLOTS_BASE_Y = 31;
    private static final int FILTER_LINE_H = 20;

    // Player inventory positions
    private static final int PLAYER_INV_X = 8;
    private static final int PLAYER_INV_Y = 110;
    private static final int HOTBAR_Y = 168;

    @Nullable
    private final NetworkInterfaceBlockEntity blockEntity;
    private final ContainerData data;
    private final ContainerLevelAccess access;
    private final GhostSlot[] ghostSlots = new GhostSlot[TOTAL_GHOST_SLOTS];

    // Client constructor (from network)
    public NetworkInterfaceMenu(int containerId, Inventory playerInv, FriendlyByteBuf buf) {
        this(containerId, playerInv, getBlockEntity(playerInv, buf.readBlockPos()),
             new SimpleContainerData(DATA_SIZE));
    }

    // Server constructor
    public NetworkInterfaceMenu(int containerId, Inventory playerInv,
                                 NetworkInterfaceBlockEntity be) {
        this(containerId, playerInv, be, createServerData(be));
    }

    // Full constructor
    private NetworkInterfaceMenu(int containerId, Inventory playerInv,
                                  @Nullable NetworkInterfaceBlockEntity be,
                                  ContainerData data) {
        super(BeemancerMenus.NETWORK_INTERFACE.get(), containerId);
        this.blockEntity = be;
        this.data = data;

        if (be != null && be.getLevel() != null) {
            this.access = ContainerLevelAccess.create(be.getLevel(), be.getBlockPos());
        } else {
            this.access = ContainerLevelAccess.NULL;
        }

        // Ghost filter slots (0-14): 3 filtres x 5 slots
        // Positions fixes par ligne — seul isActive() controle la visibilite
        for (int filterIdx = 0; filterIdx < MAX_FILTERS; filterIdx++) {
            ItemStackHandler handler;
            if (be != null && filterIdx < be.getFilterCount()) {
                handler = be.getFilter(filterIdx).getItems();
            } else {
                handler = new ItemStackHandler(SLOTS_PER_FILTER);
            }

            for (int slot = 0; slot < SLOTS_PER_FILTER; slot++) {
                int globalIdx = filterIdx * SLOTS_PER_FILTER + slot;
                // Position fixe: SLOTS_X + slot * 18 + 1, FILTER_ZONE_Y + filterIdx * 20 + 4
                int slotX = GHOST_SLOTS_X + slot * 18 + 1;
                int slotY = GHOST_SLOTS_BASE_Y + filterIdx * FILTER_LINE_H + 1;
                GhostSlot gs = new GhostSlot(handler, slot, slotX, slotY);
                ghostSlots[globalIdx] = gs;
                addSlot(gs);
            }
        }

        // Player inventory + hotbar
        addPlayerInventory(playerInv, PLAYER_INV_X, PLAYER_INV_Y);
        addPlayerHotbar(playerInv, PLAYER_INV_X, HOTBAR_Y);

        addDataSlots(data);

        updateFilterSlots();
    }

    @Nullable
    private static NetworkInterfaceBlockEntity getBlockEntity(Inventory playerInv, BlockPos pos) {
        BlockEntity be = playerInv.player.level().getBlockEntity(pos);
        if (be instanceof NetworkInterfaceBlockEntity nibe) {
            return nibe;
        }
        return null;
    }

    private static ContainerData createServerData(NetworkInterfaceBlockEntity be) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case DATA_IS_IMPORT -> be.isImport() ? 1 : 0;
                    case DATA_IS_LINKED -> be.isLinked() ? 1 : 0;
                    case DATA_FILTER_COUNT -> be.getFilterCount();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) { }

            @Override
            public int getCount() { return DATA_SIZE; }
        };
    }

    /**
     * Met a jour l'etat actif/inactif des ghost slots en fonction du nombre de filtres
     * et de leur mode. Appele lors de l'ajout/suppression de filtres.
     */
    public void updateFilterSlots() {
        if (blockEntity == null) return;

        int filterCount = blockEntity.getFilterCount();

        for (int filterIdx = 0; filterIdx < MAX_FILTERS; filterIdx++) {
            boolean filterExists = filterIdx < filterCount;
            InterfaceFilter filter = filterExists ? blockEntity.getFilter(filterIdx) : null;
            boolean isItemMode = filter != null && filter.getMode() == InterfaceFilter.FilterMode.ITEM;

            // Relier le handler au bon filtre si il existe
            if (filter != null) {
                for (int slot = 0; slot < SLOTS_PER_FILTER; slot++) {
                    int globalIdx = filterIdx * SLOTS_PER_FILTER + slot;
                    ghostSlots[globalIdx].setActive(isItemMode);
                }
            } else {
                for (int slot = 0; slot < SLOTS_PER_FILTER; slot++) {
                    int globalIdx = filterIdx * SLOTS_PER_FILTER + slot;
                    ghostSlots[globalIdx].setActive(false);
                }
            }
        }
    }

    // === Ghost Slot Click Handling ===

    @Override
    public void clicked(int slotId, int button, net.minecraft.world.inventory.ClickType clickType,
                         Player player) {
        if (slotId >= FILTER_START && slotId < FILTER_END && blockEntity != null) {
            int filterIdx = slotId / SLOTS_PER_FILTER;
            int slotInFilter = slotId % SLOTS_PER_FILTER;

            InterfaceFilter filter = blockEntity.getFilter(filterIdx);
            if (filter != null && filter.getMode() == InterfaceFilter.FilterMode.ITEM) {
                ItemStack carried = getCarried();
                if (!carried.isEmpty()) {
                    blockEntity.setFilterItem(filterIdx, slotInFilter, carried);
                } else {
                    blockEntity.clearFilterItem(filterIdx, slotInFilter);
                }
                return;
            }
        }
        super.clicked(slotId, button, clickType, player);
    }

    // === Quick Move ===

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (slotIndex >= FILTER_START && slotIndex < FILTER_END) {
            return ItemStack.EMPTY;
        }

        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(slotIndex);

        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            // Player inv <-> hotbar
            if (slotIndex >= PLAYER_START && slotIndex < PLAYER_START + 27) {
                if (!moveItemStackTo(stack, PLAYER_START + 27, PLAYER_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (slotIndex >= PLAYER_START + 27 && slotIndex < PLAYER_END) {
                if (!moveItemStackTo(stack, PLAYER_START, PLAYER_START + 27, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity != null) {
            return stillValid(access, player, blockEntity.getBlockState().getBlock());
        }
        return true;
    }

    // === Data Accessors ===

    public boolean isImport() { return data.get(DATA_IS_IMPORT) != 0; }
    public boolean isLinked() { return data.get(DATA_IS_LINKED) != 0; }
    public int getFilterCount() { return data.get(DATA_FILTER_COUNT); }

    @Nullable
    public NetworkInterfaceBlockEntity getBlockEntity() { return blockEntity; }

    public GhostSlot getGhostSlot(int globalIndex) {
        if (globalIndex < 0 || globalIndex >= TOTAL_GHOST_SLOTS) return null;
        return ghostSlots[globalIndex];
    }
}
