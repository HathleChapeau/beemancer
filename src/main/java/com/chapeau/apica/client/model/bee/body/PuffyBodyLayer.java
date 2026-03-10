/**
 * ============================================================
 * [PuffyBodyLayer.java]
 * Description: LayerDefinition du corps PUFFY (corps ARMORED sans les plaques)
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
 * Corps PUFFY: copie exacte de ARMORED sans les 3 plaques.
 * head 7x7x3, body 8x8x9, tail 7x2x3. Pattes flat.
 * Texture 128x64 (meme layout qu'ARMORED).
 *
 * Layout Z:
 *   Head(-7.5..-4.5) -> Body(-4.5..+4.5) -> Tail(+4.5..+7.5)
 */
public final class PuffyBodyLayer {

    private PuffyBodyLayer() {}

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition partRoot = mesh.getRoot();
        PartDefinition bone = partRoot.addOrReplaceChild("bone",
                CubeListBuilder.create(), PartPose.offset(0.0F, 19.0F, 0.0F));

        // body_corpus: head + body + tail (sans plaques)
        bone.addOrReplaceChild("body_corpus",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.0F, -4.0F, -4.5F, 8.0F, 8.0F, 9.0F)       // Body (8x8x9)
                        .texOffs(34, 0).addBox(-3.5F, -3.5F, -7.5F, 7.0F, 7.0F, 3.0F)       // Head (7x7x3)
                        .texOffs(54, 0).addBox(-3.5F, 1.5F, 4.0F, 7.0F, 2.0F, 3.0F),        // Tail (7x2x3)
                PartPose.ZERO);

        // body_stripe: vide (ARMORED a les rayures sur les plaques uniquement)
        bone.addOrReplaceChild("body_stripe",
                CubeListBuilder.create(),
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

        // Legs: flat sprites like default body
        bone.addOrReplaceChild("front_legs",
                CubeListBuilder.create().texOffs(0, 40)
                        .addBox(-5.0F, 0.0F, 0.0F, 7.0F, 2.0F, 0.0F),
                PartPose.offset(1.5F, 4.0F, -2.0F));
        bone.addOrReplaceChild("middle_legs",
                CubeListBuilder.create().texOffs(0, 42)
                        .addBox(-5.0F, 0.0F, 0.0F, 7.0F, 2.0F, 0.0F),
                PartPose.offset(1.5F, 4.0F, 0.0F));
        bone.addOrReplaceChild("back_legs",
                CubeListBuilder.create().texOffs(0, 44)
                        .addBox(-5.0F, 0.0F, 0.0F, 7.0F, 2.0F, 0.0F),
                PartPose.offset(1.5F, 4.0F, 2.0F));

        return LayerDefinition.create(mesh, 128, 64);
    }
}
