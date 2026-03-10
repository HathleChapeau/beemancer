/**
 * ============================================================
 * [PuffyBodyLayer.java]
 * Description: LayerDefinition du corps PUFFY (head + body + tail + 3 bandes rayees + pattes flat)
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
 * Corps PUFFY: meme base que ARMORED (head 7x7x3, body 8x8x9, tail 7x2x3)
 * mais les 3 plaques sont remplacees par des bandes rayees.
 * Le body_stripe couvre le corps ET les bandes (contrairement a ARMORED qui
 * ne stripe que les plaques). Cela permet des rayures sur tout le corps.
 * Texture 128x64.
 *
 * Layout Z:
 *   Head(-7.5..-4.5) → Body(-4.5..+4.5) → Tail(+4.5..+7.5)
 *   Band1(-4.5..-1.5) Band2(+1.5..+4.5) sur body, Band3(+4.5..+7.5) sur tail
 */
public final class PuffyBodyLayer {

    private PuffyBodyLayer() {}

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition partRoot = mesh.getRoot();
        PartDefinition bone = partRoot.addOrReplaceChild("bone",
                CubeListBuilder.create(), PartPose.offset(0.0F, 19.0F, 0.0F));

        // body_corpus: head + body + tail + 3 bandes (meme geometrie que les plaques armored)
        bone.addOrReplaceChild("body_corpus",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.0F, -4.0F, -4.5F, 8.0F, 8.0F, 9.0F)       // Body (8x8x9)
                        .texOffs(34, 0).addBox(-3.5F, -3.5F, -7.5F, 7.0F, 7.0F, 3.0F)       // Head (7x7x3)
                        .texOffs(54, 0).addBox(-3.5F, 1.5F, 4.0F, 7.0F, 2.0F, 3.0F)         // Tail (7x2x3)
                        .texOffs(74, 0).addBox(-4.5F, -4.5F, -3.5F, 9.0F, 7.0F, 3.0F)       // Band 1 front
                        .texOffs(74, 0).addBox(-4.5F, -4.5F, 0.5F, 9.0F, 7.0F, 3.0F)        // Band 2 back
                        .texOffs(74, 0).addBox(-4.5F, -3.5F, 4.5F, 9.0F, 5.0F, 3.0F),       // Band 3 tail
                PartPose.ZERO);

        // body_stripe: body + 3 bandes (rayures sur corps et bandes, pas sur head/tail)
        bone.addOrReplaceChild("body_stripe",
                CubeListBuilder.create()
                        .texOffs(0, 17).addBox(-4.0F, -4.0F, -4.5F, 8.0F, 8.0F, 9.0F)      // Body stripe
                        .texOffs(74, 10).addBox(-4.5F, -4.5F, -3.5F, 9.0F, 7.0F, 3.0F)      // Band 1 stripe
                        .texOffs(74, 10).addBox(-4.5F, -4.5F, 0.5F, 9.0F, 7.0F, 3.0F)       // Band 2 stripe
                        .texOffs(74, 10).addBox(-4.5F, -3.5F, 4.5F, 9.0F, 5.0F, 3.0F),      // Band 3 stripe
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
