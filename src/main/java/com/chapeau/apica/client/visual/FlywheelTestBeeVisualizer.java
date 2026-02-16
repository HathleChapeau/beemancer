/**
 * ============================================================
 * [FlywheelTestBeeVisualizer.java]
 * Description: Enregistrement du visual Flywheel pour FlywheelTestBee
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance              | Raison                  | Utilisation                    |
 * |-------------------------|------------------------|--------------------------------|
 * | SimpleEntityVisualizer  | Registration Flywheel  | Lier entite a son visual       |
 * | ApicaEntities       | Type entite            | FLYWHEEL_TEST_BEE              |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (appel register)
 *
 * ============================================================
 */
package com.chapeau.apica.client.visual;

import com.chapeau.apica.core.registry.ApicaEntities;
import dev.engine_room.flywheel.lib.visualization.SimpleEntityVisualizer;

public class FlywheelTestBeeVisualizer {

    public static void register() {
        SimpleEntityVisualizer.builder(ApicaEntities.FLYWHEEL_TEST_BEE.get())
            .factory(FlywheelTestBeeVisual::new)
            .skipVanillaRender(entity -> true)
            .apply();
    }
}
