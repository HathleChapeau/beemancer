/**
 * ============================================================
 * [NetworkInterfaceScreen.java]
 * Description: GUI pour Import/Export Interface avec filtres par ligne
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | NetworkInterfaceMenu          | Donnees container    | Slots, data accessors          |
 * | InterfaceActionPacket         | Envoi actions C2S    | Add/remove filter, mode, text  |
 * | GuiRenderHelper               | Rendu programmatique | Background, slots, boutons     |
 * | InterfaceFilter               | Donnees filtre       | Mode, quantite                 |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (enregistrement ecran)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.screen.storage;

import com.chapeau.beemancer.client.gui.GuiRenderHelper;
import com.chapeau.beemancer.common.blockentity.storage.InterfaceFilter;
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
 * Layout (176w x 182h):
 * - Titre + status (Linked / Not Linked)
 * - Zone filtres: jusqu'a 3 lignes de 20px
 *   Chaque ligne: [T] [5 slots OU EditBox] [S] [qty EditBox] [-]
 * - Bouton [+] si < 3 filtres
 * - Bouton [S] global si 0 filtres
 * - Separateur
 * - Inventaire joueur
 */
public class NetworkInterfaceScreen extends AbstractContainerScreen<NetworkInterfaceMenu> {

    private static final int BG_WIDTH = 176;
    private static final int BG_HEIGHT = 182;

    // Filter line layout
    private static final int FILTER_ZONE_Y = 18;
    private static final int FILTER_LINE_H = 20;

    // Per-line element positions (relative to line start)
    private static final int TOGGLE_X = 7;
    private static final int TOGGLE_W = 14;
    private static final int TOGGLE_H = 14;

    private static final int SLOTS_X = 23;
    private static final int SLOTS_COUNT = 5;

    private static final int SELECT_X = 115;
    private static final int SELECT_W = 14;
    private static final int SELECT_H = 14;

    private static final int QTY_X = 131;
    private static final int QTY_W = 26;
    private static final int QTY_H = 14;

    private static final int REMOVE_X = 159;
    private static final int REMOVE_W = 14;
    private static final int REMOVE_H = 14;

    // Add button
    private static final int ADD_BTN_W = 14;
    private static final int ADD_BTN_H = 14;

    // Player inventory
    private static final int PLAYER_INV_Y = 100;
    private static final int HOTBAR_Y = 158;

    // Filter text EditBoxes (one per possible filter line)
    private final EditBox[] filterTextBoxes = new EditBox[InterfaceFilter.MAX_FILTERS];
    // Quantity EditBoxes (one per possible filter line)
    private final EditBox[] qtyBoxes = new EditBox[InterfaceFilter.MAX_FILTERS];

    private int lastFilterCount = -1;

    public NetworkInterfaceScreen(NetworkInterfaceMenu menu, Inventory playerInventory,
                                   Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = BG_WIDTH;
        this.imageHeight = BG_HEIGHT;
        this.inventoryLabelY = PLAYER_INV_Y - 10;
    }

    @Override
    protected void init() {
        super.init();

        for (int i = 0; i < InterfaceFilter.MAX_FILTERS; i++) {
            int lineY = topPos + FILTER_ZONE_Y + i * FILTER_LINE_H + 3;

            // Text filter EditBox (shown in TEXT mode)
            EditBox textBox = new EditBox(font, leftPos + SLOTS_X, lineY,
                SLOTS_COUNT * 18, TOGGLE_H, Component.empty());
            textBox.setMaxLength(64);
            textBox.setBordered(true);
            textBox.setVisible(false);

            final int idx = i;
            textBox.setResponder(text -> onFilterTextChanged(idx, text));
            filterTextBoxes[i] = textBox;
            addRenderableWidget(textBox);

            // Quantity EditBox
            EditBox qtyBox = new EditBox(font, leftPos + QTY_X, lineY,
                QTY_W, QTY_H, Component.empty());
            qtyBox.setMaxLength(5);
            qtyBox.setBordered(true);
            qtyBox.setVisible(false);
            qtyBox.setValue("0");

            qtyBox.setResponder(text -> onQuantityChanged(idx, text));
            qtyBoxes[i] = qtyBox;
            addRenderableWidget(qtyBox);
        }

        loadFilterData();
        updateWidgetVisibility();
    }

    private void loadFilterData() {
        NetworkInterfaceBlockEntity be = menu.getBlockEntity();
        if (be == null) return;

        for (int i = 0; i < be.getFilterCount() && i < InterfaceFilter.MAX_FILTERS; i++) {
            InterfaceFilter filter = be.getFilter(i);
            if (filter == null) continue;

            if (filter.getTextFilter() != null && !filter.getTextFilter().isEmpty()) {
                filterTextBoxes[i].setValue(filter.getTextFilter());
            }
            qtyBoxes[i].setValue(String.valueOf(filter.getQuantity()));
        }
    }

