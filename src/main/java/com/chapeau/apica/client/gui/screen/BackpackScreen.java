/**
 * ============================================================
 * [BackpackScreen.java]
 * Description: Ecran du backpack — texture vanilla coffre simple + tabs navigation
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | BackpackMenu        | Donnees container    | Menu reference                 |
 * | AccessoryClientCache| Icone backpack       | Tab icon rendering + tab list  |
 * | IAccessory          | Type check           | Tab click delegation           |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (enregistrement ecran)
 *
 * ============================================================
 */
package com.chapeau.apica.client.gui.screen;

import com.chapeau.apica.client.gui.AccessoryClientCache;
import com.chapeau.apica.common.item.accessory.IAccessory;
import com.chapeau.apica.common.menu.BackpackMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Ecran du backpack utilisant la texture vanilla du coffre simple (3 rangees).
 * Affiche des tabs dynamiques en haut: Player + un par accessoire avec hasInventoryTab().
 */
public class BackpackScreen extends AbstractContainerScreen<BackpackMenu> {

    /** Position du curseur a restaurer quand le screen s'ouvre apres un tab switch asynchrone. */
    private static double[] pendingCursorRestore = null;

    /** Texture vanilla du coffre (generic_54 gere 1 a 6 rangees). */
    private static final ResourceLocation CHEST_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    private static final int ROWS = 3;

    // Tab sprites (vanilla advancement above-type tabs)
    private static final ResourceLocation TAB_SELECTED_LEFT =
            ResourceLocation.withDefaultNamespace("advancements/tab_above_left_selected");
    private static final ResourceLocation TAB_UNSELECTED_LEFT =
            ResourceLocation.withDefaultNamespace("advancements/tab_above_left");
    private static final ResourceLocation TAB_SELECTED_MIDDLE =
            ResourceLocation.withDefaultNamespace("advancements/tab_above_middle_selected");
    private static final ResourceLocation TAB_UNSELECTED_MIDDLE =
            ResourceLocation.withDefaultNamespace("advancements/tab_above_middle");

    private static final int TAB_W = 28;
    private static final int TAB_H = 32;
    private static final int TAB_PROTRUDE = 28;
    private static final int TAB_GAP = 1;

    private static ItemStack playerIcon;
    private static ItemStack getPlayerIcon() {
        if (playerIcon == null) playerIcon = new ItemStack(Items.CRAFTING_TABLE);
        return playerIcon;
    }

    public BackpackScreen(BackpackMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 114 + ROWS * 18;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        // Restaurer la position du curseur apres un tab switch asynchrone (backpack→backpack)
        if (pendingCursorRestore != null) {
            Minecraft mc = Minecraft.getInstance();
            GLFW.glfwSetCursorPos(mc.getWindow().getWindow(),
                    pendingCursorRestore[0], pendingCursorRestore[1]);
            pendingCursorRestore = null;
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // Top part: titre + rangees de slots container
        graphics.blit(CHEST_TEXTURE, x, y, 0, 0, imageWidth, ROWS * 18 + 17);

        // Bottom part: inventaire joueur (toujours a l'offset 126 dans la texture)
        graphics.blit(CHEST_TEXTURE, x, y + ROWS * 18 + 17, 0, 126, imageWidth, 96);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        // --- Tabs (dynamiques: Player + un par accessoire avec tab) ---
        List<Integer> tabSlots = AccessoryClientCache.getTabSlots();
        if (!tabSlots.isEmpty()) {
            int tabY = topPos - TAB_PROTRUDE;
            int tabX = leftPos;
            int currentSlot = menu.getAccessorySlot();

            // Player tab (unselected — we're on BackpackScreen)
            graphics.blitSprite(TAB_UNSELECTED_LEFT, tabX, tabY, TAB_W, TAB_H);
            graphics.renderItem(getPlayerIcon(), tabX + 6, tabY + 9);
            tabX += TAB_W + TAB_GAP;

            // Accessory tabs
            for (int slot : tabSlots) {
                boolean selected = (slot == currentSlot);
                ResourceLocation sprite = selected ? TAB_SELECTED_MIDDLE : TAB_UNSELECTED_MIDDLE;
                graphics.blitSprite(sprite, tabX, tabY, TAB_W, TAB_H);
                ItemStack accStack = AccessoryClientCache.getSlot(slot);
                ItemStack tabIcon = accStack;
                if (accStack.getItem() instanceof IAccessory acc && acc.getTabIcon() != null) {
                    tabIcon = acc.getTabIcon();
                }
                graphics.renderItem(tabIcon, tabX + 6, tabY + 9);
                tabX += TAB_W + TAB_GAP;
            }
        }

        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            List<Integer> tabSlots = AccessoryClientCache.getTabSlots();
            if (!tabSlots.isEmpty()) {
                int tabY = topPos - TAB_PROTRUDE;
                int tabX = leftPos;
                Minecraft mc = Minecraft.getInstance();

                // Player tab → close backpack, open inventory (synchrone)
                if (mouseX >= tabX && mouseX < tabX + TAB_W
                        && mouseY >= tabY && mouseY < tabY + TAB_H) {
                    if (mc.player != null) {
                        saveCursorPosition(mc);
                        mc.player.closeContainer();
                        mc.setScreen(new InventoryScreen(mc.player));
                        restoreCursorNow(mc);
                        return true;
                    }
                }
                tabX += TAB_W + TAB_GAP;

                // Accessory tabs (asynchrone — le serveur ouvre le nouveau screen)
                int currentSlot = menu.getAccessorySlot();
                for (int slot : tabSlots) {
                    if (slot != currentSlot
                            && mouseX >= tabX && mouseX < tabX + TAB_W
                            && mouseY >= tabY && mouseY < tabY + TAB_H) {
                        ItemStack stack = AccessoryClientCache.getSlot(slot);
                        if (mc.player != null && stack.getItem() instanceof IAccessory acc) {
                            saveCursorPosition(mc);
                            mc.player.closeContainer();
                            acc.onInventoryTabClicked(slot);
                            return true;
                        }
                    }
                    tabX += TAB_W + TAB_GAP;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** Sauvegarde la position actuelle du curseur pour restauration ulterieure. */
    private static void saveCursorPosition(Minecraft mc) {
        long window = mc.getWindow().getWindow();
        double[] xBuf = new double[1];
        double[] yBuf = new double[1];
        GLFW.glfwGetCursorPos(window, xBuf, yBuf);
        pendingCursorRestore = new double[]{ xBuf[0], yBuf[0] };
    }

    /** Restaure immediatement la position du curseur (pour les transitions synchrones). */
    private static void restoreCursorNow(Minecraft mc) {
        if (pendingCursorRestore != null) {
            GLFW.glfwSetCursorPos(mc.getWindow().getWindow(),
                    pendingCursorRestore[0], pendingCursorRestore[1]);
            pendingCursorRestore = null;
        }
    }
}
