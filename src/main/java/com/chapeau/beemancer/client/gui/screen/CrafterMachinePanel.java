/**
 * ============================================================
 * [CrafterMachinePanel.java]
 * Description: Panneau mode MACHINE du CrafterScreen - inputs/output dynamiques scrollables
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | GuiRenderHelper               | Rendu programmatique | Slots, boutons                 |
 * | CrafterMenu                   | Donnees container    | hasBlankPaper                  |
 * | CrafterInscribePacket         | Inscription C2S      | Envoi mode=1                   |
 * | CrafterBlockEntity            | BE reference         | BlockPos pour packet            |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - CrafterScreen.java (composition, mode MACHINE)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.screen;

import com.chapeau.beemancer.client.gui.GuiRenderHelper;
import com.chapeau.beemancer.common.blockentity.storage.CrafterBlockEntity;
import com.chapeau.beemancer.common.menu.storage.CrafterMenu;
import com.chapeau.beemancer.core.network.packets.CrafterInscribePacket;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Gere le rendu et l'interaction du mode MACHINE dans le CrafterScreen.
 * Inputs dynamiques (ajout/suppression) + output + scroll + inscription.
 */
public class CrafterMachinePanel {

    // Layout relative to screen top-left
    private static final int PANEL_X = 8;
    private static final int PANEL_Y = 18;
    private static final int PANEL_W = 160;
    private static final int LINE_H = 20;
    private static final int MAX_VISIBLE_LINES = 4;

    // Per-line element offsets (relative to line X)
    private static final int SLOT_OFFSET_X = 0;
    private static final int COUNT_MINUS_X = 20;
    private static final int COUNT_LABEL_X = 34;
    private static final int COUNT_PLUS_X = 62;
    private static final int DELETE_X = 80;
    private static final int BTN_SIZE = 14;

    // Add button
    private static final int ADD_BTN_Y_OFFSET = 2;

    // Output section
    private static final int OUTPUT_LABEL_Y = 90;
    private static final int OUTPUT_LINE_Y = 100;

    /** Client-side input entries (item + count). */
    private final List<MachineEntry> inputs = new ArrayList<>();

    /** Client-side output entry. */
    private MachineEntry output = new MachineEntry(ItemStack.EMPTY, 1);

    private int scrollOffset = 0;

    // === Data structures ===

    public static class MachineEntry {
        ItemStack item;
        int count;

        MachineEntry(ItemStack item, int count) {
            this.item = item;
            this.count = count;
        }
    }

    // === Reset on mode switch ===

    public void reset() {
        inputs.clear();
        output = new MachineEntry(ItemStack.EMPTY, 1);
        scrollOffset = 0;
    }

    // === Rendering ===

    public void render(GuiGraphics g, Font font, int x, int y, int mouseX, int mouseY) {
        g.drawString(font, "Inputs", x + PANEL_X, y + PANEL_Y - 10, 0x606060, false);

        // Visible input lines (scrolled)
        int visibleCount = Math.min(inputs.size(), MAX_VISIBLE_LINES);
        for (int i = 0; i < visibleCount; i++) {
            int dataIdx = i + scrollOffset;
            if (dataIdx >= inputs.size()) break;
            int lineY = y + PANEL_Y + i * LINE_H;
            renderInputLine(g, font, x + PANEL_X, lineY, inputs.get(dataIdx), mouseX, mouseY);
        }

        // [+] Add input button (below visible inputs)
        int addY = y + PANEL_Y + visibleCount * LINE_H + ADD_BTN_Y_OFFSET;
        boolean addHovered = isOver(mouseX, mouseY, x + PANEL_X, addY, BTN_SIZE, BTN_SIZE);
        GuiRenderHelper.renderButton(g, font, x + PANEL_X, addY, BTN_SIZE, BTN_SIZE, "+", addHovered);

        // Scroll indicators
        if (scrollOffset > 0) {
            g.drawString(font, "\u25B2", x + PANEL_X + PANEL_W - 10, y + PANEL_Y - 10, 0x404040, false);
        }
        if (scrollOffset + MAX_VISIBLE_LINES < inputs.size()) {
            g.drawString(font, "\u25BC", x + PANEL_X + PANEL_W - 10,
                    y + PANEL_Y + MAX_VISIBLE_LINES * LINE_H, 0x404040, false);
        }

        // Output section
        int outLabelY = y + OUTPUT_LABEL_Y;
        int outLineY = y + OUTPUT_LINE_Y;
        g.drawString(font, "Output", x + PANEL_X, outLabelY, 0x606060, false);
        renderOutputLine(g, font, x + PANEL_X, outLineY, mouseX, mouseY);
    }

