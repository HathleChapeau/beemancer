/**
 * ============================================================
 * [NetworkInterfaceScreen.java]
 * Description: GUI pour Import/Export Interface avec ghost slots, text inputs, mode toggle
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | NetworkInterfaceMenu          | Donnees container    | Slots, data accessors          |
 * | InterfaceActionPacket         | Envoi actions C2S    | Mode switch, text, count       |
 * | GuiRenderHelper               | Rendu programmatique | Background, slots, boutons     |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (enregistrement ecran)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.screen.storage;

import com.chapeau.beemancer.client.gui.GuiRenderHelper;
import com.chapeau.beemancer.common.blockentity.storage.NetworkInterfaceBlockEntity;
import com.chapeau.beemancer.common.menu.storage.NetworkInterfaceMenu;
import com.chapeau.beemancer.core.network.packets.InterfaceActionPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Screen programmatique pour Import/Export Interface.
 *
 * Layout (176w x 180h):
 * - Titre
 * - Mode toggle [Item] [Text] + [Select Slots] si adjacent GUI
 * - Mode ITEM: 3x3 ghost slots / Mode TEXT: 9 EditBox
 * - Count: Max (import) ou Keep (export) avec +/- boutons
 * - Status: Linked / Not Linked
 * - Inventaire joueur
 */
public class NetworkInterfaceScreen extends AbstractContainerScreen<NetworkInterfaceMenu> {

    // Layout constants
    private static final int BG_WIDTH = 176;
    private static final int BG_HEIGHT = 182;

    // Mode buttons
    private static final int MODE_BTN_Y = 17;
    private static final int MODE_BTN_W = 30;
    private static final int MODE_BTN_H = 12;
    private static final int MODE_ITEM_X = 8;
    private static final int MODE_TEXT_X = 42;

    // Ghost slots area
    private static final int GHOST_X = 62;
    private static final int GHOST_Y = 30;

    // Text filter area (9 EditBoxes in a column on the left)
    private static final int TEXT_X = 8;
    private static final int TEXT_Y = 30;
    private static final int TEXT_W = 160;
    private static final int TEXT_H = 12;
    private static final int TEXT_SPACING = 14;

    // Count area
    private static final int COUNT_Y = 86;
    private static final int COUNT_BTN_W = 20;
    private static final int COUNT_BTN_H = 12;

    // Player inventory
    private static final int PLAYER_INV_Y = 100;
    private static final int HOTBAR_Y = 158;

    private final EditBox[] textBoxes = new EditBox[9];
    private boolean lastModeWasItem = true;

    public NetworkInterfaceScreen(NetworkInterfaceMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = BG_WIDTH;
        this.imageHeight = BG_HEIGHT;
        this.inventoryLabelY = PLAYER_INV_Y - 10;
    }

    @Override
    protected void init() {
        super.init();

        // Create 9 EditBoxes for text filter mode
        for (int i = 0; i < 9; i++) {
            int bx = leftPos + TEXT_X;
            int by = topPos + TEXT_Y + i * TEXT_SPACING;
            EditBox box = new EditBox(font, bx, by, TEXT_W, TEXT_H, Component.empty());
            box.setMaxLength(64);
            box.setBordered(true);
            box.setVisible(false);

            // Load existing text from BE if available
            NetworkInterfaceBlockEntity be = menu.getBlockEntity();
            if (be != null) {
                String existing = be.getTextFilter(i);
                if (existing != null && !existing.isEmpty()) {
                    box.setValue(existing);
                }
            }

            final int idx = i;
            box.setResponder(text -> onTextFilterChanged(idx, text));

            textBoxes[i] = box;
            addRenderableWidget(box);
        }

        updateTextBoxVisibility();
    }

    private void updateTextBoxVisibility() {
        boolean isTextMode = menu.getFilterModeOrdinal() == 1;
        for (EditBox box : textBoxes) {
            if (box != null) box.setVisible(isTextMode);
        }
        lastModeWasItem = !isTextMode;
    }

