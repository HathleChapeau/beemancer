/**
 * ============================================================
 * [ArmoredBodyLayer.java]
 * Description: LayerDefinition du corps ARMORED (head + body + tail + 3 plaques + pattes 3D anglees)
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
 * Corps ARMORED: head 7x7x3, body 8x8x9, tail 7x2x3 (0.5 plus haut), 3 plaques 9x5x3.
 * Plaques depassent 0.5px (9 vs 8 body). 2 sur body (front/back), 1 sur tail (posee dessus).
 * Pattes 3D: 6 pattes individuelles (5x1x1), anglees 60° vers le bas (50° pour les milieu).
 * Texture 128x64.
 *
 * Layout Z:
 *   Head(-7.5..-4.5) → Body(-4.5..+4.5) → Tail(+4.5..+7.5)
 *   Plate1(-4.5..-1.5) Plate2(+1.5..+4.5) sur body, Plate3(+4.5..+7.5) sur tail
 */
public final class ArmoredBodyLayer {

    private static final float DEG60 = (float) Math.toRadians(60);
    private static final float DEG50 = (float) Math.toRadians(50);

    private ArmoredBodyLayer() {}

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition partRoot = mesh.getRoot();
        PartDefinition bone = partRoot.addOrReplaceChild("bone",
                CubeListBuilder.create(), PartPose.offset(0.0F, 19.0F, 0.0F));

        // body_corpus: head + body + tail + 3 plates
        bone.addOrReplaceChild("body_corpus",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.0F, -4.0F, -4.5F, 8.0F, 8.0F, 9.0F)       // Body (8x8x9)
                        .texOffs(34, 0).addBox(-3.5F, -3.5F, -7.5F, 7.0F, 7.0F, 3.0F)       // Head (7x7x3)
                        .texOffs(54, 0).addBox(-3.5F, 1.5F, 4.5F, 7.0F, 2.0F, 3.0F)         // Tail (7x2x3) +1px up
                        .texOffs(74, 0).addBox(-4.5F, -4.5F, -3.5F, 9.0F, 5.0F, 3.0F)       // Plate 1 front body (-1px Z)
                        .texOffs(74, 0).addBox(-4.5F, -4.5F, 0.5F, 9.0F, 5.0F, 3.0F)        // Plate 2 back body (+1px Z)
                        .texOffs(74, 0).addBox(-4.5F, -3.5F, 4.5F, 9.0F, 5.0F, 3.0F),       // Plate 3 tail +1px up
                PartPose.ZERO);

        // body_stripe: plaques seulement (le corps armored n'a pas de rayures)
        bone.addOrReplaceChild("body_stripe",
                CubeListBuilder.create()
                        .texOffs(74, 8).addBox(-4.5F, -4.5F, -3.5F, 9.0F, 5.0F, 3.0F)
                        .texOffs(74, 8).addBox(-4.5F, -4.5F, 0.5F, 9.0F, 5.0F, 3.0F)
                        .texOffs(74, 8).addBox(-4.5F, -3.5F, 4.5F, 9.0F, 5.0F, 3.0F),
                PartPose.ZERO);

        // Eyes on head front face (Z=-7.5)
        bone.addOrReplaceChild("eyes",
                CubeListBuilder.create()
                        .texOffs(0, 34).addBox(-3.51F, -0.5F, -7.51F, 2.0F, 3.0F, 1.0F)
                        .texOffs(8, 34).addBox(1.51F, -0.5F, -7.51F, 2.0F, 3.0F, 1.0F),
                PartPose.ZERO);

        bone.addOrReplaceChild("left_pupil",
                CubeListBuilder.create().texOffs(0, 38)
                        .addBox(-2.51F, -0.5F, -7.52F, 1.0F, 1.0F, 0.0F),
                PartPose.ZERO);
        bone.addOrReplaceChild("right_pupil",
                CubeListBuilder.create().texOffs(4, 38)
                        .addBox(1.51F, -0.5F, -7.52F, 1.0F, 1.0F, 0.0F),
                PartPose.ZERO);

        // Legs: 3D angled (5x1x1), angles inverted, lowered 1px
        // Front legs (60° down) — under plate 1, Z=-2
        PartDefinition frontLegs = bone.addOrReplaceChild("front_legs",
                CubeListBuilder.create(), PartPose.ZERO);
        frontLegs.addOrReplaceChild("right_front",
                CubeListBuilder.create().texOffs(0, 40)
                        .addBox(0.0F, -0.5F, -0.5F, 3.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(4.0F, 1.5F, -2.0F, 0, 0, DEG60));
        frontLegs.addOrReplaceChild("left_front",
                CubeListBuilder.create().texOffs(0, 40)
                        .addBox(-3.0F, -0.5F, -0.5F, 3.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(-4.0F, 1.5F, -2.0F, 0, 0, -DEG60));

        // Middle legs (50° down) — between front and back legs, Z=0
        PartDefinition middleLegs = bone.addOrReplaceChild("middle_legs",
                CubeListBuilder.create(), PartPose.ZERO);
        middleLegs.addOrReplaceChild("right_middle",
                CubeListBuilder.create().texOffs(0, 42)
                        .addBox(0.0F, -0.5F, -0.5F, 3.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(4.0F, 1.5F, 0.0F, 0, 0, DEG50));
        middleLegs.addOrReplaceChild("left_middle",
                CubeListBuilder.create().texOffs(0, 42)
                        .addBox(-3.0F, -0.5F, -0.5F, 3.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(-4.0F, 1.5F, 0.0F, 0, 0, -DEG50));

        // Back legs (60° down) — under plate 2, Z=2
        PartDefinition backLegs = bone.addOrReplaceChild("back_legs",
                CubeListBuilder.create(), PartPose.ZERO);
        backLegs.addOrReplaceChild("right_back",
                CubeListBuilder.create().texOffs(0, 44)
                        .addBox(0.0F, -0.5F, -0.5F, 3.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(4.0F, 1.5F, 2.0F, 0, 0, DEG60));
        backLegs.addOrReplaceChild("left_back",
                CubeListBuilder.create().texOffs(0, 44)
                        .addBox(-3.0F, -0.5F, -0.5F, 3.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(-4.0F, 1.5F, 2.0F, 0, 0, -DEG60));

        return LayerDefinition.create(mesh, 128, 64);
    }
}
