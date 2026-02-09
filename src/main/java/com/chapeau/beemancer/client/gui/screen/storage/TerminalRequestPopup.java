/**
 * ============================================================
 * [TerminalRequestPopup.java]
 * Description: Popup modale de demande d'item dans le Storage Terminal
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance               | Raison                | Utilisation              |
 * |--------------------------|----------------------|--------------------------|
 * | StorageRequestPacket     | Envoi requete        | submitRequest()          |
 * | StorageTerminalMenu      | Position bloc        | getBlockPos()            |
 * | GuiGraphics              | Rendu GUI            | draw/fill/renderItem     |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - StorageTerminalScreen.java (composition)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.screen.storage;

import com.chapeau.beemancer.core.network.packets.StorageRequestPacket;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Popup modale pour demander un item du reseau de stockage.
 * Affiche l'item, un compteur +/- et les boutons Cancel/Request.
 */
public class TerminalRequestPopup {

    private static final int POPUP_WIDTH = 120;
    private static final int POPUP_HEIGHT = 80;

    private boolean visible = false;
    private ItemStack requestItem = ItemStack.EMPTY;
    private int requestCount = 0;
    private int requestMax = 0;

    private final StorageTerminalScreen screen;

    public TerminalRequestPopup(StorageTerminalScreen screen) {
        this.screen = screen;
    }

    public boolean isVisible() {
        return visible;
    }

    public void open(ItemStack item, int maxCount) {
        this.visible = true;
        this.requestItem = item.copy();
        this.requestMax = maxCount;
        this.requestCount = Math.min(64, requestMax);
    }

    public void close() {
        this.visible = false;
        this.requestItem = ItemStack.EMPTY;
        this.requestCount = 0;
        this.requestMax = 0;
    }

    public void render(GuiGraphics g, Font font, int screenWidth, int screenHeight,
                       int mouseX, int mouseY) {
        int popupX = (screenWidth - POPUP_WIDTH) / 2;
        int popupY = (screenHeight - POPUP_HEIGHT) / 2;

        g.pose().pushPose();
        g.pose().translate(0, 0, 400);

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

        String countText = String.valueOf(requestCount);
        g.drawCenteredString(font, countText, popupX + POPUP_WIDTH / 2, popupY + 42, 0xFFFF00);

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

        g.drawString(font, Component.translatable("gui.beemancer.cancel"),
            cancelX, actionY, hoverCancel ? 0xFFFF8080 : 0xFFAAAAAA);
        g.drawString(font, Component.translatable("gui.beemancer.request"),
            requestXBtn, actionY, hoverRequest ? 0xFF80FF80 : 0xFFAAAAAA);

        g.pose().popPose();
        g.pose().popPose();
    }

    public boolean handleClick(double mouseX, double mouseY, int screenWidth, int screenHeight) {
        int popupX = (screenWidth - POPUP_WIDTH) / 2;
        int popupY = (screenHeight - POPUP_HEIGHT) / 2;

        int buttonY = popupY + 55;
        int[] positions = {popupX + 5, popupX + 25, popupX + 45, popupX + 70, popupX + 85, popupX + 100};
        int[] deltas = {-64, -10, -1, 1, 10, 64};

        for (int i = 0; i < 6; i++) {
            if (mouseX >= positions[i] && mouseX < positions[i] + 15 &&
                mouseY >= buttonY && mouseY < buttonY + 12) {
                requestCount = Mth.clamp(requestCount + deltas[i], 0, requestMax);
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
            if (mouseX >= requestXBtn && mouseX < requestXBtn + 40) {
                submitRequest();
                return true;
            }
        }

        if (mouseX < popupX || mouseX > popupX + POPUP_WIDTH ||
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

    private void submitRequest() {
        if (requestCount > 0 && !requestItem.isEmpty()) {
            PacketDistributor.sendToServer(
                new StorageRequestPacket(screen.getMenu().getBlockPos(), requestItem, requestCount)
            );
        }
        close();
    }
}
