/**
 * ============================================================
 * [AdjacentGuiOverlayRenderer.java]
 * Description: Overlay visuel quand on ouvre un GUI adjacent depuis le bouton Debug
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | NetworkInterfaceScreen        | Flag openedFromDebug | Savoir quand afficher overlay  |
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

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.HashSet;
import java.util.Set;

/**
 * Dessine un overlay noir plein ecran par-dessus le GUI d'un bloc adjacent
 * quand il a ete ouvert depuis le bouton Debug de l'interface.
 * Affiche un bouton toggle par-dessus chaque slot du container (pas ceux du joueur).
 *
 * Utilise drawManaged pour forcer le rendu apres les items (meme technique
 * que renderTooltipInternal dans GuiGraphics).
 */
public class AdjacentGuiOverlayRenderer {

    private static final ItemStack BEE_ICON = new ItemStack(Items.BEE_SPAWN_EGG);
    private static final int SLOT_SIZE = 18;

    private static final int COLOR_OVERLAY = 0xE0000000;
    private static final int COLOR_TOGGLE_OFF = 0x80FF0000;
    private static final int COLOR_TOGGLE_ON = 0x8000FF00;
    private static final int COLOR_TOGGLE_HOVER = 0x40FFFFFF;

    private static final Set<Integer> toggledSlots = new HashSet<>();

    @SubscribeEvent
    public static void onContainerForeground(ContainerScreenEvent.Render.Foreground event) {
        if (!NetworkInterfaceScreen.openedFromDebugButton) return;
        AbstractContainerScreen<?> screen = event.getContainerScreen();
        if (screen instanceof NetworkInterfaceScreen) return;

        GuiGraphics g = event.getGuiGraphics();

        int leftPos = screen.getGuiLeft();
        int topPos = screen.getGuiTop();
        int screenWidth = screen.width;
        int screenHeight = screen.height;

        double mouseX = screen.getMinecraft().mouseHandler.xpos()
                * screenWidth / screen.getMinecraft().getWindow().getWidth();
        double mouseY = screen.getMinecraft().mouseHandler.ypos()
                * screenHeight / screen.getMinecraft().getWindow().getHeight();

        g.drawManaged(() -> {
            g.pose().pushPose();
            g.pose().translate(-leftPos, -topPos, 400);

            // Fond noir plein ecran
            g.fill(0, 0, screenWidth, screenHeight, COLOR_OVERLAY);

            g.pose().popPose();

            // Boutons toggle par-dessus chaque slot du container (coordonnees relatives)
            var menu = screen.getMenu();
            for (int i = 0; i < menu.slots.size(); i++) {
                Slot slot = menu.slots.get(i);
                if (slot.container instanceof Inventory) continue;
                if (!slot.isActive()) continue;

                int sx = slot.x - 1;
                int sy = slot.y - 1;

                boolean toggled = toggledSlots.contains(i);
                int color = toggled ? COLOR_TOGGLE_ON : COLOR_TOGGLE_OFF;
                g.fill(sx, sy, sx + SLOT_SIZE, sy + SLOT_SIZE, color);

                // Hover highlight
                double relMouseX = mouseX - leftPos;
                double relMouseY = mouseY - topPos;
                if (relMouseX >= sx && relMouseX < sx + SLOT_SIZE
                        && relMouseY >= sy && relMouseY < sy + SLOT_SIZE) {
                    g.fill(sx, sy, sx + SLOT_SIZE, sy + SLOT_SIZE, COLOR_TOGGLE_HOVER);
                }
            }

            // Icone abeille en haut a gauche du container
            g.renderItem(BEE_ICON, 0, -18);
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

        // Bloque tout autre clic (fond noir = pas d'interaction)
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onMouseRelease(ScreenEvent.MouseButtonReleased.Pre event) {
        if (!NetworkInterfaceScreen.openedFromDebugButton) return;
        if (event.getScreen() instanceof NetworkInterfaceScreen) return;
        if (!(event.getScreen() instanceof AbstractContainerScreen<?>)) return;

        // Bloque tous les mouse release pour eviter les interactions avec le container
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onScreenClosing(ScreenEvent.Closing event) {
        if (NetworkInterfaceScreen.openedFromDebugButton) {
            if (!(event.getScreen() instanceof NetworkInterfaceScreen)) {
                NetworkInterfaceScreen.openedFromDebugButton = false;
                toggledSlots.clear();
            }
        }
    }
}
