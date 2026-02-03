/**
 * ============================================================
 * [DebugImageScreen.java]
 * Description: Ecran debug pour tester un cadre GUI dynamique avec textures tilees
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | Beemancer           | MOD_ID               | ResourceLocation textures      |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - DebugKeyHandler.java (ouverture via touche T)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.screen;

import com.chapeau.beemancer.Beemancer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Ecran debug qui affiche un cadre 300x200 construit dynamiquement
 * avec codex_corner (coins) et codex_bar (barres) tiles et scales x2.
 * Background uni #F3E1BB a l'interieur.
 */
@OnlyIn(Dist.CLIENT)
public class DebugImageScreen extends Screen {

    private static final ResourceLocation CORNER_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Beemancer.MOD_ID, "textures/gui/codex_corner.png"
    );
    private static final ResourceLocation BAR_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            Beemancer.MOD_ID, "textures/gui/codex_bar.png"
    );

    // Textures source
    private static final int CORNER_SRC = 5;
    private static final int BAR_SRC_W = 80;
    private static final int BAR_SRC_H = 5;

    // Rendu scale x2
    private static final int BORDER = 10;

    // Cadre
    private static final int FRAME_WIDTH = 300;
    private static final int FRAME_HEIGHT = 200;

    // Background color #F3E1BB avec alpha opaque
    private static final int BG_COLOR = 0xFFF3E1BB;

    public DebugImageScreen() {
        super(Component.literal("Debug Image Viewer"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);

        int frameX = (width - FRAME_WIDTH) / 2;
        int frameY = (height - FRAME_HEIGHT) / 2;

        // 1. Background uni
        graphics.fill(frameX, frameY, frameX + FRAME_WIDTH, frameY + FRAME_HEIGHT, BG_COLOR);

        // 2. Barres horizontales (haut et bas) - tilees avec scissor pour masquer le depassement
        int barRenderW = BAR_SRC_W * 2; // 160px rendu (scale x2)
        int innerLeft = frameX + BORDER;
        int innerRight = frameX + FRAME_WIDTH - BORDER;
        int innerWidth = FRAME_WIDTH - BORDER * 2; // 280px

        graphics.enableScissor(innerLeft, frameY, innerRight, frameY + BORDER);
        for (int offset = 0; offset < innerWidth; offset += barRenderW) {
            graphics.blit(BAR_TEXTURE,
                    innerLeft + offset, frameY,
                    barRenderW, BORDER,
                    0, 0,
                    BAR_SRC_W, BAR_SRC_H,
                    BAR_SRC_W, BAR_SRC_H);
        }
        graphics.disableScissor();

        graphics.enableScissor(innerLeft, frameY + FRAME_HEIGHT - BORDER, innerRight, frameY + FRAME_HEIGHT);
        for (int offset = 0; offset < innerWidth; offset += barRenderW) {
            graphics.blit(BAR_TEXTURE,
                    innerLeft + offset, frameY + FRAME_HEIGHT - BORDER,
                    barRenderW, BORDER,
                    0, BAR_SRC_H,
                    BAR_SRC_W, -BAR_SRC_H,
                    BAR_SRC_W, BAR_SRC_H);
        }
        graphics.disableScissor();

        // 3. Barres verticales (gauche et droite) - bar tournee de 90°, tilees avec scissor
        int innerTop = frameY + BORDER;
        int innerBottom = frameY + FRAME_HEIGHT - BORDER;
        int innerHeight = FRAME_HEIGHT - BORDER * 2; // 180px

        // Barre gauche
        graphics.enableScissor(frameX, innerTop, frameX + BORDER, innerBottom);
        for (int offset = 0; offset < innerHeight; offset += barRenderW) {
            PoseStack pose = graphics.pose();
            pose.pushPose();
            pose.translate(frameX, innerTop + offset + barRenderW, 0);
            pose.mulPose(Axis.ZP.rotationDegrees(-90));
            graphics.blit(BAR_TEXTURE,
                    0, 0,
                    barRenderW, BORDER,
                    0, 0,
                    BAR_SRC_W, BAR_SRC_H,
                    BAR_SRC_W, BAR_SRC_H);
            pose.popPose();
        }
        graphics.disableScissor();

        // Barre droite
        graphics.enableScissor(frameX + FRAME_WIDTH - BORDER, innerTop, frameX + FRAME_WIDTH, innerBottom);
        for (int offset = 0; offset < innerHeight; offset += barRenderW) {
            PoseStack pose = graphics.pose();
            pose.pushPose();
            pose.translate(frameX + FRAME_WIDTH, innerTop + offset, 0);
            pose.mulPose(Axis.ZP.rotationDegrees(90));
            graphics.blit(BAR_TEXTURE,
                    0, 0,
                    barRenderW, BORDER,
                    0, 0,
                    BAR_SRC_W, BAR_SRC_H,
                    BAR_SRC_W, BAR_SRC_H);
            pose.popPose();
        }
        graphics.disableScissor();

        // 4. Coins (5x5 rendu en 10x10) - avec flips via UV
        // Haut-gauche
        graphics.blit(CORNER_TEXTURE,
                frameX, frameY,
                BORDER, BORDER,
                0, 0,
                CORNER_SRC, CORNER_SRC,
                CORNER_SRC, CORNER_SRC);

        // Haut-droit (flip horizontal)
        graphics.blit(CORNER_TEXTURE,
                frameX + FRAME_WIDTH - BORDER, frameY,
                BORDER, BORDER,
                CORNER_SRC, 0,
                -CORNER_SRC, CORNER_SRC,
                CORNER_SRC, CORNER_SRC);

        // Bas-gauche (flip vertical)
        graphics.blit(CORNER_TEXTURE,
                frameX, frameY + FRAME_HEIGHT - BORDER,
                BORDER, BORDER,
                0, CORNER_SRC,
                CORNER_SRC, -CORNER_SRC,
                CORNER_SRC, CORNER_SRC);

        // Bas-droit (flip horizontal + vertical)
        graphics.blit(CORNER_TEXTURE,
                frameX + FRAME_WIDTH - BORDER, frameY + FRAME_HEIGHT - BORDER,
                BORDER, BORDER,
                CORNER_SRC, CORNER_SRC,
                -CORNER_SRC, -CORNER_SRC,
                CORNER_SRC, CORNER_SRC);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
