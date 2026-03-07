/**
 * ============================================================
 * [DemonAntennaLayer.java]
 * Description: LayerDefinition des antennes DEMON (cornes qui s'ecartent puis remontent)
 * ============================================================
 *
 * DEPENDANCES:
 * ------------------------------------------------------------
 * | Dependance          | Raison                | Utilisation                    |
 * |---------------------|----------------------|--------------------------------|
 * | (aucune)            |                      |                                |
 * ------------------------------------------------------------
 *
 * UTILISE PAR:
 * - ApicaBeeModel (createAntennaLayerFor)
 *
 * ============================================================
 */
package com.chapeau.apica.client.model.bee.antenna;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Cornes de demon: base 1x2x1 montant droit, coude 1x1x1 vers l'exterieur,
 * pointe 1x2x1 remontant depuis le bord externe.
 * Texture 32x32.
 */
public final class DemonAntennaLayer {

    private DemonAntennaLayer() {}

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition partRoot = mesh.getRoot();
        PartDefinition bone = partRoot.addOrReplaceChild("bone",
                CubeListBuilder.create(), PartPose.offset(0.0F, 19.0F, 0.0F));

        // Left horn: up, outward (-X), then up again
        bone.addOrReplaceChild("left_antenna",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-1.0F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F)     // Base (1x2x1) straight up
                        .texOffs(0, 3).addBox(-2.0F, -3.0F, -0.5F, 1.0F, 1.0F, 1.0F)      // Bend (1x1x1) outward
                        .texOffs(0, 5).addBox(-2.0F, -5.0F, -0.5F, 1.0F, 2.0F, 1.0F),     // Tip (1x2x1) up from outer
                PartPose.ZERO);

        // Right horn: mirrored (outward = +X)
        bone.addOrReplaceChild("right_antenna",
                CubeListBuilder.create()
                        .texOffs(8, 0).addBox(0.0F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F)
                        .texOffs(8, 3).addBox(1.0F, -3.0F, -0.5F, 1.0F, 1.0F, 1.0F)
                        .texOffs(8, 5).addBox(1.0F, -5.0F, -0.5F, 1.0F, 2.0F, 1.0F),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 32, 32);
    }
}
