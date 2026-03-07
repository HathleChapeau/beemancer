/**
 * ============================================================
 * [SegmentedBodyLayer.java]
 * Description: LayerDefinition du corps SEGMENTED (4 segments 7x7 avec gaps)
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
 * Corps SEGMENTED: 4 segments de section 7x7, separes par des gaps de 2px.
 * Head (7x7x5) -> 2px -> Body (7x7x2, ailes) -> 2px -> Waist (7x7x2) -> 2px -> Tail (7x7x3).
 * Double couche (corpus + stripe) sur tous sauf waist.
 * Yeux et antennes sur la tete (comme vanilla). Pattes sous la tete. Dard derriere la tail.
 * Texture 64x64.
 */
public final class SegmentedBodyLayer {

    private SegmentedBodyLayer() {}

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition partRoot = mesh.getRoot();
        PartDefinition bone = partRoot.addOrReplaceChild("bone",
                CubeListBuilder.create(), PartPose.offset(0.0F, 19.0F, 0.0F));

        // body_corpus: head + body + waist + tail
        bone.addOrReplaceChild("body_corpus",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-3.5F, -4.0F, -9.0F, 7.0F, 7.0F, 5.0F)       // Head (7x7x5)
                        .texOffs(0, 12).addBox(-3.5F, -4.0F, -2.0F, 7.0F, 7.0F, 2.0F)       // Body (7x7x2)
                        .texOffs(36, 12).addBox(-3.5F, -4.0F, 2.0F, 7.0F, 7.0F, 2.0F)       // Waist (7x7x2)
                        .texOffs(0, 21).addBox(-3.5F, -4.0F, 6.0F, 7.0F, 7.0F, 3.0F),       // Tail (7x7x3)
                PartPose.ZERO);

        // body_stripe: head + body + waist + tail
        bone.addOrReplaceChild("body_stripe",
                CubeListBuilder.create()
                        .texOffs(24, 0).addBox(-3.5F, -4.0F, -9.0F, 7.0F, 7.0F, 5.0F)
                        .texOffs(18, 12).addBox(-3.5F, -4.0F, -2.0F, 7.0F, 7.0F, 2.0F)
                        .texOffs(36, 21).addBox(-3.5F, -4.0F, 2.0F, 7.0F, 7.0F, 2.0F)
                        .texOffs(20, 21).addBox(-3.5F, -4.0F, 6.0F, 7.0F, 7.0F, 3.0F),
                PartPose.ZERO);

        // Eyes on head front face (Z=-9)
        bone.addOrReplaceChild("eyes",
                CubeListBuilder.create()
                        .texOffs(0, 37).addBox(-3.51F, -1.0F, -9.01F, 2.0F, 3.0F, 1.0F)
                        .texOffs(8, 37).addBox(1.51F, -1.0F, -9.01F, 2.0F, 3.0F, 1.0F),
                PartPose.ZERO);

        bone.addOrReplaceChild("left_pupil",
                CubeListBuilder.create().texOffs(0, 41)
                        .addBox(-2.51F, -1.0F, -9.02F, 1.0F, 1.0F, 0.0F),
                PartPose.ZERO);
        bone.addOrReplaceChild("right_pupil",
                CubeListBuilder.create().texOffs(4, 41)
                        .addBox(1.51F, -1.0F, -9.02F, 1.0F, 1.0F, 0.0F),
                PartPose.ZERO);

        // Legs: front under head (1px from back), middle under body center, back under waist center
        bone.addOrReplaceChild("front_legs",
                CubeListBuilder.create().texOffs(0, 31)
                        .addBox(-5.0F, 0.0F, 0.0F, 7.0F, 2.0F, 0.0F),
                PartPose.offset(1.5F, 3.0F, -5.0F));
        bone.addOrReplaceChild("middle_legs",
                CubeListBuilder.create().texOffs(0, 33)
                        .addBox(-5.0F, 0.0F, 0.0F, 7.0F, 2.0F, 0.0F),
                PartPose.offset(1.5F, 3.0F, -1.0F));
        bone.addOrReplaceChild("back_legs",
                CubeListBuilder.create().texOffs(0, 35)
                        .addBox(-5.0F, 0.0F, 0.0F, 7.0F, 2.0F, 0.0F),
                PartPose.offset(1.5F, 3.0F, 3.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }
}
