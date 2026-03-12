/**
 * ============================================================
 * [MagazineInputHelper.java]
 * Description: Bridge common/client pour mouse state (CLIENT ONLY)
 * ============================================================
 */
package com.chapeau.apica.common.item.magazine;

/**
 * ATTENTION: Cette méthode ne doit être appelée que côté CLIENT.
 * Côté serveur, utiliser IMagazineHolder.needsReload().
 */
public final class MagazineInputHelper {

    private MagazineInputHelper() {}

    /** True si mouse vient d'être appuyé (DOWN cette frame). CLIENT ONLY. */
    public static boolean isMouseDown() {
        return com.chapeau.apica.client.input.MouseButtonTracker.isMouseDown();
    }
}
