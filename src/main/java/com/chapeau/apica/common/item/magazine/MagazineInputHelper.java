/**
 * ============================================================
 * [MagazineInputHelper.java]
 * Description: Bridge common/client pour mouse state (CLIENT ONLY)
 * ============================================================
 */
package com.chapeau.apica.common.item.magazine;

/**
 * ATTENTION: Ces méthodes ne doivent être appelées que côté CLIENT.
 * Côté serveur, utiliser IMagazineHolder.needsReload() à la place de canReload().
 */
public final class MagazineInputHelper {

    private MagazineInputHelper() {}

    /** True si mouse vient d'être appuyé (DOWN ce tick, pas held). CLIENT ONLY. */
    public static boolean isMouseDown() {
        return com.chapeau.apica.client.input.MouseButtonTracker.isMouseDown();
    }

    /** Marquer le click comme consommé. CLIENT ONLY. */
    public static void consume() {
        com.chapeau.apica.client.input.MouseButtonTracker.consume();
    }
}
