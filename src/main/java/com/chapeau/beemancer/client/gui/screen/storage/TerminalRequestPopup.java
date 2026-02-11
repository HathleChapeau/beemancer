/**
 * ============================================================
 * [TerminalRequestPopup.java]
 * Description: Popup modale de demande d'item dans le Storage Terminal
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                | Utilisation              |
 * |-------------------------------|----------------------|--------------------------|
 * | StorageRequestPacket          | Envoi requete stock  | submitRequest()          |
 * | StorageCraftRequestPacket     | Envoi requete craft  | submitRequest()          |
 * | StorageTerminalMenu           | Position bloc        | getBlockPos()            |
 * | CraftableRecipe               | Recette craftable    | Calcul materiaux         |
 * | TerminalCraftMaterialsPanel   | Panneau materiaux    | Composition              |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - StorageTerminalScreen.java (composition)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.screen.storage;

import com.chapeau.beemancer.common.data.CraftableRecipe;
import com.chapeau.beemancer.core.network.packets.StorageCraftRequestPacket;
import com.chapeau.beemancer.core.network.packets.StorageRequestPacket;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * Popup modale pour demander un item du reseau de stockage.
 * Supporte les items craftables: au-dela du stock, affiche un split stock/craft
 * et un panneau de materiaux a droite.
 */
public class TerminalRequestPopup {

    private static final int POPUP_WIDTH = 120;
    private static final int POPUP_HEIGHT = 80;
    private static final int MATERIALS_GAP = 4;

    private boolean visible = false;
    private ItemStack requestItem = ItemStack.EMPTY;
    private int requestCount = 0;
    private int requestMax = 0;
    private int stockMax = 0;
    @Nullable
    private CraftableRecipe craftableRecipe;

    private final StorageTerminalScreen screen;
    private final TerminalCraftMaterialsPanel materialsPanel = new TerminalCraftMaterialsPanel();

    public TerminalRequestPopup(StorageTerminalScreen screen) {
        this.screen = screen;
    }

    public boolean isVisible() {
        return visible;
    }

    public void open(ItemStack item, int maxStock, @Nullable CraftableRecipe recipe) {
        this.visible = true;
        this.requestItem = item.copy();
        this.stockMax = maxStock;
        this.craftableRecipe = recipe;
        // Craftable: allow up to stock + 27*64 (a chest worth of crafts)
        this.requestMax = recipe != null ? stockMax + 64 * 27 : maxStock;
        this.requestCount = Math.min(64, requestMax);
        updateMaterialsPanel();
    }

    public void close() {
        this.visible = false;
        this.requestItem = ItemStack.EMPTY;
        this.requestCount = 0;
        this.requestMax = 0;
        this.stockMax = 0;
        this.craftableRecipe = null;
        materialsPanel.clear();
    }

    public void render(GuiGraphics g, Font font, int screenWidth, int screenHeight,
                       int mouseX, int mouseY) {
        int totalWidth = POPUP_WIDTH + (materialsPanel.isActive() ? MATERIALS_GAP + 110 : 0);
        int popupX = (screenWidth - totalWidth) / 2;
        int popupY = (screenHeight - POPUP_HEIGHT) / 2;

        g.pose().pushPose();
        g.pose().translate(0, 0, 400);

        // Main popup background
        g.fill(popupX - 2, popupY - 2, popupX + POPUP_WIDTH + 2, popupY + POPUP_HEIGHT + 2, 0xFF000000);
        g.fill(popupX, popupY, popupX + POPUP_WIDTH, popupY + POPUP_HEIGHT, 0xFF3C3C3C);

        Component title = Component.translatable("gui.beemancer.storage_terminal.request");
        g.drawCenteredString(font, title, popupX + POPUP_WIDTH / 2, popupY + 5, 0xFFFFFF);

        g.renderItem(requestItem, popupX + 10, popupY + 20);

        g.pose().pushPose();
        g.pose().translate(0, 0, 200);

        String name = requestItem.getHoverName().getString();
        if (name.length() > 14) name = name.substring(0, 12) + "..";
        g.drawString(font, name, popupX + 30, popupY + 24, 0xFFFFFF);

        // Count display: split when craft is active
        int craftCount = getCraftCount();
        int actualStock = getStockCount();

        if (craftCount > 0) {
            // Stock line
            String stockStr = String.valueOf(actualStock);
            g.drawCenteredString(font, stockStr, popupX + POPUP_WIDTH / 2, popupY + 37, 0xFFFF00);
            // Craft line
            String craftStr = "craft: " + craftCount;
            g.drawCenteredString(font, craftStr, popupX + POPUP_WIDTH / 2, popupY + 47, 0xFF80FF80);
        } else {
            String countText = String.valueOf(requestCount);
            g.drawCenteredString(font, countText, popupX + POPUP_WIDTH / 2, popupY + 42, 0xFFFF00);
        }

        int buttonY = popupY + 55;
        int[] positions = {popupX + 5, popupX + 25, popupX + 45, popupX + 70, popupX + 85, popupX + 100};
        String[] labels = {"---", "--", "-", "+", "++", "+++"};

        for (int i = 0; i < 6; i++) {
            boolean hover = mouseX >= positions[i] && mouseX < positions[i] + 15 &&
                mouseY >= buttonY && mouseY < buttonY + 12;
            int color = hover ? 0xFF8080FF : 0xFFAAAAAA;
            g.drawString(font, labels[i], positions[i], buttonY, color);
        }

        int cancelX = popupX + 10;
        int requestXBtn = popupX + 70;
        int actionY = popupY + POPUP_HEIGHT - 15;

        boolean hoverCancel = mouseX >= cancelX && mouseX < cancelX + 40 &&
            mouseY >= actionY && mouseY < actionY + 10;
        boolean hoverRequest = mouseX >= requestXBtn && mouseX < requestXBtn + 40 &&
            mouseY >= actionY && mouseY < actionY + 10;

        boolean canSubmit = canSubmit();
        g.drawString(font, Component.translatable("gui.beemancer.cancel"),
            cancelX, actionY, hoverCancel ? 0xFFFF8080 : 0xFFAAAAAA);
        g.drawString(font, Component.translatable("gui.beemancer.request"),
            requestXBtn, actionY,
            canSubmit ? (hoverRequest ? 0xFF80FF80 : 0xFFAAAAAA) : 0xFF666666);

        g.pose().popPose();

        // Materials panel (to the right of popup)
        if (materialsPanel.isActive()) {
            int matX = popupX + POPUP_WIDTH + MATERIALS_GAP;
            materialsPanel.render(g, font, matX, popupY, screen.getMenu());
        }

        g.pose().popPose();
    }