    private void renderInputLine(GuiGraphics g, Font font, int lx, int ly,
                                  MachineEntry entry, int mouseX, int mouseY) {
        // Ghost slot
        GuiRenderHelper.renderSlot(g, lx + SLOT_OFFSET_X, ly);
        if (!entry.item.isEmpty()) {
            g.renderItem(entry.item, lx + SLOT_OFFSET_X + 1, ly + 1);
        }

        // [-] count [+]
        boolean minusHov = isOver(mouseX, mouseY, lx + COUNT_MINUS_X, ly, BTN_SIZE, BTN_SIZE);
        GuiRenderHelper.renderButton(g, font, lx + COUNT_MINUS_X, ly, BTN_SIZE, BTN_SIZE, "-", minusHov);

        String countStr = String.valueOf(entry.count);
        int countW = font.width(countStr);
        g.drawString(font, countStr, lx + COUNT_LABEL_X + (24 - countW) / 2, ly + 4, 0x404040, false);

        boolean plusHov = isOver(mouseX, mouseY, lx + COUNT_PLUS_X, ly, BTN_SIZE, BTN_SIZE);
        GuiRenderHelper.renderButton(g, font, lx + COUNT_PLUS_X, ly, BTN_SIZE, BTN_SIZE, "+", plusHov);

        // Delete button
        boolean delHov = isOver(mouseX, mouseY, lx + DELETE_X, ly, BTN_SIZE, BTN_SIZE);
        GuiRenderHelper.renderButton(g, font, lx + DELETE_X, ly, BTN_SIZE, BTN_SIZE, "X", delHov);
    }

    private void renderOutputLine(GuiGraphics g, Font font, int lx, int ly,
                                   int mouseX, int mouseY) {
        GuiRenderHelper.renderSlot(g, lx + SLOT_OFFSET_X, ly);
        if (!output.item.isEmpty()) {
            g.renderItem(output.item, lx + SLOT_OFFSET_X + 1, ly + 1);
        }

        boolean minusHov = isOver(mouseX, mouseY, lx + COUNT_MINUS_X, ly, BTN_SIZE, BTN_SIZE);
        GuiRenderHelper.renderButton(g, font, lx + COUNT_MINUS_X, ly, BTN_SIZE, BTN_SIZE, "-", minusHov);

        String countStr = String.valueOf(output.count);
        int countW = font.width(countStr);
        g.drawString(font, countStr, lx + COUNT_LABEL_X + (24 - countW) / 2, ly + 4, 0x404040, false);

        boolean plusHov = isOver(mouseX, mouseY, lx + COUNT_PLUS_X, ly, BTN_SIZE, BTN_SIZE);
        GuiRenderHelper.renderButton(g, font, lx + COUNT_PLUS_X, ly, BTN_SIZE, BTN_SIZE, "+", plusHov);
    }

    // === Interaction ===

