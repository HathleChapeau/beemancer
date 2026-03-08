/**
 * ============================================================
 * [ButterflyAntennaLayer.java]
 * Description: LayerDefinition des antennes BUTTERFLY (tige + quad 6x6 au bout)
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
 * Antennes BUTTERFLY: tige default (0x2x3) + quad plat 6x6 au sommet.
 * Le quad est centre sur le haut de la tige, oriente face avant (plan XY, depth=0).
 * Texture 32x32.
 */
public final class ButterflyAntennaLayer {

    private ButterflyAntennaLayer() {}

    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition partRoot = mesh.getRoot();
        PartDefinition bone = partRoot.addOrReplaceChild("bone",
                CubeListBuilder.create(), PartPose.offset(0.0F, 19.0F, 0.0F));

        // Left antenna: tige + quad 6x6
        bone.addOrReplaceChild("left_antenna",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(0.0F, -2.0F, -3.0F, 0.0F, 2.0F, 3.0F)       // Tige (comme default)
                        .texOffs(0, 14).addBox(-3.0F, -8.0F, -1.5F, 6.0F, 6.0F, 0.0F),     // Quad 6x6
                PartPose.ZERO);

        // Right antenna: tige + quad 6x6
        bone.addOrReplaceChild("right_antenna",
                CubeListBuilder.create()
                        .texOffs(0, 5).addBox(0.0F, -2.0F, -3.0F, 0.0F, 2.0F, 3.0F)        // Tige (comme default)
                        .texOffs(0, 20).addBox(-3.0F, -8.0F, -1.5F, 6.0F, 6.0F, 0.0F),     // Quad 6x6
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 32, 32);
    }
}
