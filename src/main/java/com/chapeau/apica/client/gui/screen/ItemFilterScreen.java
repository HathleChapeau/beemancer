/**
 * ============================================================
 * [ItemFilterScreen.java]
 * Description: GUI du filtre d'item pipe — ghost slots ou texte, mode, priority
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
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Ecran du filtre d'item pipe.
 * Supporte deux modes d'input : ghost slots (SLOT) ou champ texte (TEXT).
 * Affiche un toggle Accept Only/Deny Only, des boutons +/- pour la priority,
 * et l'inventaire joueur en bas.
 */
public class ItemFilterScreen extends AbstractContainerScreen<ItemFilterMenu> {

    private static final int GUI_WIDTH = 176;
    private static final int PLAYER_INV_Y = 100;
    private static final int GUI_HEIGHT = PLAYER_INV_Y + 90;

    private static final int GHOST_SLOT_Y = 30;
    private static final int GHOST_SLOT_SIZE = 18;
    private static final int GHOST_SLOT_COUNT = 9;

    private final PlayerInventoryWidget playerInventoryWidget;
    private Button modeButton;
    private Button inputModeButton;
    private EditBox textFilterBox;

    /** Tracks the last known inputMode to detect server-side changes. */
    private int lastInputMode = -1;

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

        // Priority: label is drawn in renderBg, buttons positioned after it
        int priorityLabelW = font.width(
            Component.translatable("gui.apica.item_filter.priority").getString() + ":");
        int prioStartX = x + 8;
        int btnMinusX = prioStartX + priorityLabelW + 4;

        addRenderableWidget(Button.builder(Component.literal("-"), btn -> {
            PacketDistributor.sendToServer(new ItemFilterActionPacket(
                menu.containerId, ItemFilterActionPacket.ACTION_CHANGE_PRIORITY, -1, ItemStack.EMPTY));
        }).bounds(btnMinusX, y + 6, 16, 16).build());

        addRenderableWidget(Button.builder(Component.literal("+"), btn -> {
            PacketDistributor.sendToServer(new ItemFilterActionPacket(
                menu.containerId, ItemFilterActionPacket.ACTION_CHANGE_PRIORITY, 1, ItemStack.EMPTY));
        }).bounds(btnMinusX + 40, y + 6, 16, 16).build());

        // Input mode toggle (Slots / Text)
        inputModeButton = addRenderableWidget(Button.builder(getInputModeText(), btn -> {
            PacketDistributor.sendToServer(new ItemFilterActionPacket(
                menu.containerId, ItemFilterActionPacket.ACTION_TOGGLE_INPUT_MODE, 0, ItemStack.EMPTY));
        }).bounds(x + 126, y + 6, 42, 16).build());

        // Text filter EditBox (same area as ghost slots)
        int slotsStartX = x + (imageWidth - GHOST_SLOT_COUNT * GHOST_SLOT_SIZE) / 2;
        textFilterBox = new EditBox(font, slotsStartX, y + GHOST_SLOT_Y + 1,
            GHOST_SLOT_COUNT * GHOST_SLOT_SIZE, 16, Component.empty());
        textFilterBox.setMaxLength(64);
        textFilterBox.setBordered(true);
        textFilterBox.setTextColor(0xFFFFFF);
        textFilterBox.setValue(menu.getTextFilter());
        textFilterBox.setResponder(this::onTextFilterChanged);
        addRenderableWidget(textFilterBox);

        // Mode toggle button (Accept Only / Deny Only)
        modeButton = addRenderableWidget(Button.builder(getModeText(), btn -> {
            PacketDistributor.sendToServer(new ItemFilterActionPacket(
                menu.containerId, ItemFilterActionPacket.ACTION_TOGGLE_MODE, 0, ItemStack.EMPTY));
        }).bounds(x + 50, y + 56, 76, 16).build());

        // Set initial visibility
        updateInputModeVisibility();
    }

    private void onTextFilterChanged(String text) {
        PacketDistributor.sendToServer(new ItemFilterActionPacket(
            menu.containerId, ItemFilterActionPacket.ACTION_SET_TEXT_FILTER, 0,
            ItemStack.EMPTY, text));
    }

    private Component getModeText() {
        boolean isAccept = menu.getMode() == 0;
        return Component.translatable(isAccept
            ? "gui.apica.item_filter.accept"
            : "gui.apica.item_filter.deny");
    }

    private Component getInputModeText() {
        boolean isSlot = menu.getInputMode() == 0;
        return Component.translatable(isSlot
            ? "gui.apica.item_filter.mode_slots"
            : "gui.apica.item_filter.mode_text");
    }

    private boolean isTextMode() {
        return menu.getInputMode() == 1;
    }

    private void updateInputModeVisibility() {
        boolean textMode = isTextMode();
        if (textFilterBox != null) {
            textFilterBox.setVisible(textMode);
            textFilterBox.setFocused(textMode);
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (modeButton != null) {
            modeButton.setMessage(getModeText());
        }
        if (inputModeButton != null) {
            inputModeButton.setMessage(getInputModeText());
        }

        // Detect inputMode change from server and update visibility
        int currentInputMode = menu.getInputMode();
        if (currentInputMode != lastInputMode) {
            lastInputMode = currentInputMode;
            updateInputModeVisibility();
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

        // Priority label + value (layout: "Priority:" [-] value [+])
        String prioLabel = Component.translatable("gui.apica.item_filter.priority").getString() + ":";
        int prioLabelW = font.width(prioLabel);
        int prioStartX = x + 8;
        g.drawString(font, prioLabel, prioStartX, y + 10, 0xFFFFFF, false);

        // Priority value centered between the two buttons
        int btnMinusX = prioStartX + prioLabelW + 4;
        String prioValue = String.valueOf(menu.getPriority());
        int valueW = font.width(prioValue);
        g.drawString(font, prioValue, btnMinusX + 16 + (24 - valueW) / 2, y + 10, 0xFFFFFF, false);

        // Ghost slots — only in SLOT mode
        if (!isTextMode()) {
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

        // Ghost slot tooltips (only in SLOT mode)
        if (!isTextMode()) {
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
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Ghost slot clicks only in SLOT mode
        if (!isTextMode()) {
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
                        toSet = carried.copyWithCount(1);
                    } else if (!ghostItem.isEmpty()) {
                        toSet = ItemStack.EMPTY;
                    } else {
                        return super.mouseClicked(mouseX, mouseY, button);
                    }

                    PacketDistributor.sendToServer(new ItemFilterActionPacket(
                        menu.containerId, ItemFilterActionPacket.ACTION_SET_GHOST_SLOT, i, toSet));
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
}