    private void updateWidgetVisibility() {
        NetworkInterfaceBlockEntity be = menu.getBlockEntity();
        int filterCount = be != null ? be.getFilterCount() : 0;

        for (int i = 0; i < InterfaceFilter.MAX_FILTERS; i++) {
            boolean filterExists = i < filterCount;
            InterfaceFilter filter = (filterExists && be != null) ? be.getFilter(i) : null;
            boolean isTextMode = filter != null
                && filter.getMode() == InterfaceFilter.FilterMode.TEXT;

            filterTextBoxes[i].setVisible(filterExists && isTextMode);
            qtyBoxes[i].setVisible(filterExists);
        }

        // Update ghost slot positions
        updateGhostSlotPositions();
        lastFilterCount = filterCount;
    }

    private void updateGhostSlotPositions() {
        // Positions are fixed at menu construction time.
        // We only toggle isActive() via menu.updateFilterSlots()
        menu.updateFilterSlots();
    }

    private void onFilterTextChanged(int filterIdx, String text) {
        PacketDistributor.sendToServer(new InterfaceActionPacket(
            menu.containerId, InterfaceActionPacket.ACTION_SET_FILTER_TEXT, filterIdx, text));
    }

    private void onQuantityChanged(int filterIdx, String text) {
        PacketDistributor.sendToServer(new InterfaceActionPacket(
            menu.containerId, InterfaceActionPacket.ACTION_SET_FILTER_QUANTITY, filterIdx, text));
    }

    // === Rendering ===

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        boolean isImport = menu.isImport();

        // Background
        String titleKey = isImport ? "container.beemancer.import_interface"
                                    : "container.beemancer.export_interface";
        GuiRenderHelper.renderContainerBackground(g, font, x, y, BG_WIDTH, BG_HEIGHT,
            titleKey, PLAYER_INV_Y - 2);

        // Status: Linked / Not Linked (top right)
        renderStatus(g, x, y);

        // Filter lines
        NetworkInterfaceBlockEntity be = menu.getBlockEntity();
        int filterCount = be != null ? be.getFilterCount() : 0;

        for (int i = 0; i < filterCount; i++) {
            renderFilterLine(g, x, y, i, mouseX, mouseY);
        }

        // [+] Add filter button (if < 3 filters)
        if (filterCount < InterfaceFilter.MAX_FILTERS) {
            int addY = y + FILTER_ZONE_Y + filterCount * FILTER_LINE_H + 3;
            boolean addHovered = isMouseOver(mouseX, mouseY,
                x + TOGGLE_X, addY, ADD_BTN_W, ADD_BTN_H);
            GuiRenderHelper.renderButton(g, font, x + TOGGLE_X, addY,
                ADD_BTN_W, ADD_BTN_H, "+", addHovered);

            // Global [S] button (only when 0 filters)
            if (filterCount == 0) {
                boolean sHovered = isMouseOver(mouseX, mouseY,
                    x + TOGGLE_X + ADD_BTN_W + 2, addY, SELECT_W, SELECT_H);
                GuiRenderHelper.renderButton(g, font, x + TOGGLE_X + ADD_BTN_W + 2, addY,
                    SELECT_W, SELECT_H, "S", sHovered);
            }
        }

