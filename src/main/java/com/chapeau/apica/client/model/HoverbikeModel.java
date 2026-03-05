/**
 * ============================================================
 * [HoverbikeModel.java]
 * Description: Alias type du GiantBeeModel pour l'entite HoverbikeEntity
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | GiantBeeModel       | Modele abeille       | Geometrie + animations         |
 * | HoverbikeEntity     | Entite cible         | Typage generique               |
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
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;

/**
 * Specialisation du GiantBeeModel pour le Hoverbike.
 * Toute la geometrie et les animations sont definies dans GiantBeeModel.
 */
public class HoverbikeModel extends GiantBeeModel<HoverbikeEntity> {

    public HoverbikeModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        return GiantBeeModel.createBodyLayer();
    }
}
