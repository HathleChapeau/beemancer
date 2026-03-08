/**
 * ============================================================
 * [ButterflyAntennaLayer.java]
 * Description: LayerDefinition des antennes BUTTERFLY (quad plat 0x6x6)
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
 * Antennes BUTTERFLY: quads plats 0x6x6 (meme structure que default mais plus grand).
 * Geometrie en position neutre (PartPose.ZERO), positionnement via attachment points.
 */
public final class ButterflyAntennaLayer {

    private ButterflyAntennaLayer() {}

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition partRoot = mesh.getRoot();
        PartDefinition bone = partRoot.addOrReplaceChild("bone",
                CubeListBuilder.create(), PartPose.offset(0.0F, 19.0F, 0.0F));

        bone.addOrReplaceChild("left_antenna",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(0.0F, -6.0F, -6.0F, 0.0F, 6.0F, 6.0F),
                PartPose.ZERO);
        bone.addOrReplaceChild("right_antenna",
                CubeListBuilder.create().texOffs(0, 12)
                        .addBox(0.0F, -6.0F, -6.0F, 0.0F, 6.0F, 6.0F),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 32, 32);
    }
}
