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
 * | ScreenEvent                   | Events NeoForge      | Render.Post, Closing           |
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
import net.neoforged.neoforge.client.event.ScreenEvent;

/**
 * Dessine un overlay (rectangle noir + icone abeille) par-dessus le GUI
 * d'un bloc adjacent quand il a ete ouvert depuis le bouton Debug de l'interface.
 *
 * Le flag est pose par NetworkInterfaceScreen.openedFromDebugButton et reset
 * quand le screen se ferme.
 */
public class AdjacentGuiOverlayRenderer {

    private static final ItemStack BEE_ICON = new ItemStack(Items.BEE_SPAWN_EGG);

    @SubscribeEvent
    public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        if (!NetworkInterfaceScreen.openedFromDebugButton) return;
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) return;
        if (event.getScreen() instanceof NetworkInterfaceScreen) return;

        GuiGraphics g = event.getGuiGraphics();
        int screenWidth = event.getScreen().width;
        int screenHeight = event.getScreen().height;

        // Rectangle noir couvrant la zone inventaire joueur (bas de l'ecran)
        int overlayTop = screenHeight / 2 + 20;
        g.fill(0, overlayTop, screenWidth, screenHeight, 0xE0000000);

        // Icone abeille centree dans le rectangle noir
        int iconX = screenWidth / 2 - 8;
        int iconY = overlayTop + (screenHeight - overlayTop) / 2 - 8;
        g.renderItem(BEE_ICON, iconX, iconY);
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
