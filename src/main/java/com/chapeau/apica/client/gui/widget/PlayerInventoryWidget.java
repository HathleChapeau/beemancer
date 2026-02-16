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
 * | Apica           | MOD_ID               | Chemin texture                 |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - Tous les screens d'alchimie (AlembicScreen, InfuserScreen, etc.)
 * - MagicHiveScreen, IncubatorScreen
 *
 * ============================================================
 */
package com.chapeau.apica.client.gui.widget;

import com.chapeau.apica.Apica;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class PlayerInventoryWidget {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
        Apica.MOD_ID, "textures/gui/inventory.png");

    private static final int TEX_WIDTH = 176;
    private static final int TEX_HEIGHT = 90;

    private final int startY;
    private final int xOffset;

    /**
     * @param startY  position Y ou la texture commence, relative au container.
     * @param xOffset decalage X pour centrer dans un container plus large que 176px.
     *                Calcul: (containerWidth - 176) / 2
     */
    public PlayerInventoryWidget(int startY, int xOffset) {
        this.startY = startY;
        this.xOffset = xOffset;
    }

    /**
     * Rend le fond de l'inventaire joueur (27 slots + 9 hotbar) via texture.
     *
     * @param g       contexte de rendu
     * @param screenX position X du container a l'ecran
     * @param screenY position Y du container a l'ecran
     */
    public void render(GuiGraphics g, int screenX, int screenY) {
        g.blit(TEXTURE, screenX + xOffset, screenY + startY, 0, 0, TEX_WIDTH, TEX_HEIGHT, TEX_WIDTH, TEX_HEIGHT);
    }
}
