/**
 * ============================================================
 * [DefaultBodyLayer.java]
 * Description: LayerDefinition du corps DEFAULT (vanilla-like, 7x7x10)
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
 * - ApicaBeeModel (createBodyLayerFor)
 *
 * ============================================================
 */
package com.chapeau.apica.client.model.bee.body;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public final class DefaultBodyLayer {

    private DefaultBodyLayer() {}

    /** Corps DEFAULT: vanilla-like (7x7x10), texture 64x64. */
    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition partRoot = mesh.getRoot();
        PartDefinition bone = partRoot.addOrReplaceChild("bone",
                CubeListBuilder.create(), PartPose.offset(0.0F, 19.0F, 0.0F));

        bone.addOrReplaceChild("body_corpus",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-3.5F, -4.0F, -5.0F, 7.0F, 7.0F, 10.0F),
                PartPose.ZERO);
        bone.addOrReplaceChild("body_stripe",
                CubeListBuilder.create().texOffs(0, 17)
                        .addBox(-3.5F, -4.0F, -5.0F, 7.0F, 7.0F, 10.0F),
                PartPose.ZERO);
        bone.addOrReplaceChild("eyes",
                CubeListBuilder.create()
                        .texOffs(0, 42).addBox(-3.51F, -1.0F, -5.01F, 2.0F, 3.0F, 1.0F)
                        .texOffs(8, 42).addBox(1.51F, -1.0F, -5.01F, 2.0F, 3.0F, 1.0F),
                PartPose.ZERO);
        bone.addOrReplaceChild("left_pupil",
                CubeListBuilder.create().texOffs(0, 46)
                        .addBox(-2.51F, -1.0F, -5.02F, 1.0F, 1.0F, 0.0F),
                PartPose.ZERO);
        bone.addOrReplaceChild("right_pupil",
                CubeListBuilder.create().texOffs(4, 46)
                        .addBox(1.51F, -1.0F, -5.02F, 1.0F, 1.0F, 0.0F),
                PartPose.ZERO);
        bone.addOrReplaceChild("front_legs",
                CubeListBuilder.create().texOffs(0, 35)
                        .addBox(-5.0F, 0.0F, 0.0F, 7.0F, 2.0F, 0.0F),
                PartPose.offset(1.5F, 3.0F, -2.0F));
        bone.addOrReplaceChild("middle_legs",
                CubeListBuilder.create().texOffs(0, 37)
                        .addBox(-5.0F, 0.0F, 0.0F, 7.0F, 2.0F, 0.0F),
                PartPose.offset(1.5F, 3.0F, 0.0F));
        bone.addOrReplaceChild("back_legs",
                CubeListBuilder.create().texOffs(0, 39)
                        .addBox(-5.0F, 0.0F, 0.0F, 7.0F, 2.0F, 0.0F),
                PartPose.offset(1.5F, 3.0F, 2.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }
}
