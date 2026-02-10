/**
 * ============================================================
 * [CrafterScreen.java]
 * Description: GUI du Crafter - mode CRAFT (ghost 3x3) et mode MACHINE (overlay dynamique)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | CrafterMenu                   | Donnees container    | Slots, data, ghost slots       |
 * | GuiRenderHelper               | Rendu programmatique | Background, slots, boutons     |
 * | CrafterGhostSlotPacket        | Ghost slot C2S       | Placement ghost items          |
 * | CrafterInscribePacket         | Inscription C2S      | Bouton Inscribe                |
 * | CrafterModeTogglePacket       | Toggle mode C2S      | Bouton CRAFT/MACHINE           |
 * | CrafterBlockEntity            | BE reference         | Ghost items, blockpos          |
 * | CrafterMachinePanel           | Mode MACHINE         | Inputs dynamiques, scroll      |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (enregistrement ecran)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.screen;

import com.chapeau.beemancer.client.gui.GuiRenderHelper;
import com.chapeau.beemancer.common.blockentity.storage.CrafterBlockEntity;
import com.chapeau.beemancer.common.menu.storage.CrafterMenu;
import com.chapeau.beemancer.core.network.packets.CrafterGhostSlotPacket;
import com.chapeau.beemancer.core.network.packets.CrafterInscribePacket;
import com.chapeau.beemancer.core.network.packets.CrafterModeTogglePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen du Crafter avec deux modes superposes (meme zone):
 * - CRAFT: grille 3x3 ghost + preview resultat crafting
 * - MACHINE: inputs dynamiques scrollables + output via CrafterMachinePanel
 * Header: toggle top-left, paper slot, output slots.
 * Library: 9 slots de large, scrollable.
 * Pas de titre.
 */
public class CrafterScreen extends AbstractContainerScreen<CrafterMenu> {

    private static final int BG_WIDTH = 176;
    private static final int BG_HEIGHT = 220;

    // Header row
    private static final int MODE_BTN_X = 8;
    private static final int MODE_BTN_Y = 5;
    private static final int MODE_BTN_W = 50;
    private static final int MODE_BTN_H = 14;

    // Mode content area (overlay: craft OR machine, same zone)
    private static final int GHOST_X = CrafterMenu.GHOST_X;
    private static final int GHOST_Y = CrafterMenu.GHOST_Y;
    private static final int PREVIEW_X = 116;
    private static final int PREVIEW_Y = 44;
    private static final int ARROW_X = 82;
    private static final int ARROW_Y = 44;

    // Inscribe button (below content area)
    private static final int INSCRIBE_BTN_X = 28;
    private static final int INSCRIBE_BTN_Y = 84;
    private static final int INSCRIBE_BTN_W = 50;
    private static final int INSCRIBE_BTN_H = 14;

    // Library (9 wide, 1 visible row, scrollable)
    private static final int LIBRARY_X = CrafterMenu.LIBRARY_X;
    private static final int LIBRARY_Y = CrafterMenu.LIBRARY_Y;

    // Library scroll buttons
    private static final int LIB_SCROLL_UP_X = 8;
    private static final int LIB_SCROLL_UP_Y = 100;
    private static final int LIB_SCROLL_DN_X = 20;
    private static final int LIB_SCROLL_DN_Y = 100;
    private static final int LIB_SCROLL_W = 10;
    private static final int LIB_SCROLL_H = 9;

    // Player inventory
    private static final int PLAYER_INV_Y = CrafterMenu.PLAYER_INV_Y;
    private static final int HOTBAR_Y = CrafterMenu.HOTBAR_Y;
    private static final int SEPARATOR_Y = 130;

    // Machine mode panel (composition)
    private final CrafterMachinePanel machinePanel = new CrafterMachinePanel();
    private int lastRenderedMode = -1;

