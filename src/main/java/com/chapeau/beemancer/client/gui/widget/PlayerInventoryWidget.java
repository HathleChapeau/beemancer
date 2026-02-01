/**
 * ============================================================
 * [PlayerInventoryWidget.java]
 * Description: Widget de rendu de l'inventaire joueur via texture
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | GuiGraphics         | API de rendu         | Blit texture                   |
 * | Beemancer           | MOD_ID               | Chemin texture                 |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - Tous les screens d'alchimie (AlembicScreen, InfuserScreen, etc.)
 * - MagicHiveScreen, IncubatorScreen
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.widget;

import com.chapeau.beemancer.Beemancer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class PlayerInventoryWidget {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        Beemancer.MOD_ID, "textures/gui/inventory.png");

    private static final int TEX_WIDTH = 176;
    private static final int TEX_HEIGHT = 90;

    private final int startY;

    /**
     * @param startY position Y ou la texture commence, relative au container.
     *               Calcul: machineHeight + gap (ex: 80 pour standard, 104 pour MagicHive)
     */
    public PlayerInventoryWidget(int startY) {
        this.startY = startY;
    }

    /**
     * Rend le fond de l'inventaire joueur (27 slots + 9 hotbar) via texture.
     *
     * @param g       contexte de rendu
     * @param screenX position X du container a l'ecran
     * @param screenY position Y du container a l'ecran
     */
    public void render(GuiGraphics g, int screenX, int screenY) {
        g.blit(TEXTURE, screenX, screenY + startY, 0, 0, TEX_WIDTH, TEX_HEIGHT, TEX_WIDTH, TEX_HEIGHT);
    }
}