    public boolean handleClick(double mouseX, double mouseY, int screenWidth, int screenHeight) {
        int totalWidth = POPUP_WIDTH + (materialsPanel.isActive() ? MATERIALS_GAP + 110 : 0);
        int popupX = (screenWidth - totalWidth) / 2;
        int popupY = (screenHeight - POPUP_HEIGHT) / 2;

        int buttonY = popupY + 55;
        int[] positions = {popupX + 5, popupX + 25, popupX + 45, popupX + 70, popupX + 85, popupX + 100};
        int[] deltas = {-64, -10, -1, 1, 10, 64};

        for (int i = 0; i < 6; i++) {
            if (mouseX >= positions[i] && mouseX < positions[i] + 15 &&
                mouseY >= buttonY && mouseY < buttonY + 12) {
                requestCount = Mth.clamp(requestCount + deltas[i], 0, requestMax);
                updateMaterialsPanel();
                return true;
            }
        }

        int cancelX = popupX + 10;
        int requestXBtn = popupX + 70;
        int actionY = popupY + POPUP_HEIGHT - 15;

        if (mouseY >= actionY && mouseY < actionY + 10) {
            if (mouseX >= cancelX && mouseX < cancelX + 40) {
                close();
                return true;
            }
            if (mouseX >= requestXBtn && mouseX < requestXBtn + 40 && canSubmit()) {
                submitRequest();
                return true;
            }
        }

        // Click outside popup+materials → close
        int fullRight = popupX + totalWidth;
        if (mouseX < popupX || mouseX > fullRight ||
            mouseY < popupY || mouseY > popupY + POPUP_HEIGHT) {
            close();
            return true;
        }

        return true;
    }

    public boolean handleKey(int keyCode) {
        if (keyCode == 256) {
            close();
            return true;
        }
        return true;
    }

    public boolean handleScroll(double scrollY) {
        return materialsPanel.handleScroll(scrollY);
    }

    // === Internal ===

    private int getStockCount() {
        return Math.min(requestCount, stockMax);
    }

    private int getCraftCount() {
        if (craftableRecipe == null) return 0;
        return Math.max(0, requestCount - stockMax);
    }

    private boolean canSubmit() {
        if (requestCount <= 0 || requestItem.isEmpty()) return false;
        int craft = getCraftCount();
        if (craft > 0) {
            return materialsPanel.canLaunchCraft(screen.getMenu());
        }
        return true;
    }

    private void updateMaterialsPanel() {
        int craft = getCraftCount();
        if (craft > 0 && craftableRecipe != null) {
            materialsPanel.update(craftableRecipe, craft);
        } else {
            materialsPanel.clear();
        }
    }

    private void submitRequest() {
        if (requestCount <= 0 || requestItem.isEmpty()) {
            close();
            return;
        }

        int stock = getStockCount();
        int craft = getCraftCount();

        if (craft > 0) {
            // Hybrid request: stock + craft
            PacketDistributor.sendToServer(
                new StorageCraftRequestPacket(
                    screen.getMenu().getBlockPos(), requestItem, stock, craft)
            );
        } else {
            // Stock-only request (existing flow)
            PacketDistributor.sendToServer(
                new StorageRequestPacket(
                    screen.getMenu().getBlockPos(), requestItem, requestCount)
            );
        }
        close();
    }
}
