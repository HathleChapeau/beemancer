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
 * | BeemancerEntities       | Type entite            | FLYWHEEL_TEST_BEE              |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ClientSetup.java (appel register)
 *
 * ============================================================
 */
package com.chapeau.beemancer.client.visual;

import com.chapeau.beemancer.core.registry.BeemancerEntities;
import dev.engine_room.flywheel.lib.visualization.SimpleEntityVisualizer;

public class FlywheelTestBeeVisualizer {

    public static void register() {
        SimpleEntityVisualizer.builder(BeemancerEntities.FLYWHEEL_TEST_BEE.get())
            .factory(FlywheelTestBeeVisual::new)
            .skipVanillaRender(entity -> true)
            .apply();
    }
}
