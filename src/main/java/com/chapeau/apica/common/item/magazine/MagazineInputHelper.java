/**
 * ============================================================
 * [MagazineInputHelper.java]
 * Description: Bridge entre common et client pour detection mouse down
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | MouseButtonTracker  | Detection souris     | Appel via DistExecutor         |
 * | DistExecutor        | Code client safe     | Evite ClassNotFound            |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - IMagazineHolder.java (verification click down)
 *
 * ============================================================
 */
package com.chapeau.apica.common.item.magazine;

import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.api.distmarker.Dist;

/**
 * Helper pour verifier l'etat de la souris depuis le code common.
 * Sur server, retourne toujours true (server ne gere pas les inputs).
 * Sur client, verifie si le bouton droit vient d'etre enfonce.
 */
public final class MagazineInputHelper {

    private MagazineInputHelper() {}

    /**
     * Verifie si l'action de reload doit etre autorisee.
     * Server: toujours true (le server fait confiance au client pour envoyer le packet).
     * Client: true seulement si le bouton droit vient d'etre enfonce (pas maintenu).
     */
    public static boolean shouldAllowReload() {
        if (FMLEnvironment.dist == Dist.DEDICATED_SERVER) {
            return true;
        }
        // Client side - check mouse state
        return checkClientMouseState();
    }

    /**
     * Verifie l'etat de la souris cote client.
     * Isole dans une methode separee pour eviter le chargement de classes client sur server.
     */
    private static boolean checkClientMouseState() {
        try {
            return com.chapeau.apica.client.input.MouseButtonTracker.isRightMouseJustPressed();
        } catch (NoClassDefFoundError e) {
            // Fallback si on est sur un server dedie sans classes client
            return true;
        }
    }
}
