/**
 * ============================================================
 * [BackpackMenu.java]
 * Description: Menu du backpack — 27 slots container + inventaire joueur
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | ApicaMenu           | Base menu            | Player inventory helpers       |
 * | ApicaMenus          | Registration         | Menu type                      |
 * | BackpackItem        | Type check           | Slot filter + validation       |
 * | DataComponents      | Stockage items       | CONTAINER read/write           |
 * | ApicaAttachments    | Acces attachment     | stillValid check               |
 * | AccessoryPlayerData | Donnees accessoire   | stillValid check               |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - BackpackOpenPacket.java (ouverture)
 * - BackpackScreen.java (affichage)
 * - ClientSetup.java (registration)
 *
 * ============================================================
 */
package com.chapeau.apica.common.menu;

import com.chapeau.apica.common.data.AccessoryPlayerData;
import com.chapeau.apica.common.item.BackpackItem;
import com.chapeau.apica.core.registry.ApicaAttachments;
import com.chapeau.apica.core.registry.ApicaMenus;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu pour le backpack. 27 slots d'inventaire backed par un SimpleContainer.
 * Le contenu est charge depuis DataComponents.CONTAINER et sauvegarde a la fermeture.
 * Le backpack est stocke dans un slot accessoire (AccessoryPlayerData).
 */
public class BackpackMenu extends ApicaMenu {

    private static final int CONTAINER_SLOTS = 27;
    private static final int CONTAINER_ROWS = 3;

    private final SimpleContainer container;
    private final ItemStack backpackStack;
    private final int accessorySlot;

    public int getAccessorySlot() {
        return accessorySlot;
    }

    /** Client constructor (appele par IMenuTypeExtension). */
    public BackpackMenu(int containerId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(containerId, playerInventory, ItemStack.EMPTY, buf.readInt());
    }

    /** Server constructor. */
    public BackpackMenu(int containerId, Inventory playerInventory,
                        ItemStack backpackStack, int accessorySlot) {
        super(ApicaMenus.BACKPACK.get(), containerId);
        this.backpackStack = backpackStack;
        this.accessorySlot = accessorySlot;
        this.container = new SimpleContainer(CONTAINER_SLOTS);

        // Charger les items depuis DataComponents.CONTAINER
        ItemContainerContents contents = backpackStack.get(DataComponents.CONTAINER);
        if (contents != null) {
            NonNullList<ItemStack> temp = NonNullList.withSize(CONTAINER_SLOTS, ItemStack.EMPTY);
            contents.copyInto(temp);
            for (int i = 0; i < CONTAINER_SLOTS; i++) {
                container.setItem(i, temp.get(i));
            }
        }

        // 27 slots container (3 rangees de 9)
        for (int row = 0; row < CONTAINER_ROWS; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new BackpackSlot(container, col + row * 9,
                        8 + col * 18, 18 + row * 18));
            }
        }

        // Inventaire joueur (y=84) + hotbar (y=142)
        addPlayerInventory(playerInventory, 8, 84);
        addPlayerHotbar(playerInventory, 8, 142);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return doQuickMove(index, CONTAINER_SLOTS, 0, CONTAINER_SLOTS,
                stack -> !(stack.getItem() instanceof BackpackItem));
    }

    @Override
    public boolean stillValid(Player player) {
        if (backpackStack.isEmpty()) return true; // Client-side
        AccessoryPlayerData data = player.getData(ApicaAttachments.ACCESSORY_DATA);
        return data.getAccessory(accessorySlot).getItem() instanceof BackpackItem;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        // Sauvegarder le contenu dans le backpack ItemStack
        if (!backpackStack.isEmpty()) {
            List<ItemStack> items = new ArrayList<>(CONTAINER_SLOTS);
            for (int i = 0; i < container.getContainerSize(); i++) {
                items.add(container.getItem(i));
            }
            backpackStack.set(DataComponents.CONTAINER,
                    ItemContainerContents.fromItems(items));
        }
    }

    /**
     * Slot custom qui rejette les BackpackItem (pas de backpack dans backpack).
     */
    private static class BackpackSlot extends Slot {
        public BackpackSlot(SimpleContainer container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return !(stack.getItem() instanceof BackpackItem);
        }
    }
}