    public CrafterScreen(CrafterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = BG_WIDTH;
        this.imageHeight = BG_HEIGHT;
        this.inventoryLabelY = -999;
        this.titleLabelY = -999;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        boolean isCraftMode = menu.getMode() == 0;

        // Update slot visibility on mode change
        if (lastRenderedMode != menu.getMode()) {
            lastRenderedMode = menu.getMode();
            menu.updateGhostSlotVisibility();
            menu.updateOutputVisibility();
        }

        // Background (no title)
        GuiRenderHelper.renderContainerBackgroundNoTitle(g, x, y, BG_WIDTH, BG_HEIGHT);

        // Separator above player inventory
        g.fill(x + 4, y + SEPARATOR_Y, x + BG_WIDTH - 4, y + SEPARATOR_Y + 1, 0xFF7B7B7B);

        // === Header: toggle, paper, outputs ===
        String modeLabel = isCraftMode ? "CRAFT" : "MACHINE";
        boolean modeHovered = isMouseOver(mouseX, mouseY,
                x + MODE_BTN_X, y + MODE_BTN_Y, MODE_BTN_W, MODE_BTN_H);
        GuiRenderHelper.renderButton(g, font, x + MODE_BTN_X, y + MODE_BTN_Y,
                MODE_BTN_W, MODE_BTN_H, modeLabel, modeHovered);

        // Reserve slot (paper, right of toggle)
        GuiRenderHelper.renderSlot(g, x + CrafterMenu.RESERVE_X - 1, y + CrafterMenu.RESERVE_Y - 1);

        // Output A
        GuiRenderHelper.renderSlot(g, x + CrafterMenu.OUTPUT_A_X - 1, y + CrafterMenu.OUTPUT_A_Y - 1);
        // Output B (machine mode only)
        if (!isCraftMode) {
            GuiRenderHelper.renderSlot(g, x + CrafterMenu.OUTPUT_B_X - 1, y + CrafterMenu.OUTPUT_B_Y - 1);
        }

        // === Mode content area (overlay) ===
        if (isCraftMode) {
            GuiRenderHelper.renderSlotGrid(g, x + GHOST_X - 1, y + GHOST_Y - 1, 3, 3);
            GuiRenderHelper.renderSlot(g, x + PREVIEW_X, y + PREVIEW_Y);
            GuiRenderHelper.renderProgressArrow(g, x + ARROW_X, y + ARROW_Y, 0f);
        } else {
            machinePanel.render(g, font, x, y, mouseX, mouseY);
        }

        // === Inscribe button ===
        renderInscribeButton(g, x, y, mouseX, mouseY, canInscribe());

        // === Library (9 wide, scrollable) ===
        g.drawString(font, "Library", x + LIBRARY_X + 34, y + LIBRARY_Y - 9, 0x606060, false);
        GuiRenderHelper.renderSlotGrid(g, x + LIBRARY_X - 1, y + LIBRARY_Y - 1, 9, 1);

        // Scroll indicators
        int scrollRow = menu.getLibraryScrollRow();
        if (scrollRow > 0) {
            boolean hov = isMouseOver(mouseX, mouseY, x + LIB_SCROLL_UP_X, y + LIB_SCROLL_UP_Y, LIB_SCROLL_W, LIB_SCROLL_H);
            GuiRenderHelper.renderButton(g, font, x + LIB_SCROLL_UP_X, y + LIB_SCROLL_UP_Y,
                    LIB_SCROLL_W, LIB_SCROLL_H, "\u25B2", hov);
        }
        if (scrollRow < CrafterMenu.LIBRARY_ROWS - 1) {
            boolean hov = isMouseOver(mouseX, mouseY, x + LIB_SCROLL_DN_X, y + LIB_SCROLL_DN_Y, LIB_SCROLL_W, LIB_SCROLL_H);
            GuiRenderHelper.renderButton(g, font, x + LIB_SCROLL_DN_X, y + LIB_SCROLL_DN_Y,
                    LIB_SCROLL_W, LIB_SCROLL_H, "\u25BC", hov);
        }

        // Row indicator
        g.drawString(font, (scrollRow + 1) + "/" + CrafterMenu.LIBRARY_ROWS,
                x + LIBRARY_X, y + LIBRARY_Y - 9, 0x909090, false);

        // === Player inventory ===
        GuiRenderHelper.renderPlayerInventory(g, x, y, PLAYER_INV_Y - 1, HOTBAR_Y - 1);

        // Craft preview (ghost result)
        if (isCraftMode) {
            renderCraftPreview(g, x, y);
        }
    }

    private void renderInscribeButton(GuiGraphics g, int x, int y,
                                       int mouseX, int mouseY, boolean canInscribe) {
        int bx = x + INSCRIBE_BTN_X;
        int by = y + INSCRIBE_BTN_Y;
        boolean hovered = isMouseOver(mouseX, mouseY, bx, by, INSCRIBE_BTN_W, INSCRIBE_BTN_H);

        if (canInscribe) {
            GuiRenderHelper.renderButton(g, font, bx, by,
                    INSCRIBE_BTN_W, INSCRIBE_BTN_H, "Inscribe", hovered);
        } else {
            g.fill(bx, by, bx + INSCRIBE_BTN_W, by + INSCRIBE_BTN_H, 0xFFA0A0A0);
            g.fill(bx, by, bx + INSCRIBE_BTN_W, by + 1, 0xFFBBBBBB);
            g.fill(bx, by, bx + 1, by + INSCRIBE_BTN_H, 0xFFBBBBBB);
            g.fill(bx, by + INSCRIBE_BTN_H - 1, bx + INSCRIBE_BTN_W, by + INSCRIBE_BTN_H, 0xFF777777);
            g.fill(bx + INSCRIBE_BTN_W - 1, by, bx + INSCRIBE_BTN_W, by + INSCRIBE_BTN_H, 0xFF777777);
            int tw = font.width("Inscribe");
            g.drawString(font, "Inscribe", bx + (INSCRIBE_BTN_W - tw) / 2,
                    by + (INSCRIBE_BTN_H - 8) / 2, 0x808080, false);
        }
    }

