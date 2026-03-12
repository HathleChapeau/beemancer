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

    /** True si reload autorise (mouse DOWN ce tick, pas bloque). */
    public static boolean canReload() {
        if (FMLEnvironment.dist == Dist.DEDICATED_SERVER) return true;
        return com.chapeau.apica.client.input.MouseButtonTracker.canReload();
    }

    /** True si action bloquee (reload en cours). */
    public static boolean isBlocked() {
        if (FMLEnvironment.dist == Dist.DEDICATED_SERVER) return false;
        return com.chapeau.apica.client.input.MouseButtonTracker.isBlocked();
    }

    /** Bloquer jusqu'au relachement souris. */
    public static void block() {
        if (FMLEnvironment.dist != Dist.DEDICATED_SERVER) {
            com.chapeau.apica.client.input.MouseButtonTracker.block();
        }
    }
}
