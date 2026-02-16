/**
 * ============================================================
 * [AnimationTimer.java]
 * Description: Compteur de temps client-side pour animations sans stutter
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | ClientTickEvent     | Increment tick       | tick() appele chaque frame     |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - Tous les renderers avec animations (StorageControllerRenderer, etc.)
 * - ClientSetup.java (enregistrement event)
 *
 * ============================================================
 */
package com.chapeau.apica.client.animation;

/**
 * Fournit un temps de rendu fluide pour les animations client-side.
 * Maintient son propre compteur de ticks au lieu de lire level.getGameTime(),
 * ce qui evite le stutter cause par les inconsistances entre gameTime et partialTick.
 *
 * Pattern inspire de Create (AnimationTickHolder / Catnip).
 */
public final class AnimationTimer {

    private static int ticks = 0;

    /**
     * Appele une fois par ClientTickEvent (post).
     */
    public static void tick() {
        ticks++;
    }

    /**
     * Reset au chargement/dechargement d'un monde.
     */
    public static void reset() {
        ticks = 0;
    }

    /**
     * Retourne le temps de rendu fluide (ticks + partialTick).
     * Utiliser partout au lieu de level.getGameTime() + partialTick.
     */
    public static float getRenderTime(float partialTick) {
        return ticks + partialTick;
    }

    public static int getTicks() {
        return ticks;
    }

    private AnimationTimer() {
    }
}
