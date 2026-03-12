/**
 * ============================================================
 * [MagazineInputHelper.java]
 * Description: Bridge common/client pour mouse state
 * ============================================================
 */
package com.chapeau.apica.common.item.magazine;

import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.api.distmarker.Dist;

public final class MagazineInputHelper {

    private MagazineInputHelper() {}

    /** True si mouse vient d'être appuyé (DOWN ce tick). Server: toujours true. */
    public static boolean isMouseDown() {
        if (FMLEnvironment.dist == Dist.DEDICATED_SERVER) return true;
        return com.chapeau.apica.client.input.MouseButtonTracker.isMouseDown();
    }

    /** Marquer le click comme consommé (bloque les prochains isMouseDown jusqu'au relâchement). */
    public static void consume() {
        if (FMLEnvironment.dist != Dist.DEDICATED_SERVER) {
            com.chapeau.apica.client.input.MouseButtonTracker.consume();
        }
    }
}
