/**
 * ============================================================
 * [TimingEffect.java]
 * Description: Effets temporels (loop, boomerang) pour le systeme d'animation
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance                    | Raison                | Utilisation                    |
 * |-------------------------------|----------------------|--------------------------------|
 * | (aucune)                      | Enum standalone      | -                              |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - Animation.java (transformation du delta brut)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.animation;

import net.minecraft.util.Mth;

/**
 * Effets temporels qui transforment le delta brut de l'animation.
 * Applique avant le TimingType dans le pipeline de calcul du progress.
 */
public enum TimingEffect {
    /** Pas de boucle : clamp entre 0 et 1 */
    NONE,
    /** Boucle infinie : modulo 1 */
    LOOP,
    /** Ping-pong : aller-retour entre 0 et 1 */
    BOOMERANG;

    /**
     * Transforme le delta brut selon l'effet temporel.
     *
     * @param rawDelta delta brut (peut depasser 1.0 si l'animation a depasse sa duree)
     * @return delta transforme entre 0.0 et 1.0
     */
    public float apply(float rawDelta) {
        return switch (this) {
            case NONE -> Mth.clamp(rawDelta, 0f, 1f);
            case LOOP -> {
                float mod = rawDelta % 1f;
                yield mod < 0f ? mod + 1f : mod;
            }
            case BOOMERANG -> {
                float mod2 = rawDelta % 2f;
                if (mod2 < 0f) mod2 += 2f;
                yield mod2 > 1f ? 1f - (mod2 % 1f) : mod2;
            }
        };
    }
}