    /**
     * Handles mouse click in machine mode. Returns true if consumed.
     */
    public boolean handleClick(double mouseX, double mouseY, int x, int y, ItemStack carried) {
        // Input lines
        int visibleCount = Math.min(inputs.size(), MAX_VISIBLE_LINES);
        for (int i = 0; i < visibleCount; i++) {
            int dataIdx = i + scrollOffset;
            if (dataIdx >= inputs.size()) break;
            int lx = x + PANEL_X;
            int ly = y + PANEL_Y + i * LINE_H;

            // Ghost slot click: set/clear item
            if (isOver(mouseX, mouseY, lx + SLOT_OFFSET_X + 1, ly + 1, 16, 16)) {
                inputs.get(dataIdx).item = carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1);
                return true;
            }
            // [-] count
            if (isOver(mouseX, mouseY, lx + COUNT_MINUS_X, ly, BTN_SIZE, BTN_SIZE)) {
                MachineEntry e = inputs.get(dataIdx);
                e.count = Math.max(1, e.count - 1);
                return true;
            }
            // [+] count
            if (isOver(mouseX, mouseY, lx + COUNT_PLUS_X, ly, BTN_SIZE, BTN_SIZE)) {
                MachineEntry e = inputs.get(dataIdx);
                e.count = Math.min(64, e.count + 1);
                return true;
            }
            // [X] delete
            if (isOver(mouseX, mouseY, lx + DELETE_X, ly, BTN_SIZE, BTN_SIZE)) {
                inputs.remove(dataIdx);
                scrollOffset = Mth.clamp(scrollOffset, 0, Math.max(0, inputs.size() - MAX_VISIBLE_LINES));
                return true;
            }
        }

        // [+] Add input button
        int addY = y + PANEL_Y + visibleCount * LINE_H + ADD_BTN_Y_OFFSET;
        if (isOver(mouseX, mouseY, x + PANEL_X, addY, BTN_SIZE, BTN_SIZE)) {
            inputs.add(new MachineEntry(ItemStack.EMPTY, 1));
            return true;
        }

        // Output ghost slot click
        int outLineY = y + OUTPUT_LINE_Y;
        int lx = x + PANEL_X;
        if (isOver(mouseX, mouseY, lx + SLOT_OFFSET_X + 1, outLineY + 1, 16, 16)) {
            output.item = carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1);
            return true;
        }
        // Output [-]
        if (isOver(mouseX, mouseY, lx + COUNT_MINUS_X, outLineY, BTN_SIZE, BTN_SIZE)) {
            output.count = Math.max(1, output.count - 1);
            return true;
        }
        // Output [+]
        if (isOver(mouseX, mouseY, lx + COUNT_PLUS_X, outLineY, BTN_SIZE, BTN_SIZE)) {
            output.count = Math.min(64, output.count + 1);
            return true;
        }

        return false;
    }

    /** Handles scroll in machine mode. Returns true if consumed. */
    public boolean handleScroll(double scrollY) {
        if (inputs.size() <= MAX_VISIBLE_LINES) return false;
        int maxScroll = inputs.size() - MAX_VISIBLE_LINES;
        scrollOffset = Mth.clamp(scrollOffset - (int) scrollY, 0, maxScroll);
        return true;
    }

    // === Inscribe logic ===

    /** Whether machine mode inscribe is possible. */
    public boolean canInscribe(CrafterMenu menu) {
        if (!menu.hasBlankPaper()) return false;
        // At least one input with an item
        boolean hasInput = inputs.stream().anyMatch(e -> !e.item.isEmpty());
        if (!hasInput) return false;
        // Output must have an item
        return !output.item.isEmpty();
    }

    /** Sends the CrafterInscribePacket for mode=1 (machine). */
    public void sendInscribe(CrafterBlockEntity be) {
        List<ItemStack> machineInputs = new ArrayList<>();
        for (MachineEntry e : inputs) {
            if (!e.item.isEmpty()) {
                machineInputs.add(e.item.copyWithCount(e.count));
            }
        }
        List<ItemStack> machineOutputs = List.of(output.item.copyWithCount(output.count));

        PacketDistributor.sendToServer(new CrafterInscribePacket(
                be.getBlockPos(), 1, machineInputs, machineOutputs));
    }

    private boolean isOver(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
