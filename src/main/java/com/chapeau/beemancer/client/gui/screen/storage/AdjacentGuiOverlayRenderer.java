/**
 * ============================================================
 * [AdjacentGuiOverlayRenderer.java]
 * Description: Overlay visuel pour la selection de slots sur un GUI adjacent
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | NetworkInterfaceScreen        | Flags statiques      | openedFromDebugButton, overlay*|
 * | InterfaceActionPacket         | Envoi selection      | ACTION_SET_SELECTED_SLOTS etc  |
 * | ContainerScreenEvent          | Event NeoForge       | Render.Foreground (apres items)|
 * | ScreenEvent                   | Event NeoForge       | Closing, Mouse (block clics)   |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (enregistrement event bus)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.screen.storage;

import com.chapeau.beemancer.core.network.packets.InterfaceActionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import com.chapeau.beemancer.common.blockentity.storage.NetworkInterfaceBlockEntity;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Dessine un overlay semi-transparent par-dessus le GUI d'un bloc adjacent
 * quand il a ete ouvert pour la selection de slots depuis le bouton [S].
 * Affiche un bouton toggle par-dessus chaque slot du container (pas ceux du joueur).
 *
 * Inclut un titre "Select Slots", un bouton Validate (envoie la selection)
 * et un bouton Cancel (annule sans envoyer).
 */
public class AdjacentGuiOverlayRenderer {

    private static final int SLOT_SIZE = 18;

    // Overlay
    private static final int COLOR_OVERLAY = 0xC0000000;

    // Slots — plus clairs et moins transparents
    private static final int COLOR_SLOT_OFF = 0xB0555555;
    private static final int COLOR_SLOT_ON = 0xD050C050;
    private static final int COLOR_SLOT_BORDER_OFF = 0xC0888888;
    private static final int COLOR_SLOT_BORDER_ON = 0xD070E070;
    private static final int COLOR_HOVER = 0x50FFFFFF;

    // Boutons
    private static final int BTN_W = 50;
    private static final int BTN_H = 14;
    private static final int BTN_SPACING = 6;
    private static final int BTN_MARGIN_BOTTOM = 8;

    // Titre
    private static final String TITLE_TEXT = "Select Slots";
    private static final int TITLE_COLOR = 0xFFFFFF;
    private static final int TITLE_MARGIN_TOP = 6;

    private static final Set<Integer> toggledSlots = new HashSet<>();
    private static boolean initialized = false;
    private static boolean cancelled = false;

