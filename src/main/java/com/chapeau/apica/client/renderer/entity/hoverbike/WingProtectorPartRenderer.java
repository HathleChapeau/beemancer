/**
 * ============================================================
 * [WingProtectorPartRenderer.java]
 * Description: Rendu des protecteurs d'ailes HoverBee (synchronisation avec ailes)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | HoverbikePartModel  | Modele partie        | Acces aux children             |
 * | ModelPart           | Parties du modele    | Rotation sync                  |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - HoverbikePartLayer.java: Rendu des protecteurs d'ailes
 *
 * ============================================================
 */
package com.chapeau.apica.client.renderer.entity.hoverbike;

import com.chapeau.apica.client.model.hoverbike.HoverbikePartModel;
import net.minecraft.client.model.geom.ModelPart;

/**
 * Renderer specifique pour les protecteurs d'ailes du HoverBee.
 * Synchronise la rotation des protecteurs avec les ailes du modele parent.
 */
public final class WingProtectorPartRenderer {

    private WingProtectorPartRenderer() {}

    /**
     * Synchronise la rotation des protecteurs d'aile avec les ailes du modele parent.
     * Les ModelParts "protector_right" et "protector_left" suivent le yRot et zRot
     * de l'aile correspondante.
     *
     * @param part Modele du protecteur d'aile
     * @param rightWing Aile droite du modele parent
     * @param leftWing Aile gauche du modele parent
     */
    public static void syncWingRotation(HoverbikePartModel part, ModelPart rightWing, ModelPart leftWing) {
        try {
            ModelPart protRight = part.root().getChild("protector_right");
            ModelPart protLeft = part.root().getChild("protector_left");

            // Copie les rotations des ailes
            protRight.yRot = rightWing.yRot;
            protRight.zRot = rightWing.zRot;
            protLeft.yRot = leftWing.yRot;
            protLeft.zRot = leftWing.zRot;
        } catch (Exception ignored) {
            // Variantes sans ces enfants: pas de synchronisation
        }
    }
}
