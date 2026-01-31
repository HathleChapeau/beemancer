/**
 * ============================================================
 * [NetworkInterfaceMenu.java]
 * Description: Menu partage pour Import/Export Interface avec ghost slots et ContainerData
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | NetworkInterfaceBlockEntity   | Source des donnees   | Filtres, controller, mode      |
 * | ImportInterfaceBlockEntity    | Detection import     | isImport flag                  |
 * | GhostSlot                    | Slots fantomes       | Filter slots 0-8               |
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

import com.chapeau.beemancer.common.blockentity.storage.ExportInterfaceBlockEntity;
import com.chapeau.beemancer.common.blockentity.storage.ImportInterfaceBlockEntity;
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
 * - 0-8:   Ghost filter slots (3x3)
 * - 9-35:  Player inventory (27)
 * - 36-44: Player hotbar (9)
 */
public class NetworkInterfaceMenu extends BeemancerMenu {

    // Slot ranges
    public static final int FILTER_START = 0;
    public static final int FILTER_END = 9;
    public static final int PLAYER_START = 9;
    public static final int PLAYER_END = 45;

    // ContainerData indices
    public static final int DATA_IS_IMPORT = 0;
    public static final int DATA_IS_LINKED = 1;
    public static final int DATA_FILTER_MODE = 2;
    public static final int DATA_HAS_ADJACENT_GUI = 3;
    public static final int DATA_COUNT_VALUE = 4;
    public static final int DATA_SIZE = 5;

    // Ghost slot positions
    private static final int GHOST_X = 62;
    private static final int GHOST_Y = 30;

    // Player inventory positions
    private static final int PLAYER_INV_X = 8;
    private static final int PLAYER_INV_Y = 100;
    private static final int HOTBAR_Y = 158;

    @Nullable
    private final NetworkInterfaceBlockEntity blockEntity;
    private final ContainerData data;
    private final ContainerLevelAccess access;

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

        // Ghost filter slots (0-8)
        ItemStackHandler filterHandler = be != null ? be.getFilterSlots() : new ItemStackHandler(9);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int index = col + row * 3;
                addSlot(new GhostSlot(filterHandler, index,
                    GHOST_X + col * 18, GHOST_Y + row * 18));
            }
        }

        // Player inventory + hotbar
        addPlayerInventory(playerInv, PLAYER_INV_X, PLAYER_INV_Y);
        addPlayerHotbar(playerInv, PLAYER_INV_X, HOTBAR_Y);

        addDataSlots(data);
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
                    case DATA_FILTER_MODE -> be.getFilterMode().ordinal();
                    case DATA_HAS_ADJACENT_GUI -> be.hasAdjacentGui() ? 1 : 0;
                    case DATA_COUNT_VALUE -> {
                        if (be instanceof ImportInterfaceBlockEntity imp) {
                            yield imp.getMaxCount();
                        } else if (be instanceof ExportInterfaceBlockEntity exp) {
                            yield exp.getMinKeep();
                        }
                        yield 0;
                    }
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) { }

            @Override
            public int getCount() { return DATA_SIZE; }
        };
    }

    // === Ghost Slot Click Handling ===

    @Override
    public void clicked(int slotId, int button, net.minecraft.world.inventory.ClickType clickType,
                         Player player) {
        if (slotId >= FILTER_START && slotId < FILTER_END && blockEntity != null
                && blockEntity.getFilterMode() == NetworkInterfaceBlockEntity.FilterMode.ITEM) {
            ItemStack carried = getCarried();
            if (!carried.isEmpty()) {
                blockEntity.setFilter(slotId, carried);
            } else {
                blockEntity.clearFilter(slotId);
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    // === Quick Move ===

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        // Ghost slots: nothing to move
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
    public int getFilterModeOrdinal() { return data.get(DATA_FILTER_MODE); }
    public boolean hasAdjacentGui() { return data.get(DATA_HAS_ADJACENT_GUI) != 0; }
    public int getCountValue() { return data.get(DATA_COUNT_VALUE); }

    @Nullable
    public NetworkInterfaceBlockEntity getBlockEntity() { return blockEntity; }
}
