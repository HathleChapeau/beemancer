/**
 * ============================================================
 * [ItemFilterScreen.java]
 * Description: GUI du filtre d'item pipe — ghost slots, mode, priority
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | ItemFilterMenu          | Donnees container    | Mode, priority, ghost items    |
 * | ItemFilterActionPacket  | Packet C2S           | Actions du joueur              |
 * | PlayerInventoryWidget   | Rendu inventaire     | Fond inventaire joueur         |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (enregistrement ecran)
 *
 * ============================================================
 */
package com.chapeau.apica.client.gui.screen;

import com.chapeau.apica.client.gui.widget.PlayerInventoryWidget;
import com.chapeau.apica.common.menu.ItemFilterMenu;
import com.chapeau.apica.core.network.packets.ItemFilterActionPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Ecran du filtre d'item pipe.
 * Affiche 9 ghost slots, un toggle Accept/Deny, des boutons +/- pour la priority,
 * et l'inventaire joueur en bas.
 */
public class ItemFilterScreen extends AbstractContainerScreen<ItemFilterMenu> {

    private static final int GUI_WIDTH = 176;
    private static final int PLAYER_INV_Y = 80;
    private static final int GUI_HEIGHT = PLAYER_INV_Y + 90;

    private static final int GHOST_SLOT_Y = 30;
    private static final int GHOST_SLOT_SIZE = 18;
    private static final int GHOST_SLOT_COUNT = 9;

    private final PlayerInventoryWidget playerInventoryWidget;
    private Button modeButton;

    public ItemFilterScreen(ItemFilterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        this.inventoryLabelY = -999;
        this.titleLabelY = -999;
        this.playerInventoryWidget = new PlayerInventoryWidget(PLAYER_INV_Y, 0);
    }

    @Override
    protected void init() {
        super.init();
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // Priority buttons
        addRenderableWidget(Button.builder(Component.literal("-"), btn -> {
            PacketDistributor.sendToServer(new ItemFilterActionPacket(
                menu.containerId, ItemFilterActionPacket.ACTION_CHANGE_PRIORITY, -1, ItemStack.EMPTY));
        }).bounds(x + 50, y + 6, 20, 16).build());

        addRenderableWidget(Button.builder(Component.literal("+"), btn -> {
            PacketDistributor.sendToServer(new ItemFilterActionPacket(
                menu.containerId, ItemFilterActionPacket.ACTION_CHANGE_PRIORITY, 1, ItemStack.EMPTY));
        }).bounds(x + 106, y + 6, 20, 16).build());

        // Mode toggle button
        modeButton = addRenderableWidget(Button.builder(getModeText(), btn -> {
            PacketDistributor.sendToServer(new ItemFilterActionPacket(
                menu.containerId, ItemFilterActionPacket.ACTION_TOGGLE_MODE, 0, ItemStack.EMPTY));
        }).bounds(x + 50, y + 56, 76, 16).build());
    }

    private Component getModeText() {
        boolean isAccept = menu.getMode() == 0;
        return Component.translatable(isAccept
            ? "gui.apica.item_filter.accept"
            : "gui.apica.item_filter.deny");
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (modeButton != null) {
            modeButton.setMessage(getModeText());
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // Background panel (filter area only, above player inventory)
        g.fill(x, y, x + imageWidth, y + PLAYER_INV_Y, 0xCC1A1A2E);
        // Border
        g.fill(x, y, x + imageWidth, y + 1, 0xFF555555);
        g.fill(x, y + PLAYER_INV_Y - 1, x + imageWidth, y + PLAYER_INV_Y, 0xFF555555);
        g.fill(x, y, x + 1, y + PLAYER_INV_Y, 0xFF555555);
        g.fill(x + imageWidth - 1, y, x + imageWidth, y + PLAYER_INV_Y, 0xFF555555);

        // Title
        g.drawCenteredString(font, Component.translatable("container.apica.item_filter"),
            x + imageWidth / 2, y + -10, 0xDDDDDD);

        // Priority label
        String priorityText = Component.translatable("gui.apica.item_filter.priority").getString()
            + ": " + menu.getPriority();
        g.drawCenteredString(font, priorityText, x + imageWidth / 2, y + 9, 0xFFFFFF);

        // Ghost slots (vanilla slot background)
        int slotsStartX = x + (imageWidth - GHOST_SLOT_COUNT * GHOST_SLOT_SIZE) / 2;
        for (int i = 0; i < GHOST_SLOT_COUNT; i++) {
            int slotX = slotsStartX + i * GHOST_SLOT_SIZE;
            int slotY = y + GHOST_SLOT_Y;

            // Vanilla-style slot background (dark inset border)
            g.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF8B8B8B);
            g.fill(slotX + 1, slotY + 1, slotX + 18, slotY + 18, 0xFFFFFFFF);
            g.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0xFFC6C6C6);

            // Render ghost item
            ItemStack ghostItem = menu.getGhostItem(i);
            if (!ghostItem.isEmpty()) {
                g.renderItem(ghostItem, slotX + 1, slotY + 1);
            }
        }

        // Mode indicator color bar
        boolean isAccept = menu.getMode() == 0;
        int barColor = isAccept ? 0xFF44AA44 : 0xFFAA4444;
        g.fill(x + 50, y + 54, x + 126, y + 55, barColor);

        // Player inventory widget
        playerInventoryWidget.render(g, x, y);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);

        // Ghost slot tooltips
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        int slotsStartX = x + (imageWidth - GHOST_SLOT_COUNT * GHOST_SLOT_SIZE) / 2;
        for (int i = 0; i < GHOST_SLOT_COUNT; i++) {
            int slotX = slotsStartX + i * GHOST_SLOT_SIZE;
            int slotY = y + GHOST_SLOT_Y;
            if (mouseX >= slotX && mouseX < slotX + GHOST_SLOT_SIZE
                && mouseY >= slotY && mouseY < slotY + GHOST_SLOT_SIZE) {
                ItemStack ghostItem = menu.getGhostItem(i);
                if (!ghostItem.isEmpty()) {
                    g.renderTooltip(font, ghostItem, mouseX, mouseY);
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Check ghost slot clicks
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        int slotsStartX = x + (imageWidth - GHOST_SLOT_COUNT * GHOST_SLOT_SIZE) / 2;

        for (int i = 0; i < GHOST_SLOT_COUNT; i++) {
            int slotX = slotsStartX + i * GHOST_SLOT_SIZE;
            int slotY = y + GHOST_SLOT_Y;
            if (mouseX >= slotX && mouseX < slotX + GHOST_SLOT_SIZE
                && mouseY >= slotY && mouseY < slotY + GHOST_SLOT_SIZE) {

                ItemStack carried = menu.getCarried();
                ItemStack ghostItem = menu.getGhostItem(i);

                ItemStack toSet;
                if (!carried.isEmpty()) {
                    // Place a copy of the carried item as ghost
                    toSet = carried.copyWithCount(1);
                } else if (!ghostItem.isEmpty()) {
                    // Clear the ghost slot
                    toSet = ItemStack.EMPTY;
                } else {
                    return super.mouseClicked(mouseX, mouseY, button);
                }

                PacketDistributor.sendToServer(new ItemFilterActionPacket(
                    menu.containerId, ItemFilterActionPacket.ACTION_SET_GHOST_SLOT, i, toSet));
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
}
