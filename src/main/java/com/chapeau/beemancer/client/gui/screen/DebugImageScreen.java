/**
 * ============================================================
 * [DebugImageScreen.java]
 * Description: Ecran debug pour visualiser une image GUI a sa taille reelle
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | Beemancer           | MOD_ID               | ResourceLocation texture       |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - DebugKeyHandler.java (ouverture via touche T)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.screen;

import com.chapeau.beemancer.Beemancer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Ecran debug qui affiche une image de GUI a sa taille pixel reelle (1:1).
 * Utilise pour verifier le format et le rendu d'une texture GUI.
 */
@OnlyIn(Dist.CLIENT)
public class DebugImageScreen extends Screen {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Beemancer.MOD_ID, "textures/gui/codex_book.png"
    );

    private static final int IMAGE_WIDTH = 300;
    private static final int IMAGE_HEIGHT = 204;

    public DebugImageScreen() {
        super(Component.literal("Debug Image Viewer"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);

        int x = (width - IMAGE_WIDTH) / 2;
        int y = (height - IMAGE_HEIGHT) / 2;

        graphics.blit(TEXTURE, x, y, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT, IMAGE_WIDTH, IMAGE_HEIGHT);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
