/**
 * ============================================================
 * [GlassesAntennaLayer.java]
 * Description: LayerDefinition des antennes GLASSES (lunettes)
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
 * Lunettes: chaque cote (left/right) a 3 pieces miroir.
 * Frame 4x1x5 (branche), Side 2x2x1 (charniere), Nose 1x1x1 (pont).
 * Left s'etend en -X, Right en +X. Texture 32x32.
 */
public final class GlassesAntennaLayer {

    private GlassesAntennaLayer() {}

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition partRoot = mesh.getRoot();
        PartDefinition bone = partRoot.addOrReplaceChild("bone",
                CubeListBuilder.create(), PartPose.offset(0.0F, 18.0F, 0.0F));

        // Left lens: extends in -X
        bone.addOrReplaceChild("left_antenna",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-2.25F, 0.0F, 0.75F, 4.0F, 1.0F, 6.0F)     // Frame (branche)
                        .texOffs(0, 6).addBox(-1.75F, -0.5F, -0.5F, 2.0F, 2.0F, 1.0F)     // Side (charniere, 0.25 in)
                        .texOffs(0, 9).addBox(-1.25F, 0.0F, -0.75F, 1.0F, 1.0F, 1.0F),     // Nose (pont)
                PartPose.ZERO);

        // Right lens: extends in +X (mirrored)
        bone.addOrReplaceChild("right_antenna",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-1.75F, 0.0F, 0.75F, 4.0F, 1.0F, 6.0F)
                        .texOffs(0, 6).addBox(-0.25F, -0.5F, -0.5F, 2.0F, 2.0F, 1.0F)
                        .texOffs(0, 9).addBox(0.25F, 0.0F, -0.75F, 1.0F, 1.0F, 1.0F),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 32, 32);
    }
}
