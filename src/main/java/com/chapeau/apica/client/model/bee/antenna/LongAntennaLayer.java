/**
 * ============================================================
 * [LongAntennaLayer.java]
 * Description: LayerDefinition des antennes LONG (longues, quad 0x2x5)
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
 * Antennes LONG: quads plats 0x2x5 (face interieure visible des 2 cotes).
 * Geometrie en position neutre (PartPose.ZERO), positionnement via attachment points.
 * Box a X=0, la face interieure est le plan YZ au pivot de chaque antenne.
 */
public final class LongAntennaLayer {

    private LongAntennaLayer() {}

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition partRoot = mesh.getRoot();
        PartDefinition bone = partRoot.addOrReplaceChild("bone",
                CubeListBuilder.create(), PartPose.offset(0.0F, 19.0F, 0.0F));

        bone.addOrReplaceChild("left_antenna",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(0.0F, -2.0F, -5.0F, 0.0F, 2.0F, 5.0F),
                PartPose.ZERO);
        bone.addOrReplaceChild("right_antenna",
                CubeListBuilder.create().texOffs(0, 7)
                        .addBox(0.0F, -2.0F, -5.0F, 0.0F, 2.0F, 5.0F),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 32, 32);
    }
}