    private void renderCraftPreview(GuiGraphics g, int x, int y) {
        if (minecraft == null || minecraft.level == null) return;

        List<ItemStack> ingredients = new ArrayList<>(9);
        boolean hasAny = false;
        for (int i = 0; i < CrafterBlockEntity.GHOST_GRID_SIZE; i++) {
            ItemStack ghost = menu.getGhostSlots()[i].getItem();
            ingredients.add(ghost.isEmpty() ? ItemStack.EMPTY : ghost);
            if (!ghost.isEmpty()) hasAny = true;
        }
        if (!hasAny) return;

        CraftingInput craftInput = CraftingInput.of(3, 3, ingredients);
        var recipeHolder = minecraft.level.getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, craftInput, minecraft.level);
        if (recipeHolder.isEmpty()) return;

        ItemStack result = recipeHolder.get().value()
                .assemble(craftInput, minecraft.level.registryAccess());
        if (result.isEmpty()) return;

        g.renderItem(result, x + PREVIEW_X + 1, y + PREVIEW_Y + 1);
        g.renderItemDecorations(font, result, x + PREVIEW_X + 1, y + PREVIEW_Y + 1);
    }

    private boolean canInscribe() {
        boolean isCraftMode = menu.getMode() == 0;
        if (isCraftMode) {
            if (!menu.hasBlankPaper()) return false;
            for (int i = 0; i < CrafterBlockEntity.GHOST_GRID_SIZE; i++) {
                if (!menu.getGhostSlots()[i].getItem().isEmpty()) return true;
            }
            return false;
        } else {
            return machinePanel.canInscribe(menu);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = leftPos;
        int y = topPos;
        CrafterBlockEntity be = menu.getBlockEntity();

        // Mode toggle button
        if (isMouseOver(mouseX, mouseY, x + MODE_BTN_X, y + MODE_BTN_Y,
                MODE_BTN_W, MODE_BTN_H) && be != null) {
            machinePanel.reset();
            PacketDistributor.sendToServer(new CrafterModeTogglePacket(be.getBlockPos()));
            return true;
        }

        // Inscribe button
        if (isMouseOver(mouseX, mouseY, x + INSCRIBE_BTN_X, y + INSCRIBE_BTN_Y,
                INSCRIBE_BTN_W, INSCRIBE_BTN_H) && canInscribe() && be != null) {
            if (menu.getMode() == 0) {
                PacketDistributor.sendToServer(new CrafterInscribePacket(
                        be.getBlockPos(), 0, List.of(), List.of()));
            } else {
                machinePanel.sendInscribe(be);
            }
            return true;
        }

        // Library scroll buttons
        if (isMouseOver(mouseX, mouseY, x + LIB_SCROLL_UP_X, y + LIB_SCROLL_UP_Y,
                LIB_SCROLL_W, LIB_SCROLL_H)) {
            menu.scrollLibrary(-1);
            return true;
        }
        if (isMouseOver(mouseX, mouseY, x + LIB_SCROLL_DN_X, y + LIB_SCROLL_DN_Y,
                LIB_SCROLL_W, LIB_SCROLL_H)) {
            menu.scrollLibrary(1);
            return true;
        }

        boolean isCraftMode = menu.getMode() == 0;

        // Ghost slot clicks (mode CRAFT only)
        if (isCraftMode && be != null) {
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 3; col++) {
                    int slotX = x + GHOST_X + col * 18;
                    int slotY = y + GHOST_Y + row * 18;
                    if (mouseX >= slotX && mouseX < slotX + 16
                            && mouseY >= slotY && mouseY < slotY + 16) {
                        int idx = row * 3 + col;
                        ItemStack carried = menu.getCarried();
                        ItemStack ghostItem = carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1);
                        PacketDistributor.sendToServer(new CrafterGhostSlotPacket(
                                be.getBlockPos(), idx, ghostItem));
                        be.setGhostItem(idx, ghostItem);
                        return true;
                    }
                }
            }
        }

        // Machine panel clicks (mode MACHINE only)
        if (!isCraftMode && machinePanel.handleClick(mouseX, mouseY, x, y, menu.getCarried())) {
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int x = leftPos;
        int y = topPos;

        // Library area scroll
        if (mouseY >= y + LIBRARY_Y - 10 && mouseY <= y + LIBRARY_Y + 20) {
            menu.scrollLibrary(scrollY > 0 ? -1 : 1);
            return true;
        }

        // Machine mode content area scroll
        if (menu.getMode() != 0 && machinePanel.handleScroll(scrollY)) {
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        // No labels (no title, no inventory label)
    }

    private boolean isMouseOver(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }
}