    @SubscribeEvent
    public static void onContainerForeground(ContainerScreenEvent.Render.Foreground event) {
        if (!NetworkInterfaceScreen.openedFromDebugButton) return;
        AbstractContainerScreen<?> screen = event.getContainerScreen();
        if (screen instanceof NetworkInterfaceScreen) return;

        if (!initialized) {
            toggledSlots.clear();
            toggledSlots.addAll(NetworkInterfaceScreen.overlayInitialSelection);
            initialized = true;
            cancelled = false;
        }

        GuiGraphics g = event.getGuiGraphics();
        Font font = Minecraft.getInstance().font;

        int leftPos = screen.getGuiLeft();
        int topPos = screen.getGuiTop();
        int screenWidth = screen.width;
        int screenHeight = screen.height;
        int guiWidth = screen.getXSize();

        double mouseX = screen.getMinecraft().mouseHandler.xpos()
                * screenWidth / screen.getMinecraft().getWindow().getWidth();
        double mouseY = screen.getMinecraft().mouseHandler.ypos()
                * screenHeight / screen.getMinecraft().getWindow().getHeight();

        double relMouseX = mouseX - leftPos;
        double relMouseY = mouseY - topPos;

        var menu = screen.getMenu();

        // Calcul des positions des boutons (coordonnees absolues, converties en relatives)
        int totalBtnW = BTN_W * 2 + BTN_SPACING;
        int btnBaseX = (guiWidth - totalBtnW) / 2;
        int btnBaseY = screenHeight - topPos - BTN_MARGIN_BOTTOM - BTN_H;

        int validateX = btnBaseX;
        int cancelX = btnBaseX + BTN_W + BTN_SPACING;

        // Position du titre (relatif au container)
        int titleY = -TITLE_MARGIN_TOP - 10;

        g.drawManaged(() -> {
            // Fond semi-transparent plein ecran
            g.pose().pushPose();
            g.pose().translate(-leftPos, -topPos, 400);
            g.fill(0, 0, screenWidth, screenHeight, COLOR_OVERLAY);
            g.pose().popPose();

            // Elements UI (z=401, coordonnees relatives au container)
            g.pose().pushPose();
            g.pose().translate(0, 0, 401);

            // Titre "Select Slots" centre au-dessus du GUI
            int titleWidth = font.width(TITLE_TEXT);
            int titleX = (guiWidth - titleWidth) / 2;
            g.drawString(font, TITLE_TEXT, titleX, titleY, TITLE_COLOR, true);

            // Boutons toggle par slot
            for (int i = 0; i < menu.slots.size(); i++) {
                Slot slot = menu.slots.get(i);
                if (slot.container instanceof Inventory) continue;
                if (!slot.isActive()) continue;

                int sx = slot.x - 1;
                int sy = slot.y - 1;

                boolean toggled = toggledSlots.contains(i);

                // Bordure
                int borderColor = toggled ? COLOR_SLOT_BORDER_ON : COLOR_SLOT_BORDER_OFF;
                g.fill(sx, sy, sx + SLOT_SIZE, sy + 1, borderColor);
                g.fill(sx, sy + SLOT_SIZE - 1, sx + SLOT_SIZE, sy + SLOT_SIZE, borderColor);
                g.fill(sx, sy, sx + 1, sy + SLOT_SIZE, borderColor);
                g.fill(sx + SLOT_SIZE - 1, sy, sx + SLOT_SIZE, sy + SLOT_SIZE, borderColor);

                // Fond du slot
                int color = toggled ? COLOR_SLOT_ON : COLOR_SLOT_OFF;
                g.fill(sx + 1, sy + 1, sx + SLOT_SIZE - 1, sy + SLOT_SIZE - 1, color);

                // Hover
                if (relMouseX >= sx && relMouseX < sx + SLOT_SIZE
                        && relMouseY >= sy && relMouseY < sy + SLOT_SIZE) {
                    g.fill(sx + 1, sy + 1, sx + SLOT_SIZE - 1, sy + SLOT_SIZE - 1, COLOR_HOVER);
                }
            }

            // Bouton Validate
            boolean valHover = relMouseX >= validateX && relMouseX < validateX + BTN_W
                    && relMouseY >= btnBaseY && relMouseY < btnBaseY + BTN_H;
            renderButton(g, font, validateX, btnBaseY, BTN_W, BTN_H,
                    "Validate", valHover, 0xFF408040, 0xFF509050);

            // Bouton Cancel
            boolean canHover = relMouseX >= cancelX && relMouseX < cancelX + BTN_W
                    && relMouseY >= btnBaseY && relMouseY < btnBaseY + BTN_H;
            renderButton(g, font, cancelX, btnBaseY, BTN_W, BTN_H,
                    "Cancel", canHover, 0xFF804040, 0xFF905050);

            g.pose().popPose();
        });
    }

    /**
     * Rendu d'un bouton avec bordure 3D et couleur personnalisee.
     */
    private static void renderButton(GuiGraphics g, Font font, int x, int y, int w, int h,
                                      String label, boolean hovered, int bgColor, int hoverColor) {
        int bg = hovered ? hoverColor : bgColor;
        g.fill(x, y, x + w, y + h, bg);
        // Bordures 3D claires en haut/gauche, sombres en bas/droite
        int light = 0x40FFFFFF;
        int dark = 0x40000000;
        g.fill(x, y, x + w, y + 1, light);
        g.fill(x, y, x + 1, y + h, light);
        g.fill(x, y + h - 1, x + w, y + h, dark);
        g.fill(x + w - 1, y, x + w, y + h, dark);
        // Label centre
        int textWidth = font.width(label);
        g.drawString(font, label, x + (w - textWidth) / 2, y + (h - 8) / 2, 0xFFFFFFFF, false);
    }

