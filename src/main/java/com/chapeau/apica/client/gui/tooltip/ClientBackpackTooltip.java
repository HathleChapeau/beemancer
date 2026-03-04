/**
 * ============================================================
 * [ClientBackpackTooltip.java]
 * Description: Rendu client du tooltip backpack — grille 6x2 d'items
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | BackpackTooltip     | Donnees items        | Source des stacks a rendre     |
 * | GuiGraphics         | Rendu                | Dessin slots + items           |
 * | Font                | Texte                | Item decorations (count)       |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (registration via RegisterClientTooltipComponentsEvent)
 *
 * ============================================================
 */
package com.chapeau.apica.client.gui.tooltip;

import com.chapeau.apica.common.item.BackpackTooltip;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Rend une grille 6x2 de slots avec les items du backpack.
 * Chaque cellule fait 18x18 pixels (taille d'un slot vanilla).
 */
public class ClientBackpackTooltip implements ClientTooltipComponent {

    private static final int SLOT_SIZE = 18;
    private static final int PADDING = 1;

    private final List<ItemStack> items;
    private final int rows;

    public ClientBackpackTooltip(BackpackTooltip tooltip) {
        this.items = tooltip.items();
        this.rows = items.isEmpty() ? 0 : Math.min(2, (items.size() + BackpackTooltip.COLUMNS - 1) / BackpackTooltip.COLUMNS);
    }

    @Override
    public int getHeight() {
        return rows * SLOT_SIZE + 2 * PADDING;
    }

    @Override
    public int getWidth(Font font) {
        return BackpackTooltip.COLUMNS * SLOT_SIZE;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
        for (int i = 0; i < items.size(); i++) {
            int col = i % BackpackTooltip.COLUMNS;
            int row = i / BackpackTooltip.COLUMNS;
            int slotX = x + col * SLOT_SIZE;
            int slotY = y + PADDING + row * SLOT_SIZE;

            renderSlotBg(graphics, slotX, slotY);
            graphics.renderItem(items.get(i), slotX + 1, slotY + 1);
            graphics.renderItemDecorations(font, items.get(i), slotX + 1, slotY + 1);
        }

        // Rendre les slots vides restants pour completer la grille
        int totalSlots = rows * BackpackTooltip.COLUMNS;
        for (int i = items.size(); i < totalSlots; i++) {
            int col = i % BackpackTooltip.COLUMNS;
            int row = i / BackpackTooltip.COLUMNS;
            int slotX = x + col * SLOT_SIZE;
            int slotY = y + PADDING + row * SLOT_SIZE;
            renderSlotBg(graphics, slotX, slotY);
        }
    }

    /** Dessine un fond de slot vanilla (18x18). */
    private void renderSlotBg(GuiGraphics graphics, int x, int y) {
        graphics.fill(x, y, x + SLOT_SIZE, y + 1, 0xFF373737);
        graphics.fill(x, y + 1, x + 1, y + SLOT_SIZE - 1, 0xFF373737);
        graphics.fill(x + SLOT_SIZE - 1, y + 1, x + SLOT_SIZE, y + SLOT_SIZE, 0xFFFFFFFF);
        graphics.fill(x, y + SLOT_SIZE - 1, x + SLOT_SIZE - 1, y + SLOT_SIZE, 0xFFFFFFFF);
        graphics.fill(x + 1, y + 1, x + SLOT_SIZE - 1, y + SLOT_SIZE - 1, 0xFF8B8B8B);
    }
}
