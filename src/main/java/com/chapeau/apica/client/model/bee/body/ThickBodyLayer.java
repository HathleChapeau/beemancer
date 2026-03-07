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
 * Thorax a l'avant (3px d'ecart devant le body), waist a l'arriere du body, tail derriere.
 * Double couche (corpus + stripe) sur tous les cubes sauf waist.
 * Texture 64x64.
 */
public final class ThickBodyLayer {

    private ThickBodyLayer() {}

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition partRoot = mesh.getRoot();
        PartDefinition bone = partRoot.addOrReplaceChild("bone",
                CubeListBuilder.create(), PartPose.offset(0.0F, 19.0F, 0.0F));

        // body_corpus: body + thorax + waist + tail
        // Thorax is 3px in front of body (body front Z=-5, gap 3px, thorax Z=-13 to -8)
        bone.addOrReplaceChild("body_corpus",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-3.5F, -4.0F, -5.0F, 7.0F, 7.0F, 10.0F)      // Body (7x7x10)
                        .texOffs(34, 0).addBox(-4.0F, -4.5F, -13.0F, 8.0F, 8.0F, 5.0F)      // Thorax (8x8x5)
                        .texOffs(0, 34).addBox(-3.0F, -3.5F, 5.0F, 6.0F, 6.0F, 1.0F)        // Waist (6x6x1)
                        .texOffs(34, 26).addBox(-3.5F, -4.0F, 6.0F, 7.0F, 7.0F, 6.0F),      // Tail (7x7x6)
                PartPose.ZERO);

        // body_stripe: body + thorax + tail (NO waist)
        bone.addOrReplaceChild("body_stripe",
                CubeListBuilder.create()
                        .texOffs(0, 17).addBox(-3.5F, -4.0F, -5.0F, 7.0F, 7.0F, 10.0F)
                        .texOffs(34, 13).addBox(-4.0F, -4.5F, -13.0F, 8.0F, 8.0F, 5.0F)
                        .texOffs(34, 39).addBox(-3.5F, -4.0F, 6.0F, 7.0F, 7.0F, 6.0F),
                PartPose.ZERO);

        // Eyes on body front face (Z=-5)
        bone.addOrReplaceChild("eyes",
                CubeListBuilder.create()
                        .texOffs(0, 47).addBox(-3.51F, -1.0F, -5.01F, 2.0F, 3.0F, 1.0F)
                        .texOffs(8, 47).addBox(1.51F, -1.0F, -5.01F, 2.0F, 3.0F, 1.0F),
                PartPose.ZERO);

        bone.addOrReplaceChild("left_pupil",
                CubeListBuilder.create().texOffs(0, 51)
                        .addBox(-2.51F, -1.0F, -5.02F, 1.0F, 1.0F, 0.0F),
                PartPose.ZERO);
        bone.addOrReplaceChild("right_pupil",
                CubeListBuilder.create().texOffs(4, 51)
                        .addBox(1.51F, -1.0F, -5.02F, 1.0F, 1.0F, 0.0F),
                PartPose.ZERO);

        // Legs under thorax (thorax Z: -13 to -8, bottom Y: 3.5)
        bone.addOrReplaceChild("front_legs",
                CubeListBuilder.create().texOffs(0, 41)
                        .addBox(-5.0F, 0.0F, 0.0F, 7.0F, 2.0F, 0.0F),
                PartPose.offset(1.5F, 3.5F, -12.0F));
        bone.addOrReplaceChild("middle_legs",
                CubeListBuilder.create().texOffs(0, 43)
                        .addBox(-5.0F, 0.0F, 0.0F, 7.0F, 2.0F, 0.0F),
                PartPose.offset(1.5F, 3.5F, -10.5F));
        bone.addOrReplaceChild("back_legs",
                CubeListBuilder.create().texOffs(0, 45)
                        .addBox(-5.0F, 0.0F, 0.0F, 7.0F, 2.0F, 0.0F),
                PartPose.offset(1.5F, 3.5F, -9.0F));

        // Antennae on thorax front (Z=-13), top (Y=-4.5)
        bone.addOrReplaceChild("left_antenna",
                CubeListBuilder.create().texOffs(0, 53)
                        .addBox(0.0F, -1.0F, -3.0F, 1.0F, 1.0F, 3.0F),
                PartPose.offset(-2.0F, -4.5F, -13.0F));
        bone.addOrReplaceChild("right_antenna",
                CubeListBuilder.create().texOffs(10, 53)
                        .addBox(-1.0F, -1.0F, -3.0F, 1.0F, 1.0F, 3.0F),
                PartPose.offset(2.0F, -4.5F, -13.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }
}
