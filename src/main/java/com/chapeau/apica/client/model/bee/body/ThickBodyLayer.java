/**
 * ============================================================
 * [ThickBodyLayer.java]
 * Description: LayerDefinition du corps THICK (body + thorax + waist + tail)
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

/**
 * Corps THICK: segmente (body 7x7x10 + thorax 8x8x5 + waist 6x6x1 + tail 7x7x6).
 * Body a l'avant (yeux/face), thorax au centre, waist, tail derriere.
 * Double couche (corpus + stripe) sur tous les cubes sauf waist.
 * Texture 64x64.
 *
 * Layout Z (thorax centre a Z=0):
 *   Body(-15.5..-5.5) → gap(3px) → Thorax(-2.5..+2.5) → Waist(+2.5..+3.5) → Tail(+3.5..+9.5)
 */
public final class ThickBodyLayer {

    private ThickBodyLayer() {}

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition partRoot = mesh.getRoot();
        PartDefinition bone = partRoot.addOrReplaceChild("bone",
                CubeListBuilder.create(), PartPose.offset(0.0F, 19.0F, 0.0F));

        // body_corpus: body (front) + thorax (center) + waist + tail
        bone.addOrReplaceChild("body_corpus",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-3.5F, -4.0F, -15.5F, 7.0F, 7.0F, 10.0F)     // Body (7x7x10) Z:-15.5..-5.5
                        .texOffs(34, 0).addBox(-4.0F, -4.5F, -2.5F, 8.0F, 8.0F, 5.0F)       // Thorax (8x8x5) Z:-2.5..+2.5
                        .texOffs(0, 34).addBox(-3.0F, -3.5F, 2.5F, 6.0F, 6.0F, 1.0F)        // Waist (6x6x1) Z:+2.5..+3.5
                        .texOffs(34, 26).addBox(-3.5F, -4.0F, 3.5F, 7.0F, 7.0F, 6.0F),      // Tail (7x7x6) Z:+3.5..+9.5
                PartPose.ZERO);

        // body_stripe: body + thorax + tail (NO waist)
        bone.addOrReplaceChild("body_stripe",
                CubeListBuilder.create()
                        .texOffs(0, 17).addBox(-3.5F, -4.0F, -15.5F, 7.0F, 7.0F, 10.0F)
                        .texOffs(34, 13).addBox(-4.0F, -4.5F, -2.5F, 8.0F, 8.0F, 5.0F)
                        .texOffs(34, 39).addBox(-3.5F, -4.0F, 3.5F, 7.0F, 7.0F, 6.0F),
                PartPose.ZERO);

        // Eyes on body front face (Z=-15.5)
        bone.addOrReplaceChild("eyes",
                CubeListBuilder.create()
                        .texOffs(0, 47).addBox(-3.51F, -1.0F, -15.51F, 2.0F, 3.0F, 1.0F)
                        .texOffs(8, 47).addBox(1.51F, -1.0F, -15.51F, 2.0F, 3.0F, 1.0F),
                PartPose.ZERO);

        bone.addOrReplaceChild("left_pupil",
                CubeListBuilder.create().texOffs(0, 51)
                        .addBox(-2.51F, -1.0F, -15.52F, 1.0F, 1.0F, 0.0F),
                PartPose.ZERO);
        bone.addOrReplaceChild("right_pupil",
                CubeListBuilder.create().texOffs(4, 51)
                        .addBox(1.51F, -1.0F, -15.52F, 1.0F, 1.0F, 0.0F),
                PartPose.ZERO);

        // Legs under body (body Z: -15.5 to -5.5, bottom Y: 3)
        bone.addOrReplaceChild("front_legs",
                CubeListBuilder.create().texOffs(0, 41)
                        .addBox(-5.0F, 0.0F, 0.0F, 7.0F, 2.0F, 0.0F),
                PartPose.offset(1.5F, 3.0F, -13.0F));
        bone.addOrReplaceChild("middle_legs",
                CubeListBuilder.create().texOffs(0, 43)
                        .addBox(-5.0F, 0.0F, 0.0F, 7.0F, 2.0F, 0.0F),
                PartPose.offset(1.5F, 3.0F, -10.5F));
        bone.addOrReplaceChild("back_legs",
                CubeListBuilder.create().texOffs(0, 45)
                        .addBox(-5.0F, 0.0F, 0.0F, 7.0F, 2.0F, 0.0F),
                PartPose.offset(1.5F, 3.0F, -8.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }
}
