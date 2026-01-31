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
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Screen programmatique pour Import/Export Interface.
 *
 * Layout (176w x 192h):
 * - Titre + status (Linked / Not Linked)
 * - Adjacent block info line
 * - Zone filtres: jusqu'a 3 lignes de 20px
 *   Chaque ligne: [T] [5 slots OU EditBox] [S] [qty EditBox] [-]
 * - Bouton [+] si < 3 filtres
 * - Bouton [S] global si 0 filtres
 * - Separateur
 * - Inventaire joueur
 */
public class NetworkInterfaceScreen extends AbstractContainerScreen<NetworkInterfaceMenu> {

    /** Flag positionne quand on ouvre le GUI adjacent pour la selection de slots. */
    public static boolean openedFromDebugButton = false;

    /** Index du filtre en cours de selection de slots via l'overlay (-1=none, 99=global). */
    public static int overlaySelectingFilterIndex = -1;

    /** Selection initiale a charger dans l'overlay au premier frame. */
    public static Set<Integer> overlayInitialSelection = new HashSet<>();

    /** ContainerId du menu Interface pour envoyer le packet de retour. */
    public static int overlayContainerId = -1;

    private static final int BG_WIDTH = 176;
    private static final int BG_HEIGHT = 192;

    // Filter line layout
    private static final int FILTER_ZONE_Y = 28;
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
    private static final int PLAYER_INV_Y = 110;
    private static final int HOTBAR_Y = 168;

    // Filter text EditBoxes (one per possible filter line)
    private final EditBox[] filterTextBoxes = new EditBox[InterfaceFilter.MAX_FILTERS];
    // Quantity EditBoxes (one per possible filter line)
    private final EditBox[] qtyBoxes = new EditBox[InterfaceFilter.MAX_FILTERS];

    private int lastFilterCount = -1;

    // Slot selector overlay state
    private int selectingFilterIndex = -1;
    private final Set<Integer> currentSelection = new HashSet<>();
    private int adjacentContainerSize = 0;

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

