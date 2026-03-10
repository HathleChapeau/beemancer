/**
 * ============================================================
 * [DefaultWingLayer.java]
 * Description: LayerDefinition des ailes DEFAULT (grandes, 18x12)
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
 * - ApicaBeeModel (createWingLayerFor)
 *
 * ============================================================
 */
package com.chapeau.apica.client.model.bee.wing;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public final class DefaultWingLayer {

    private DefaultWingLayer() {}

    /** Ailes DEFAULT: grandes (18x12), texture 32x32. */
    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition partRoot = mesh.getRoot();
        PartDefinition bone = partRoot.addOrReplaceChild("bone",
                CubeListBuilder.create(), PartPose.offset(0.0F, 19.0F, 0.0F));

        // Double-sided wings: tiny height renders both top and bottom faces
        bone.addOrReplaceChild("right_wing",
                CubeListBuilder.create().texOffs(2, 20)
                        .addBox(-18.0F, -0.005F, 0.0F, 18.0F, 0.01F, 12.0F),
                PartPose.offsetAndRotation(-1.5F, -4.0F, -3.0F, 0.0F, -0.2618F, 0.0F));

        bone.addOrReplaceChild("left_wing",
                CubeListBuilder.create().texOffs(-12, 7)
                        .addBox(0.0F, -0.005F, 0.0F, 18.0F, 0.01F, 12.0F),
                PartPose.offsetAndRotation(1.5F, -4.0F, -3.0F, 0.0F, 0.2618F, 0.0F));

        return LayerDefinition.create(mesh, 32, 32);
    }
}
