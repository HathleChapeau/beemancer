/**
 * ============================================================
 * [RoyalBodyLayer.java]
 * Description: LayerDefinition du corps ROYAL (body + thorax imbrique + waist + tail)
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
 * Corps ROYAL: body 7x7x10 avec thorax 8x8x5 imbrique dedans + waist + tail.
 * Le thorax est DANS le body, 3px du bord avant. Il depasse sur les cotes (8x8 vs 7x7).
 * Double couche (corpus + stripe) sur tous les cubes sauf waist.
 * Texture 64x64.
 *
 * Layout Z (thorax centre a Z=0):
 *   Body(-5.5..+4.5) contient Thorax(-2.5..+2.5) → Waist(+4.5..+5.5) → Tail(+5.5..+11.5)
 */
public final class RoyalBodyLayer {

    private RoyalBodyLayer() {}

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition partRoot = mesh.getRoot();
        PartDefinition bone = partRoot.addOrReplaceChild("bone",
                CubeListBuilder.create(), PartPose.offset(0.0F, 19.0F, 0.0F));

        // body_corpus: body (englobant) + thorax (imbrique, depasse sur cotes) + waist + tail
        bone.addOrReplaceChild("body_corpus",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-3.5F, -4.0F, -5.5F, 7.0F, 7.0F, 10.0F)      // Body (7x7x10) Z:-5.5..+4.5
                        .texOffs(34, 0).addBox(-4.0F, -4.5F, -2.5F, 8.0F, 8.0F, 5.0F)       // Thorax (8x8x5) Z:-2.5..+2.5 (dans body, 3px du front)
                        .texOffs(0, 34).addBox(-3.0F, -3.5F, 4.5F, 6.0F, 6.0F, 1.0F)        // Waist (6x6x1) Z:+4.5..+5.5
                        .texOffs(34, 26).addBox(-3.5F, -4.0F, 5.5F, 7.0F, 7.0F, 6.0F),      // Tail (7x7x6) Z:+5.5..+11.5
                PartPose.ZERO);

        // body_stripe: body + thorax + tail (NO waist)
        bone.addOrReplaceChild("body_stripe",
                CubeListBuilder.create()
                        .texOffs(0, 17).addBox(-3.5F, -4.0F, -5.5F, 7.0F, 7.0F, 10.0F)
                        .texOffs(34, 13).addBox(-4.0F, -4.5F, -2.5F, 8.0F, 8.0F, 5.0F)
                        .texOffs(34, 39).addBox(-3.5F, -4.0F, 5.5F, 7.0F, 7.0F, 6.0F),
                PartPose.ZERO);

        // Eyes on body front face (Z=-5.5)
        bone.addOrReplaceChild("eyes",
                CubeListBuilder.create()
                        .texOffs(0, 47).addBox(-3.51F, -1.0F, -5.51F, 2.0F, 3.0F, 1.0F)
                        .texOffs(8, 47).addBox(1.51F, -1.0F, -5.51F, 2.0F, 3.0F, 1.0F),
                PartPose.ZERO);

        bone.addOrReplaceChild("left_pupil",
                CubeListBuilder.create().texOffs(0, 51)
                        .addBox(-2.51F, -1.0F, -5.52F, 1.0F, 1.0F, 0.0F),
                PartPose.ZERO);
        bone.addOrReplaceChild("right_pupil",
                CubeListBuilder.create().texOffs(4, 51)
                        .addBox(1.51F, -1.0F, -5.52F, 1.0F, 1.0F, 0.0F),
                PartPose.ZERO);

        // Legs under body (body Z: -5.5 to +4.5, bottom Y: 3)
        bone.addOrReplaceChild("front_legs",
                CubeListBuilder.create().texOffs(0, 41)
                        .addBox(-5.0F, 0.0F, 0.0F, 7.0F, 2.0F, 0.0F),
                PartPose.offset(1.5F, 3.0F, -3.0F));
        bone.addOrReplaceChild("middle_legs",
                CubeListBuilder.create().texOffs(0, 43)
                        .addBox(-5.0F, 0.0F, 0.0F, 7.0F, 2.0F, 0.0F),
                PartPose.offset(1.5F, 3.0F, -0.5F));
        bone.addOrReplaceChild("back_legs",
                CubeListBuilder.create().texOffs(0, 45)
                        .addBox(-5.0F, 0.0F, 0.0F, 7.0F, 2.0F, 0.0F),
                PartPose.offset(1.5F, 3.0F, 2.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }
}