        // Player inventory
        GuiRenderHelper.renderPlayerInventory(g, x, y, PLAYER_INV_Y - 1, HOTBAR_Y - 1);
    }

    private void renderStatus(GuiGraphics g, int x, int y) {
        String statusText;
        int statusColor;
        if (menu.isLinked()) {
            statusText = "Linked";
            statusColor = 0x206020;
        } else {
            statusText = "Not Linked";
            statusColor = 0x802020;
        }
        int statusWidth = font.width(statusText);
        g.drawString(font, statusText, x + BG_WIDTH - 8 - statusWidth, y + 6,
            statusColor, false);
    }

    private void renderFilterLine(GuiGraphics g, int x, int y, int filterIdx,
                                   int mouseX, int mouseY) {
        NetworkInterfaceBlockEntity be = menu.getBlockEntity();
        if (be == null) return;
        InterfaceFilter filter = be.getFilter(filterIdx);
        if (filter == null) return;

        int lineY = y + FILTER_ZONE_Y + filterIdx * FILTER_LINE_H + 3;
        boolean isItemMode = filter.getMode() == InterfaceFilter.FilterMode.ITEM;

        // [T] Toggle button
        boolean toggleHovered = isMouseOver(mouseX, mouseY,
            x + TOGGLE_X, lineY, TOGGLE_W, TOGGLE_H);
        String toggleLabel = isItemMode ? "I" : "T";
        GuiRenderHelper.renderButton(g, font, x + TOGGLE_X, lineY,
            TOGGLE_W, TOGGLE_H, toggleLabel, toggleHovered);

        // Ghost slots (ITEM mode) - rendered by the container system via GhostSlot
        if (isItemMode) {
            for (int slot = 0; slot < SLOTS_COUNT; slot++) {
                GuiRenderHelper.renderSlot(g,
                    x + SLOTS_X + slot * 18, lineY - 1);
            }
        }
        // TEXT mode: EditBox is rendered by the widget system

        // [S] Select Slots button
        boolean selectHovered = isMouseOver(mouseX, mouseY,
            x + SELECT_X, lineY, SELECT_W, SELECT_H);
        GuiRenderHelper.renderButton(g, font, x + SELECT_X, lineY,
            SELECT_W, SELECT_H, "S", selectHovered);

        // Quantity EditBox background is rendered by the widget system

        // [-] Remove button
        boolean removeHovered = isMouseOver(mouseX, mouseY,
            x + REMOVE_X, lineY, REMOVE_W, REMOVE_H);
        GuiRenderHelper.renderButton(g, font, x + REMOVE_X, lineY,
            REMOVE_W, REMOVE_H, "-", removeHovered);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Check if filter count changed (ContainerData sync) and update visibility
        NetworkInterfaceBlockEntity be = menu.getBlockEntity();
        int currentCount = be != null ? be.getFilterCount() : 0;
        if (currentCount != lastFilterCount) {
            updateWidgetVisibility();
        }

        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }

    // === Mouse Handling ===

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = leftPos;
        int y = topPos;
        NetworkInterfaceBlockEntity be = menu.getBlockEntity();
        int filterCount = be != null ? be.getFilterCount() : 0;

        // Check filter line buttons
        for (int i = 0; i < filterCount; i++) {
            int lineY = y + FILTER_ZONE_Y + i * FILTER_LINE_H + 3;

            // [T] Toggle
            if (isMouseOver(mouseX, mouseY, x + TOGGLE_X, lineY, TOGGLE_W, TOGGLE_H)) {
                InterfaceFilter filter = be.getFilter(i);
                if (filter != null) {
                    String newMode = filter.getMode() == InterfaceFilter.FilterMode.ITEM
                        ? "TEXT" : "ITEM";
                    PacketDistributor.sendToServer(new InterfaceActionPacket(
                        menu.containerId, InterfaceActionPacket.ACTION_SET_FILTER_MODE,
                        i, newMode));
                    // Immediate local update for responsive UI
                    filter.setMode(InterfaceFilter.FilterMode.valueOf(newMode));
                    updateWidgetVisibility();
                    menu.updateFilterSlots();
                }
                return true;
            }

            // [S] Select Slots (noop for now - Phase 4)
            if (isMouseOver(mouseX, mouseY, x + SELECT_X, lineY, SELECT_W, SELECT_H)) {
                return true;
            }

            // [-] Remove
            if (isMouseOver(mouseX, mouseY, x + REMOVE_X, lineY, REMOVE_W, REMOVE_H)) {
                PacketDistributor.sendToServer(new InterfaceActionPacket(
                    menu.containerId, InterfaceActionPacket.ACTION_REMOVE_FILTER, i, ""));
                // Immediate local update
                if (be != null) {
                    be.removeFilter(i);
                    updateWidgetVisibility();
                    menu.updateFilterSlots();
                }
                return true;
            }
        }

        // [+] Add filter button
        if (filterCount < InterfaceFilter.MAX_FILTERS) {
            int addY = y + FILTER_ZONE_Y + filterCount * FILTER_LINE_H + 3;
            if (isMouseOver(mouseX, mouseY, x + TOGGLE_X, addY, ADD_BTN_W, ADD_BTN_H)) {
                PacketDistributor.sendToServer(new InterfaceActionPacket(
                    menu.containerId, InterfaceActionPacket.ACTION_ADD_FILTER, 0, ""));
                // Immediate local update
                if (be != null) {
                    be.addFilter();
                    updateWidgetVisibility();
                    menu.updateFilterSlots();
                }
                return true;
            }

            // Global [S] (only when 0 filters, noop for now - Phase 4)
            if (filterCount == 0) {
                if (isMouseOver(mouseX, mouseY, x + TOGGLE_X + ADD_BTN_W + 2, addY,
                    SELECT_W, SELECT_H)) {
                    return true;
                }
            }
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
