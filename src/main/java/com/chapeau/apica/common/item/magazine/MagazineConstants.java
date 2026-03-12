/**
 * ============================================================
 * [MagazineConstants.java]
 * Description: Constantes partagees pour le systeme de magazines
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | (aucune)            | -                    | Constantes pures               |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - IMagazineHolder.java (getAcceptedFluids default)
 * - MagazineSweepShader.java (couleurs shader)
 * - LeafBlowerItem.java, MiningLaserItem.java, etc. (couleurs barre)
 * - MagazineReloadHelper.java (fluid IDs)
 *
 * ============================================================
 */
package com.chapeau.apica.common.item.magazine;

import org.joml.Vector3f;
import java.util.Set;

/**
 * Constantes centralisees pour le systeme de magazines.
 * Evite la duplication des IDs fluides et couleurs.
 */
public final class MagazineConstants {

    private MagazineConstants() {}

    // =========================================================================
    // Fluid IDs
    // =========================================================================

    public static final String HONEY_ID = "apica:honey";
    public static final String ROYAL_JELLY_ID = "apica:royal_jelly";
    public static final String NECTAR_ID = "apica:nectar";

    /** Set standard des 3 fluides acceptes par la plupart des items. */
    public static final Set<String> STANDARD_FLUIDS = Set.of(HONEY_ID, ROYAL_JELLY_ID, NECTAR_ID);

    // =========================================================================
    // Couleurs Item Bar (format 0xRRGGBB pour Item.getBarColor)
    // =========================================================================

    public static final int HONEY_BAR_COLOR = 0xE8A317;
    public static final int ROYAL_JELLY_BAR_COLOR = 0xFFF8DC;
    public static final int NECTAR_BAR_COLOR = 0xB050FF;
    public static final int DEFAULT_BAR_COLOR = 0x888888;

    /**
     * Retourne la couleur de barre pour un ID fluide donne.
     * Utilise par getBarColor() dans tous les IMagazineHolder items.
     */
    public static int getBarColorForFluid(String fluidId) {
        if (fluidId == null || fluidId.isEmpty()) return DEFAULT_BAR_COLOR;
        if (fluidId.contains("honey")) return HONEY_BAR_COLOR;
        if (fluidId.contains("royal_jelly")) return ROYAL_JELLY_BAR_COLOR;
        if (fluidId.contains("nectar")) return NECTAR_BAR_COLOR;
        return DEFAULT_BAR_COLOR;
    }

    // =========================================================================
    // Couleurs Shader (format Vector3f RGB 0-1 pour GLSL)
    // =========================================================================

    public static final Vector3f HONEY_SHADER_COLOR = new Vector3f(0.91f, 0.64f, 0.09f);
    public static final Vector3f ROYAL_JELLY_SHADER_COLOR = new Vector3f(1.0f, 0.97f, 0.86f);
    public static final Vector3f NECTAR_SHADER_COLOR = new Vector3f(0.69f, 0.31f, 1.0f);
    public static final Vector3f EMPTY_SHADER_COLOR = new Vector3f(0.53f, 0.53f, 0.53f);

    /**
     * Retourne la couleur shader pour un ID fluide donne.
     * Utilise par MagazineSweepShader.
     */
    public static Vector3f getShaderColorForFluid(String fluidId) {
        if (fluidId == null || fluidId.isEmpty()) return EMPTY_SHADER_COLOR;
        return switch (fluidId) {
            case HONEY_ID -> HONEY_SHADER_COLOR;
            case ROYAL_JELLY_ID -> ROYAL_JELLY_SHADER_COLOR;
            case NECTAR_ID -> NECTAR_SHADER_COLOR;
            default -> EMPTY_SHADER_COLOR;
        };
    }
}
