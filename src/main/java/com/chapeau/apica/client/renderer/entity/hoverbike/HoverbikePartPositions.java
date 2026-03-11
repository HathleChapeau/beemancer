/**
 * ============================================================
 * [HoverbikePartPositions.java]
 * Description: Positions des parties HoverBee selon le body type
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | BeeBodyType         | Type de corps        | Switch sur body type           |
 * | HoverbikePart       | Type de partie       | Switch sur part type           |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - HoverbikePartLayer.java: Positionnement des parties
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.entity.hoverbike;

import com.chapeau.apica.common.block.beecreator.BeeBodyType;
import com.chapeau.apica.common.entity.mount.HoverbikePart;
import net.minecraft.world.phys.Vec3;

/**
 * Fournit les positions en coordonnees modele (1 unite = 1/16 bloc) pour chaque
 * partie selon le body type. Les modeles de partie sont centres a l'origine.
 */
public final class HoverbikePartPositions {

    private HoverbikePartPositions() {}

    /**
     * Position absolue dans l'espace du modele pour une partie donnee.
     */
    public static Vec3 getPosition(HoverbikePart partType, BeeBodyType bodyType) {
        return switch (partType) {
            case SADDLE -> getSaddlePosition(bodyType);
            case WING_PROTECTOR -> getWingProtectorPosition(bodyType);
            case CONTROL_LEFT -> getControlLeftPosition(bodyType);
            case CONTROL_RIGHT -> getControlRightPosition(bodyType);
        };
    }

    /** Selle: sur le dessus du corps, zone arriere (ou le rider s'assoit). */
    private static Vec3 getSaddlePosition(BeeBodyType bodyType) {
        return switch (bodyType) {
            case DEFAULT -> new Vec3(0, 14.5, 2.0);
            case ROYAL -> new Vec3(0, 14.0, -2.0);
            case SEGMENTED -> new Vec3(0, 14.5, 5.0);
            case ARMORED, PUFFY -> new Vec3(0, 14.5, 1.0);
        };
    }

    /**
     * Protecteurs d'aile: position du centre entre les deux ailes.
     * Les offsets X de ±1.5 dans le modele servent de pivot pour la rotation.
     */
    private static Vec3 getWingProtectorPosition(BeeBodyType bodyType) {
        return switch (bodyType) {
            case DEFAULT -> new Vec3(0, 15.0, -3.0);
            case ROYAL -> new Vec3(0, 14.5, -3.0);
            case SEGMENTED -> new Vec3(0, 15.0, -1.0);
            case ARMORED, PUFFY -> new Vec3(0, 14.5, -2.0);
        };
    }

    /** Controle gauche: flanc gauche du corps, mi-hauteur, zone arriere. */
    private static Vec3 getControlLeftPosition(BeeBodyType bodyType) {
        return switch (bodyType) {
            case DEFAULT -> new Vec3(-4.0, 18.5, 2.5);
            case ROYAL -> new Vec3(-4.5, 18.5, -1.5);
            case SEGMENTED -> new Vec3(-4.0, 18.5, 7.5);
            case ARMORED, PUFFY -> new Vec3(-4.5, 18.5, 2.5);
        };
    }

    /** Controle droit: flanc droit du corps, mi-hauteur, zone arriere. */
    private static Vec3 getControlRightPosition(BeeBodyType bodyType) {
        return switch (bodyType) {
            case DEFAULT -> new Vec3(4.0, 18.5, 2.5);
            case ROYAL -> new Vec3(4.5, 18.5, -1.5);
            case SEGMENTED -> new Vec3(4.0, 18.5, 7.5);
            case ARMORED, PUFFY -> new Vec3(4.5, 18.5, 2.5);
        };
    }
}
