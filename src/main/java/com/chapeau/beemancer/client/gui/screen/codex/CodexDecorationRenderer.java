/**
 * ============================================================
 * [CodexDecorationRenderer.java]
 * Description: Générateur et renderer de décorations (cracks, stains, borders) sur le fond du Codex
 * ============================================================
 *
 * DÉPENDANCES:
 * ------------------------------------------------------------
 * | Dépendance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | CodexPage           | Identifiant tab      | Config par page                |
 * | GuiGraphics         | Rendu GUI            | Blit des textures              |
 * ------------------------------------------------------------
 *
 * UTILISÉ PAR:
 * - CodexScreen (rendu des décorations sur le background)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.gui.screen.codex;

import com.chapeau.beemancer.Beemancer;
import com.chapeau.beemancer.common.codex.CodexPage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CodexDecorationRenderer {

    private static final Logger LOGGER = LoggerFactory.getLogger(CodexDecorationRenderer.class);

    // --- Textures ---
    private static final ResourceLocation[] CRACK_TEXTURES = {
        ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, "textures/gui/codex/crack_1.png"),
        ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, "textures/gui/codex/crack_2.png"),
        ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, "textures/gui/codex/crack_3.png"),
        ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, "textures/gui/codex/crack_4.png")
    };
    private static final int[][] CRACK_SIZES = {{4, 5}, {7, 7}, {2, 5}, {7, 2}};

    private static final ResourceLocation[] STAIN_TEXTURES = {
        ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, "textures/gui/codex/stain_1.png"),
        ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, "textures/gui/codex/stain_2.png")
    };
    private static final int[][] STAIN_SIZES = {{25, 21}, {39, 35}};

    private static final ResourceLocation BORDER_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(Beemancer.MOD_ID, "textures/gui/codex/page_border.png");
    private static final int BORDER_SRC_W = 33;
    private static final int BORDER_SRC_H = 19;

    // --- Scales ---
    private static final int CRACK_SCALE = 3;
    private static final int STAIN_SCALE = 2;
    private static final int BORDER_SCALE = 2;

    // --- Distance minimums (en pixels rendus) ---
    private static final int MIN_DISTANCE = 60;

    // --- Config par tab : {cracks, stains, borders} ---
    private static final int[][] TAB_CONFIG = {
        {4, 2, 1}, // APICA
        {3, 2, 1}, // BEES
        {5, 3, 1}, // ALCHEMY
        {6, 2, 1}, // ARTIFACTS
        {4, 1, 1}  // LOGISTICS
    };

    /**
     * Décoration avec coordonnées relatives au centre de la zone de contenu.
     * Les positions sont en espace "scroll" (comme les nodes).
     */
    public record Decoration(ResourceLocation texture, int relX, int relY,
                              int srcW, int srcH, int scale, float rotation) {}

    /**
     * Génère des décorations dans l'espace scrollable complet.
     * Les coordonnées sont relatives au centre de la zone de contenu (comme les nodes).
     *
     * @param scrollMinX borne min scroll X (négative)
     * @param scrollMaxX borne max scroll X (positive)
     * @param scrollMinY borne min scroll Y (négative)
     * @param scrollMaxY borne max scroll Y (positive)
     * @param contentW   largeur de la zone de contenu visible
     * @param contentH   hauteur de la zone de contenu visible
     */
    public static List<Decoration> generate(CodexPage page,
                                             double scrollMinX, double scrollMaxX,
                                             double scrollMinY, double scrollMaxY,
                                             int contentW, int contentH) {
        long seed = new Random().nextLong();
        LOGGER.debug("Codex decorations seed for {}: {}", page.getId(), seed);
        Random rng = new Random(seed);

        int tabIndex = page.ordinal();
        int[] config = tabIndex < TAB_CONFIG.length ? TAB_CONFIG[tabIndex] : TAB_CONFIG[0];
        int numCracks = config[0];
        int numStains = config[1];
        int numBorders = config[2];

        // Zone scrollable totale (en pixels relatifs au centre)
        int halfW = contentW / 2;
        int halfH = contentH / 2;
        int areaMinX = (int) scrollMinX - halfW - 20;
        int areaMaxX = (int) scrollMaxX + halfW + 20;
        int areaMinY = (int) scrollMinY - halfH - 20;
        int areaMaxY = (int) scrollMaxY + halfH + 20;
        int areaW = areaMaxX - areaMinX;
        int areaH = areaMaxY - areaMinY;

        List<Decoration> decorations = new ArrayList<>();
        List<int[]> placedPositions = new ArrayList<>();

        // Stains (placed first — larger, need more space)
        for (int i = 0; i < numStains; i++) {
            int idx = rng.nextInt(STAIN_TEXTURES.length);
            int srcW = STAIN_SIZES[idx][0];
            int srcH = STAIN_SIZES[idx][1];
            int renderW = srcW * STAIN_SCALE;
            int renderH = srcH * STAIN_SCALE;

            int[] pos = findPosition(rng, areaMinX, areaMinY, areaW - renderW, areaH - renderH, placedPositions, MIN_DISTANCE);
            if (pos != null) {
                placedPositions.add(pos);
                decorations.add(new Decoration(STAIN_TEXTURES[idx], pos[0], pos[1], srcW, srcH, STAIN_SCALE, 0));
            }
        }

        // Cracks
        for (int i = 0; i < numCracks; i++) {
            int idx = rng.nextInt(CRACK_TEXTURES.length);
            int srcW = CRACK_SIZES[idx][0];
            int srcH = CRACK_SIZES[idx][1];
            int renderW = srcW * CRACK_SCALE;
            int renderH = srcH * CRACK_SCALE;

            int[] pos = findPosition(rng, areaMinX, areaMinY, areaW - renderW, areaH - renderH, placedPositions, MIN_DISTANCE);
            if (pos != null) {
                placedPositions.add(pos);
                decorations.add(new Decoration(CRACK_TEXTURES[idx], pos[0], pos[1], srcW, srcH, CRACK_SCALE, 0));
            }
        }

        // Borders (placés sur les bords de la zone visible, position relative au centre)
        for (int i = 0; i < numBorders; i++) {
            int side = rng.nextInt(4); // 0=top, 1=right, 2=bottom, 3=left
            float rotation = side * 90f;

            int renderW = BORDER_SRC_W * BORDER_SCALE;
            int renderH = BORDER_SRC_H * BORDER_SCALE;

            int bx, by;
            switch (side) {
                case 0 -> { // Top
                    bx = areaMinX + rng.nextInt(Math.max(1, areaW - renderW));
                    by = areaMinY;
                }
                case 1 -> { // Right
                    bx = areaMaxX - renderH;
                    by = areaMinY + rng.nextInt(Math.max(1, areaH - renderW));
                }
                case 2 -> { // Bottom
                    bx = areaMinX + rng.nextInt(Math.max(1, areaW - renderW));
                    by = areaMaxY - renderH;
                }
                default -> { // Left
                    bx = areaMinX;
                    by = areaMinY + rng.nextInt(Math.max(1, areaH - renderW));
                }
            }

            decorations.add(new Decoration(BORDER_TEXTURE, bx, by, BORDER_SRC_W, BORDER_SRC_H, BORDER_SCALE, rotation));
        }

        return decorations;
    }

    /**
     * Rend les décorations avec le scroll offset appliqué.
     * Les décorations sont en coordonnées relatives au centre de la zone de contenu.
     *
     * @param centerX centre X de la zone de contenu (écran)
     * @param centerY centre Y de la zone de contenu (écran)
     * @param scrollX offset scroll horizontal
     * @param scrollY offset scroll vertical
     */
    public static void render(GuiGraphics graphics, List<Decoration> decorations,
                               int centerX, int centerY, double scrollX, double scrollY) {
        if (decorations == null) return;

        PoseStack pose = graphics.pose();
        for (Decoration d : decorations) {
            // Position écran = centre + position relative + scroll
            float screenX = (float) (centerX + d.relX + scrollX);
            float screenY = (float) (centerY + d.relY + scrollY);

            pose.pushPose();

            if (d.rotation != 0) {
                int renderW = d.srcW * d.scale;
                int renderH = d.srcH * d.scale;
                float cx = screenX + renderW / 2f;
                float cy = screenY + renderH / 2f;
                pose.translate(cx, cy, 0);
                pose.mulPose(Axis.ZP.rotationDegrees(d.rotation));
                pose.translate(-cx, -cy, 0);
            }

            pose.translate(screenX, screenY, 0);
            pose.scale(d.scale, d.scale, 1);
            graphics.blit(d.texture, 0, 0, d.srcW, d.srcH,
                    0, 0, d.srcW, d.srcH, d.srcW, d.srcH);

            pose.popPose();
        }
    }

    private static int[] findPosition(Random rng, int areaX, int areaY,
                                       int maxW, int maxH,
                                       List<int[]> existing, int minDist) {
        if (maxW <= 0 || maxH <= 0) return null;

        for (int attempt = 0; attempt < 20; attempt++) {
            int px = areaX + rng.nextInt(maxW);
            int py = areaY + rng.nextInt(maxH);

            boolean tooClose = false;
            for (int[] pos : existing) {
                double dist = Math.sqrt((px - pos[0]) * (px - pos[0]) + (py - pos[1]) * (py - pos[1]));
                if (dist < minDist) {
                    tooClose = true;
                    break;
                }
            }

            if (!tooClose) {
                return new int[]{px, py};
            }
        }
        return null;
    }
}
