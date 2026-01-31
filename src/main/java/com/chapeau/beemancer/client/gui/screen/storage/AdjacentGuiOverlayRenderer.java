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
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Dessine un overlay semi-transparent par-dessus le GUI d'un bloc adjacent
 * quand il a ete ouvert pour la selection de slots depuis le bouton [S].
 * Affiche un bouton toggle par-dessus chaque slot du container (pas ceux du joueur).
 *
 * Quand le GUI se ferme, envoie la selection au serveur via InterfaceActionPacket.
 */
public class AdjacentGuiOverlayRenderer {

    private static final int SLOT_SIZE = 18;

    private static final int COLOR_OVERLAY = 0xC0000000;
    private static final int COLOR_SLOT_OFF = 0x60333333;
    private static final int COLOR_SLOT_ON = 0x8040C040;
    private static final int COLOR_SLOT_BORDER_OFF = 0x40666666;
    private static final int COLOR_SLOT_BORDER_ON = 0x8060E060;
    private static final int COLOR_HOVER = 0x30FFFFFF;

    private static final Set<Integer> toggledSlots = new HashSet<>();
    private static boolean initialized = false;

    @SubscribeEvent
    public static void onContainerForeground(ContainerScreenEvent.Render.Foreground event) {
        if (!NetworkInterfaceScreen.openedFromDebugButton) return;
        AbstractContainerScreen<?> screen = event.getContainerScreen();
        if (screen instanceof NetworkInterfaceScreen) return;

        // Load initial selection on first frame
        if (!initialized) {
            toggledSlots.clear();
            toggledSlots.addAll(NetworkInterfaceScreen.overlayInitialSelection);
            initialized = true;
        }

        GuiGraphics g = event.getGuiGraphics();

        int leftPos = screen.getGuiLeft();
        int topPos = screen.getGuiTop();
        int screenWidth = screen.width;
        int screenHeight = screen.height;

        double mouseX = screen.getMinecraft().mouseHandler.xpos()
                * screenWidth / screen.getMinecraft().getWindow().getWidth();
        double mouseY = screen.getMinecraft().mouseHandler.ypos()
                * screenHeight / screen.getMinecraft().getWindow().getHeight();

        var menu = screen.getMenu();

        g.drawManaged(() -> {
            // Fond semi-transparent plein ecran (coordonnees absolues)
            g.pose().pushPose();
            g.pose().translate(-leftPos, -topPos, 400);
            g.fill(0, 0, screenWidth, screenHeight, COLOR_OVERLAY);
            g.pose().popPose();

            // Boutons toggle par-dessus chaque slot (coordonnees relatives container, z=401)
            g.pose().pushPose();
            g.pose().translate(0, 0, 401);

            for (int i = 0; i < menu.slots.size(); i++) {
                Slot slot = menu.slots.get(i);
                if (slot.container instanceof Inventory) continue;
                if (!slot.isActive()) continue;

                int sx = slot.x - 1;
                int sy = slot.y - 1;

                boolean toggled = toggledSlots.contains(i);

                // Bordure du slot
                int borderColor = toggled ? COLOR_SLOT_BORDER_ON : COLOR_SLOT_BORDER_OFF;
                g.fill(sx, sy, sx + SLOT_SIZE, sy + 1, borderColor);
                g.fill(sx, sy + SLOT_SIZE - 1, sx + SLOT_SIZE, sy + SLOT_SIZE, borderColor);
                g.fill(sx, sy, sx + 1, sy + SLOT_SIZE, borderColor);
                g.fill(sx + SLOT_SIZE - 1, sy, sx + SLOT_SIZE, sy + SLOT_SIZE, borderColor);

                // Fond du slot
                int color = toggled ? COLOR_SLOT_ON : COLOR_SLOT_OFF;
                g.fill(sx + 1, sy + 1, sx + SLOT_SIZE - 1, sy + SLOT_SIZE - 1, color);

                // Hover highlight
                double relMouseX = mouseX - leftPos;
                double relMouseY = mouseY - topPos;
                if (relMouseX >= sx && relMouseX < sx + SLOT_SIZE
                        && relMouseY >= sy && relMouseY < sy + SLOT_SIZE) {
                    g.fill(sx + 1, sy + 1, sx + SLOT_SIZE - 1, sy + SLOT_SIZE - 1, COLOR_HOVER);
                }
            }

            g.pose().popPose();
        });
    }

    /**
     * Intercepte tous les clics. Gere les toggles de slots et bloque
     * tout clic qui n'est pas sur un toggle.
     */
    @SubscribeEvent
    public static void onMouseClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!NetworkInterfaceScreen.openedFromDebugButton) return;
        if (event.getScreen() instanceof NetworkInterfaceScreen) return;
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) return;

        int leftPos = screen.getGuiLeft();
        int topPos = screen.getGuiTop();
        double relX = event.getMouseX() - leftPos;
        double relY = event.getMouseY() - topPos;

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
                // Envoie la selection au serveur avant de reset
                sendSlotSelection();

                NetworkInterfaceScreen.openedFromDebugButton = false;
                NetworkInterfaceScreen.overlaySelectingFilterIndex = -1;
                NetworkInterfaceScreen.overlayInitialSelection.clear();
                NetworkInterfaceScreen.overlayContainerId = -1;
                toggledSlots.clear();
                initialized = false;
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
        if (filterIndex < 0 || containerId < 0) return;

        String slotsStr = toggledSlots.stream()
            .map(String::valueOf)
            .collect(Collectors.joining(","));

        if (filterIndex == 99) {
            PacketDistributor.sendToServer(new InterfaceActionPacket(
                containerId, InterfaceActionPacket.ACTION_SET_GLOBAL_SELECTED_SLOTS,
                0, slotsStr));
        } else {
            PacketDistributor.sendToServer(new InterfaceActionPacket(
                containerId, InterfaceActionPacket.ACTION_SET_SELECTED_SLOTS,
                filterIndex, slotsStr));
        }
    }
}