        updateGhostSlotPositions();
        lastFilterCount = filterCount;
    }

    private void updateGhostSlotPositions() {
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

    // === Key Handling ===

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // When an EditBox is focused, consume all keys except ESC
        // to prevent the inventory key (E) from closing the screen
        if (keyCode != 256 && isAnyEditBoxFocused()) {
            for (EditBox box : filterTextBoxes) {
                if (box != null && box.isVisible() && box.isFocused()) {
                    box.keyPressed(keyCode, scanCode, modifiers);
                    return true;
                }
            }
            for (EditBox box : qtyBoxes) {
                if (box != null && box.isVisible() && box.isFocused()) {
                    box.keyPressed(keyCode, scanCode, modifiers);
                    return true;
                }
            }
            return true;
        }

        // Close slot selector on ESC
        if (keyCode == 256 && selectingFilterIndex >= 0) {
            closeSlotSelector();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private boolean isAnyEditBoxFocused() {
        for (EditBox box : filterTextBoxes) {
            if (box != null && box.isVisible() && box.isFocused()) return true;
        }
        for (EditBox box : qtyBoxes) {
            if (box != null && box.isVisible() && box.isFocused()) return true;
        }
        return false;
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

        // Adjacent block info + Debug button
        renderAdjacentBlock(g, x, y, mouseX, mouseY);

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

    private void renderAdjacentBlock(GuiGraphics g, int x, int y, int mouseX, int mouseY) {
        NetworkInterfaceBlockEntity be = menu.getBlockEntity();
        if (be == null || be.getLevel() == null) return;

        BlockPos adjPos = be.getAdjacentPos();
        BlockState adjState = be.getLevel().getBlockState(adjPos);

        String blockName;
        int color;
        if (adjState.isAir()) {
            blockName = "No block";
            color = 0x802020;
        } else {
            blockName = adjState.getBlock().getName().getString();
            BlockEntity adjBe = be.getLevel().getBlockEntity(adjPos);
            color = (adjBe instanceof Container) ? 0x206020 : 0x806020;
        }

        g.drawString(font, "\u2192 " + blockName, x + 8, y + 17, color, false);
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

        // [S] Select Slots button
        boolean hasSelection = !filter.getSelectedSlots().isEmpty();
        boolean selectHovered = isMouseOver(mouseX, mouseY,
            x + SELECT_X, lineY, SELECT_W, SELECT_H);
        String selectLabel = hasSelection ? "S*" : "S";
        GuiRenderHelper.renderButton(g, font, x + SELECT_X, lineY,
            SELECT_W, SELECT_H, selectLabel, selectHovered);

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

        // Slot selector overlay (rendered on top of everything)
        if (selectingFilterIndex >= 0) {
            renderSlotSelectorOverlay(g, mouseX, mouseY);
        }
    }

    // === Slot Selector Overlay ===

    private void openSlotSelector(int filterIndex) {
        NetworkInterfaceBlockEntity be = menu.getBlockEntity();
        if (be == null || be.getLevel() == null) return;

        BlockPos adjPos = be.getAdjacentPos();
        BlockEntity adjBe = be.getLevel().getBlockEntity(adjPos);

        // Load existing selection
        Set<Integer> existingSelection = new HashSet<>();
        if (filterIndex == 99) {
            existingSelection.addAll(be.getGlobalSelectedSlots());
        } else {
            InterfaceFilter filter = be.getFilter(filterIndex);
            if (filter != null) {
                existingSelection.addAll(filter.getSelectedSlots());
            }
        }

        // If adjacent block has a MenuProvider, use the overlay system (open real GUI)
        if (adjBe instanceof net.minecraft.world.MenuProvider) {
            overlaySelectingFilterIndex = filterIndex;
            overlayInitialSelection.clear();
            overlayInitialSelection.addAll(existingSelection);
            overlayContainerId = menu.containerId;
            openedFromDebugButton = true;
            PacketDistributor.sendToServer(new InterfaceActionPacket(
                menu.containerId, InterfaceActionPacket.ACTION_OPEN_ADJACENT_GUI, 0, ""));
            return;
        }

        // Otherwise, use old grid-based overlay (no MenuProvider)
        if (!(adjBe instanceof Container container)) return;

        adjacentContainerSize = container.getContainerSize();
        if (adjacentContainerSize <= 0) return;

        selectingFilterIndex = filterIndex;
        currentSelection.clear();
        currentSelection.addAll(existingSelection);
    }

    private void closeSlotSelector() {
        if (selectingFilterIndex < 0) return;

        // Send selected slots to server
        String slotsStr = currentSelection.stream()
            .map(String::valueOf)
            .collect(Collectors.joining(","));

        if (selectingFilterIndex == 99) {
            PacketDistributor.sendToServer(new InterfaceActionPacket(
                menu.containerId, InterfaceActionPacket.ACTION_SET_GLOBAL_SELECTED_SLOTS,
                0, slotsStr));
            NetworkInterfaceBlockEntity be = menu.getBlockEntity();
            if (be != null) {
                be.setGlobalSelectedSlots(new HashSet<>(currentSelection));
            }
        } else {
            PacketDistributor.sendToServer(new InterfaceActionPacket(
                menu.containerId, InterfaceActionPacket.ACTION_SET_SELECTED_SLOTS,
                selectingFilterIndex, slotsStr));
            NetworkInterfaceBlockEntity be = menu.getBlockEntity();
            if (be != null) {
                be.setFilterSelectedSlots(selectingFilterIndex, new HashSet<>(currentSelection));
            }
        }

        selectingFilterIndex = -1;
        currentSelection.clear();
    }

    private void renderSlotSelectorOverlay(GuiGraphics g, int mouseX, int mouseY) {
        int cols = 9;
        int rows = (adjacentContainerSize + cols - 1) / cols;

        int panelW = cols * 18 + 14;
        int panelH = rows * 18 + 30;
        int panelX = leftPos + (BG_WIDTH - panelW) / 2;
        int panelY = topPos + 20;

        // Dim background
        g.fill(leftPos, topPos, leftPos + BG_WIDTH, topPos + BG_HEIGHT, 0x80000000);

        // Panel background
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xFFC6C6C6);
        g.fill(panelX, panelY, panelX + panelW, panelY + 1, 0xFFFFFFFF);
        g.fill(panelX, panelY, panelX + 1, panelY + panelH, 0xFFFFFFFF);
        g.fill(panelX, panelY + panelH - 1, panelX + panelW, panelY + panelH, 0xFF555555);
        g.fill(panelX + panelW - 1, panelY, panelX + panelW, panelY + panelH, 0xFF555555);

        // Title
        String title = selectingFilterIndex == 99
            ? "Select Slots (Global)"
            : "Select Slots (Filter " + (selectingFilterIndex + 1) + ")";
        g.drawString(font, title, panelX + 7, panelY + 5, 0x404040, false);

        // Slots grid
        int gridX = panelX + 7;
        int gridY = panelY + 16;

        for (int i = 0; i < adjacentContainerSize; i++) {
            int col = i % cols;
            int row = i / cols;
            int slotX = gridX + col * 18;
            int slotY = gridY + row * 18;

            GuiRenderHelper.renderSlot(g, slotX, slotY);

            // Highlight selected slots
            if (currentSelection.contains(i)) {
                g.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0x6000FF00);
            }

            // Hover highlight
            if (mouseX >= slotX + 1 && mouseX < slotX + 17
                && mouseY >= slotY + 1 && mouseY < slotY + 17) {
                g.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0x40FFFFFF);
            }

            // Slot number
            String num = String.valueOf(i);
            g.drawString(font, num, slotX + 9 - font.width(num) / 2,
                slotY + 5, 0x606060, false);
        }

        // Done button
        int btnW = 40;
        int btnH = 12;
        int btnX = panelX + panelW - btnW - 5;
        int btnY = panelY + panelH - btnH - 3;
        boolean btnHovered = mouseX >= btnX && mouseX < btnX + btnW
            && mouseY >= btnY && mouseY < btnY + btnH;
        GuiRenderHelper.renderButton(g, font, btnX, btnY, btnW, btnH, "Done", btnHovered);
    }

    // === Mouse Handling ===

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Slot selector overlay takes priority
        if (selectingFilterIndex >= 0) {
            return handleSlotSelectorClick(mouseX, mouseY);
        }

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
                    filter.setMode(InterfaceFilter.FilterMode.valueOf(newMode));
                    updateWidgetVisibility();
                    menu.updateFilterSlots();
                }
                return true;
            }

            // [S] Select Slots
            if (isMouseOver(mouseX, mouseY, x + SELECT_X, lineY, SELECT_W, SELECT_H)) {
                openSlotSelector(i);
                return true;
            }

            // [-] Remove
            if (isMouseOver(mouseX, mouseY, x + REMOVE_X, lineY, REMOVE_W, REMOVE_H)) {
                PacketDistributor.sendToServer(new InterfaceActionPacket(
                    menu.containerId, InterfaceActionPacket.ACTION_REMOVE_FILTER, i, ""));
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
                if (be != null) {
                    be.addFilter();
                    updateWidgetVisibility();
                    menu.updateFilterSlots();
                }
                return true;
            }

            // Global [S] (only when 0 filters)
            if (filterCount == 0) {
                if (isMouseOver(mouseX, mouseY, x + TOGGLE_X + ADD_BTN_W + 2, addY,
                    SELECT_W, SELECT_H)) {
                    openSlotSelector(99);
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleSlotSelectorClick(double mouseX, double mouseY) {
        int cols = 9;
        int rows = (adjacentContainerSize + cols - 1) / cols;
        int panelW = cols * 18 + 14;
        int panelH = rows * 18 + 30;
        int panelX = leftPos + (BG_WIDTH - panelW) / 2;
        int panelY = topPos + 20;

        int gridX = panelX + 7;
        int gridY = panelY + 16;

        // Check slot clicks
        for (int i = 0; i < adjacentContainerSize; i++) {
            int col = i % cols;
            int row = i / cols;
            int slotX = gridX + col * 18 + 1;
            int slotY = gridY + row * 18 + 1;

            if (mouseX >= slotX && mouseX < slotX + 16
                && mouseY >= slotY && mouseY < slotY + 16) {
                if (currentSelection.contains(i)) {
                    currentSelection.remove(i);
                } else {
                    currentSelection.add(i);
                }
                return true;
            }
        }

        // Check Done button
        int btnW = 40;
        int btnH = 12;
        int btnX = panelX + panelW - btnW - 5;
        int btnY = panelY + panelH - btnH - 3;
        if (mouseX >= btnX && mouseX < btnX + btnW
            && mouseY >= btnY && mouseY < btnY + btnH) {
            closeSlotSelector();
            return true;
        }

        // Click outside panel closes it
        if (mouseX < panelX || mouseX >= panelX + panelW
            || mouseY < panelY || mouseY >= panelY + panelH) {
            closeSlotSelector();
            return true;
        }

        return true;
    }

    private boolean isMouseOver(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // Title is rendered by renderContainerBackground, skip default
    }
}
