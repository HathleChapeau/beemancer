/**
 * ============================================================
 * [HoverbikeModel.java]
 * Description: Specialisation du GiantBeeModel pour le Hoverbike avec animations contextuelles
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | GiantBeeModel       | Modele abeille       | Geometrie + helpers animation  |
 * | HoverbikeEntity     | Entite cible         | Etat mouvement/mode/saut       |
 * | HoverbikeMode       | Enum mode            | HOVER vs RUN                   |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - HoverbikeRenderer.java: Rendu de l'entite
 * - HoverbikePartLayer.java: Type generique du layer
 * - ClientSetup.java: Enregistrement du layer
 *
 * ============================================================
 */
package com.chapeau.apica.client.model;

import com.chapeau.apica.common.entity.mount.HoverbikeEntity;
import com.chapeau.apica.common.entity.mount.HoverbikeMode;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;

/**
 * Modele du Hoverbike-abeille avec battement d'ailes variable :
 * - Arret : pas de battement
 * - Marche (hover + mouvement) : battement lent et leger
 * - Run : battement moyen
 * - Saut/vol (espace) : battement normal
 *
 * Bias vers le haut pour que les ailes restent legerement relevees au repos.
 */
public class HoverbikeModel extends GiantBeeModel<HoverbikeEntity> {

    /** Bias vers le haut en radians (~3 degres) */
    private static final float UP_BIAS = 0.05F;

    public HoverbikeModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        return GiantBeeModel.createBodyLayer();
    }

    @Override
    public void setupAnim(HoverbikeEntity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {

        float speed;
        float amplitude;
        float legTuck;

        // Detecter le mouvement via la velocite reelle (limbSwingAmount ne fonctionne pas pour un vehicule)
        double horizontalSpeed = entity.getDeltaMovement().horizontalDistance();
        boolean isMoving = horizontalSpeed > 0.01;

        if (entity.isJumpPressed()) {
            // Vol actif (espace appuye) : battement normal
            speed = 1.8F;
            amplitude = 0.12F;
            legTuck = 0.7854F;
        } else if (entity.getSynchedMode() == HoverbikeMode.RUN) {
            // Mode run : battement moyen
            speed = 1.0F;
            amplitude = 0.08F;
            legTuck = 0.5F;
        } else if (isMoving) {
            // Hover en mouvement : battement lent et leger
            speed = 0.6F;
            amplitude = 0.05F;
            legTuck = 0.3F;
        } else {
            // A l'arret : pas de battement, ailes relevees
            speed = 0.0F;
            amplitude = 0.0F;
            legTuck = 0.15F;
        }

        animateWings(ageInTicks, speed, amplitude, UP_BIAS);
        animateLegs(legTuck);
    }
}