    private void onTextFilterChanged(int slot, String text) {
        PacketDistributor.sendToServer(new InterfaceActionPacket(
            menu.containerId, InterfaceActionPacket.ACTION_SET_TEXT_FILTER, slot, text));
    }

    // === Rendering ===

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        boolean isTextMode = menu.getFilterModeOrdinal() == 1;
        boolean isImport = menu.isImport();

        // Background
        String titleKey = isImport ? "container.beemancer.import_interface"
                                    : "container.beemancer.export_interface";
        GuiRenderHelper.renderContainerBackground(g, font, x, y, BG_WIDTH, BG_HEIGHT,
            titleKey, PLAYER_INV_Y - 2);

        // Mode toggle buttons
        boolean itemHovered = isMouseOver(mouseX, mouseY, x + MODE_ITEM_X, y + MODE_BTN_Y,
            MODE_BTN_W, MODE_BTN_H);
        boolean textHovered = isMouseOver(mouseX, mouseY, x + MODE_TEXT_X, y + MODE_BTN_Y,
            MODE_BTN_W, MODE_BTN_H);

        renderModeButton(g, x + MODE_ITEM_X, y + MODE_BTN_Y, "Item", !isTextMode, itemHovered);
        renderModeButton(g, x + MODE_TEXT_X, y + MODE_BTN_Y, "Text", isTextMode, textHovered);

        // Ghost slots (mode ITEM only)
        if (!isTextMode) {
            GuiRenderHelper.renderSlotGrid(g, x + GHOST_X - 1, y + GHOST_Y - 1, 3, 3);
        }

        // Count controls
        String countLabel;
        int countValue = menu.getCountValue();
        if (isImport) {
            countLabel = "Max: " + countValue;
        } else {
            countLabel = "Keep: " + countValue;
        }
        g.drawString(font, countLabel, x + 8, y + COUNT_Y + 2, 0x404040, false);

        // +/- buttons
        int btnBaseX = x + 80;
        boolean minus16Hov = isMouseOver(mouseX, mouseY, btnBaseX, y + COUNT_Y, COUNT_BTN_W, COUNT_BTN_H);
        boolean minus1Hov = isMouseOver(mouseX, mouseY, btnBaseX + 22, y + COUNT_Y, COUNT_BTN_W, COUNT_BTN_H);
        boolean plus1Hov = isMouseOver(mouseX, mouseY, btnBaseX + 44, y + COUNT_Y, COUNT_BTN_W, COUNT_BTN_H);
        boolean plus16Hov = isMouseOver(mouseX, mouseY, btnBaseX + 66, y + COUNT_Y, COUNT_BTN_W, COUNT_BTN_H);

        GuiRenderHelper.renderButton(g, font, btnBaseX, y + COUNT_Y, COUNT_BTN_W, COUNT_BTN_H,
            "-16", minus16Hov);
        GuiRenderHelper.renderButton(g, font, btnBaseX + 22, y + COUNT_Y, COUNT_BTN_W, COUNT_BTN_H,
            "-1", minus1Hov);
        GuiRenderHelper.renderButton(g, font, btnBaseX + 44, y + COUNT_Y, COUNT_BTN_W, COUNT_BTN_H,
            "+1", plus1Hov);
        GuiRenderHelper.renderButton(g, font, btnBaseX + 66, y + COUNT_Y, COUNT_BTN_W, COUNT_BTN_H,
            "+16", plus16Hov);

        // Status
        if (menu.isLinked()) {
            NetworkInterfaceBlockEntity be = menu.getBlockEntity();
            if (be != null && be.getControllerPos() != null) {
                String status = String.format("Linked (%d, %d, %d)",
                    be.getControllerPos().getX(),
                    be.getControllerPos().getY(),
                    be.getControllerPos().getZ());
                g.drawString(font, status, x + 8, y + COUNT_Y - 10, 0x206020, false);
            }
        } else {
            g.drawString(font, "Not Linked", x + 8, y + COUNT_Y - 10, 0x802020, false);
        }

