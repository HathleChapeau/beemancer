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
 * | ScreenEvent                   | Event NeoForge       | Closing (reset flag)           |
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ContainerScreenEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Dessine un overlay (rectangle noir + icone abeille) par-dessus le GUI
 * d'un bloc adjacent quand il a ete ouvert depuis le bouton Debug de l'interface.
 *
 * Utilise ContainerScreenEvent.Render.Foreground qui est fire APRES le rendu
 * des items dans les slots (ligne 118 de AbstractContainerScreen.render()).
 * Le depth test est desactive pendant le rendu du container, donc le z-level
 * est ignore — seul l'ordre d'appel compte.
 *
 * Le flag est pose par NetworkInterfaceScreen.openedFromDebugButton et reset
 * quand le screen se ferme.
 */
public class AdjacentGuiOverlayRenderer {

    private static final ItemStack BEE_ICON = new ItemStack(Items.BEE_SPAWN_EGG);

    @SubscribeEvent
    public static void onContainerForeground(ContainerScreenEvent.Render.Foreground event) {
        if (!NetworkInterfaceScreen.openedFromDebugButton) return;
        AbstractContainerScreen<?> screen = event.getContainerScreen();
        if (screen instanceof NetworkInterfaceScreen) return;

        GuiGraphics g = event.getGuiGraphics();

        // drawManaged: flush avant (vide le batch items), execute le runnable,
        // flush apres. Exactement ce que fait renderTooltipInternal pour
        // dessiner le fond de tooltip PAR-DESSUS les items.
        int leftPos = screen.getGuiLeft();
        int topPos = screen.getGuiTop();
        int screenWidth = screen.width;
        int screenHeight = screen.height;
        int overlayTop = screenHeight / 2 + 20;

        g.drawManaged(() -> {
            g.pose().pushPose();
            g.pose().translate(-leftPos, -topPos, 400);

            g.fill(0, overlayTop, screenWidth, screenHeight, 0xE0000000);

            int iconX = screenWidth / 2 - 8;
            int iconY = overlayTop + (screenHeight - overlayTop) / 2 - 8;
            g.renderItem(BEE_ICON, iconX, iconY);

            g.pose().popPose();
        });
    }

    @SubscribeEvent
    public static void onScreenClosing(ScreenEvent.Closing event) {
        if (NetworkInterfaceScreen.openedFromDebugButton) {
            if (!(event.getScreen() instanceof NetworkInterfaceScreen)) {
                NetworkInterfaceScreen.openedFromDebugButton = false;
            }
        }
    }
}
