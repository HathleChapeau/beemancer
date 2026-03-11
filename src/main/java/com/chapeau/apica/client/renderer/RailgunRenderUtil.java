/**
 * ============================================================
 * [RailgunRenderUtil.java]
 * Description: Utilitaires partages pour le rendu du Railgun (couleurs, positions)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                | Utilisation                    |
 * |-------------------------|----------------------|--------------------------------|
 * | MagazineData            | Lecture fluide       | getFluidTint()                 |
 * | ItemDisplayContext      | Contexte rendu       | Positions FPS left/right       |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - RailgunItemRenderer.java (BEWLR)
 * - RailgunBeamRenderer.java (world renderer)
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer;

import com.chapeau.apica.common.item.magazine.MagazineData;
import com.chapeau.apica.common.item.tool.RailgunItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Constantes et utilitaires partages pour le rendu du Railgun.
 * Centralise les positions des effets et utilitaires de conversion.
 * Les couleurs sont definies dans RailgunItem pour eviter la duplication.
 */
public final class RailgunRenderUtil {

    private RailgunRenderUtil() {}

    // ========== Couleurs par fluide (reference RailgunItem) ==========

    /**
     * Retourne la couleur de tinte selon l'ID du fluide.
     */
    public static int getFluidTint(String fluidId) {
        if (fluidId == null || fluidId.isEmpty()) return RailgunItem.DEFAULT_TINT;
        if (fluidId.contains("honey")) return RailgunItem.HONEY_TINT;
        if (fluidId.contains("royal_jelly")) return RailgunItem.ROYAL_JELLY_TINT;
        if (fluidId.contains("nectar")) return RailgunItem.NECTAR_TINT;
        return RailgunItem.DEFAULT_TINT;
    }

    /**
     * Retourne la couleur de tinte selon le magazine equipe.
     */
    public static int getFluidTint(ItemStack stack) {
        if (stack.isEmpty()) return RailgunItem.DEFAULT_TINT;
        return getFluidTint(MagazineData.getFluidId(stack));
    }

    /**
     * Convertit une couleur int (0xRRGGBB) en floats RGB [0-1].
     */
    public static float[] tintToRgb(int tint) {
        return new float[] {
            ((tint >> 16) & 0xFF) / 255f,
            ((tint >> 8) & 0xFF) / 255f,
            (tint & 0xFF) / 255f
        };
    }

    // ========== Positions BlackHole (BEWLR, coordonnees modele) ==========

    /** Position BlackHole pour FPS main droite. */
    public static final float BLACKHOLE_RIGHT_X = 0.6f;
    public static final float BLACKHOLE_RIGHT_Y = 0.5f;
    public static final float BLACKHOLE_RIGHT_Z = 2.3f;

    /** Position BlackHole pour FPS main gauche (miroir X). */
    public static final float BLACKHOLE_LEFT_X = 0.4f;
    public static final float BLACKHOLE_LEFT_Y = 0.5f;
    public static final float BLACKHOLE_LEFT_Z = 2.3f;

    /**
     * Retourne true si le contexte est FPS main droite.
     */
    public static boolean isRightHandFps(ItemDisplayContext context) {
        return context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
    }

    /**
     * Retourne true si le contexte est FPS main gauche.
     */
    public static boolean isLeftHandFps(ItemDisplayContext context) {
        return context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
    }

    /**
     * Retourne true si le contexte est FPS (gauche ou droite).
     */
    public static boolean isFps(ItemDisplayContext context) {
        return isRightHandFps(context) || isLeftHandFps(context);
    }

    /**
     * Retourne true si le contexte est en main (FPS ou TPS).
     */
    public static boolean isInHand(ItemDisplayContext context) {
        return context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND
            || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
            || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
            || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
    }

    // ========== Positions Beam (world-space) ==========

    /** Offsets FPS pour l'origine du beam (relatifs a la camera). */
    public static final float BEAM_FPS_SIDE_OFFSET = 0.35f;
    public static final float BEAM_FPS_SIDE_BASE = 0.3f;
    public static final float BEAM_FPS_UP_OFFSET = -0.55f;
    public static final float BEAM_FPS_FORWARD_OFFSET = 1.9f;

    /** Offsets TPS pour l'origine du beam (relatifs au joueur). */
    public static final float BEAM_TPS_HEIGHT = 1.2f;
    public static final float BEAM_TPS_SIDE_OFFSET = 0.4f;
    public static final float BEAM_TPS_FORWARD_OFFSET = 1.2f;
}