    @SubscribeEvent
    public static void onMouseClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!NetworkInterfaceScreen.openedFromDebugButton) return;
        if (event.getScreen() instanceof NetworkInterfaceScreen) return;
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) return;

        int leftPos = screen.getGuiLeft();
        int topPos = screen.getGuiTop();
        int screenWidth = screen.width;
        int screenHeight = screen.height;
        int guiWidth = screen.getXSize();
        double relX = event.getMouseX() - leftPos;
        double relY = event.getMouseY() - topPos;

        // Calcul positions boutons (memes formules que le rendu)
        int totalBtnW = BTN_W * 2 + BTN_SPACING;
        int btnBaseX = (guiWidth - totalBtnW) / 2;
        int btnBaseY = screenHeight - topPos - BTN_MARGIN_BOTTOM - BTN_H;

        int validateX = btnBaseX;
        int cancelX = btnBaseX + BTN_W + BTN_SPACING;

        // Bouton Validate
        if (relX >= validateX && relX < validateX + BTN_W
                && relY >= btnBaseY && relY < btnBaseY + BTN_H) {
            screen.onClose();
            event.setCanceled(true);
            return;
        }

        // Bouton Cancel
        if (relX >= cancelX && relX < cancelX + BTN_W
                && relY >= btnBaseY && relY < btnBaseY + BTN_H) {
            cancelled = true;
            screen.onClose();
            event.setCanceled(true);
            return;
        }

        // Slots toggle
        var menu = screen.getMenu();
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            if (slot.container instanceof Inventory) continue;
            if (!slot.isActive()) continue;

            int sx = slot.x - 1;
            int sy = slot.y - 1;
            if (relX >= sx && relX < sx + SLOT_SIZE && relY >= sy && relY < sy + SLOT_SIZE) {
                if (toggledSlots.contains(i)) {
                    toggledSlots.remove(i);
                } else {
                    toggledSlots.add(i);
                }
                event.setCanceled(true);
                return;
            }
        }

        // Bloque tout autre clic
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onMouseRelease(ScreenEvent.MouseButtonReleased.Pre event) {
        if (!NetworkInterfaceScreen.openedFromDebugButton) return;
        if (event.getScreen() instanceof NetworkInterfaceScreen) return;
        if (!(event.getScreen() instanceof AbstractContainerScreen<?>)) return;

        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onScreenClosing(ScreenEvent.Closing event) {
        if (NetworkInterfaceScreen.openedFromDebugButton) {
            if (!(event.getScreen() instanceof NetworkInterfaceScreen)) {
                if (!cancelled) {
                    sendSlotSelection();
                }

                NetworkInterfaceScreen.openedFromDebugButton = false;
                NetworkInterfaceScreen.overlaySelectingFilterIndex = -1;
                NetworkInterfaceScreen.overlayInitialSelection.clear();
                NetworkInterfaceScreen.overlayContainerId = -1;
                NetworkInterfaceScreen.overlayBlockEntity = null;
                toggledSlots.clear();
                initialized = false;
                cancelled = false;
            }
        }
    }

    /**
     * Construit la string CSV des slots selectionnes et envoie le packet
     * ACTION_SET_SELECTED_SLOTS ou ACTION_SET_GLOBAL_SELECTED_SLOTS.
     */
    private static void sendSlotSelection() {
        int filterIndex = NetworkInterfaceScreen.overlaySelectingFilterIndex;
        int containerId = NetworkInterfaceScreen.overlayContainerId;
        NetworkInterfaceBlockEntity be = NetworkInterfaceScreen.overlayBlockEntity;
        if (filterIndex < 0 || containerId < 0) return;

        Set<Integer> selection = new HashSet<>(toggledSlots);
        String slotsStr = selection.stream()
            .map(String::valueOf)
            .collect(Collectors.joining(","));

        if (filterIndex == 99) {
            PacketDistributor.sendToServer(new InterfaceActionPacket(
                containerId, InterfaceActionPacket.ACTION_SET_GLOBAL_SELECTED_SLOTS,
                0, slotsStr));
            if (be != null) {
                be.setGlobalSelectedSlots(selection);
            }
        } else {
            PacketDistributor.sendToServer(new InterfaceActionPacket(
                containerId, InterfaceActionPacket.ACTION_SET_SELECTED_SLOTS,
                filterIndex, slotsStr));
            if (be != null) {
                be.setFilterSelectedSlots(filterIndex, selection);
            }
        }
    }
}