        // Player inventory
        GuiRenderHelper.renderPlayerInventory(g, x, y, PLAYER_INV_Y - 1, HOTBAR_Y - 1);
    }

    private void renderModeButton(GuiGraphics g, int x, int y, String label,
                                    boolean active, boolean hovered) {
        int bg;
        if (active) {
            bg = 0xFFDBDBDB;
        } else if (hovered) {
            bg = 0xFFBBBBBB;
        } else {
            bg = 0xFFAAAAAA;
        }
        g.fill(x, y, x + MODE_BTN_W, y + MODE_BTN_H, bg);
        // Border
        g.fill(x, y, x + MODE_BTN_W, y + 1, active ? 0xFFFFFFFF : 0xFFDBDBDB);
        g.fill(x, y, x + 1, y + MODE_BTN_H, active ? 0xFFFFFFFF : 0xFFDBDBDB);
        g.fill(x, y + MODE_BTN_H - 1, x + MODE_BTN_W, y + MODE_BTN_H, 0xFF555555);
        g.fill(x + MODE_BTN_W - 1, y, x + MODE_BTN_W, y + MODE_BTN_H, 0xFF555555);
        // Label
        int textWidth = font.width(label);
        g.drawString(font, label, x + (MODE_BTN_W - textWidth) / 2,
            y + (MODE_BTN_H - 8) / 2, active ? 0x404040 : 0x606060, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Check if mode changed (ContainerData sync) and update visibility
        boolean currentlyTextMode = menu.getFilterModeOrdinal() == 1;
        if (lastModeWasItem == currentlyTextMode) {
            updateTextBoxVisibility();
        }

        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }

    // === Mouse Handling ===

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = leftPos;
        int y = topPos;

        // Mode Item button
        if (isMouseOver(mouseX, mouseY, x + MODE_ITEM_X, y + MODE_BTN_Y, MODE_BTN_W, MODE_BTN_H)) {
            PacketDistributor.sendToServer(new InterfaceActionPacket(
                menu.containerId, InterfaceActionPacket.ACTION_SET_FILTER_MODE, 0, "ITEM"));
            updateTextBoxVisibility();
            return true;
        }

        // Mode Text button
        if (isMouseOver(mouseX, mouseY, x + MODE_TEXT_X, y + MODE_BTN_Y, MODE_BTN_W, MODE_BTN_H)) {
            PacketDistributor.sendToServer(new InterfaceActionPacket(
                menu.containerId, InterfaceActionPacket.ACTION_SET_FILTER_MODE, 0, "TEXT"));
            updateTextBoxVisibility();
            return true;
        }

        // Count buttons
        int btnBaseX = x + 80;
        int actionType = menu.isImport() ? InterfaceActionPacket.ACTION_SET_MAX_COUNT
                                          : InterfaceActionPacket.ACTION_SET_MIN_KEEP;

        if (isMouseOver(mouseX, mouseY, btnBaseX, y + COUNT_Y, COUNT_BTN_W, COUNT_BTN_H)) {
            PacketDistributor.sendToServer(new InterfaceActionPacket(
                menu.containerId, actionType, -16, ""));
            return true;
        }
        if (isMouseOver(mouseX, mouseY, btnBaseX + 22, y + COUNT_Y, COUNT_BTN_W, COUNT_BTN_H)) {
            PacketDistributor.sendToServer(new InterfaceActionPacket(
                menu.containerId, actionType, -1, ""));
            return true;
        }
        if (isMouseOver(mouseX, mouseY, btnBaseX + 44, y + COUNT_Y, COUNT_BTN_W, COUNT_BTN_H)) {
            PacketDistributor.sendToServer(new InterfaceActionPacket(
                menu.containerId, actionType, 1, ""));
            return true;
        }
        if (isMouseOver(mouseX, mouseY, btnBaseX + 66, y + COUNT_Y, COUNT_BTN_W, COUNT_BTN_H)) {
            PacketDistributor.sendToServer(new InterfaceActionPacket(
                menu.containerId, actionType, 16, ""));
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isMouseOver(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // Title is rendered by renderContainerBackground, skip default
    }
}
